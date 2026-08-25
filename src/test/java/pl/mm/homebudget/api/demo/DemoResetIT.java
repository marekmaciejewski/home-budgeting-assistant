package pl.mm.homebudget.api.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///demoresettest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.url=jdbc:h2:mem:demoresettest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        })
@AutoConfigureWebTestClient
class DemoResetIT {

    private static final String SEED_REGISTERS = """
            [
              {"id":"Wallet","balance":1000.00},
              {"id":"Savings","balance":5000.00},
              {"id":"Insurance policy","balance":0.00},
              {"id":"Food expenses","balance":0.00}
            ]
            """;

    @Autowired
    private WebTestClient testClient;

    @Test
    void resetDemo_restoresRegisters() {
        testClient.post().uri("/demo/reset")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .json(SEED_REGISTERS);

        testClient.get().uri("/registers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(SEED_REGISTERS);
    }
}
