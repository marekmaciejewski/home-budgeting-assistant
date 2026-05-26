package com.solera.budgeting;

import com.solera.budgeting.entities.Operation;
import com.solera.budgeting.entities.Register;
import com.solera.budgeting.model.RechargeRequest;
import com.solera.budgeting.model.TransferRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
class RegisterService {

    private final RegisterRepository registerRepository;
    private final OperationRepository operationRepository;
    private final RegisterConverter converter;

    @Transactional
    public Mono<Void> recharge(RechargeRequest request) {
        return Mono.just(request.getAmount())
                .map(converter::createOperation)
                .flatMap(recharge -> getRegister(request.getRegisterName())
                        .flatMap(target -> saveRecharge(target, recharge)));
    }

    @Transactional
    public Mono<Void> transfer(TransferRequest request) {
        return Mono.just(request.getAmount())
                .map(converter::createOperation)
                .flatMap(transfer -> getRegister(request.getSourceRegister())
                        .flatMap(source -> getRegister(request.getTargetRegister())
                                .flatMap(target -> saveTransfer(source, target, transfer))));
    }

    private Mono<Void> saveRecharge(Register target, Operation recharge) {
        converter.updateTarget(target, recharge);
        return registerRepository.save(target)
                .then(operationRepository.save(recharge))
                .then();
    }

    private Mono<Void> saveTransfer(Register source, Register target, Operation transfer) {
        converter.updateSource(source, transfer);
        converter.updateTarget(target, transfer);
        return registerRepository.save(source)
                .then(registerRepository.save(target))
                .then(operationRepository.save(transfer))
                .then();
    }

    private Mono<Register> getRegister(String registerId) {
        return registerRepository.findById(registerId)
                .filter(Register::isActive)
                .switchIfEmpty(Mono.error(new NotFoundException(registerId + " register not found or not active")));
    }

    Flux<String> getBalances() {
        return registerRepository.findAll()
                .filter(Register::isActive)
                .map(converter::getPrintout);
    }
}
