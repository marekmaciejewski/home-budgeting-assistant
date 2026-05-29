package pl.mm.homebudget.api.register;

import pl.mm.homebudget.api.error.GlobalExceptionHandler;
import pl.mm.homebudget.application.RegisterService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@MockitoBean(types = RegisterService.class)
@WebFluxTest(RegisterController.class)
@Import(GlobalExceptionHandler.class)
class RegisterControllerIT {

    @Autowired
    private WebTestClient testClient;

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"amount\":null}",
            "{\"amount\":0}",
            "{\"amount\":-2500}",
            "{\"amount\":1.001}",
            "{\"amount\":1.230}",
            "{\"amount\":100000000000000000}"
    })
    void createRecharge_returnsBadRequest(String payload) {
        testClient
                .post().uri("/registers/Wallet/recharges")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Request validation failed")
                .jsonPath("$.instance").isEqualTo("/registers/Wallet/recharges")
                .jsonPath("$.errors[0].field").isEqualTo("amount")
                .jsonPath("$.errors[0].message").exists();
    }
}
