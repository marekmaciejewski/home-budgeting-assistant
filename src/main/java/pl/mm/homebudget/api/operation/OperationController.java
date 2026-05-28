package pl.mm.homebudget.api.operation;

import pl.mm.homebudget.api.OperationsApi;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.application.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class OperationController implements OperationsApi {

    private final RegisterService service;

    @Override
    public Flux<OperationResponse> getOperations(ServerWebExchange exchange) {
        return service.getOperations();
    }

    @Override
    public Mono<OperationResponse> getOperation(Long operationId, ServerWebExchange exchange) {
        return service.getOperation(operationId);
    }
}
