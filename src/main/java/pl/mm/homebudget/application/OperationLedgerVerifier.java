package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.mm.homebudget.api.dto.LedgerMismatch;
import pl.mm.homebudget.domain.LedgerHasher;
import pl.mm.homebudget.persistence.entity.Operation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
@RequiredArgsConstructor
class OperationLedgerVerifier {

    private static final String FIRST_SEQUENCE_MISMATCH = "Ledger sequence must start at 1.";
    private static final String PREVIOUS_HASH_MISMATCH =
            "Stored previous hash does not match the expected previous hash.";
    private static final String PAYLOAD_HASH_MISMATCH =
            "Stored payload hash does not match the recalculated payload hash.";
    private static final String OPERATION_HASH_MISMATCH =
            "Stored operation hash does not match the recalculated operation hash.";

    private final LedgerHasher ledgerHasher;

    Mono<LedgerScanState> verifyOperations(Flux<Operation> operations) {
        return operations.reduceWith(LedgerScanState::createInitial, this::verifyNext);
    }

    private LedgerScanState verifyNext(LedgerScanState state, Operation operation) {
        if (state.isInvalid()) {
            return state.observeOperationAfterMismatch(operation);
        }

        long sequenceNumber = operation.getSequenceNumber();
        if (sequenceNumber != state.expectedSequenceNumber()) {
            return state.recordMismatch(
                    operation,
                    createSequenceMismatch(operation, state.expectedSequenceNumber(), sequenceNumber));
        }

        if (!Objects.equals(operation.getPreviousHash(), state.expectedPreviousHash())) {
            return state.recordMismatch(
                    operation,
                    createMismatch(sequenceNumber, operation.getId(), PREVIOUS_HASH_MISMATCH));
        }

        String expectedPayloadHash = calculateExpectedPayloadHash(operation);
        if (!Objects.equals(operation.getPayloadHash(), expectedPayloadHash)) {
            return state.recordMismatch(
                    operation,
                    createMismatch(sequenceNumber, operation.getId(), PAYLOAD_HASH_MISMATCH));
        }

        String expectedOperationHash = calculateExpectedOperationHash(
                sequenceNumber,
                state.expectedPreviousHash(),
                expectedPayloadHash);
        if (!Objects.equals(operation.getOperationHash(), expectedOperationHash)) {
            return state.recordMismatch(
                    operation,
                    createMismatch(sequenceNumber, operation.getId(), OPERATION_HASH_MISMATCH));
        }

        return state.recordVerifiedOperation(operation);
    }

    private LedgerMismatch createSequenceMismatch(
            Operation operation,
            long expectedSequenceNumber,
            long storedSequenceNumber) {
        if (storedSequenceNumber > expectedSequenceNumber) {
            return createMismatch(
                    expectedSequenceNumber,
                    null,
                    "Missing operation for sequence " + expectedSequenceNumber + ".");
        }
        if (expectedSequenceNumber == 1) {
            return createMismatch(expectedSequenceNumber, operation.getId(), FIRST_SEQUENCE_MISMATCH);
        }
        return createMismatch(
                expectedSequenceNumber,
                operation.getId(),
                "Expected ledger sequence " + expectedSequenceNumber + " but found " + storedSequenceNumber + ".");
    }

    private String calculateExpectedPayloadHash(Operation operation) {
        return ledgerHasher.payloadHash(
                operation.getOperationType(),
                operation.getTimestamp(),
                operation.getAmount(),
                operation.getSourceRegisterId(),
                operation.getTargetRegisterId());
    }

    private String calculateExpectedOperationHash(
            long sequenceNumber,
            String expectedPreviousHash,
            String expectedPayloadHash) {
        return ledgerHasher.operationHash(
                sequenceNumber,
                expectedPreviousHash,
                expectedPayloadHash);
    }

    private LedgerMismatch createMismatch(long sequenceNumber, Long operationId, String reason) {
        return new LedgerMismatch(sequenceNumber, reason).operationId(operationId);
    }
}
