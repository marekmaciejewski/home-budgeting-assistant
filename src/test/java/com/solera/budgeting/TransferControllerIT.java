package com.solera.budgeting;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@MockitoBean(types = RegisterService.class)
@WebFluxTest(TransferController.class)
class TransferControllerIT {

    @Autowired
    private WebTestClient testClient;

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"sourceRegisterId\":null,\"targetRegisterId\":\"Food expenses\",\"amount\":1500}",
            "{\"sourceRegisterId\":\"\",\"targetRegisterId\":\"Food expenses\",\"amount\":1500}",
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":null,\"amount\":1500}",
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"\",\"amount\":1500}",
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Food expenses\",\"amount\":null}",
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Food expenses\",\"amount\":0}",
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Food expenses\",\"amount\":-1500}"
    })
    void createTransfer_returnsBadRequest(String payload) {
        testClient
                .post().uri("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.timestamp").exists()
                .jsonPath("$.path").isEqualTo("/transfers")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request");
    }
}
