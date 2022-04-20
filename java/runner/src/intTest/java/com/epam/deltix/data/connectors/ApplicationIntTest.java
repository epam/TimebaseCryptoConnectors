package com.epam.deltix.data.connectors;

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

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Stream;

public class ApplicationIntTest extends TbIntTestPreparation {

    public static GenericContainer APP_CONTAINER = new GenericContainer(
            new ImageFromDockerfile("localhost/timebase-crypto-connectors:snapshot")
                    .withFileFromPath("Dockerfile", Path.of("src/main/docker/Dockerfile"))
                    .withFileFromPath(".", Path.of("build/docker"))).
            withReuse(true).
            withNetwork(NETWORK).
            //withExposedPorts(8055) // uncomment when the service really exposes the port when all DCs have been started
                    withEnv("JAVA_OPTS", "-Dtimebase.url=dxtick://timebase:8011").
                    withStartupTimeout(Duration.ofSeconds(240));

    @BeforeAll
    static void beforeAll() throws Exception {
        TIMEBASE_CONTAINER.start();
        APP_CONTAINER.start();

        Thread.sleep(5000); // let all DCs to be started. TODO: replace by port/8055 availability
        // checking with specifying exposedPorts in the container specification
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
        final String[] connectors = new String[]{
                "coinbase",
                "kraken-spot"
                /* TODO: read from application.yaml */
        };

        return Arrays.stream(connectors).map(connector -> DynamicTest.dynamicTest(
                connector,
                () -> tryReadPackageHeaders(connector)
        ));
    }

    void tryReadPackageHeaders(final String connector) throws Exception {
        final int expectedNumOfPackageHeader = 10;
        final int timeoutSeconds = 5;

        final DXTickStream stream = db.getStream(connector);

        Assertions.assertNotNull(stream, "Connector " + connector + " not started as expected. " +
                "No output stream found.");

        try (TickCursor cursor = stream.select(TimeConstants.USE_CURRENT_TIME,
                new SelectionOptions(true, true))) {

            Assertions.assertTimeout(Duration.ofSeconds(timeoutSeconds), () -> {
                int packageHeaders = 0;

                while (cursor.next()) {
                    if (++packageHeaders == expectedNumOfPackageHeader) {
                        break;
                    }
                }
            });
        }
    }
}
