package pl.mm.homebudget.application;

import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.RegisterResponse;
import pl.mm.homebudget.domain.InvalidTransferException;
import pl.mm.homebudget.domain.OperationNotFoundException;
import pl.mm.homebudget.domain.RegisterNotFoundException;
import pl.mm.homebudget.persistence.OperationRepository;
import pl.mm.homebudget.persistence.RegisterConverter;
import pl.mm.homebudget.persistence.RegisterRepository;
import pl.mm.homebudget.persistence.entity.Operation;
import pl.mm.homebudget.persistence.entity.Register;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final RegisterRepository registerRepository;
    private final OperationRepository operationRepository;
    private final RegisterConverter converter;

    @Transactional
    public Mono<OperationResponse> recharge(String registerId, BigDecimal amount) {
        return applyRecharge(registerId, amount)
                .map(converter::toResponse);
    }

    private Mono<Operation> applyRecharge(String registerId, BigDecimal amount) {
        return Mono.zip(
                        getActiveRegister(registerId),
                        Mono.fromSupplier(() -> converter.createOperation(amount)))
                .map(converter::applyRechargeToRegister)
                .flatMap(inputs -> saveRecharge(inputs.getT1(), inputs.getT2()));
    }

    private Mono<Operation> saveRecharge(Register target, Operation recharge) {
        return registerRepository.save(target)
                .then(operationRepository.save(recharge));
    }

    @Transactional
    public Mono<OperationResponse> transfer(String sourceRegister, String targetRegister, BigDecimal amount) {
        return applyTransfer(sourceRegister, targetRegister, amount)
                .map(converter::toResponse);
    }

    private Mono<Operation> applyTransfer(String sourceRegister, String targetRegister, BigDecimal amount) {
        return Mono.zip(
                        getActiveRegister(sourceRegister),
                        getActiveRegister(targetRegister),
                        Mono.fromSupplier(() -> converter.createOperation(amount)))
                .doFirst(() -> validateDifferentTransferRegisters(sourceRegister, targetRegister))
                .map(this::validateSourceRegisterHasSufficientBalance)
                .map(converter::applyTransferToRegisters)
                .flatMap(inputs -> saveTransfer(inputs.getT1(), inputs.getT2(), inputs.getT3()));
    }

    private void validateDifferentTransferRegisters(String sourceRegister, String targetRegister) {
        if (sourceRegister.equals(targetRegister)) {
            throw new InvalidTransferException("source and target register must be different");
        }
    }

    private Tuple3<Register, Register, Operation> validateSourceRegisterHasSufficientBalance(
            Tuple3<Register, Register, Operation> inputs) {
        Register source = inputs.getT1();
        Operation transfer = inputs.getT3();
        if (source.getBalance().compareTo(transfer.getAmount()) < 0) {
            throw new InvalidTransferException(source.getId() + " register has insufficient balance");
        }
        return inputs;
    }

    private Mono<Operation> saveTransfer(Register source, Register target, Operation transfer) {
        return Mono.when(
                        registerRepository.save(source),
                        registerRepository.save(target))
                .then(operationRepository.save(transfer));
    }

    public Flux<RegisterResponse> getRegisters() {
        return registerRepository.findAll()
                .filter(Register::isActive)
                .map(converter::toResponse);
    }

    public Mono<RegisterResponse> getRegister(String registerId) {
        return getActiveRegister(registerId)
                .map(converter::toResponse);
    }

    private Mono<Register> getActiveRegister(String registerId) {
        return registerRepository.findById(registerId)
                .filter(Register::isActive)
                .switchIfEmpty(Mono.error(new RegisterNotFoundException(registerId + " register not found or not active")));
    }

    public Flux<OperationResponse> getOperations() {
        return operationRepository.findAll()
                .map(converter::toResponse);
    }

    public Mono<OperationResponse> getOperation(long operationId) {
        return operationRepository.findById(operationId)
                .switchIfEmpty(Mono.error(new OperationNotFoundException(operationId + " operation not found")))
                .map(converter::toResponse);
    }
}
