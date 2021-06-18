package com.solera.budgeting;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RegisterServiceIT {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"registerName\":\"test\",\"amount\":2500}",
            "{\"registerName\":\"Idle\",\"amount\":2500}"
    })
    void recharge_returnsNotFound(String body) {
        String actualMessage = given()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .when()
                .post("/registers/recharge")
                .then()
                .assertThat()
                .statusCode(404)
                .extract().body().asString();
        assertThat(actualMessage).endsWith(" register not found or not active");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"sourceRegister\":\"test\",\"targetRegister\":\"Food expenses\",\"amount\":1500}",
            "{\"sourceRegister\":\"Idle\",\"targetRegister\":\"Food expenses\",\"amount\":1500}",
            "{\"sourceRegister\":\"Wallet\",\"targetRegister\":\"test\",\"amount\":1500}",
            "{\"sourceRegister\":\"Wallet\",\"targetRegister\":\"Idle\",\"amount\":1500}"
    })
    void transfer_returnsNotFound(String body) {
        String actualMessage = given()
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .when()
                .post("/registers/transfer")
                .then()
                .assertThat()
                .statusCode(404)
                .extract().body().asString();
        assertThat(actualMessage).endsWith(" register not found or not active");
    }
}
