package com.solera.budgeting;

import com.solera.budgeting.entities.Operation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationRepository extends ReactiveCrudRepository<Operation, Long> {
}
