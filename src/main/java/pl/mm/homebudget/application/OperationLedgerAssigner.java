package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.mm.homebudget.domain.LedgerHasher;
import pl.mm.homebudget.persistence.entity.Operation;

@Component
@RequiredArgsConstructor
class OperationLedgerAssigner {

    private final LedgerHasher ledgerHasher;

    Operation assign(
            Operation operation,
            long sequenceNumber,
            String previousHash) {
        String payloadHash = ledgerHasher.payloadHash(
                operation.getOperationType(),
                operation.getTimestamp(),
                operation.getAmount(),
                operation.getSourceRegisterId(),
                operation.getTargetRegisterId());
        String operationHash = ledgerHasher.operationHash(sequenceNumber, previousHash, payloadHash);

        operation.setSequenceNumber(sequenceNumber);
        operation.setPreviousHash(previousHash);
        operation.setPayloadHash(payloadHash);
        operation.setOperationHash(operationHash);
        return operation;
    }
}
