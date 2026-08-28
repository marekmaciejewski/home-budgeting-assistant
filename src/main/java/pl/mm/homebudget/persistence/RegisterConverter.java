package pl.mm.homebudget.persistence;

import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.OperationType;
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

    public Operation createRechargeOperation(BigDecimal amount, String registerId) {
        return operationProvider.getObject(amount, OperationType.RECHARGE, null, registerId);
    }

    public Operation createTransferOperation(BigDecimal amount, String sourceRegisterId, String targetRegisterId) {
        return operationProvider.getObject(amount, OperationType.TRANSFER, sourceRegisterId, targetRegisterId);
    }

    public void applyRechargeToRegister(Tuple2<Register, Operation> inputs) {
        Register target = inputs.getT1();
        Operation recharge = inputs.getT2();
        target.setBalance(target.getBalance().add(recharge.getAmount()));
    }

    public void applyTransferToRegisters(Tuple3<Register, Register, Operation> inputs) {
        Register source = inputs.getT1();
        Register target = inputs.getT2();
        Operation transfer = inputs.getT3();
        source.setBalance(source.getBalance().subtract(transfer.getAmount()));
        target.setBalance(target.getBalance().add(transfer.getAmount()));
    }

    public RegisterResponse toResponse(Register register) {
        return registerResponseProvider.getObject(register);
    }

    public OperationResponse toResponse(Operation operation) {
        return operationResponseProvider.getObject(operation);
    }
}
