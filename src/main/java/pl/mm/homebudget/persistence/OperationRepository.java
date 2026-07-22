package pl.mm.homebudget.persistence;

import pl.mm.homebudget.persistence.entity.Operation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OperationRepository extends ReactiveCrudRepository<Operation, Long> {

    Flux<Operation> findAllByOrderByTimestampAscIdAsc();

    Flux<Operation> findAllByOrderBySequenceNumberAsc();

    Mono<Operation> findFirstByOrderBySequenceNumberAsc();

    Mono<Operation> findFirstByOrderBySequenceNumberDesc();
}
