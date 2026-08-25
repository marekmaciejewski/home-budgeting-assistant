package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mm.homebudget.api.dto.LedgerStatus;
import pl.mm.homebudget.api.dto.LedgerVerificationResponse;
import pl.mm.homebudget.api.dto.TamperLedgerCommand;
import pl.mm.homebudget.api.dto.TamperLedgerResponse;
import pl.mm.homebudget.domain.LedgerConflictException;
import pl.mm.homebudget.domain.OperationNotFoundException;
import pl.mm.homebudget.persistence.OperationRepository;
import pl.mm.homebudget.persistence.entity.Operation;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class LedgerTamperSimulationService {

    private final OperationRepository operationRepository;
    private final OperationTamperer operationTamperer;
    private final OperationLedgerVerificationService verificationService;
    private final TamperLedgerResponseAssembler responseAssembler;

    @Transactional
    public Mono<TamperLedgerResponse> simulateTamper(TamperLedgerCommand command) {
        return requireVerifiedNonEmptyLedger()
                .then(selectTarget(command))
                .flatMap(operation -> operationTamperer.tamper(operation, command.getMode()))
                .map(responseAssembler::assemble);
    }

    private Mono<LedgerVerificationResponse> requireVerifiedNonEmptyLedger() {
        return verificationService.verifyLedger()
                .doOnNext(verification -> {
                    if (verification.getStatus() == LedgerStatus.INVALID) {
                        throw new LedgerConflictException(
                                "Ledger is already invalid. Repair or reset it before simulating another mismatch.");
                    }
                    if (verification.getOperationCount() == 0) {
                        throw new LedgerConflictException(
                                "Ledger is empty. Create an operation before simulating a mismatch.");
                    }
                });
    }

    private Mono<Operation> selectTarget(TamperLedgerCommand command) {
        return Mono.fromSupplier(command::getSequenceNumber)
                .flatMap(sequenceNumber -> operationRepository.findBySequenceNumber(sequenceNumber)
                        .switchIfEmpty(Mono.error(new OperationNotFoundException(
                                sequenceNumber + " ledger sequence not found"))))
                .switchIfEmpty(operationRepository.findFirstByOrderBySequenceNumberDesc());
    }
}
