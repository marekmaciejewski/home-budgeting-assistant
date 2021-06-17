package com.solera.budgeting;

import com.solera.budgeting.entities.Operation;
import com.solera.budgeting.entities.Register;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;

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
        source.setBalance(balance);
        operation.setAmount(amount);
        // when
        converter.updateSource(source, operation);
        // then
        assertThat(source)
                .isSameAs(operation.getSourceRegister())
                .returns(BigDecimal.valueOf(2000), from(Register::getBalance));
    }

    @Test
    void updateTarget_setsUpRelationBetweenRegisterAndOperation() {
        // given
        BigDecimal amount = BigDecimal.valueOf(1000);
        BigDecimal balance = BigDecimal.valueOf(2000);
        Register target = new Register();
        Operation operation = new Operation();
        target.setBalance(balance);
        target.setOperationsTo(new ArrayList<>());
        operation.setAmount(amount);
        // when
        converter.updateTarget(target, operation);
        // then
        assertThat(target)
                .isSameAs(operation.getTargetRegister())
                .returns(BigDecimal.valueOf(3000), from(Register::getBalance))
                .extracting(Register::getOperationsTo, InstanceOfAssertFactories.list(Operation.class))
                .containsExactly(operation);
    }

    @Test
    void getPrintout_returnsFormattedStringRepresentationOfRegister() {
        // given
        Register register = new Register();
        register.setId("Test id");
        register.setBalance(BigDecimal.TEN);
        String expectedPrintout = "Test id: 10";
        // when
        String actualPrintout = converter.getPrintout(register);
        // then
        assertThat(actualPrintout).isEqualTo(expectedPrintout);
    }
}
