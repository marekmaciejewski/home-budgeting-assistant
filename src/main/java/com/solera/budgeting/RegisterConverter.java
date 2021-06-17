package com.solera.budgeting;

import com.solera.budgeting.entities.Operation;
import com.solera.budgeting.entities.Register;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
class RegisterConverter {

    Operation createOperation(BigDecimal amount) {
        Operation recharge = new Operation();
        recharge.setTimestamp(Instant.now());
        recharge.setAmount(amount);
        return recharge;
    }

    void updateSource(Register source, Operation operation) {
        operation.setSourceRegister(source);
        source.setBalance(source.getBalance().subtract(operation.getAmount()));
    }

    void updateTarget(Register target, Operation operation) {
        operation.setTargetRegister(target);
        target.setBalance(target.getBalance().add(operation.getAmount()));
        target.getOperationsTo().add(operation);
    }

    String getPrintout(Register register) {
        return String.format("%s: %s", register.getId(), register.getBalance());
    }
}
