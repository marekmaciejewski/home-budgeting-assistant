package pl.mm.homebudget;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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
        String[] balances = {
                "\"id\":\"Wallet\",\"balance\":1000.00",
                "\"id\":\"Savings\",\"balance\":5000.00",
                "\"id\":\"Insurance policy\",\"balance\":0.00",
                "\"id\":\"Food expenses\",\"balance\":0.00"
        };
        return Stream.of(dynamicTest("check initial balances", () -> checkBalances(balances)));
    }

    private Stream<DynamicNode> rechargeWallet() {
        String[] balances = {
                "\"id\":\"Wallet\",\"balance\":3500.00",
                "\"id\":\"Savings\",\"balance\":5000.00",
                "\"id\":\"Insurance policy\",\"balance\":0.00",
                "\"id\":\"Food expenses\",\"balance\":0.00"
        };
        return Stream.of(
                dynamicTest("recharge", () -> recharge("Wallet", 2500)),
                dynamicTest("check balances", () -> checkBalances(balances)));
    }

    private Stream<DynamicNode> transferFromWalletToFoodExpenses() {
        String[] balances = {
                "\"id\":\"Wallet\",\"balance\":2000.00",
                "\"id\":\"Savings\",\"balance\":5000.00",
                "\"id\":\"Insurance policy\",\"balance\":0.00",
                "\"id\":\"Food expenses\",\"balance\":1500.00"
        };
        return Stream.of(
                dynamicTest("transfer", () -> transfer("Wallet", "Food expenses", 1500)),
                dynamicTest("check balances", () -> checkBalances(balances)));
    }

    private Stream<DynamicNode> transferFromSavingsToInsurancePolicy() {
        String[] balances = {
                "\"id\":\"Wallet\",\"balance\":2000.00",
                "\"id\":\"Savings\",\"balance\":4500.00",
                "\"id\":\"Insurance policy\",\"balance\":500.00",
                "\"id\":\"Food expenses\",\"balance\":1500.00"
        };
        return Stream.of(
                dynamicTest("transfer", () -> transfer("Savings", "Insurance policy", 500)),
                dynamicTest("check balances", () -> checkBalances(balances)));
    }

    private Stream<DynamicNode> transferFromWalletToSavings() {
        String[] balances = {
                "\"id\":\"Wallet\",\"balance\":1000.00",
                "\"id\":\"Savings\",\"balance\":5500.00",
                "\"id\":\"Insurance policy\",\"balance\":500.00",
                "\"id\":\"Food expenses\",\"balance\":1500.00"
        };
        return Stream.of(
                dynamicTest("transfer", () -> transfer("Wallet", "Savings", 1000)),
                dynamicTest("check final balances", () -> checkBalances(balances)));
    }

    private void recharge(String register, int amount) {
        String body = String.format("{\"amount\":%d}", amount);
        executeOperation(body, "/registers/" + register + "/recharges", null, register);
    }

    private void transfer(String source, String target, int amount) {
        String body = String
                .format("{\"sourceRegisterId\":\"%s\",\"targetRegisterId\":\"%s\",\"amount\":%d}", source, target, amount);
        executeOperation(body, "/transfers", source, target);
    }

    private void executeOperation(String body, String path, String sourceRegisterId, String targetRegisterId) {
        var result = testClient.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", "/operations/\\d+")
                .expectBody(String.class)
                .returnResult();
        String response = result.getResponseBody();
        assertThat(response)
                .contains("\"sourceRegisterId\":" + nullableJson(sourceRegisterId))
                .contains("\"targetRegisterId\":\"" + targetRegisterId + "\"");

        String location = result.getResponseHeaders().getLocation().toString();
        testClient.get().uri(location)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").exists();
    }

    private void checkBalances(String[] balances) {
        String actualPrintout = testClient.get().uri("/registers")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(actualPrintout).contains(balances);
    }

    private String nullableJson(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value + "\"";
    }
}
