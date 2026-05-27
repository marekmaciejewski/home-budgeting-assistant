package com.solera.budgeting;

import com.solera.budgeting.entities.Operation;
import com.solera.budgeting.entities.Register;
import com.solera.budgeting.model.OperationResponse;
import com.solera.budgeting.model.RegisterResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;

class RegisterConverterTest {

    private Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    private RegisterConverter converter = new RegisterConverter(clock);

    @Test
    void createOperation_returnsCorrectlySetUpObject() {
        // given
        BigDecimal amount = BigDecimal.ZERO;
        // when
        Operation operation = converter.createOperation(amount);
        // then
        assertThat(operation)
                .isNotNull()
                .returns(clock.instant(), from(Operation::getTimestamp))
                .returns(amount, from(Operation::getAmount));
    }

    @Test
    void updateSource_setsUpRelationBetweenRegisterAndOperation() {
        // given
        BigDecimal amount = BigDecimal.valueOf(1000);
        BigDecimal balance = BigDecimal.valueOf(3000);
        Register source = new Register();
        Operation operation = new Operation();
        source.setId("source");
        source.setBalance(balance);
        operation.setAmount(amount);
        // when
        converter.updateSource(source, operation);
        // then
        assertThat(operation).returns("source", from(Operation::getSourceRegisterId));
        assertThat(source).returns(BigDecimal.valueOf(2000), from(Register::getBalance));
    }

    @Test
    void updateTarget_setsUpRelationBetweenRegisterAndOperation() {
        // given
        BigDecimal amount = BigDecimal.valueOf(1000);
        BigDecimal balance = BigDecimal.valueOf(2000);
        Register target = new Register();
        Operation operation = new Operation();
        target.setId("target");
        target.setBalance(balance);
        operation.setAmount(amount);
        // when
        converter.updateTarget(target, operation);
        // then
        assertThat(operation).returns("target", from(Operation::getTargetRegisterId));
        assertThat(target).returns(BigDecimal.valueOf(3000), from(Register::getBalance));
    }

    @Test
    void toResponse_returnsRegisterResponse() {
        // given
        Register register = new Register();
        register.setId("Test id");
        register.setBalance(BigDecimal.TEN);
        // when
        RegisterResponse response = converter.toResponse(register);
        // then
        assertThat(response)
                .returns("Test id", from(RegisterResponse::id))
                .returns(BigDecimal.TEN, from(RegisterResponse::balance));
    }

    @Test
    void toResponse_returnsOperationResponse() {
        // given
        Operation operation = new Operation();
        operation.setId(1L);
        operation.setTimestamp(clock.instant());
        operation.setAmount(BigDecimal.TEN);
        operation.setSourceRegisterId("source");
        operation.setTargetRegisterId("target");
        // when
        OperationResponse response = converter.toResponse(operation);
        // then
        assertThat(response)
                .returns(1L, from(OperationResponse::id))
                .returns(clock.instant(), from(OperationResponse::timestamp))
                .returns(BigDecimal.TEN, from(OperationResponse::amount))
                .returns("source", from(OperationResponse::sourceRegisterId))
                .returns("target", from(OperationResponse::targetRegisterId));
    }
}
