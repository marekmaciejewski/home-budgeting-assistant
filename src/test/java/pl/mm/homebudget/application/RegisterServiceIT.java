package pl.mm.homebudget.application;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
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
            "test",
            "Idle"
    })
    void recharge_returnsNotFound(String registerId) {
        String actualMessage = testClient.post().uri("/registers/" + registerId + "/recharges")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":2500}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(actualMessage).endsWith(" register not found or not active");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"sourceRegisterId\":\"test\",\"targetRegisterId\":\"Food expenses\",\"amount\":1500}",
            "{\"sourceRegisterId\":\"Idle\",\"targetRegisterId\":\"Food expenses\",\"amount\":1500}",
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"test\",\"amount\":1500}",
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Idle\",\"amount\":1500}"
    })
    void transfer_returnsNotFound(String body) {
        String actualMessage = testClient.post().uri("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        assertThat(actualMessage).endsWith(" register not found or not active");
    }

    @Test
    void transfer_returnsBadRequest_whenSourceAndTargetRegisterAreTheSame() {
        String body = "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Wallet\",\"amount\":1500}";

        String actualMessage = testClient.post().uri("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertThat(actualMessage).isEqualTo("source and target register must be different");
    }
}
