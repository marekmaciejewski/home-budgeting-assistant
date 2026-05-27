package com.solera.budgeting;

import com.solera.budgeting.entities.Operation;
import com.solera.budgeting.entities.Register;
import com.solera.budgeting.model.OperationResponse;
import com.solera.budgeting.model.RegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
class RegisterService {

    private final RegisterRepository registerRepository;
    private final OperationRepository operationRepository;
    private final RegisterConverter converter;

    @Transactional
    public Mono<OperationResponse> recharge(String registerId, BigDecimal amount) {
        return applyRecharge(registerId, amount)
                .map(converter::toResponse);
    }

    private Mono<Operation> applyRecharge(String registerId, BigDecimal amount) {
        return Mono.just(amount)
                .map(converter::createOperation)
                .flatMap(recharge -> getActiveRegister(registerId)
                        .flatMap(target -> saveRecharge(target, recharge)));
    }

    @Transactional
    public Mono<OperationResponse> transfer(String sourceRegister, String targetRegister, BigDecimal amount) {
        return applyTransfer(sourceRegister, targetRegister, amount)
                .map(converter::toResponse);
    }

    private Mono<Operation> applyTransfer(String sourceRegister, String targetRegister, BigDecimal amount) {
        if (Objects.equals(sourceRegister, targetRegister)) {
            return Mono.error(new InvalidTransferException("source and target register must be different"));
        }
        return Mono.just(amount)
                .map(converter::createOperation)
                .flatMap(transfer -> getActiveRegister(sourceRegister)
                        .flatMap(source -> getActiveRegister(targetRegister)
                                .flatMap(target -> saveTransfer(source, target, transfer))));
    }

    private Mono<Operation> saveRecharge(Register target, Operation recharge) {
        converter.updateTarget(target, recharge);
        return registerRepository.save(target)
                .then(operationRepository.save(recharge));
    }

    private Mono<Operation> saveTransfer(Register source, Register target, Operation transfer) {
        converter.updateSource(source, transfer);
        converter.updateTarget(target, transfer);
        return registerRepository.save(source)
                .then(registerRepository.save(target))
                .then(operationRepository.save(transfer));
    }

    private Mono<Register> getActiveRegister(String registerId) {
        return registerRepository.findById(registerId)
                .filter(Register::isActive)
                .switchIfEmpty(Mono.error(new RegisterNotFoundException(registerId + " register not found or not active")));
    }

    Flux<RegisterResponse> getRegisters() {
        return registerRepository.findAll()
                .filter(Register::isActive)
                .map(converter::toResponse);
    }

    Mono<RegisterResponse> getRegister(String registerId) {
        return getActiveRegister(registerId)
                .map(converter::toResponse);
    }

    Flux<OperationResponse> getOperations() {
        return operationRepository.findAll()
                .map(converter::toResponse);
    }

    Mono<OperationResponse> getOperation(long operationId) {
        return operationRepository.findById(operationId)
                .switchIfEmpty(Mono.error(new OperationNotFoundException(operationId + " operation not found")))
                .map(converter::toResponse);
    }
}
