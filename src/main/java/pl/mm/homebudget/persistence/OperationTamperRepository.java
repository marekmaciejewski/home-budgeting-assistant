package pl.mm.homebudget.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class OperationTamperRepository {

    private final DatabaseClient databaseClient;

    public Mono<Long> updateAmount(long operationId, BigDecimal previousValue, BigDecimal newValue) {
        return updateField("""
                UPDATE OPERATIONS
                   SET AMOUNT = :newValue
                 WHERE ID = :operationId
                   AND AMOUNT = :previousValue
                """, operationId, previousValue, newValue);
    }

    public Mono<Long> updatePreviousHash(long operationId, String previousValue, String newValue) {
        return updateField("""
                UPDATE OPERATIONS
                   SET PREVIOUS_HASH = :newValue
                 WHERE ID = :operationId
                   AND PREVIOUS_HASH = :previousValue
                """, operationId, previousValue, newValue);
    }

    public Mono<Long> updatePayloadHash(long operationId, String previousValue, String newValue) {
        return updateField("""
                UPDATE OPERATIONS
                   SET PAYLOAD_HASH = :newValue
                 WHERE ID = :operationId
                   AND PAYLOAD_HASH = :previousValue
                """, operationId, previousValue, newValue);
    }

    public Mono<Long> updateOperationHash(long operationId, String previousValue, String newValue) {
        return updateField("""
                UPDATE OPERATIONS
                   SET OPERATION_HASH = :newValue
                 WHERE ID = :operationId
                   AND OPERATION_HASH = :previousValue
                """, operationId, previousValue, newValue);
    }

    private Mono<Long> updateField(
            String statement,
            long operationId,
            Object previousValue,
            Object newValue) {
        return databaseClient.sql(statement)
                .bind("newValue", newValue)
                .bind("operationId", operationId)
                .bind("previousValue", previousValue)
                .fetch()
                .rowsUpdated();
    }
}
