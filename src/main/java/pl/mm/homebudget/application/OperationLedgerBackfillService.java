package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.mm.homebudget.domain.LedgerHasher;
import pl.mm.homebudget.persistence.OperationRepository;
import pl.mm.homebudget.persistence.entity.Operation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Service
@RequiredArgsConstructor
public class OperationLedgerBackfillService {

    private final OperationRepository operationRepository;
    private final OperationLedgerAssigner operationLedgerAssigner;

    @Transactional
    public Flux<Operation> backfillIfNeeded() {
        return Mono.zip(
                        operationRepository.findFirstByOrderBySequenceNumberAsc(),
                        operationRepository.findFirstByOrderBySequenceNumberDesc())
                .flatMapMany(this::backfillIfNeeded);
    }

    private Flux<Operation> backfillIfNeeded(Tuple2<Operation, Operation> sequenceRange) {
        Long minSequenceNumber = sequenceRange.getT1().getSequenceNumber();
        Long maxSequenceNumber = sequenceRange.getT2().getSequenceNumber();
        if (minSequenceNumber > 0) {
            return Flux.empty();
        }
        if (maxSequenceNumber < 1) {
            return backfillAll();
        }
        return Flux.error(mixedBackfillState());
    }

    private Flux<Operation> backfillAll() {
        return operationRepository.findAllByOrderByTimestampAscIdAsc()
                .scan(genesisOperation(), this::assignAfter)
                .skip(1)
                .concatMap(operationRepository::save);
    }

    private Operation assignAfter(Operation previousOperation, Operation operation) {
        return operationLedgerAssigner.assign(
                operation,
                previousOperation.getSequenceNumber() + 1,
                previousOperation.getOperationHash());
    }

    private static IllegalStateException mixedBackfillState() {
        return new IllegalStateException("Operation ledger has a mixed backfill state");
    }

    private static Operation genesisOperation() {
        Operation operation = new Operation();
        operation.setSequenceNumber(0L);
        operation.setOperationHash(LedgerHasher.GENESIS_HASH);
        return operation;
    }
}
