package com.epam.deltix.data.connectors;

import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonValue;
import com.epam.deltix.data.connectors.commons.json.JsonValueParser;
import com.epam.deltix.data.connectors.test.fixtures.integration.TbIntTestPreparation;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
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

    @BeforeAll
    static void beforeAll() throws Exception {
        TIMEBASE_CONTAINER.start();
        APP_CONTAINER.start();

        Thread.sleep(5000);
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

    @TestFactory
    Stream<DynamicTest> testDataFeedInTb() throws Exception {
        final JsonArray connectors =
                rest("http://localhost:" + APP_CONTAINER.getFirstMappedPort() + "/api/v0/connectors").
                        asArrayRequired();

        final ConnectorStream[] connectorStreams = connectors.items().
                map(JsonValue::asObjectRequired).
                map(o -> new ConnectorStream(
                        o.getStringRequired("name"),
                        o.getStringRequired("stream")
                        )).toArray(ConnectorStream[]::new);

        return Arrays.stream(connectorStreams).map(connector -> DynamicTest.dynamicTest(
                connector.connector,
                () -> tryReadSomeData(connector)
        ));
    }

    void tryReadSomeData(final ConnectorStream connector) {
        System.out.println("Test " + connector);
        // we are trying to read 10 messages
        final int expectedNumOfPackageHeader = 10;
        final int timeoutSeconds = 5;

        final DXTickStream stream = db.getStream(connector.stream);

        Assertions.assertNotNull(stream, "Connector " + connector + " not started as expected. " +
                "No output stream found.");

        try (TickCursor cursor = stream.select(TimeConstants.USE_CURRENT_TIME,
                new SelectionOptions(true, true))) {
            Assertions.assertTimeoutPreemptively(Duration.ofSeconds(timeoutSeconds), () -> {
                int packageHeaders = 0;
                while (cursor.next()) {
                    if (++packageHeaders == expectedNumOfPackageHeader) {
                        break;
                    }
                }
            }, "Cannot read data for " + connector);
        }
    }

    private static JsonValue rest(final String url) throws Exception {
        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(5000))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .timeout(Duration.ofSeconds(3))
                .build();

        final HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return new JsonValueParser().parseAndEoj(response.body());
    }

    private static class ConnectorStream {
        private final String connector;
        private final String stream;

        private ConnectorStream(final String connector, final String stream) {
            this.connector = connector;
            this.stream = stream;
        }

        @Override
        public String toString() {
            return "connector='" + connector + '\'' +
                    ", stream='" + stream + '\'';
        }
    }
}
