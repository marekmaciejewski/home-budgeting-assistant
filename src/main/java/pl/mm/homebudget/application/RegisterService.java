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

import java.math.BigDecimal;
import java.util.Objects;

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
        if (source.getBalance().compareTo(transfer.getAmount()) < 0) {
            return Mono.error(new InvalidTransferException(source.getId() + " register has insufficient balance"));
        }
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

    public Flux<RegisterResponse> getRegisters() {
        return registerRepository.findAll()
                .filter(Register::isActive)
                .map(converter::toResponse);
    }

    public Mono<RegisterResponse> getRegister(String registerId) {
        return getActiveRegister(registerId)
                .map(converter::toResponse);
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
