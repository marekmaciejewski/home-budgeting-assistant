package pl.mm.homebudget.config;

import lombok.RequiredArgsConstructor;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.RegisterResponse;
import pl.mm.homebudget.persistence.entity.Operation;
import pl.mm.homebudget.persistence.entity.Register;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Configuration
@RequiredArgsConstructor
public class ModelConfiguration {

    private final Clock clock;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public Operation operation(BigDecimal amount) {
        Operation operation = new Operation();
        operation.setTimestamp(Instant.now(clock));
        operation.setAmount(amount);
        return operation;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RegisterResponse registerResponse(Register register) {
        return new RegisterResponse(register.getId(), register.getBalance());
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public OperationResponse operationResponse(Operation operation) {
        return new OperationResponse(
                operation.getId(),
                operation.getTimestamp().atZone(clock.getZone()).toOffsetDateTime(),
                operation.getAmount())
                .sourceRegisterId(operation.getSourceRegisterId())
                .targetRegisterId(operation.getTargetRegisterId());
    }
}
