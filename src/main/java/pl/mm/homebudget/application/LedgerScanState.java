package pl.mm.homebudget.application;

import pl.mm.homebudget.api.dto.LedgerMismatch;
import pl.mm.homebudget.domain.LedgerHasher;
import pl.mm.homebudget.persistence.entity.Operation;

record LedgerScanState(
        long operationCount,
        long latestSequenceNumber,
        String ledgerHeadHash,
        long expectedSequenceNumber,
        String expectedPreviousHash,
        long verifiedThroughSequence,
        LedgerMismatch mismatch) {

    static LedgerScanState createInitial() {
        return new LedgerScanState(
                0,
                0,
                LedgerHasher.GENESIS_HASH,
                1,
                LedgerHasher.GENESIS_HASH,
                0,
                null);
    }

    boolean isInvalid() {
        return mismatch != null;
    }

    LedgerScanState recordVerifiedOperation(Operation operation) {
        long sequenceNumber = operation.getSequenceNumber();
        return new LedgerScanState(
                operationCount + 1,
                sequenceNumber,
                operation.getOperationHash(),
                sequenceNumber + 1,
                operation.getOperationHash(),
                sequenceNumber,
                null);
    }

    LedgerScanState recordMismatch(Operation operation, LedgerMismatch detectedMismatch) {
        return new LedgerScanState(
                operationCount + 1,
                operation.getSequenceNumber(),
                operation.getOperationHash(),
                expectedSequenceNumber,
                expectedPreviousHash,
                verifiedThroughSequence,
                detectedMismatch);
    }

    LedgerScanState observeOperationAfterMismatch(Operation operation) {
        return new LedgerScanState(
                operationCount + 1,
                operation.getSequenceNumber(),
                operation.getOperationHash(),
                expectedSequenceNumber,
                expectedPreviousHash,
                verifiedThroughSequence,
                mismatch);
    }
}
