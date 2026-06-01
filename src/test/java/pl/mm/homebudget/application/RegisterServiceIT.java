package pl.mm.homebudget.application;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

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
        String body = "{\"registerId\":\"" + registerId + "\",\"amount\":2500}";
        String expectedProblem = problemJson(
                HttpStatus.NOT_FOUND,
                registerId + " register not found or not active",
                "/operations/recharges");

        testClient.post().uri("/operations/recharges")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .json(expectedProblem);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, textBlock = """
            sourceRegisterId, targetRegisterId, missingRegisterId
            test,             Food expenses,    test
            Idle,             Food expenses,    Idle
            Wallet,           test,             test
            Wallet,           Idle,             Idle
            """)
    void transfer_returnsNotFound(String sourceRegisterId, String targetRegisterId, String missingRegisterId) {
        String body = "{\"sourceRegisterId\":\"%s\",\"targetRegisterId\":\"%s\",\"amount\":1500}"
                .formatted(sourceRegisterId, targetRegisterId);
        String expectedProblem = problemJson(
                HttpStatus.NOT_FOUND,
                missingRegisterId + " register not found or not active",
                "/operations/transfers");

        testClient.post().uri("/operations/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .json(expectedProblem);
    }

    @Test
    void transfer_returnsBadRequest_whenSourceAndTargetRegisterAreTheSame() {
        String body = "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Wallet\",\"amount\":1500}";
        String expectedProblem = problemJson(
                HttpStatus.BAD_REQUEST,
                "source and target register must be different",
                "/operations/transfers");

        testClient.post().uri("/operations/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .json(expectedProblem);
    }

    @Test
    void transfer_returnsBadRequest_whenSourceRegisterBalanceIsInsufficient() {
        String body = "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Savings\",\"amount\":999999}";
        String expectedProblem = problemJson(
                HttpStatus.BAD_REQUEST,
                "Wallet register has insufficient balance",
                "/operations/transfers");

        testClient.post().uri("/operations/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .json(expectedProblem);
    }

    @Test
    void getOperation_returnsNotFound() {
        String expectedProblem = problemJson(
                HttpStatus.NOT_FOUND,
                "999 operation not found",
                "/operations/999");

        testClient.get().uri("/operations/{operationId}", 999L)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .json(expectedProblem);
    }

    private static String problemJson(HttpStatus status, String detail, String instance) {
        return """
                {
                  "title": "%s",
                  "status": %d,
                  "detail": "%s",
                  "instance": "%s"
                }
                """.formatted(status.getReasonPhrase(), status.value(), detail, instance);
    }
}
