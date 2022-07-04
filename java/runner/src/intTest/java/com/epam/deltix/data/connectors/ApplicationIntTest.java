package com.epam.deltix.data.connectors;

import com.epam.deltix.containers.interfaces.LogProcessor;
import com.epam.deltix.containers.interfaces.Severity;
import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.commons.json.JsonValue;
import com.epam.deltix.data.connectors.commons.json.JsonValueParser;
import com.epam.deltix.data.connectors.test.fixtures.integration.TbIntTestPreparation;
import com.epam.deltix.data.connectors.validator.DataValidator;
import com.epam.deltix.data.connectors.validator.DataValidatorImpl;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.universal.PackageHeaderInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class ApplicationIntTest extends TbIntTestPreparation {

    public static final GenericContainer APP_CONTAINER = new GenericContainer(
            new ImageFromDockerfile("localhost/timebase-crypto-connectors:snapshot").
                    withFileFromPath("Dockerfile", Path.of("src/main/docker/Dockerfile")).
                    withFileFromPath(".", Path.of("build/docker"))).
            withReuse(true).
            withNetwork(NETWORK).
            withExposedPorts(8055).
            withEnv("JAVA_OPTS", "-Dtimebase.url=dxtick://timebase:8011").
            withStartupTimeout(Duration.ofMinutes(5));

    private static final int SMOKE_READ_TIMEOUT_S = 30;
    private static final int SMOKE_READ_MESSAGES = 10;

    private static final int VALIDATION_READ_TIMEOUT_S = 500;
    private static final int VALIDATION_READ_MESSAGES = 2000;

    @BeforeAll
    static void beforeAll() throws Exception {
        TIMEBASE_CONTAINER.start();
        APP_CONTAINER.start();

        Thread.sleep(10_000);
    }

    @AfterAll
    static void afterAll() {
        APP_CONTAINER.stop();
        TIMEBASE_CONTAINER.stop();
    }

    DXTickDB db;

    @BeforeEach
    void setupTb() {
        db = TickDBFactory.createFromUrl("dxtick://localhost:" + TIMEBASE_CONTAINER.getFirstMappedPort());
        db.open(true);
    }

    @AfterEach
    void closeTb() {
        db.close();
    }

//    @TestFactory
//    Stream<DynamicTest> testDataFeedInTbSmoke() throws Exception {
//        return Arrays.stream(listStreams()).
//                map(c -> DynamicTest.dynamicTest(
//                        c.connector,
//                        () -> tryReadSomeData(c)
//                ));
//    }

    @TestFactory
    Stream<DynamicTest> testDataFeedInTbValidateOrderBook() throws Exception {
        return Arrays.stream(listStreams()).
            filter(s -> ("huobi-futures".equalsIgnoreCase(s.stream) || "ascendex".equalsIgnoreCase(s.stream))).
            map(c -> DynamicTest.dynamicTest(
                c.connector,
                () -> tryBuildOrderBook(c)
            ));
    }

    private static ConnectorStream[] listStreams() throws Exception {
        final JsonArray connectors =
            rest("http://localhost:" + APP_CONTAINER.getFirstMappedPort() + "/api/v0/connectors").
                asArrayRequired();

        return connectors.items().
            map(JsonValue::asObjectRequired).
            map(ConnectorStream::new).
            toArray(ConnectorStream[]::new);
    }

    void tryReadSomeData(final ConnectorStream connector) {
        // we are trying to read N messages
        final int expectedNumOfMessages = SMOKE_READ_MESSAGES;
        final int timeoutSeconds = SMOKE_READ_TIMEOUT_S;

        final DXTickStream stream = db.getStream(connector.stream);

        Assertions.assertNotNull(stream, "Connector " + connector + " not started as expected. " +
                "No output stream found.");

        try (TickCursor cursor = stream.select(TimeConstants.TIMESTAMP_UNKNOWN,
                new SelectionOptions(true, true))) {
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(timeoutSeconds), () -> {
                int messages = 0;
                while (cursor.next()) {
                    if (++messages == expectedNumOfMessages) {
                        break;
                    }
                }
            }, "Cannot read data for the " + connector);
        }
    }

    void tryBuildOrderBook(final ConnectorStream connector) {
        final int expectedNumOfMessages = VALIDATION_READ_MESSAGES;
        final int timeoutSeconds = VALIDATION_READ_TIMEOUT_S;

        final DXTickStream stream = db.getStream(connector.stream);

        Assertions.assertNotNull(stream, "Connector " + connector + " not started as expected. " +
            "No output stream found.");

        try (TickCursor cursor = stream.select(TimeConstants.TIMESTAMP_UNKNOWN, new SelectionOptions(false, true))) {
            Map<String, DataValidator> validators = new HashMap<>();
            AtomicLong errors = new AtomicLong();
            LogProcessor logProcessor = new LogProcessor() {
                @Override
                public void onLogEvent(Object sender, Severity severity, Throwable exception, CharSequence stringMessage) {
                    if (severity == Severity.ERROR) {
                        errors.addAndGet(1);
                    }
                    System.out.println(stringMessage);
                }
            };
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(timeoutSeconds), () -> {
                int messages = 0;
                while (cursor.next()) {
                    InstrumentMessage message = cursor.getMessage();
                    if (message.getSymbol() == null) {
                        continue;
                    }

                    // validate
                    if (message instanceof PackageHeaderInfo) {
                        PackageHeaderInfo packageHeader = (PackageHeaderInfo) message;
                        DataValidator validator = validators.computeIfAbsent(
                            message.getSymbol().toString(), k -> createValidator(k, logProcessor)
                        );

                        validator.sendPackage(packageHeader);

                        if (++messages == expectedNumOfMessages) {
                            System.out.println("Processed " + messages + " package headers");
                            break;
                        }
                    }
                }
            }, "Cannot read data for the " + connector);

            Assertions.assertEquals(0L, errors.get());
        }
    }

    private static DataValidator createValidator(String symbol, LogProcessor log) {
        DataValidatorImpl validator = new DataValidatorImpl(symbol, log,
            Decimal64Utils.parse("0.0000000000000000001"), Decimal64Utils.parse("0.0000000000000000001"),
            true
        );
        validator.setCheckEmptySide(true);
        validator.setCheckBidMoreThanAsk(true);

        return validator;
    }

    private static JsonValue rest(final String url) throws Exception {
        final HttpClient client = HttpClient.newBuilder().
                connectTimeout(Duration.ofMillis(5000)).
                followRedirects(HttpClient.Redirect.NORMAL).
                build();

        final HttpRequest request = HttpRequest.newBuilder().
                uri(new URI(url)).
                GET().
                timeout(Duration.ofSeconds(3)).
                build();

        final HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return new JsonValueParser().parseAndEoj(response.body());
    }

    private static class ConnectorStream {
        private final String connector;
        private final String stream;

        private ConnectorStream(final JsonObject fromJsom) {
            this(fromJsom.getStringRequired("name"),
                    fromJsom.getStringRequired("stream"));
        }

        private ConnectorStream(final String connector, final String stream) {
            this.connector = connector;
            this.stream = stream;
        }

        @Override
        public String toString() {
            return "connector='" + connector + "', stream='" + stream + '\'';
        }
    }
}
