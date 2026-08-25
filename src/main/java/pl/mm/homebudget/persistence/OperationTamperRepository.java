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

    public Mono<Boolean> updateAmount(long operationId, BigDecimal previousValue, BigDecimal newValue) {
        return databaseClient.sql("""
                        UPDATE OPERATIONS
                           SET AMOUNT = :newValue
                         WHERE ID = :operationId
                           AND AMOUNT = :previousValue
                        """)
                .bind("newValue", newValue)
                .bind("operationId", operationId)
                .bind("previousValue", previousValue)
                .fetch()
                .rowsUpdated()
                .map(updated -> updated == 1);
    }

    public Mono<Boolean> updatePreviousHash(long operationId, String previousValue, String newValue) {
        return updateHash("""
                UPDATE OPERATIONS
                   SET PREVIOUS_HASH = :newValue
                 WHERE ID = :operationId
                   AND PREVIOUS_HASH = :previousValue
                """, operationId, previousValue, newValue);
    }

    public Mono<Boolean> updatePayloadHash(long operationId, String previousValue, String newValue) {
        return updateHash("""
                UPDATE OPERATIONS
                   SET PAYLOAD_HASH = :newValue
                 WHERE ID = :operationId
                   AND PAYLOAD_HASH = :previousValue
                """, operationId, previousValue, newValue);
    }

    public Mono<Boolean> updateOperationHash(long operationId, String previousValue, String newValue) {
        return updateHash("""
                UPDATE OPERATIONS
                   SET OPERATION_HASH = :newValue
                 WHERE ID = :operationId
                   AND OPERATION_HASH = :previousValue
                """, operationId, previousValue, newValue);
    }

    private Mono<Boolean> updateHash(
            String statement,
            long operationId,
            String previousValue,
            String newValue) {
        return databaseClient.sql(statement)
                .bind("newValue", newValue)
                .bind("operationId", operationId)
                .bind("previousValue", previousValue)
                .fetch()
                .rowsUpdated()
                .map(updated -> updated == 1);
    }
}
