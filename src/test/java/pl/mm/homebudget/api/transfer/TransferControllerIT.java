package pl.mm.homebudget.api.transfer;

import pl.mm.homebudget.api.error.GlobalExceptionHandler;
import pl.mm.homebudget.application.RegisterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@MockitoBean(types = RegisterService.class)
@WebFluxTest(TransferController.class)
@Import(GlobalExceptionHandler.class)
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
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Food expenses\",\"amount\":-1500}",
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Food expenses\",\"amount\":1.001}",
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Food expenses\",\"amount\":1.230}",
            "{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Food expenses\",\"amount\":100000000000000000}"
    })
    void createTransfer_returnsBadRequest(String payload) {
        testClient
                .post().uri("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Request validation failed")
                .jsonPath("$.instance").isEqualTo("/transfers")
                .jsonPath("$.errors[0].field").exists()
                .jsonPath("$.errors[0].message").exists();
    }

    @Test
    void createTransfer_returnsBadRequest_whenBodyIsMalformed() {
        testClient
                .post().uri("/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Request body is invalid")
                .jsonPath("$.instance").isEqualTo("/transfers")
                .jsonPath("$.errors").doesNotExist();
    }
}
