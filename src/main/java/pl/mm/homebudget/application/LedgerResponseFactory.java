package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.mm.homebudget.api.dto.LedgerStatus;
import pl.mm.homebudget.api.dto.LedgerVerificationResponse;
import pl.mm.homebudget.api.dto.OperationProofResponse;
import pl.mm.homebudget.domain.LedgerHasher;
import pl.mm.homebudget.persistence.entity.Operation;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;

@Component
@RequiredArgsConstructor
class LedgerResponseFactory {

    private final LedgerHasher ledgerHasher;
    private final Clock clock;

    LedgerVerificationResponse createVerificationResponse(LedgerScanState state) {
        LedgerStatus status = state.isInvalid() ? LedgerStatus.INVALID : LedgerStatus.VERIFIED;
        return new LedgerVerificationResponse(
                status,
                state.operationCount(),
                state.verifiedThroughSequence(),
                state.latestSequenceNumber(),
                state.ledgerHeadHash(),
                OffsetDateTime.now(clock),
                state.mismatch());
    }

    OperationProofResponse createOperationProofResponse(Operation operation, String expectedPreviousHash) {
        ProofPayload proofPayload = calculateProofPayload(operation);
        String canonicalOperationHashInput = ledgerHasher.canonicalOperationHashInput(
                operation.getSequenceNumber(),
                expectedPreviousHash,
                proofPayload.expectedPayloadHash());
        String expectedOperationHash = ledgerHasher.operationHash(
                operation.getSequenceNumber(),
                expectedPreviousHash,
                proofPayload.expectedPayloadHash());
        boolean previousHashValid = Objects.equals(operation.getPreviousHash(), expectedPreviousHash);
        boolean operationHashValid = Objects.equals(operation.getOperationHash(), expectedOperationHash);

        return createOperationProofResponse(
                operation,
                proofPayload,
                canonicalOperationHashInput,
                expectedPreviousHash,
                expectedOperationHash,
                previousHashValid,
                operationHashValid);
    }

    OperationProofResponse createOperationProofResponseForMissingPreviousOperation(Operation operation) {
        return createOperationProofResponse(
                operation,
                calculateProofPayload(operation),
                null,
                null,
                null,
                false,
                false);
    }

    private ProofPayload calculateProofPayload(Operation operation) {
        String canonicalPayload = ledgerHasher.canonicalPayload(
                operation.getOperationType(),
                operation.getTimestamp(),
                operation.getAmount(),
                operation.getSourceRegisterId(),
                operation.getTargetRegisterId());
        String expectedPayloadHash = ledgerHasher.payloadHash(
                operation.getOperationType(),
                operation.getTimestamp(),
                operation.getAmount(),
                operation.getSourceRegisterId(),
                operation.getTargetRegisterId());
        boolean payloadHashValid = Objects.equals(operation.getPayloadHash(), expectedPayloadHash);

        return new ProofPayload(canonicalPayload, expectedPayloadHash, payloadHashValid);
    }

    private OperationProofResponse createOperationProofResponse(
            Operation operation,
            ProofPayload proofPayload,
            String canonicalOperationHashInput,
            String expectedPreviousHash,
            String expectedOperationHash,
            boolean previousHashValid,
            boolean operationHashValid) {
        return new OperationProofResponse(
                operation.getId(),
                operation.getOperationType(),
                operation.getTimestamp().atZone(clock.getZone()).toOffsetDateTime(),
                operation.getAmount(),
                operation.getSourceRegisterId(),
                operation.getTargetRegisterId(),
                operation.getSequenceNumber(),
                operation.getPreviousHash(),
                operation.getPayloadHash(),
                operation.getOperationHash(),
                proofPayload.canonicalPayload(),
                canonicalOperationHashInput,
                expectedPreviousHash,
                proofPayload.expectedPayloadHash(),
                expectedOperationHash,
                previousHashValid,
                proofPayload.payloadHashValid(),
                operationHashValid,
                previousHashValid && proofPayload.payloadHashValid() && operationHashValid);
    }

    private record ProofPayload(
            String canonicalPayload,
            String expectedPayloadHash,
            boolean payloadHashValid) {
    }
}
