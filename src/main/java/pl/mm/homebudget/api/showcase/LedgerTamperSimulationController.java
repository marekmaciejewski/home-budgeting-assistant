package pl.mm.homebudget.api.showcase;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import pl.mm.homebudget.api.ShowcaseApi;
import pl.mm.homebudget.api.dto.TamperLedgerCommand;
import pl.mm.homebudget.api.dto.TamperLedgerResponse;
import pl.mm.homebudget.application.LedgerTamperSimulationService;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnProperty(
        name = "app.features.ledger-tamper-simulation-enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class LedgerTamperSimulationController implements ShowcaseApi {

    private final LedgerTamperSimulationService ledgerTamperSimulationService;

    @Override
    public Mono<TamperLedgerResponse> simulateLedgerTamper(
            Mono<TamperLedgerCommand> tamperLedgerCommand,
            ServerWebExchange exchange) {
        return tamperLedgerCommand.flatMap(ledgerTamperSimulationService::simulateTamper);
    }
}
