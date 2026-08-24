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
    private final OperationLedgerAssigner operationLedgerAssigner;

    public Mono<Operation> prepareForAppend(Operation operation) {
        return operationRepository.findFirstByOrderBySequenceNumberDesc()
                .map(head -> operationLedgerAssigner.assign(
                        operation,
                        head.getSequenceNumber() + 1,
                        head.getOperationHash()))
                .switchIfEmpty(Mono.fromSupplier(() -> operationLedgerAssigner.assign(
                        operation,
                        1L,
                        LedgerHasher.GENESIS_HASH)));
    }
}
