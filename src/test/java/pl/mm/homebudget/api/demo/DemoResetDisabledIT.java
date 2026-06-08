package pl.mm.homebudget.api.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class DemoResetDisabledIT {

    @Autowired
    private WebTestClient testClient;

    @Test
    void resetDemo_isNotExposedWithoutDemoProfile() {
        testClient.post().uri("/demo/reset")
                .exchange()
                .expectStatus().isNotFound();
    }
}
