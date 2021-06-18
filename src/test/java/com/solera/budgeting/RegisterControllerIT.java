package com.solera.budgeting;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@MockBean(RegisterService.class)
@WebFluxTest(RegisterController.class)
class RegisterControllerIT {

    @Autowired
    private WebTestClient testClient;

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"registerName\":null,\"amount\":2500}",
            "{\"registerName\":\"\",\"amount\":2500}",
            "{\"registerName\":\"Wallet\",\"amount\":0}",
            "{\"registerName\":\"Wallet\",\"amount\":-2500}"
    })
    void recharge_returnsBadRequest(String payload) {
        testClient
                .post().uri("/registers/recharge")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.path").isEqualTo("/registers/recharge")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"sourceRegister\":null,\"targetRegister\":\"Food expenses\",\"amount\":1500}",
            "{\"sourceRegister\":\"\",\"targetRegister\":\"Food expenses\",\"amount\":1500}",
            "{\"sourceRegister\":\"Wallet\",\"targetRegister\":null,\"amount\":1500}",
            "{\"sourceRegister\":\"Wallet\",\"targetRegister\":\"\",\"amount\":1500}",
            "{\"sourceRegister\":\"Wallet\",\"targetRegister\":\"Food expenses\",\"amount\":0}",
            "{\"sourceRegister\":\"Wallet\",\"targetRegister\":\"Food expenses\",\"amount\":-1500}"
    })
    void transfer_returnsBadRequest(String payload) {
        testClient
                .post().uri("/registers/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.path").isEqualTo("/registers/transfer")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request");
    }
}
