package com.epam.deltix.data.connectors;

import com.epam.deltix.containers.interfaces.LogProcessor;
import com.epam.deltix.containers.interfaces.Severity;
import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonValue;
import com.epam.deltix.data.connectors.commons.json.JsonValueParser;
import com.epam.deltix.data.connectors.reports.ReportGenerator;
import com.epam.deltix.data.connectors.reports.TestConnectorReports;
import com.epam.deltix.data.connectors.test.fixtures.integration.TbIntTestPreparation;
import com.epam.deltix.data.connectors.validator.DataValidator;
import com.epam.deltix.data.connectors.validator.DataValidatorImpl;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.stream.MessageWriter2;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.universal.*;
import com.epam.deltix.util.collections.generated.ObjectList;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IntegrationTest extends TbIntTestPreparation {

    private static final Logger LOG = Logger.getLogger(IntegrationTest.class.getName());

    private static final Set<String> SKIP_CONNECTORS = Set.of("uniswap", "uniswap-l2");

    public static final GenericContainer APP_CONTAINER = new GenericContainer(
            new ImageFromDockerfile("localhost/timebase-crypto-connectors:snapshot").
                    withFileFromPath("Dockerfile", Path.of("src/main/docker/Dockerfile")).
                    withFileFromPath(".", Path.of("build/docker"))).
            withReuse(true).
            withNetwork(NETWORK).
            withExposedPorts(8055).
            withEnv("JAVA_OPTS",
                "-Dtimebase.url=dxtick://timebase:8011 " +
                    "-Dlogging.config=/runner/config/logback.xml " +
                    "-Dconnectors.binance-spot.wsUrl=wss://stream.binance.us:9443/ws " +
                    "-Dconnectors.binance-spot.restUrl=https://api.binance.us/api/v3 " +
                    "-Dconnectors.binance-futures.wsUrl=wss://fstream.binance.us/stream " +
                    "-Dconnectors.binance-futures.restUrl=https://fapi.binance.us/fapi/v1 " +
                    "-Dconnectors.binance-dlv.wsUrl=wss://dstream.binance.us/stream " +
                    "-Dconnectors.binance-dlv.restUrl=https://dapi.binance.us/dapi/v1 ").
            withFileSystemBind("build/intTest/config", "/runner/config", BindMode.READ_WRITE).
            withStartupTimeout(Duration.ofMinutes(5));

    private static final int READ_TIMEOUT_S = 10;

    private static final int SMOKE_READ_MESSAGES = 100;
    private static final int VALIDATION_READ_MESSAGES = 1000;

    private static Map<String, TestConnectorReports> reports = new LinkedHashMap<>();

    @BeforeAll
    static void beforeAll() throws Exception {
        TIMEBASE_CONTAINER.start();
        APP_CONTAINER.start();

        Thread.sleep(10_000);

        Arrays.stream(listConnectors())
            .sorted(Comparator.comparing(TestConnectorReports::connector))
            .forEach(c -> reports.put(c.connector(), c));
    }

    @AfterAll
    static void afterAll() {
        APP_CONTAINER.stop();
        TIMEBASE_CONTAINER.stop();

        ReportGenerator.generate(new File("build/test-reports.md"),
            "### Status Report (" + LocalDateTime.now().withNano(0).withSecond(0) + ")",
            reports);
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
    @Order(1)
    Stream<DynamicTest> testDataFeedConnection() {
        return reports.values().stream()
            .map(c -> DynamicTest.dynamicTest(
                "Connection To " + c.connector(),
                () -> c.runTest(ReportGenerator.CONNECTION_REPORT, () -> tryReadSomeData(c))
            ));
    }

    @TestFactory
    @Order(2)
    Stream<DynamicTest> testDataFeedL2Validate() {
        return reports.values().stream()
            .map(c -> DynamicTest.dynamicTest(
                "Validate L2 for " + c.connector(),
                () -> c.runTest(ReportGenerator.VALIDATE_L2_REPORT, () -> tryBuildOrderBook(c))
            ));
    }

    private static TestConnectorReports[] listConnectors() throws Exception {
        final JsonArray connectors =
            rest("http://localhost:" + APP_CONTAINER.getFirstMappedPort() + "/api/v0/connectors").
                asArrayRequired();

        return connectors.items().
            map(JsonValue::asObjectRequired).
            map(TestConnectorReports::new).
            filter(connector -> !SKIP_CONNECTORS.contains(connector.stream())).
            toArray(TestConnectorReports[]::new);
    }

    void tryReadSomeData(final TestConnectorReports connector) {
        // we are trying to read N messages
        final int expectedNumOfMessages = SMOKE_READ_MESSAGES;
        final int timeoutSeconds = READ_TIMEOUT_S;

        final DXTickStream stream = db.getStream(connector.stream());

        Assertions.assertNotNull(stream, "Connector " + connector + " not started as expected. " +
            "No output stream found.");

        try (TickCursor cursor = stream.select(TimeConstants.TIMESTAMP_UNKNOWN,
            new SelectionOptions(true, true))) {
            int messages = 0;
            while (readWithTimeout(timeoutSeconds, cursor, connector)) {
                if (++messages == expectedNumOfMessages) {
                    break;
                }
            }
        }
    }

    void tryBuildOrderBook(final TestConnectorReports connector) {
        final int expectedNumOfMessages = VALIDATION_READ_MESSAGES;
        final int timeoutSeconds = READ_TIMEOUT_S;

        final DXTickStream stream = db.getStream(connector.stream());

        Assertions.assertNotNull(stream, "Connector " + connector + " not started as expected. " +
            "No output stream found.");

        Map<String, DataValidator> validators = new HashMap<>();
        AtomicLong errors = new AtomicLong();

        Set<String> supportedModel = new HashSet<>();
        try (TickCursor cursor = stream.select(TimeConstants.TIMESTAMP_UNKNOWN, new SelectionOptions(false, true))) {
            int messages = 0;
            while (readWithTimeout(timeoutSeconds, cursor, connector)) {
                InstrumentMessage message = cursor.getMessage();
                if (message.getSymbol() == null) {
                    continue;
                }

                // validate
                if (message instanceof PackageHeaderInfo) {
                    PackageHeaderInfo packageHeader = (PackageHeaderInfo) message;
                    updateSupportedModel(packageHeader, supportedModel);
                    DataValidator validator = validators.computeIfAbsent(
                        message.getSymbol().toString(),
                        k -> createValidator(k, (sender, severity, exception, stringMessage) -> {
                                if (severity == Severity.ERROR) {
                                    errors.addAndGet(1);
                                    LOG.severe(severity + " | " + stringMessage);
                                } else {
                                    LOG.warning(severity + " | " + stringMessage);
                                }

                                if (exception != null) {
                                    LOG.log(Level.SEVERE, "Exception", exception);
                                }
                            }, stream
                        )
                    );

                    validator.sendPackage(packageHeader);

                    if (++messages % 100 == 0) {
                        LOG.info("Processed " + messages + " package headers");
                    }

                    if (messages == expectedNumOfMessages) {
                        LOG.info("Processed " + messages + " package headers");
                        break;
                    }
                }
            }
        } finally {
            exportStream(stream);
            connector.addTestMessage(
                ReportGenerator.SUPPORTED_MODEL_REPORT,
                supportedModel.stream().sorted().collect(Collectors.joining(","))
            );
        }

        Assertions.assertEquals(0L, errors.get());
    }

    private static void updateSupportedModel(PackageHeaderInfo packageHeader, Set<String> supportedModel) {
        ObjectList<BaseEntryInfo> entries = packageHeader.getEntries();
        if (entries != null) {
            for (int i = 0; i < entries.size(); ++i) {
                BaseEntryInfo entry = entries.get(i);
                if (entry instanceof TradeEntryInfo) {
                    supportedModel.add("TRADES");
                } else if (entry instanceof L1EntryInfo) {
                    supportedModel.add("L1");
                } else if (entry instanceof L2EntryNewInfo || entry instanceof L2EntryUpdateInfo) {
                    supportedModel.add("L2");
                }
            }
        }
    }

    private static Boolean readWithTimeout(long timeoutSeconds, TickCursor cursor, TestConnectorReports connector) {
        return Assertions.assertTimeoutPreemptively(
            Duration.ofSeconds(timeoutSeconds), cursor::next,
            "Cannot read data for the " + connector
        );
    }

    private static DataValidator createValidator(String symbol, LogProcessor log, DXTickStream stream) {
        DataValidatorImpl validator = new DataValidatorImpl(symbol, log,
            Decimal64Utils.parse("0.0000000000000000001"), Decimal64Utils.parse("0.0000000000000000001"),
            true, stream
        );
        validator.setCheckEmptySide(true);
        validator.setCheckBidMoreThanAsk(true);

        return validator;
    }

    private static void exportStream(DXTickStream stream) {
        File outputFile = new File("build/intTest/streams/" + stream.getKey() + ".qsmsg.gz").getAbsoluteFile();
        outputFile.getParentFile().mkdirs();
        LOG.info("Exporting stream: " + outputFile.getAbsolutePath());

        int messages = 0;
        try (TickCursor cursor = stream.select(TimeConstants.TIMESTAMP_UNKNOWN, new SelectionOptions(true, false));
             MessageWriter2 messageWriter = MessageWriter2.create(outputFile, null, null, stream.getPolymorphicDescriptors()))
        {
            while (cursor.next()) {
                messageWriter.send(cursor.getMessage());
                messages++;
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }

        LOG.info("Exported stream: " + stream.getKey() + ". Messages count: " + messages);
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

}
