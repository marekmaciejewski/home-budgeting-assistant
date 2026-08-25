package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.mm.homebudget.api.dto.TamperLedgerResponse;

@Component
@RequiredArgsConstructor
class TamperLedgerResponseAssembler {

    private static final String VERIFICATION_PATH = "/ledger/verify";
    private static final String RESET_PATH = "/demo/reset";

    private final RuntimeCapabilityService runtimeCapabilityService;

    TamperLedgerResponse assemble(LedgerTamperChange change) {
        boolean resetAvailable = runtimeCapabilityService.isEphemeral();
        TamperLedgerResponse response = new TamperLedgerResponse()
                .operationId(change.operation().getId())
                .sequenceNumber(change.operation().getSequenceNumber())
                .mode(change.mode())
                .field(change.field())
                .previousValue(change.previousValue())
                .newValue(change.newValue())
                .verificationPath(VERIFICATION_PATH)
                .message(resetAvailable
                        ? "Ledger mismatch simulated. Copy the change details or reset the demo."
                        : "Ledger mismatch simulated. Copy the change details for manual repair.");
        if (resetAvailable) {
            response.setResetPath(RESET_PATH);
        }
        return response;
    }
}
