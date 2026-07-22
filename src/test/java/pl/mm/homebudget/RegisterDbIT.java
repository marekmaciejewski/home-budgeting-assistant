package pl.mm.homebudget;

import org.apache.commons.io.FilenameUtils;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.OperationType;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RegisterDbIT {

    private static final Instant OPERATION_INSTANT = Instant.parse("2026-06-01T10:15:30Z");
    private static final OffsetDateTime OPERATION_TIMESTAMP = OPERATION_INSTANT.atOffset(ZoneOffset.UTC);
    private static final String WALLET_RECHARGE_HASH =
            "bd17cfa30eb03af0e742822e57be0fab4fba8c2e6082d557ebd40c855177474c";
    private static final String WALLET_TO_FOOD_EXPENSES_TRANSFER_HASH =
            "e324da21bb55dc8730068ce1f132a61fb26b3d06afae0a7165aa09dbb6b7cb1e";
    private static final String SAVINGS_TO_INSURANCE_POLICY_TRANSFER_HASH =
            "c6520c6cc0c471147f92fa284e24807340f0d67c1fa70a3bc42cbdb234bcd6ca";
    private static final String WALLET_TO_SAVINGS_TRANSFER_HASH =
            "1ac8889d615726c7ecd19bdb9bf178e0b0eb96164d182f133871d5e5956ea793";
    private static final Map<Long, String> SEQ_NUM_HASH = Map.of(
            1L, WALLET_RECHARGE_HASH,
            2L, WALLET_TO_FOOD_EXPENSES_TRANSFER_HASH,
            3L, SAVINGS_TO_INSURANCE_POLICY_TRANSFER_HASH,
            4L, WALLET_TO_SAVINGS_TRANSFER_HASH);

    @Autowired
    private WebTestClient testClient;

    private long expectedNextSequenceNumber = 1;

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(OPERATION_INSTANT, ZoneOffset.UTC);
        }
    }

    @TestFactory
    Stream<DynamicNode> demoScenario() {
        return Stream.of(
                dynamicContainer("Initial state", initialState()),
                dynamicContainer("2500 -> Wallet", rechargeWallet()),
                dynamicContainer("Wallet -> 1500 -> Food expenses", transferFromWalletToFoodExpenses()),
                dynamicContainer("Savings -> 500 -> Insurance policy", transferFromSavingsToInsurancePolicy()),
                dynamicContainer("Wallet -> 1000 -> Savings", transferFromWalletToSavings()));
    }

    private Stream<DynamicNode> initialState() {
        String balances = balancesJson(1000, 5000, 0, 0);
        return Stream.of(
                dynamicTest("check initial balances", () -> checkBalances(balances)),
                dynamicTest("check single register", this::checkInitialWalletRegister),
                dynamicTest("check initial operation history", () -> checkOperationCount(0)));
    }

    private Stream<DynamicNode> rechargeWallet() {
        String balances = balancesJson(3500, 5000, 0, 0);
        return Stream.of(
                dynamicTest("recharge", this::rechargeWalletWith2500),
                dynamicTest("check balances", () -> checkBalances(balances)));
    }

    private Stream<DynamicNode> transferFromWalletToFoodExpenses() {
        String balances = balancesJson(2000, 5000, 0, 1500);
        return Stream.of(
                dynamicTest("transfer", () -> transfer("Wallet", "Food expenses", 1500)),
                dynamicTest("check balances", () -> checkBalances(balances)));
    }

    private Stream<DynamicNode> transferFromSavingsToInsurancePolicy() {
        String balances = balancesJson(2000, 4500, 500, 1500);
        return Stream.of(
                dynamicTest("transfer", () -> transfer("Savings", "Insurance policy", 500)),
                dynamicTest("check balances", () -> checkBalances(balances)));
    }

    private Stream<DynamicNode> transferFromWalletToSavings() {
        String balances = balancesJson(1000, 5500, 500, 1500);
        return Stream.of(
                dynamicTest("transfer", () -> transfer("Wallet", "Savings", 1000)),
                dynamicTest("check final balances", () -> checkBalances(balances)),
                dynamicTest("check operation history", this::checkFinalOperations));
    }

    private String balancesJson(int wallet, int savings, int insurancePolicy, int foodExpenses) {
        return """
                [
                  {"id":"Wallet","balance":%d.00},
                  {"id":"Savings","balance":%d.00},
                  {"id":"Insurance policy","balance":%d.00},
                  {"id":"Food expenses","balance":%d.00}
                ]
                """.formatted(wallet, savings, insurancePolicy, foodExpenses);
    }

    private void checkBalances(String balances) {
        testClient.get().uri("/registers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(balances);
    }

    private void checkInitialWalletRegister() {
        testClient.get().uri("/registers/{registerId}", "Wallet")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("""
                        {
                          "id": "Wallet",
                          "balance": 1000.00
                        }
                        """);
    }

    private WebTestClient.ListBodySpec<OperationResponse> checkOperationCount(int expectedCount) {
        return testClient.get().uri("/operations")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OperationResponse.class)
                .hasSize(expectedCount);
    }

    private void rechargeWalletWith2500() {
        String body = "{\"registerId\":\"Wallet\",\"amount\":2500}";
        executeOperation(body, "/operations/recharges", null, "Wallet", 2500);
    }

    private void transfer(String source, String target, int amount) {
        String body = String
                .format("{\"sourceRegisterId\":\"%s\",\"targetRegisterId\":\"%s\",\"amount\":%d}", source, target, amount);
        executeOperation(body, "/operations/transfers", source, target, amount);
    }

    private void executeOperation(
            String body,
            String path,
            String sourceRegisterId,
            String targetRegisterId,
            int amount) {
        EntityExchangeResult<OperationResponse> result = testClient.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", "/operations/\\d+")
                .expectBody(OperationResponse.class)
                .returnResult();

        OperationResponse createdOperation = result.getResponseBody();
        URI location = result.getResponseHeaders().getLocation();
        //noinspection DataFlowIssue
        long operationId = Long.parseLong(FilenameUtils.getName(location.getPath()));
        OperationType operationType = sourceRegisterId == null ? OperationType.RECHARGE : OperationType.TRANSFER;
        long sequenceNumber = expectedNextSequenceNumber++;
        String operationHash = SEQ_NUM_HASH.get(sequenceNumber);

        assertOperation(
                createdOperation,
                amount,
                operationType,
                sequenceNumber,
                operationHash,
                sourceRegisterId,
                targetRegisterId)
                .returns(operationId, from(OperationResponse::getId));
    }

    private void checkFinalOperations() {
        List<OperationResponse> operations = checkOperationCount(4).returnResult().getResponseBody();
        assertThat(operations)
                .isNotNull()
                .satisfiesExactly(operationResponse -> assertOperation(
                                operationResponse,
                                2500,
                                OperationType.RECHARGE,
                                1L,
                                WALLET_RECHARGE_HASH,
                                null,
                                "Wallet"),
                        operationResponse -> assertOperation(
                                operationResponse,
                                1500,
                                OperationType.TRANSFER,
                                2L,
                                WALLET_TO_FOOD_EXPENSES_TRANSFER_HASH,
                                "Wallet",
                                "Food expenses"),
                        operationResponse -> assertOperation(
                                operationResponse,
                                500,
                                OperationType.TRANSFER,
                                3L,
                                SAVINGS_TO_INSURANCE_POLICY_TRANSFER_HASH,
                                "Savings",
                                "Insurance policy"),
                        operationResponse -> assertOperation(
                                operationResponse,
                                1000,
                                OperationType.TRANSFER,
                                4L,
                                WALLET_TO_SAVINGS_TRANSFER_HASH,
                                "Wallet",
                                "Savings"));
    }

    private ObjectAssert<OperationResponse> assertOperation(
            OperationResponse operation,
            int amount,
            OperationType operationType,
            long sequenceNumber,
            String operationHash,
            String sourceRegisterId,
            String targetRegisterId) {
        return assertThat(operation)
                .isNotNull()
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .returns(OPERATION_TIMESTAMP, from(OperationResponse::getTimestamp))
                .returns(BigDecimal.valueOf(amount), from(OperationResponse::getAmount))
                .returns(operationType, from(OperationResponse::getOperationType))
                .returns(sequenceNumber, from(OperationResponse::getSequenceNumber))
                .returns(operationHash, from(OperationResponse::getOperationHash))
                .returns(sourceRegisterId, from(OperationResponse::getSourceRegisterId))
                .returns(targetRegisterId, from(OperationResponse::getTargetRegisterId));
    }
}
