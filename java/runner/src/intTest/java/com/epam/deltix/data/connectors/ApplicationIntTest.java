package com.epam.deltix.data.connectors;

import com.epam.deltix.data.connectors.test.fixtures.integration.TbIntTestPreparation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.time.Duration;

public class ApplicationIntTest extends TbIntTestPreparation {

    @Container
    public static GenericContainer APP_CONTAINER = new GenericContainer(
            new ImageFromDockerfile("localhost/timebase-crypto-connectors:snapshot")
                    .withFileFromPath("Dockerfile", Path.of("src/main/docker/Dockerfile"))
                    .withFileFromPath(".", Path.of("build/docker")))
            .withReuse(true)
            //.withExposedPorts(8055)
            .withStartupTimeout(Duration.ofSeconds(120));

    @BeforeAll
    public static void beforeAll() {
        System.out.println("APP beforeAll start");
        TbIntTestPreparation.beforeAll();
        APP_CONTAINER.start();
        System.out.println("APP beforeAll end");
    }

    @AfterAll
    public static void afterAll() {
        System.out.println("APP afterAll start");
        APP_CONTAINER.stop();
        TbIntTestPreparation.afterAll();
        System.out.println("APP afterAll end");
    }

    @Test
    void test1() throws Exception {
        System.out.println("test1");

        Thread.sleep(60000);
    }
}
