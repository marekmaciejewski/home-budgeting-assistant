package pl.mm.homebudget.api.ledger;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.mm.homebudget.api.LedgerApi;
import pl.mm.homebudget.api.dto.LedgerVerificationResponse;
import pl.mm.homebudget.api.dto.OperationProofResponse;
import pl.mm.homebudget.application.OperationLedgerVerificationService;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class LedgerController implements LedgerApi {

    private final OperationLedgerVerificationService operationLedgerVerificationService;

    @Override
    public Mono<OperationProofResponse> getOperationProof(Long operationId, ServerWebExchange exchange) {
        return operationLedgerVerificationService.getOperationProof(operationId);
    }

    @Override
    public Mono<LedgerVerificationResponse> verifyLedger(ServerWebExchange exchange) {
        return operationLedgerVerificationService.verifyLedger();
    }
}
