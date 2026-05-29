package pl.mm.homebudget.application;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RegisterServiceIT {

    private static final ParameterizedTypeReference<Map<String, Object>> PROBLEM_TYPE =
            new ParameterizedTypeReference<>() {
            };

    @Autowired
    private WebTestClient testClient;

    @ParameterizedTest
    @ValueSource(strings = {
            "test",
            "Idle"
    })
    void recharge_returnsNotFound(String registerId) {
        Map<String, Object> problem = testClient.post().uri("/registers/" + registerId + "/recharges")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":2500}")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody(PROBLEM_TYPE)
                .returnResult()
                .getResponseBody();

        assertProblem(problem, HttpStatus.NOT_FOUND, "/registers/" + registerId + "/recharges");
        assertThat((String) problem.get("detail")).endsWith(" register not found or not active");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"sourceRegisterId\":\"test\",\"targetRegisterId\":\"Food expenses\",\"amount\":1500}",
            "{\"sourceRegisterId\":\"Idle\",\"targetRegisterId\":\"Food expenses\",\"amount\":1500}",
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"test\",\"amount\":1500}",
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Idle\",\"amount\":1500}"
    })
    void transfer_returnsNotFound(String body) {
        Map<String, Object> problem = testClient.post().uri("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody(PROBLEM_TYPE)
                .returnResult()
                .getResponseBody();

        assertProblem(problem, HttpStatus.NOT_FOUND, "/transfers");
        assertThat((String) problem.get("detail")).endsWith(" register not found or not active");
    }

    @Test
    void transfer_returnsBadRequest_whenSourceAndTargetRegisterAreTheSame() {
        String body = "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Wallet\",\"amount\":1500}";

        Map<String, Object> problem = testClient.post().uri("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody(PROBLEM_TYPE)
                .returnResult()
                .getResponseBody();

        assertProblem(problem, HttpStatus.BAD_REQUEST, "/transfers");
        assertThat(problem).containsEntry("detail", "source and target register must be different");
    }

    @Test
    void transfer_returnsBadRequest_whenSourceRegisterBalanceIsInsufficient() {
        String body = "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Savings\",\"amount\":999999}";

        Map<String, Object> problem = testClient.post().uri("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody(PROBLEM_TYPE)
                .returnResult()
                .getResponseBody();

        assertProblem(problem, HttpStatus.BAD_REQUEST, "/transfers");
        assertThat(problem).containsEntry("detail", "Wallet register has insufficient balance");
    }

    @Test
    void getOperation_returnsNotFound() {
        Map<String, Object> problem = testClient.get().uri("/operations/{operationId}", 999L)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody(PROBLEM_TYPE)
                .returnResult()
                .getResponseBody();

        assertProblem(problem, HttpStatus.NOT_FOUND, "/operations/999");
        assertThat(problem).containsEntry("detail", "999 operation not found");
    }

    private static void assertProblem(Map<String, Object> problem, HttpStatus status, String instance) {
        assertThat(problem)
                .containsEntry("title", status.getReasonPhrase())
                .containsEntry("status", status.value())
                .containsEntry("instance", instance);
    }
}
