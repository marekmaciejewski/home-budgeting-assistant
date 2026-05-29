package pl.mm.homebudget;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RegisterDbIT {

    @Autowired
    private WebTestClient testClient;

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
        String body = String.format("{\"amount\":%d}", amount);
        executeOperation(body, "/registers/" + register + "/recharges", null, register, amount);
    }

    private void transfer(String source, String target, int amount) {
        String body = String
                .format("{\"sourceRegisterId\":\"%s\",\"targetRegisterId\":\"%s\",\"amount\":%d}", source, target, amount);
        executeOperation(body, "/transfers", source, target, amount);
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
                .expectBody()
                .json(expectedOperationJson(sourceRegisterId, targetRegisterId, amount), JsonCompareMode.LENIENT)
                .returnResult();

        String location = result.getResponseHeaders().getLocation().toString();
        testClient.get().uri(location)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").exists();
    }

    private void checkBalances(String balances) {
        testClient.get().uri("/registers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(balances, JsonCompareMode.LENIENT);
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
                        """.formatted(registerId, balance), JsonCompareMode.LENIENT);
    }

    private void checkOperations(int expectedCount) {
        testClient.get().uri("/operations")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(expectedCount);
    }

    private String nullableJson(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value + "\"";
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

    private String expectedOperationJson(String sourceRegisterId, String targetRegisterId, int amount) {
        return """
                {
                  "amount": %d,
                  "sourceRegisterId": %s,
                  "targetRegisterId": "%s"
                }
                """.formatted(amount, nullableJson(sourceRegisterId), targetRegisterId);
    }
}
