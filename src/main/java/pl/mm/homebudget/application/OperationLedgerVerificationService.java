package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.mm.homebudget.api.dto.LedgerVerificationResponse;
import pl.mm.homebudget.api.dto.OperationProofResponse;
import pl.mm.homebudget.domain.LedgerHasher;
import pl.mm.homebudget.domain.OperationNotFoundException;
import pl.mm.homebudget.persistence.OperationRepository;
import pl.mm.homebudget.persistence.entity.Operation;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OperationLedgerVerificationService {

    private final OperationRepository operationRepository;
    private final OperationLedgerVerifier operationLedgerVerifier;
    private final LedgerResponseFactory ledgerResponseFactory;

    public Mono<OperationProofResponse> getOperationProof(long operationId) {
        return operationRepository.findById(operationId)
                .switchIfEmpty(Mono.error(new OperationNotFoundException(operationId + " operation not found")))
                .flatMap(operation -> findExpectedPreviousHash(operation)
                        .map(expectedPreviousHash -> ledgerResponseFactory.createOperationProofResponse(
                                operation,
                                expectedPreviousHash))
                        .switchIfEmpty(Mono.fromSupplier(
                                () -> ledgerResponseFactory
                                        .createOperationProofResponseForMissingPreviousOperation(operation))));
    }

    public Mono<LedgerVerificationResponse> verifyLedger() {
        return operationLedgerVerifier.verifyOperations(operationRepository.findAllByOrderBySequenceNumberAsc())
                .map(ledgerResponseFactory::createVerificationResponse);
    }

    private Mono<String> findExpectedPreviousHash(Operation operation) {
        long sequenceNumber = operation.getSequenceNumber();
        if (sequenceNumber == 1) {
            return Mono.just(LedgerHasher.GENESIS_HASH);
        }
        return operationRepository.findBySequenceNumber(sequenceNumber - 1)
                .map(Operation::getOperationHash);
    }
}
