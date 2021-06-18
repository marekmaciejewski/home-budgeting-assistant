package com.solera.budgeting;

import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RegisterDbIT {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
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
        String[] balances = {"Wallet: 1000.00", "Savings: 5000.00", "Insurance policy: 0.00", "Food expenses: 0.00"};
        return Stream.of(dynamicTest("check initial balances", () -> checkBalances(balances)));
    }

    private Stream<DynamicNode> rechargeWallet() {
        String[] balances = {"Wallet: 3500.00", "Savings: 5000.00", "Insurance policy: 0.00", "Food expenses: 0.00"};
        return Stream.of(
                dynamicTest("recharge", () -> recharge("Wallet", 2500)),
                dynamicTest("check balances", () -> checkBalances(balances)));
    }

    private Stream<DynamicNode> transferFromWalletToFoodExpenses() {
        String[] balances = {"Wallet: 2000.00", "Savings: 5000.00", "Insurance policy: 0.00", "Food expenses: 1500.00"};
        return Stream.of(
                dynamicTest("transfer", () -> transfer("Wallet", "Food expenses", 1500)),
                dynamicTest("check balances", () -> checkBalances(balances)));
    }

    private Stream<DynamicNode> transferFromSavingsToInsurancePolicy() {
        String[] balances = {"Wallet: 2000.00", "Savings: 4500.00", "Insurance policy: 500.00", "Food expenses: 1500.00"};
        return Stream.of(
                dynamicTest("transfer", () -> transfer("Savings", "Insurance policy", 500)),
                dynamicTest("check balances", () -> checkBalances(balances)));
    }

    private Stream<DynamicNode> transferFromWalletToSavings() {
        String[] balances = {"Wallet: 1000.00", "Savings: 5500.00", "Insurance policy: 500.00", "Food expenses: 1500.00"};
        return Stream.of(
                dynamicTest("transfer", () -> transfer("Wallet", "Savings", 1000)),
                dynamicTest("check final balances", () -> checkBalances(balances)));
    }

    private void recharge(String register, int amount) {
        String body = String.format("{\"registerName\":\"%s\",\"amount\":%d}", register, amount);
        executeOperation(body, "/registers/recharge");
    }

    private void transfer(String source, String target, int amount) {
        String body = String
                .format("{\"sourceRegister\":\"%s\",\"targetRegister\":\"%s\",\"amount\":%d}", source, target, amount);
        executeOperation(body, "/registers/transfer");
    }

    private void executeOperation(String body, String path) {
        given()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .when()
                .post(path)
                .then()
                .assertThat()
                .statusCode(200)
                .body(Matchers.emptyString());
    }

    private void checkBalances(String[] balances) {
        String actualPrintout = given()
                .when()
                .get("/registers")
                .then()
                .assertThat()
                .statusCode(200)
                .extract().body().asString();
        assertThat(actualPrintout).contains(balances);
    }
}
