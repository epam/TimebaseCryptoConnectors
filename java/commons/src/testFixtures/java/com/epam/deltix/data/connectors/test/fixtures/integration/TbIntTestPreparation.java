package com.epam.deltix.data.connectors.test.fixtures.integration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

public class TbIntTestPreparation {
    public static final String TIMEBASE_SERVER_VERSION = System.getProperty("timebaseServerVersion", "6.1");

    public static final Network NETWORK = Network.newNetwork();

    public static GenericContainer TIMEBASE_CONTAINER = new GenericContainer(
            DockerImageName.parse("finos/timebase-ce-server:" + TIMEBASE_SERVER_VERSION))
            .withReuse(true)
            .withNetwork(NETWORK)
            .withNetworkAliases("timebase")
            .withExposedPorts(8011);
}
