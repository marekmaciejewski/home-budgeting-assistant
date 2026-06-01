package pl.mm.homebudget;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.mm.homebudget.api.dto.OperationResponse;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RegisterDbIT {

    private static final Instant OPERATION_INSTANT = Instant.parse("2026-06-01T10:15:30Z");
    private static final OffsetDateTime OPERATION_TIMESTAMP = OPERATION_INSTANT.atOffset(ZoneOffset.UTC);

    @Autowired
    private WebTestClient testClient;

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
                dynamicTest("check single register", () -> checkRegister("Wallet", new BigDecimal("1000.00"))),
                dynamicTest("check initial operation history", () -> checkOperations(0)));
    }

    private Stream<DynamicNode> rechargeWallet() {
        String balances = balancesJson(3500, 5000, 0, 0);
        return Stream.of(
                dynamicTest("recharge", () -> recharge("Wallet", 2500)),
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
                dynamicTest("check operation history", () -> checkOperations(4)));
    }

    private void recharge(String register, int amount) {
        String body = String.format("{\"registerId\":\"%s\",\"amount\":%d}", register, amount);
        executeOperation(body, "/operations/recharges", null, register, amount);
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
        var result = testClient.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", "/operations/\\d+")
                .expectBody(OperationResponse.class)
                .returnResult();

        URI location = result.getResponseHeaders().getLocation();
        long operationId = Long.parseLong(FilenameUtils.getName(location.getPath()));

        OperationResponse createdOperation = result.getResponseBody();
        assertOperation(createdOperation, operationId, sourceRegisterId, targetRegisterId, amount);
    }

    private void checkBalances(String balances) {
        testClient.get().uri("/registers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(balances);
    }

    private void checkRegister(String registerId, BigDecimal balance) {
        testClient.get().uri("/registers/{registerId}", registerId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("""
                        {
                          "id": "%s",
                          "balance": %s
                        }
                        """.formatted(registerId, balance));
    }

    private void checkOperations(int expectedCount) {
        testClient.get().uri("/operations")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(OperationResponse.class)
                .hasSize(expectedCount);
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

    private void assertOperation(
            OperationResponse operation,
            long operationId,
            String sourceRegisterId,
            String targetRegisterId,
            int amount) {
        assertThat(operation)
                .isNotNull()
                .returns(operationId, OperationResponse::getId)
                .returns(BigDecimal.valueOf(amount), OperationResponse::getAmount)
                .returns(sourceRegisterId, OperationResponse::getSourceRegisterId)
                .returns(targetRegisterId, OperationResponse::getTargetRegisterId)
                .returns(OPERATION_TIMESTAMP, OperationResponse::getTimestamp);
    }
}
