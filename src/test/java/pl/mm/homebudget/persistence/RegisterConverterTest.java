package pl.mm.homebudget.persistence;

import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.RegisterResponse;
import pl.mm.homebudget.persistence.entity.Operation;
import pl.mm.homebudget.persistence.entity.Register;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;

class RegisterConverterTest {

    private static final Instant NOW = Instant.parse("2026-05-28T10:15:30Z");
    private static final ZoneId TEST_ZONE = ZoneId.of("Europe/Warsaw");

    private Clock clock = Clock.fixed(NOW, TEST_ZONE);
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
                .returns("Test id", from(RegisterResponse::getId))
                .returns(BigDecimal.TEN, from(RegisterResponse::getBalance));
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
                .returns(1L, from(OperationResponse::getId))
                .returns(NOW.atZone(TEST_ZONE).toOffsetDateTime(), from(OperationResponse::getTimestamp))
                .returns(BigDecimal.TEN, from(OperationResponse::getAmount))
                .returns("source", from(OperationResponse::getSourceRegisterId))
                .returns("target", from(OperationResponse::getTargetRegisterId));
    }
}
