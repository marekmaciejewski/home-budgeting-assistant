package pl.mm.homebudget.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.cors.allowed-origins=http://localhost:5173")
@AutoConfigureWebTestClient
class CorsConfigIT {

    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private WebTestClient testClient;

    @Test
    void preflight_allowsConfiguredOrigin() {
        testClient.options().uri("/registers")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN)
                .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, methods -> {
                    assertThat(methods).contains("GET");
                    assertThat(methods).contains("POST");
                });
    }

    @Test
    void preflight_rejectsUnconfiguredOrigin() {
        testClient.options().uri("/registers")
                .header(HttpHeaders.ORIGIN, "https://example.invalid")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isForbidden();
    }
}
