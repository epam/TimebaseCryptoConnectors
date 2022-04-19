package com.epam.deltix.data.connectors.test.fixtures.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class TbIntTestPreparation {
    public static final String TB_VERSION = System.getProperty("timebaseServerVersion", "6.1");

    @Container
    public static GenericContainer TIMEBASE_CONTAINER = new GenericContainer(
            DockerImageName.parse("finos/timebase-ce-server:" + TB_VERSION))
            .withReuse(true)
            .withExposedPorts(8011);

    @BeforeAll
    public static void beforeAll() {
        System.out.println("TB beforeAll start");
        System.out.println("TB port = " + TIMEBASE_CONTAINER.getFirstMappedPort());
        TIMEBASE_CONTAINER.start();
        System.out.println("TB beforeAll end");
    }

    @AfterAll
    public static void afterAll() {
        System.out.println("TB afterAll start");
        TIMEBASE_CONTAINER.stop();
        System.out.println("TB afterAll end");
    }
}
