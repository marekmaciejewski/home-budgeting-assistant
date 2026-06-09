package pl.mm.homebudget.api.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.mm.homebudget.api.DemoApi;
import pl.mm.homebudget.api.dto.RegisterResponse;
import pl.mm.homebudget.application.DemoResetService;
import reactor.core.publisher.Flux;

@RestController
@Profile("demo")
@RequiredArgsConstructor
public class DemoController implements DemoApi {

    private final DemoResetService service;

    @Override
    public Flux<RegisterResponse> resetDemo(ServerWebExchange exchange) {
        return service.resetDemo();
    }
}
