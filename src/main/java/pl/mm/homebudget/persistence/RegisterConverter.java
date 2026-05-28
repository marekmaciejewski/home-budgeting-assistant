package pl.mm.homebudget.persistence;

import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.RegisterResponse;
import pl.mm.homebudget.persistence.entity.Operation;
import pl.mm.homebudget.persistence.entity.Register;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RegisterConverter {

    private final Clock clock;

    public Operation createOperation(BigDecimal amount) {
        Operation recharge = new Operation();
        recharge.setTimestamp(Instant.now(clock));
        recharge.setAmount(amount);
        return recharge;
    }

    public void updateSource(Register source, Operation operation) {
        operation.setSourceRegisterId(source.getId());
        source.setBalance(source.getBalance().subtract(operation.getAmount()));
    }

    public void updateTarget(Register target, Operation operation) {
        operation.setTargetRegisterId(target.getId());
        target.setBalance(target.getBalance().add(operation.getAmount()));
    }

    public RegisterResponse toResponse(Register register) {
        return new RegisterResponse(register.getId(), register.getBalance());
    }

    public OperationResponse toResponse(Operation operation) {
        return new OperationResponse(
                operation.getId(),
                operation.getTimestamp().atZone(clock.getZone()).toOffsetDateTime(),
                operation.getAmount())
                .sourceRegisterId(operation.getSourceRegisterId())
                .targetRegisterId(operation.getTargetRegisterId());
    }
}
