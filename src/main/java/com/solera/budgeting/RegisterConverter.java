package com.solera.budgeting;

import com.solera.budgeting.entities.Operation;
import com.solera.budgeting.entities.Register;
import com.solera.budgeting.model.OperationResponse;
import com.solera.budgeting.model.RegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
class RegisterConverter {

    private final Clock clock;

    Operation createOperation(BigDecimal amount) {
        Operation recharge = new Operation();
        recharge.setTimestamp(Instant.now(clock));
        recharge.setAmount(amount);
        return recharge;
    }

    void updateSource(Register source, Operation operation) {
        operation.setSourceRegisterId(source.getId());
        source.setBalance(source.getBalance().subtract(operation.getAmount()));
    }

    void updateTarget(Register target, Operation operation) {
        operation.setTargetRegisterId(target.getId());
        target.setBalance(target.getBalance().add(operation.getAmount()));
    }

    RegisterResponse toResponse(Register register) {
        return new RegisterResponse(register.getId(), register.getBalance());
    }

    OperationResponse toResponse(Operation operation) {
        return new OperationResponse(
                operation.getId(),
                operation.getTimestamp(),
                operation.getAmount(),
                operation.getSourceRegisterId(),
                operation.getTargetRegisterId());
    }
}
