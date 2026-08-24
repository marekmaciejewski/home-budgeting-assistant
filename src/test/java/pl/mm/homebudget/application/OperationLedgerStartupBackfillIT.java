package pl.mm.homebudget.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.OperationType;
import pl.mm.testsupport.FixedClockTestConfiguration;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static pl.mm.testsupport.FixedClockTestConfiguration.FIXED_INSTANT;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///ledgerstartupbackfill;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.url=jdbc:h2:mem:ledgerstartupbackfill;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.change-log=classpath:/db/changelog/db.changelog-ledger-startup-backfill-test.yaml"
        })
@AutoConfigureWebTestClient
@Import(FixedClockTestConfiguration.class)
class OperationLedgerStartupBackfillIT {

    private static final OffsetDateTime EARLIER_OPERATION_TIMESTAMP =
            OffsetDateTime.parse("2026-06-01T10:00:00Z");
    private static final OffsetDateTime LATER_OPERATION_TIMESTAMP = FIXED_INSTANT.atOffset(ZoneOffset.UTC);
    private static final String BACKFILLED_TRANSFER_HASH =
            "98a2c26ae8ddc502346dffaaa4584e53e2c06d259cd477828599cc4213981397";
    private static final String BACKFILLED_SAVINGS_RECHARGE_HASH =
            "0b71edbb2f88d37877e51cbaeff7a732a6f5103724ab5a9a8d84b1eaf596009f";
    private static final String BACKFILLED_WALLET_RECHARGE_HASH =
            "5d6cd72dc3f8a4cba003890da0453224b9c3fea286aea9e06aa8656ea8c65a20";

    @Autowired
    private WebTestClient testClient;

    @Test
    void startupBackfillsLegacyOperationsAfterLiquibaseMigration() {
        testClient.get().uri("/operations")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OperationResponse.class)
                .hasSize(3)
                .contains(
                        new OperationResponse()
                                .id(101L)
                                .timestamp(LATER_OPERATION_TIMESTAMP)
                                .amount(new BigDecimal("2500.00"))
                                .operationType(OperationType.RECHARGE)
                                .sequenceNumber(3L)
                                .operationHash(BACKFILLED_WALLET_RECHARGE_HASH)
                                .targetRegisterId("Wallet"),
                        new OperationResponse()
                                .id(102L)
                                .timestamp(EARLIER_OPERATION_TIMESTAMP)
                                .amount(new BigDecimal("100.00"))
                                .operationType(OperationType.TRANSFER)
                                .sequenceNumber(1L)
                                .operationHash(BACKFILLED_TRANSFER_HASH)
                                .sourceRegisterId("Wallet")
                                .targetRegisterId("Savings"),
                        new OperationResponse()
                                .id(103L)
                                .timestamp(EARLIER_OPERATION_TIMESTAMP)
                                .amount(new BigDecimal("50.00"))
                                .operationType(OperationType.RECHARGE)
                                .sequenceNumber(2L)
                                .operationHash(BACKFILLED_SAVINGS_RECHARGE_HASH)
                                .targetRegisterId("Savings"));
    }
}
