package pl.mm.homebudget.persistence;

import pl.mm.homebudget.persistence.entity.Operation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationRepository extends ReactiveCrudRepository<Operation, Long> {
}
