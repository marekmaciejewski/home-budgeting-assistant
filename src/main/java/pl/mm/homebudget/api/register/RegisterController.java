package pl.mm.homebudget.api.register;

import pl.mm.homebudget.api.RegistersApi;
import pl.mm.homebudget.api.dto.RegisterResponse;
import pl.mm.homebudget.application.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class RegisterController implements RegistersApi {

    private final RegisterService service;

    @Override
    public Flux<RegisterResponse> getRegisters(ServerWebExchange exchange) {
        return service.getRegisters();
    }

    @Override
    public Mono<RegisterResponse> getRegister(String registerId, ServerWebExchange exchange) {
        return service.getRegister(registerId);
    }
}
