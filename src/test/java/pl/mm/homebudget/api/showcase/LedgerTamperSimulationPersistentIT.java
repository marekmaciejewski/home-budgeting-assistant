package pl.mm.homebudget.api.showcase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.file.Path;
import java.util.UUID;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.features.ledger-tamper-simulation-enabled=true"
        })
@AutoConfigureWebTestClient
class LedgerTamperSimulationPersistentIT {

    private static final String DATABASE_PATH = Path.of(
                    "target",
                    "persistent-tamper-" + UUID.randomUUID())
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
    void persistentSimulation_returnsManualRepairReceiptWithoutResetPath() {
        testClient.get().uri("/capabilities")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("""
                        {
                          "storageMode": "PERSISTENT",
                          "ledgerTamperSimulationAvailable": true
                        }
                        """, JsonCompareMode.STRICT);

        testClient.post().uri("/operations/recharges")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"registerId\":\"Wallet\",\"amount\":10}")
                .exchange()
                .expectStatus().isCreated();

        testClient.post().uri("/ledger/tamper-simulations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"mode\":\"PAYLOAD_HASH\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.field").isEqualTo("PAYLOAD_HASH")
                .jsonPath("$.previousValue").isNotEmpty()
                .jsonPath("$.newValue").isNotEmpty()
                .jsonPath("$.verificationStatus").isEqualTo("INVALID")
                .jsonPath("$.resetPath").doesNotExist()
                .jsonPath("$.message")
                .isEqualTo("Ledger mismatch simulated. Copy the change details for manual repair.");
    }
}
