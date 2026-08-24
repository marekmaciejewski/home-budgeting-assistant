package pl.mm.homebudget.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.OperationType;
import pl.mm.testsupport.FixedClockTestConfiguration;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///ledgerinitializedstartup;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.url=jdbc:h2:mem:ledgerinitializedstartup;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.change-log=classpath:/db/changelog/db.changelog-ledger-startup-initialized-test.yaml"
        })
@AutoConfigureWebTestClient
@Import(FixedClockTestConfiguration.class)
class OperationLedgerInitializedStartupIT {

    private static final String FIRST_OPERATION_HASH =
            "bd17cfa30eb03af0e742822e57be0fab4fba8c2e6082d557ebd40c855177474c";
    private static final String SECOND_OPERATION_HASH =
            "4698920a2def2f9525311972bf908dc3c81107c63c4c5e0b2ebe32063090d23c";

    @Autowired
    private WebTestClient testClient;

    @Test
    void startupPreservesAlreadyInitializedLedger() {
        testClient.get().uri("/operations")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBodyList(OperationResponse.class)
                .value(operations -> assertThat(operations).containsExactly(
                        new OperationResponse()
                                .id(201L)
                                .timestamp(OffsetDateTime.parse("2026-06-01T10:15:30Z"))
                                .amount(new BigDecimal("2500.00"))
                                .operationType(OperationType.RECHARGE)
                                .sequenceNumber(1L)
                                .operationHash(FIRST_OPERATION_HASH)
                                .targetRegisterId("Wallet"),
                        new OperationResponse()
                                .id(202L)
                                .timestamp(OffsetDateTime.parse("2026-06-01T10:00:00Z"))
                                .amount(new BigDecimal("100.00"))
                                .operationType(OperationType.TRANSFER)
                                .sequenceNumber(2L)
                                .operationHash(SECOND_OPERATION_HASH)
                                .sourceRegisterId("Wallet")
                                .targetRegisterId("Savings")));

        testClient.get().uri("/ledger/verify")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .json("""
                        {
                          "status": "VERIFIED",
                          "operationCount": 2,
                          "verifiedThroughSequence": 2,
                          "latestSequenceNumber": 2,
                          "ledgerHeadHash": "%s",
                          "checkedAt": "2026-06-01T10:15:30Z",
                          "mismatch": null
                        }
                        """.formatted(SECOND_OPERATION_HASH), JsonCompareMode.STRICT);
    }
}
