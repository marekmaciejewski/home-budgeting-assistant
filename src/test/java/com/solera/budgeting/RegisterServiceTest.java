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
import org.springframework.data.jpa.repository.JpaRepository;
import org.webjars.NotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    private JpaRepository<Register, String> repository;
    @Mock
    private RegisterConverter converter;
    @InjectMocks
    private RegisterService service;

    @Test
    void recharge_invokesCorrectUpdater_andInvokesSave(
            @Mock RechargeRequest request, @Mock BigDecimal amount, @Mock Operation recharge, @Mock Register target) {
        // given
        String registerName = "register name";
        given(request.getAmount()).willReturn(amount);
        given(converter.createOperation(amount)).willReturn(recharge);
        given(request.getRegisterName()).willReturn(registerName);
        given(repository.findById(registerName)).willReturn(Optional.of(target));
        given(target.isActive()).willReturn(true);
        given(repository.save(target)).willReturn(target);
        // when
        Mono<Void> result = service.recharge(request);
        // then
        StepVerifier.create(result)
                .verifyComplete();
        then(converter).should().updateTarget(target, recharge);
        then(repository).should().save(target);
        verifyNoMoreInteractions(converter, repository);
    }

    @Test
    void recharge_returnsError_dueToMissingRegister(
            @Mock RechargeRequest request, @Mock BigDecimal amount, @Mock Operation recharge, @Mock Register target) {
        // given
        String registerName = "register name";
        given(request.getAmount()).willReturn(amount);
        given(converter.createOperation(amount)).willReturn(recharge);
        given(request.getRegisterName()).willReturn(registerName);
        given(repository.findById(registerName)).willReturn(Optional.empty());
        // when
        Mono<Void> result = service.recharge(request);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage(registerName + " register not found or not active")
                        .hasNoCause());
        then(converter).should(never()).updateTarget(target, recharge);
        then(repository).should(never()).save(target);
        verifyNoMoreInteractions(converter, repository);
    }

    @Test
    void recharge_returnsError_dueToInactiveRegister(
            @Mock RechargeRequest request, @Mock BigDecimal amount, @Mock Operation recharge, @Mock Register target) {
        // given
        String registerName = "register name";
        given(request.getAmount()).willReturn(amount);
        given(converter.createOperation(amount)).willReturn(recharge);
        given(request.getRegisterName()).willReturn(registerName);
        given(repository.findById(registerName)).willReturn(Optional.of(target));
        given(target.isActive()).willReturn(false);
        // when
        Mono<Void> result = service.recharge(request);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage(registerName + " register not found or not active")
                        .hasNoCause());
        then(converter).should(never()).updateTarget(target, recharge);
        then(repository).should(never()).save(target);
        verifyNoMoreInteractions(converter, repository);
    }

    @Test
    void transfer_invokesCorrectUpdaters_andInvokesSaveWithCorrectObject(
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
        given(repository.findById(sourceName)).willReturn(Optional.of(source));
        given(repository.findById(targetName)).willReturn(Optional.of(target));
        given(source.isActive()).willReturn(true);
        given(target.isActive()).willReturn(true);
        given(repository.save(source)).willReturn(source);
        given(repository.save(target)).willReturn(target);
        // when
        Mono<Void> result = service.transfer(request);
        // then
        StepVerifier.create(result)
                .verifyComplete();
        InOrder order = inOrder(converter, repository);
        then(converter).should(order).updateSource(source, transfer);
        then(repository).should(order).save(source);
        then(converter).should(order).updateTarget(target, transfer);
        then(repository).should(order).save(target);
        verifyNoMoreInteractions(converter, repository);
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
        given(request.getTargetRegister()).willReturn(targetName);
        given(repository.findById(sourceName)).willReturn(Optional.empty());
        // when
        Mono<Void> result = service.transfer(request);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage(sourceName + " register not found or not active")
                        .hasNoCause());
        then(converter).should(never()).updateSource(any(Register.class), same(transfer));
        then(repository).should(never()).save(any(Register.class));
        verifyNoMoreInteractions(converter, repository);
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
        given(request.getTargetRegister()).willReturn(targetName);
        given(repository.findById(sourceName)).willReturn(Optional.of(source));
        given(source.isActive()).willReturn(false);
        // when
        Mono<Void> result = service.transfer(request);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage(sourceName + " register not found or not active")
                        .hasNoCause());
        then(converter).should(never()).updateSource(any(Register.class), same(transfer));
        then(repository).should(never()).save(any(Register.class));
        verifyNoMoreInteractions(converter, repository);
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
        given(repository.findById(sourceName)).willReturn(Optional.of(source));
        given(repository.findById(targetName)).willReturn(Optional.empty());
        given(source.isActive()).willReturn(true);
        given(repository.save(source)).willReturn(source);
        // when
        Mono<Void> result = service.transfer(request);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage(targetName + " register not found or not active")
                        .hasNoCause());
        then(converter).should().updateSource(source, transfer);
        then(repository).should().save(source);
        verifyNoMoreInteractions(converter, repository);
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
        given(repository.findById(sourceName)).willReturn(Optional.of(source));
        given(repository.findById(targetName)).willReturn(Optional.of(target));
        given(source.isActive()).willReturn(true);
        given(target.isActive()).willReturn(false);
        given(repository.save(source)).willReturn(source);
        // when
        Mono<Void> result = service.transfer(request);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(NotFoundException.class)
                        .hasMessage(targetName + " register not found or not active")
                        .hasNoCause());
        then(converter).should().updateSource(source, transfer);
        then(repository).should().save(source);
        verifyNoMoreInteractions(converter, repository);
    }

    @Test
    void getBalances(@Mock Register register1, @Mock Register register2, @Mock Register register3) {
        // given
        String printout1 = "printout 1";
        String printout3 = "printout 3";
        List<Register> registers = Arrays.asList(register1, register2, register3);
        given(repository.findAll()).willReturn(registers);
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
        then(converter).should(never()).getPrintout(register2);
        verifyNoMoreInteractions(converter, repository);
    }
}
