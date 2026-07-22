package pl.mm.homebudget.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.OperationType;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = DataSourceTransactionManagerAutoConfiguration.class)
@AutoConfigureWebTestClient
@SqlConfig(dataSource = "sqlDataSource")
class OperationLedgerServiceIT {

    private static final String DELETE_OPERATIONS_SQL = "/sql/operation-ledger/delete-operations.sql";
    private static final String LEGACY_OPERATIONS_SQL = "/sql/operation-ledger/legacy-operations.sql";
    private static final String MIXED_LEDGER_STATE_SQL = "/sql/operation-ledger/mixed-ledger-state.sql";
    private static final String BACKFILLED_TRANSFER_HASH =
            "36c3b8f9ad0785a5a9007ba0ac117a748e95319ce796348be9a04e7c0c42da23";
    private static final String BACKFILLED_SAVINGS_RECHARGE_HASH =
            "e67a51026afc53fc3b5968bc61d8f183b5cb01993bcd10525cf10c48f06b29ff";
    private static final String BACKFILLED_WALLET_RECHARGE_HASH =
            "49a0371667a2dab9357a7fec781da9dc6a8470f7dec701234fce9b73651242f6";

    @Autowired
    private OperationLedgerBackfillService operationLedgerBackfillService;
    @Autowired
    private WebTestClient testClient;

    @TestConfiguration(proxyBeanMethods = false)
    static class SqlDataSourceConfiguration {

        @Bean
        DataSource sqlDataSource(@Value("${spring.liquibase.url}") String url) {
            return new DriverManagerDataSource(url);
        }
    }

    @Test
    @Sql(
            scripts = {DELETE_OPERATIONS_SQL, LEGACY_OPERATIONS_SQL},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = DELETE_OPERATIONS_SQL, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void backfillAssignsDeterministicLedgerOrderByTimestampThenId() {
        operationLedgerBackfillService.backfillIfNeeded().blockLast();

        List<OperationResponse> operations = getOperations(3);

        assertThat(operations)
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

    @Test
    @Sql(
            scripts = {DELETE_OPERATIONS_SQL, MIXED_LEDGER_STATE_SQL},
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = DELETE_OPERATIONS_SQL, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    void backfillFailsFastOnMixedLedgerState() {
        assertThatThrownBy(() -> operationLedgerBackfillService.backfillIfNeeded().blockLast())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Operation ledger has a mixed backfill state");

        assertThat(getOperations(2))
                .extracting(OperationResponse::getId, OperationResponse::getSequenceNumber)
                .containsExactly(tuple(201L, -201L), tuple(202L, 1L));
    }

    private List<OperationResponse> getOperations(int expectedCount) {
        List<OperationResponse> operations = testClient.get().uri("/operations")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OperationResponse.class)
                .hasSize(expectedCount)
                .returnResult()
                .getResponseBody();

        assertThat(operations).isNotNull();
        return operations;
    }
}
