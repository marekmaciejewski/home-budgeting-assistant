package com.solera.budgeting;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RegisterServiceIT {

    @Autowired
    private WebTestClient testClient;

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"registerName\":\"test\",\"amount\":2500}",
            "{\"registerName\":\"Idle\",\"amount\":2500}"
    })
    void recharge_returnsNotFound(String body) {
        String actualMessage = testClient.post().uri("/registers/recharge")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
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
        String actualMessage = testClient.post().uri("/registers/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(actualMessage).endsWith(" register not found or not active");
    }
}
