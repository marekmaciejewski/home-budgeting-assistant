package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.mm.homebudget.domain.LedgerHasher;
import pl.mm.homebudget.persistence.OperationRepository;
import pl.mm.homebudget.persistence.entity.Operation;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OperationLedgerService {

    private final OperationRepository operationRepository;
    private final LedgerHasher ledgerHasher;

    public Mono<Operation> prepareForAppend(Operation operation) {
        return operationRepository.findFirstByOrderBySequenceNumberDesc()
                .map(head -> assign(operation, head.getSequenceNumber() + 1, head.getOperationHash()))
                .defaultIfEmpty(assign(operation, 1L, LedgerHasher.GENESIS_HASH));
    }

    private Operation assign(Operation operation, long sequenceNumber, String previousHash) {
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
