package pl.mm.homebudget.api.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.file.Path;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class DemoResetDisabledIT {

    private static final String DATABASE_PATH = Path.of(
                    "target",
                    "demo-reset-disabled-" + UUID.randomUUID())
            .toAbsolutePath()
            .toString()
            .replace('\\', '/');

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.r2dbc.url",
                () -> "r2dbc:h2:file:///" + DATABASE_PATH + ";DB_CLOSE_ON_EXIT=FALSE");
        registry.add(
                "spring.liquibase.url",
                () -> "jdbc:h2:file:" + DATABASE_PATH + ";DB_CLOSE_ON_EXIT=FALSE");
    }

    @Autowired
    private WebTestClient testClient;

    @Test
    void resetDemo_isNotExposedForPersistentStorage() {
        testClient.post().uri("/demo/reset")
                .exchange()
                .expectStatus().isNotFound();
    }
}
