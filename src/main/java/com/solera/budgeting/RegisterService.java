package com.solera.budgeting;

import com.solera.budgeting.entities.Operation;
import com.solera.budgeting.entities.Register;
import com.solera.budgeting.model.RechargeRequest;
import com.solera.budgeting.model.TransferRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
class RegisterService {

    private final JpaRepository<Register, String> repository;
    private final RegisterConverter converter;

    @Transactional
    public Mono<Void> recharge(RechargeRequest request) {
        return Mono.just(request.getAmount())
                .map(converter::createOperation)
                .flatMap(recharge -> applyTargetOperation(request.getRegisterName(), recharge));
    }

    @Transactional
    public Mono<Void> transfer(TransferRequest request) {
        return Mono.just(request.getAmount())
                .map(converter::createOperation)
                .flatMap(transfer -> applySourceOperation(request.getSourceRegister(), transfer)
                        .then(applyTargetOperation(request.getTargetRegister(), transfer)));
    }

    private Mono<Void> applySourceOperation(String registerId, Operation operation) {
        return applyOperation(registerId, source -> converter.updateSource(source, operation));
    }

    private Mono<Void> applyTargetOperation(String registerId, Operation operation) {
        return applyOperation(registerId, target -> converter.updateTarget(target, operation));
    }

    private Mono<Void> applyOperation(String registerId, Consumer<Register> updater) {
        return getRegister(registerId)
                .doOnNext(updater)
                .map(repository::save)
                .then();
    }

    private Mono<Register> getRegister(String registerId) {
        return Mono.just(registerId)
                .map(repository::findById)
                .flatMap(Mono::justOrEmpty)
                .filter(Register::isActive)
                .switchIfEmpty(Mono.error(new NotFoundException(registerId + " register not found or not active")));
    }

    Flux<String> getBalances() {
        return Flux.fromIterable(repository.findAll())
                .filter(Register::isActive)
                .map(converter::getPrintout);
    }
}
