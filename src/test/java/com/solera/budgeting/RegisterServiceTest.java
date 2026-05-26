package com.solera.budgeting;

import com.solera.budgeting.entities.Operation;
import com.solera.budgeting.entities.Register;
import com.solera.budgeting.model.RechargeRequest;
import com.solera.budgeting.model.TransferRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.webjars.NotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private RegisterRepository registerRepository;
    @Mock
    private OperationRepository operationRepository;
    @Mock
    private RegisterConverter converter;
    @InjectMocks
    private RegisterService service;

    @Test
    void recharge_invokesCorrectUpdater_andSavesRegisterAndOperation(
            @Mock RechargeRequest request, @Mock BigDecimal amount, @Mock Operation recharge, @Mock Register target) {
        // given
        String registerName = "register name";
        given(request.getAmount()).willReturn(amount);
        given(converter.createOperation(amount)).willReturn(recharge);
        given(request.getRegisterName()).willReturn(registerName);
        given(registerRepository.findById(registerName)).willReturn(Mono.just(target));
        given(target.isActive()).willReturn(true);
        given(registerRepository.save(target)).willReturn(Mono.just(target));
        given(operationRepository.save(recharge)).willReturn(Mono.just(recharge));
        // when
        Mono<Void> result = service.recharge(request);
        // then
        StepVerifier.create(result)
                .verifyComplete();
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(registerName);
        InOrder order = inOrder(converter, registerRepository, operationRepository);
        then(converter).should(order).updateTarget(target, recharge);
        then(registerRepository).should(order).save(target);
        then(operationRepository).should(order).save(recharge);
        verifyNoMoreInteractions(converter, registerRepository, operationRepository);
    }

    @Test
    void recharge_returnsError_dueToMissingRegister(
            @Mock RechargeRequest request, @Mock BigDecimal amount, @Mock Operation recharge, @Mock Register target) {
        // given
        String registerName = "register name";
        given(request.getAmount()).willReturn(amount);
        given(converter.createOperation(amount)).willReturn(recharge);
        given(request.getRegisterName()).willReturn(registerName);
        given(registerRepository.findById(registerName)).willReturn(Mono.empty());
        // when
        Mono<Void> result = service.recharge(request);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage(registerName + " register not found or not active")
                        .hasNoCause());
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(registerName);
        then(converter).should(never()).updateTarget(target, recharge);
        then(registerRepository).should(never()).save(target);
        then(operationRepository).shouldHaveNoInteractions();
        verifyNoMoreInteractions(converter, registerRepository);
    }

    @Test
    void recharge_returnsError_dueToInactiveRegister(
            @Mock RechargeRequest request, @Mock BigDecimal amount, @Mock Operation recharge, @Mock Register target) {
        // given
        String registerName = "register name";
        given(request.getAmount()).willReturn(amount);
        given(converter.createOperation(amount)).willReturn(recharge);
        given(request.getRegisterName()).willReturn(registerName);
        given(registerRepository.findById(registerName)).willReturn(Mono.just(target));
        given(target.isActive()).willReturn(false);
        // when
        Mono<Void> result = service.recharge(request);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage(registerName + " register not found or not active")
                        .hasNoCause());
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(registerName);
        then(converter).should(never()).updateTarget(target, recharge);
        then(registerRepository).should(never()).save(target);
        then(operationRepository).shouldHaveNoInteractions();
        verifyNoMoreInteractions(converter, registerRepository);
    }

    @Test
    void transfer_invokesCorrectUpdaters_andSavesRegistersAndOperation(
            @Mock TransferRequest request,
            @Mock BigDecimal amount,
            @Mock Operation transfer,
            @Mock Register source,
            @Mock Register target) {
        // given
        String sourceName = "source name";
        String targetName = "target name";
        given(request.getAmount()).willReturn(amount);
        given(converter.createOperation(amount)).willReturn(transfer);
        given(request.getSourceRegister()).willReturn(sourceName);
        given(request.getTargetRegister()).willReturn(targetName);
        given(registerRepository.findById(sourceName)).willReturn(Mono.just(source));
        given(registerRepository.findById(targetName)).willReturn(Mono.just(target));
        given(source.isActive()).willReturn(true);
        given(target.isActive()).willReturn(true);
        given(registerRepository.save(source)).willReturn(Mono.just(source));
        given(registerRepository.save(target)).willReturn(Mono.just(target));
        given(operationRepository.save(transfer)).willReturn(Mono.just(transfer));
        // when
        Mono<Void> result = service.transfer(request);
        // then
        StepVerifier.create(result)
                .verifyComplete();
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(sourceName);
        then(registerRepository).should().findById(targetName);
        InOrder order = inOrder(converter, registerRepository, operationRepository);
        then(converter).should(order).updateSource(source, transfer);
        then(converter).should(order).updateTarget(target, transfer);
        then(registerRepository).should(order).save(source);
        then(registerRepository).should(order).save(target);
        then(operationRepository).should(order).save(transfer);
        verifyNoMoreInteractions(converter, registerRepository, operationRepository);
    }

    @Test
    void transfer_returnsError_dueToMissingSourceRegister(
            @Mock TransferRequest request, @Mock BigDecimal amount, @Mock Operation transfer) {
        // given
        String sourceName = "source name";
        String targetName = "target name";
        given(request.getAmount()).willReturn(amount);
        given(converter.createOperation(amount)).willReturn(transfer);
        given(request.getSourceRegister()).willReturn(sourceName);
        given(registerRepository.findById(sourceName)).willReturn(Mono.empty());
        // when
        Mono<Void> result = service.transfer(request);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage(sourceName + " register not found or not active")
                        .hasNoCause());
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(sourceName);
        then(request).should(never()).getTargetRegister();
        then(converter).should(never()).updateSource(any(Register.class), same(transfer));
        then(registerRepository).should(never()).save(any(Register.class));
        then(operationRepository).shouldHaveNoInteractions();
        verifyNoMoreInteractions(converter, registerRepository);
    }

    @Test
    void transfer_returnsError_dueToInactiveSourceRegister(
            @Mock TransferRequest request, @Mock BigDecimal amount, @Mock Operation transfer, @Mock Register source) {
        // given
        String sourceName = "source name";
        String targetName = "target name";
        given(request.getAmount()).willReturn(amount);
        given(converter.createOperation(amount)).willReturn(transfer);
        given(request.getSourceRegister()).willReturn(sourceName);
        given(registerRepository.findById(sourceName)).willReturn(Mono.just(source));
        given(source.isActive()).willReturn(false);
        // when
        Mono<Void> result = service.transfer(request);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage(sourceName + " register not found or not active")
                        .hasNoCause());
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(sourceName);
        then(request).should(never()).getTargetRegister();
        then(converter).should(never()).updateSource(any(Register.class), same(transfer));
        then(registerRepository).should(never()).save(any(Register.class));
        then(operationRepository).shouldHaveNoInteractions();
        verifyNoMoreInteractions(converter, registerRepository);
    }

    @Test
    void transfer_returnsError_dueToMissingTargetRegister(
            @Mock TransferRequest request, @Mock BigDecimal amount, @Mock Operation transfer, @Mock Register source) {
        // given
        String sourceName = "source name";
        String targetName = "target name";
        given(request.getAmount()).willReturn(amount);
        given(converter.createOperation(amount)).willReturn(transfer);
        given(request.getSourceRegister()).willReturn(sourceName);
        given(request.getTargetRegister()).willReturn(targetName);
        given(registerRepository.findById(sourceName)).willReturn(Mono.just(source));
        given(registerRepository.findById(targetName)).willReturn(Mono.empty());
        given(source.isActive()).willReturn(true);
        // when
        Mono<Void> result = service.transfer(request);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage(targetName + " register not found or not active")
                        .hasNoCause());
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(sourceName);
        then(registerRepository).should().findById(targetName);
        then(converter).should(never()).updateSource(source, transfer);
        then(registerRepository).should(never()).save(any(Register.class));
        then(operationRepository).shouldHaveNoInteractions();
        verifyNoMoreInteractions(converter, registerRepository);
    }

    @Test
    void transfer_returnsError_dueToInactiveTargetRegister(
            @Mock TransferRequest request,
            @Mock BigDecimal amount,
            @Mock Operation transfer,
            @Mock Register source,
            @Mock Register target) {
        // given
        String sourceName = "source name";
        String targetName = "target name";
        given(request.getAmount()).willReturn(amount);
        given(converter.createOperation(amount)).willReturn(transfer);
        given(request.getSourceRegister()).willReturn(sourceName);
        given(request.getTargetRegister()).willReturn(targetName);
        given(registerRepository.findById(sourceName)).willReturn(Mono.just(source));
        given(registerRepository.findById(targetName)).willReturn(Mono.just(target));
        given(source.isActive()).willReturn(true);
        given(target.isActive()).willReturn(false);
        // when
        Mono<Void> result = service.transfer(request);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage(targetName + " register not found or not active")
                        .hasNoCause());
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(sourceName);
        then(registerRepository).should().findById(targetName);
        then(converter).should(never()).updateSource(source, transfer);
        then(registerRepository).should(never()).save(any(Register.class));
        then(operationRepository).shouldHaveNoInteractions();
        verifyNoMoreInteractions(converter, registerRepository);
    }

    @Test
    void getBalances(@Mock Register register1, @Mock Register register2, @Mock Register register3) {
        // given
        String printout1 = "printout 1";
        String printout3 = "printout 3";
        given(registerRepository.findAll()).willReturn(Flux.just(register1, register2, register3));
        given(register1.isActive()).willReturn(true);
        given(register2.isActive()).willReturn(false);
        given(register3.isActive()).willReturn(true);
        given(converter.getPrintout(register1)).willReturn(printout1);
        given(converter.getPrintout(register3)).willReturn(printout3);
        // when
        Flux<String> result = service.getBalances();
        // then
        StepVerifier.create(result)
                .expectNext(printout1, printout3)
                .verifyComplete();
        then(registerRepository).should().findAll();
        then(converter).should().getPrintout(register1);
        then(converter).should().getPrintout(register3);
        then(converter).should(never()).getPrintout(register2);
        verifyNoMoreInteractions(converter, registerRepository, operationRepository);
    }
}
