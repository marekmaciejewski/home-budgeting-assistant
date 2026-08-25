package pl.mm.homebudget.api.showcase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///disabledtamperledgerapitest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.url=jdbc:h2:mem:disabledtamperledgerapitest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "app.features.ledger-tamper-simulation-enabled=false"
        })
@AutoConfigureWebTestClient
class LedgerTamperSimulationDisabledIT {

    @Autowired
    private WebTestClient testClient;

    @Test
    void capabilities_reportDisabledTamperWithInferredEphemeralReset() {
        testClient.get().uri("/capabilities")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("""
                        {
                          "storageMode": "EPHEMERAL",
                          "ledgerTamperSimulationAvailable": false
                        }
                        """, JsonCompareMode.STRICT);
    }

    @Test
    void tamperSimulation_isNotExposedWhenDisabled() {
        testClient.post().uri("/ledger/tamper-simulations")
                .header("Content-Type", "application/json")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isNotFound();
    }
}
