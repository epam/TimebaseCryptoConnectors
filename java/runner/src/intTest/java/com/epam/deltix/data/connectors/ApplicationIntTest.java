package com.epam.deltix.data.connectors;

import com.epam.deltix.data.connectors.test.fixtures.integration.TbIntTestPreparation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;

import static org.testcontainers.containers.output.OutputFrame.OutputType.STDERR;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

public class ApplicationIntTest extends TbIntTestPreparation {

    public static GenericContainer APP_CONTAINER = new GenericContainer(
            new ImageFromDockerfile("localhost/timebase-crypto-connectors:snapshot")
                    .withFileFromPath("Dockerfile", Path.of("src/main/docker/Dockerfile"))
                    .withFileFromPath(".", Path.of("build/docker"))).
            withNetwork(network).
            withReuse(true).
            withEnv("JAVA_OPTS", "-Dtimebase.url=dxtick://timebase:8011").
            withStartupTimeout(Duration.ofSeconds(240));

    @BeforeAll
    public static void beforeAll() {
        TbIntTestPreparation.beforeAll();

        APP_CONTAINER.start();

        APP_CONTAINER.followOutput(new Consumer<OutputFrame>() {
            @Override
            public void accept(final OutputFrame outputFrame) {
                System.out.print("O>>>" + outputFrame.getUtf8String());
            }
        }, STDOUT);
        APP_CONTAINER.followOutput(new Consumer<OutputFrame>() {
            @Override
            public void accept(final OutputFrame outputFrame) {
                System.out.print("E>>>" + outputFrame.getUtf8String());
            }
        }, STDERR);
    }

    @AfterAll
    public static void afterAll() {
        APP_CONTAINER.stop();
        TbIntTestPreparation.afterAll();
    }

    @Test
    void dataFeed() throws Exception {
        System.out.println("test1");
        // TODO: implement test
        //Thread.sleep(600000);
    }
}
