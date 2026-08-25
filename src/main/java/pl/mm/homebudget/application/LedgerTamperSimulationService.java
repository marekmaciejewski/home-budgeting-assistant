package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pl.mm.homebudget.api.dto.LedgerStatus;
import pl.mm.homebudget.api.dto.LedgerVerificationResponse;
import pl.mm.homebudget.api.dto.TamperLedgerCommand;
import pl.mm.homebudget.api.dto.TamperLedgerMode;
import pl.mm.homebudget.api.dto.TamperLedgerResponse;
import pl.mm.homebudget.domain.InvalidLedgerTamperCommandException;
import pl.mm.homebudget.domain.LedgerConflictException;
import pl.mm.homebudget.domain.OperationNotFoundException;
import pl.mm.homebudget.persistence.OperationRepository;
import pl.mm.homebudget.persistence.entity.Operation;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class LedgerTamperSimulationService {

    private static final String VERIFICATION_PATH = "/ledger/verify";
    private static final String RESET_PATH = "/demo/reset";

    private final OperationRepository operationRepository;
    private final OperationTamperer operationTamperer;
    private final OperationLedgerVerificationService verificationService;
    private final RuntimeCapabilityService runtimeCapabilityService;
    private final AtomicBoolean tamperInProgress = new AtomicBoolean();

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Mono<TamperLedgerResponse> simulateTamper(TamperLedgerCommand command) {
        return Mono.defer(() -> {
            if (!tamperInProgress.compareAndSet(false, true)) {
                return Mono.error(new LedgerConflictException(
                        "Another ledger tamper simulation is already in progress."));
            }

            return requireVerifiedNonEmptyLedger()
                    .then(selectTarget(command))
                    .flatMap(operation -> operationTamperer.tamper(
                            operation,
                            Objects.requireNonNullElse(command.getMode(), TamperLedgerMode.PAYLOAD_HASH)))
                    .flatMap(this::createVerifiedReceipt)
                    .doFinally(ignored -> tamperInProgress.set(false));
        });
    }

    private Mono<Void> requireVerifiedNonEmptyLedger() {
        return verificationService.verifyLedger()
                .flatMap(verification -> {
                    if (verification.getStatus() == LedgerStatus.INVALID) {
                        return Mono.error(new LedgerConflictException(
                                "Ledger is already invalid. Repair or reset it before simulating another mismatch."));
                    }
                    if (verification.getOperationCount() == 0) {
                        return Mono.error(new LedgerConflictException(
                                "Ledger is empty. Create an operation before simulating a mismatch."));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Operation> selectTarget(TamperLedgerCommand command) {
        Long operationId = command.getOperationId();
        Long sequenceNumber = command.getSequenceNumber();
        if (operationId != null && sequenceNumber != null) {
            return Mono.error(new InvalidLedgerTamperCommandException(
                    "operationId and sequenceNumber cannot be provided together"));
        }
        if (operationId != null) {
            return operationRepository.findById(operationId)
                    .switchIfEmpty(Mono.error(
                            new OperationNotFoundException(operationId + " operation not found")));
        }
        if (sequenceNumber != null) {
            return operationRepository.findBySequenceNumber(sequenceNumber)
                    .switchIfEmpty(Mono.error(new OperationNotFoundException(
                            sequenceNumber + " ledger sequence not found")));
        }
        return operationRepository.findFirstByOrderBySequenceNumberDesc()
                .switchIfEmpty(Mono.error(new LedgerConflictException(
                        "Ledger is empty. Create an operation before simulating a mismatch.")));
    }

    private Mono<TamperLedgerResponse> createVerifiedReceipt(LedgerTamperChange change) {
        return verificationService.verifyLedger()
                .flatMap(verification -> {
                    if (verification.getStatus() != LedgerStatus.INVALID || verification.getMismatch() == null) {
                        return Mono.error(new LedgerConflictException(
                                "Tamper simulation did not produce a detectable ledger mismatch."));
                    }
                    return Mono.just(toResponse(change, verification));
                });
    }

    private TamperLedgerResponse toResponse(
            LedgerTamperChange change,
            LedgerVerificationResponse verification) {
        boolean resetAvailable = runtimeCapabilityService.isEphemeral();
        TamperLedgerResponse response = new TamperLedgerResponse()
                .operationId(change.operation().getId())
                .sequenceNumber(change.operation().getSequenceNumber())
                .mode(change.mode())
                .field(change.field())
                .previousValue(change.previousValue())
                .newValue(change.newValue())
                .verificationStatus(verification.getStatus())
                .mismatch(verification.getMismatch())
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
