package pl.mm.homebudget.persistence;

import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.RegisterResponse;
import pl.mm.homebudget.persistence.entity.Operation;
import pl.mm.homebudget.persistence.entity.Register;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class RegisterConverter {

    private final ObjectProvider<Operation> operationProvider;
    private final ObjectProvider<RegisterResponse> registerResponseProvider;
    private final ObjectProvider<OperationResponse> operationResponseProvider;

    public Operation createOperation(BigDecimal amount) {
        return operationProvider.getObject(amount);
    }

    public Tuple2<Register, Operation> applyRechargeToRegister(Tuple2<Register, Operation> inputs) {
        applyOperationToTarget(inputs.getT1(), inputs.getT2());
        return inputs;
    }

    public Tuple3<Register, Register, Operation> applyTransferToRegisters(
            Tuple3<Register, Register, Operation> inputs) {
        Operation transfer = inputs.getT3();
        applyOperationToSource(inputs.getT1(), transfer);
        applyOperationToTarget(inputs.getT2(), transfer);
        return inputs;
    }

    private void applyOperationToSource(Register source, Operation operation) {
        operation.setSourceRegisterId(source.getId());
        source.setBalance(source.getBalance().subtract(operation.getAmount()));
    }

    private void applyOperationToTarget(Register target, Operation operation) {
        operation.setTargetRegisterId(target.getId());
        target.setBalance(target.getBalance().add(operation.getAmount()));
    }

    public RegisterResponse toResponse(Register register) {
        return registerResponseProvider.getObject(register);
    }

    public OperationResponse toResponse(Operation operation) {
        return operationResponseProvider.getObject(operation);
    }
}
