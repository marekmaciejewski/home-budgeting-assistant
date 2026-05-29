package pl.mm.homebudget.api.register;

import pl.mm.homebudget.api.RegistersApi;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.RechargeCommand;
import pl.mm.homebudget.api.dto.RegisterResponse;
import pl.mm.homebudget.application.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

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

    @Override
    public Mono<OperationResponse> createRecharge(
            String registerId,
            Mono<RechargeCommand> rechargeCommand,
            ServerWebExchange exchange) {
        return rechargeCommand
                .flatMap(request -> service.recharge(registerId, request.getAmount()))
                .doOnNext(response -> setCreatedOperationLocation(exchange, response));
    }

    private static void setCreatedOperationLocation(ServerWebExchange exchange, OperationResponse response) {
        exchange.getResponse()
                .getHeaders()
                .setLocation(URI.create("/operations/" + response.getId()));
    }
}
