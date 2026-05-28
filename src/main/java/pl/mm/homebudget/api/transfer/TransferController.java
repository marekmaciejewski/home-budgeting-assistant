package pl.mm.homebudget.api.transfer;

import pl.mm.homebudget.api.TransfersApi;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.TransferCommand;
import pl.mm.homebudget.application.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class TransferController implements TransfersApi {

    private final RegisterService service;

    @Override
    public Mono<OperationResponse> createTransfer(
            Mono<TransferCommand> transferCommand,
            ServerWebExchange exchange) {
        return transferCommand
                .flatMap(request -> service.transfer(
                        request.getSourceRegisterId(),
                        request.getTargetRegisterId(),
                        request.getAmount()))
                .doOnNext(response -> setCreatedOperationLocation(exchange, response));
    }

    private static void setCreatedOperationLocation(ServerWebExchange exchange, OperationResponse response) {
        exchange.getResponse()
                .getHeaders()
                .setLocation(URI.create("/operations/" + response.getId()));
    }
}
