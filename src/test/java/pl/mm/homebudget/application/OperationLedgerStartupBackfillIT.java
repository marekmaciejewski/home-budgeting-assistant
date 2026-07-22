package pl.mm.homebudget.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.OperationType;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///ledgerstartupbackfill;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.url=jdbc:h2:mem:ledgerstartupbackfill;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.change-log=classpath:/db/changelog/db.changelog-ledger-startup-backfill-test.yaml"
        })
@AutoConfigureWebTestClient
class OperationLedgerStartupBackfillIT {

    private static final String BACKFILLED_TRANSFER_HASH =
            "36c3b8f9ad0785a5a9007ba0ac117a748e95319ce796348be9a04e7c0c42da23";
    private static final String BACKFILLED_SAVINGS_RECHARGE_HASH =
            "e67a51026afc53fc3b5968bc61d8f183b5cb01993bcd10525cf10c48f06b29ff";
    private static final String BACKFILLED_WALLET_RECHARGE_HASH =
            "49a0371667a2dab9357a7fec781da9dc6a8470f7dec701234fce9b73651242f6";

    @Autowired
    private WebTestClient testClient;

    @Test
    void startupBackfillsLegacyOperationsAfterLiquibaseMigration() {
        List<OperationResponse> operations = testClient.get().uri("/operations")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OperationResponse.class)
                .hasSize(3)
                .returnResult()
                .getResponseBody();

        assertThat(operations)
                .isNotNull()
                .extracting(
                        OperationResponse::getId,
                        OperationResponse::getOperationType,
                        OperationResponse::getSequenceNumber,
                        OperationResponse::getAmount,
                        OperationResponse::getSourceRegisterId,
                        OperationResponse::getTargetRegisterId,
                        OperationResponse::getOperationHash)
                .containsExactly(
                        tuple(
                                102L,
                                OperationType.TRANSFER,
                                1L,
                                new BigDecimal("100.00"),
                                "Wallet",
                                "Savings",
                                BACKFILLED_TRANSFER_HASH),
                        tuple(
                                103L,
                                OperationType.RECHARGE,
                                2L,
                                new BigDecimal("50.00"),
                                null,
                                "Savings",
                                BACKFILLED_SAVINGS_RECHARGE_HASH),
                        tuple(
                                101L,
                                OperationType.RECHARGE,
                                3L,
                                new BigDecimal("2500.00"),
                                null,
                                "Wallet",
                                BACKFILLED_WALLET_RECHARGE_HASH));
    }
}
