package com.epam.deltix.data.connectors.test.fixtures.integration;

import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

public class TbIntTestPreparation {
    public static Network network = Network.newNetwork();

    public static final String TIMEBASE_SERVER_VERSION = System.getProperty("timebaseServerVersion", "6.1");

    public static GenericContainer TIMEBASE_CONTAINER = new GenericContainer(
            DockerImageName.parse("finos/timebase-ce-server:" + TIMEBASE_SERVER_VERSION))
            .withNetwork(network)
            .withNetworkAliases("timebase")
            .withReuse(true)
            .withExposedPorts(8011);

    @BeforeAll
    public static void beforeAll() {
        TIMEBASE_CONTAINER.start();
    }

    @AfterAll
    public static void afterAll() {
        TIMEBASE_CONTAINER.stop();
    }
}
