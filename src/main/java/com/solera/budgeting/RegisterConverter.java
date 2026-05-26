package com.solera.budgeting;

import com.solera.budgeting.entities.Operation;
import com.solera.budgeting.entities.Register;
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

    String getPrintout(Register register) {
        return String.format("%s: %s", register.getId(), register.getBalance());
    }
}
