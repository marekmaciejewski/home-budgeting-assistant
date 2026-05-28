package pl.mm.homebudget.application;

import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.domain.InvalidTransferException;
import pl.mm.homebudget.domain.RegisterNotFoundException;
import pl.mm.homebudget.persistence.OperationRepository;
import pl.mm.homebudget.persistence.RegisterConverter;
import pl.mm.homebudget.persistence.RegisterRepository;
import pl.mm.homebudget.persistence.entity.Operation;
import pl.mm.homebudget.persistence.entity.Register;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
            @Mock BigDecimal amount, @Mock Operation recharge, @Mock Register target) {
        // given
        String registerId = "register name";
        OperationResponse response = new OperationResponse(1L, OffsetDateTime.now(), amount)
                .targetRegisterId(registerId);
        given(converter.createOperation(amount)).willReturn(recharge);
        given(registerRepository.findById(registerId)).willReturn(Mono.just(target));
        given(target.isActive()).willReturn(true);
        given(registerRepository.save(target)).willReturn(Mono.just(target));
        given(operationRepository.save(recharge)).willReturn(Mono.just(recharge));
        given(converter.toResponse(recharge)).willReturn(response);
        // when
        Mono<OperationResponse> result = service.recharge(registerId, amount);
        // then
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        InOrder order = inOrder(converter, registerRepository, operationRepository);
        then(converter).should(order).createOperation(amount);
        then(registerRepository).should(order).findById(registerId);
        then(converter).should(order).updateTarget(target, recharge);
        then(registerRepository).should(order).save(target);
        then(operationRepository).should(order).save(recharge);
        then(converter).should(order).toResponse(recharge);
        verifyNoMoreInteractions(converter, registerRepository, operationRepository);
    }

    @Test
    void recharge_returnsError_dueToMissingRegister(
            @Mock BigDecimal amount, @Mock Operation recharge, @Mock Register target) {
        // given
        String registerId = "register name";
        given(converter.createOperation(amount)).willReturn(recharge);
        given(registerRepository.findById(registerId)).willReturn(Mono.empty());
        // when
        Mono<OperationResponse> result = service.recharge(registerId, amount);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(RegisterNotFoundException.class)
                        .hasMessage(registerId + " register not found or not active")
                        .hasNoCause());
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(registerId);
        then(converter).should(never()).updateTarget(target, recharge);
        then(converter).should(never()).toResponse(recharge);
        then(registerRepository).should(never()).save(target);
        then(operationRepository).shouldHaveNoInteractions();
        verifyNoMoreInteractions(converter, registerRepository);
    }

    @Test
    void recharge_returnsError_dueToInactiveRegister(
            @Mock BigDecimal amount, @Mock Operation recharge, @Mock Register target) {
        // given
        String registerId = "register name";
        given(converter.createOperation(amount)).willReturn(recharge);
        given(registerRepository.findById(registerId)).willReturn(Mono.just(target));
        given(target.isActive()).willReturn(false);
        // when
        Mono<OperationResponse> result = service.recharge(registerId, amount);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(RegisterNotFoundException.class)
                        .hasMessage(registerId + " register not found or not active")
                        .hasNoCause());
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(registerId);
        then(converter).should(never()).updateTarget(target, recharge);
        then(converter).should(never()).toResponse(recharge);
        then(registerRepository).should(never()).save(target);
        then(operationRepository).shouldHaveNoInteractions();
        verifyNoMoreInteractions(converter, registerRepository);
    }

    @Test
    void transfer_invokesCorrectUpdaters_andSavesRegistersAndOperation(
            @Mock BigDecimal amount,
            @Mock Operation transfer,
            @Mock Register source,
            @Mock Register target) {
        // given
        String sourceId = "source name";
        String targetId = "target name";
        OperationResponse response = new OperationResponse(
                1L, Instant.now().atOffset(ZoneOffset.UTC), amount)
                .sourceRegisterId(sourceId)
                .targetRegisterId(targetId);
        given(converter.createOperation(amount)).willReturn(transfer);
        given(registerRepository.findById(sourceId)).willReturn(Mono.just(source));
        given(registerRepository.findById(targetId)).willReturn(Mono.just(target));
        given(source.isActive()).willReturn(true);
        given(target.isActive()).willReturn(true);
        given(registerRepository.save(source)).willReturn(Mono.just(source));
        given(registerRepository.save(target)).willReturn(Mono.just(target));
        given(operationRepository.save(transfer)).willReturn(Mono.just(transfer));
        given(converter.toResponse(transfer)).willReturn(response);
        // when
        Mono<OperationResponse> result = service.transfer(sourceId, targetId, amount);
        // then
        StepVerifier.create(result)
                .expectNext(response)
                .verifyComplete();
        InOrder order = inOrder(converter, registerRepository, operationRepository);
        then(converter).should(order).createOperation(amount);
        then(registerRepository).should(order).findById(sourceId);
        then(registerRepository).should(order).findById(targetId);
        then(converter).should(order).updateSource(source, transfer);
        then(converter).should(order).updateTarget(target, transfer);
        then(registerRepository).should(order).save(source);
        then(registerRepository).should(order).save(target);
        then(operationRepository).should(order).save(transfer);
        then(converter).should(order).toResponse(transfer);
        verifyNoMoreInteractions(converter, registerRepository, operationRepository);
    }

    @Test
    void transfer_returnsError_dueToMissingSourceRegister(
            @Mock BigDecimal amount, @Mock Operation transfer) {
        // given
        String sourceId = "source name";
        String targetId = "target name";
        given(converter.createOperation(amount)).willReturn(transfer);
        given(registerRepository.findById(sourceId)).willReturn(Mono.empty());
        // when
        Mono<OperationResponse> result = service.transfer(sourceId, targetId, amount);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(RegisterNotFoundException.class)
                        .hasMessage(sourceId + " register not found or not active")
                        .hasNoCause());
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(sourceId);
        then(converter).should(never()).updateSource(any(Register.class), same(transfer));
        then(converter).should(never()).toResponse(transfer);
        then(registerRepository).should(never()).save(any(Register.class));
        then(operationRepository).shouldHaveNoInteractions();
        verifyNoMoreInteractions(converter, registerRepository);
    }

    @Test
    void transfer_returnsError_dueToInactiveSourceRegister(
            @Mock BigDecimal amount, @Mock Operation transfer, @Mock Register source) {
        // given
        String sourceId = "source name";
        String targetId = "target name";
        given(converter.createOperation(amount)).willReturn(transfer);
        given(registerRepository.findById(sourceId)).willReturn(Mono.just(source));
        given(source.isActive()).willReturn(false);
        // when
        Mono<OperationResponse> result = service.transfer(sourceId, targetId, amount);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(RegisterNotFoundException.class)
                        .hasMessage(sourceId + " register not found or not active")
                        .hasNoCause());
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(sourceId);
        then(converter).should(never()).updateSource(any(Register.class), same(transfer));
        then(converter).should(never()).toResponse(transfer);
        then(registerRepository).should(never()).save(any(Register.class));
        then(operationRepository).shouldHaveNoInteractions();
        verifyNoMoreInteractions(converter, registerRepository);
    }

    @Test
    void transfer_returnsError_dueToSameSourceAndTargetRegister(@Mock BigDecimal amount) {
        // given
        String registerId = "register name";
        // when
        Mono<OperationResponse> result = service.transfer(registerId, registerId, amount);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(InvalidTransferException.class)
                        .hasMessage("source and target register must be different")
                        .hasNoCause());
        then(converter).shouldHaveNoInteractions();
        then(registerRepository).shouldHaveNoInteractions();
        then(operationRepository).shouldHaveNoInteractions();
    }

    @Test
    void transfer_returnsError_dueToMissingTargetRegister(
            @Mock BigDecimal amount, @Mock Operation transfer, @Mock Register source) {
        // given
        String sourceId = "source name";
        String targetId = "target name";
        given(converter.createOperation(amount)).willReturn(transfer);
        given(registerRepository.findById(sourceId)).willReturn(Mono.just(source));
        given(registerRepository.findById(targetId)).willReturn(Mono.empty());
        given(source.isActive()).willReturn(true);
        // when
        Mono<OperationResponse> result = service.transfer(sourceId, targetId, amount);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(RegisterNotFoundException.class)
                        .hasMessage(targetId + " register not found or not active")
                        .hasNoCause());
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(sourceId);
        then(registerRepository).should().findById(targetId);
        then(converter).should(never()).updateSource(source, transfer);
        then(converter).should(never()).toResponse(transfer);
        then(registerRepository).should(never()).save(any(Register.class));
        then(operationRepository).shouldHaveNoInteractions();
        verifyNoMoreInteractions(converter, registerRepository);
    }

    @Test
    void transfer_returnsError_dueToInactiveTargetRegister(
            @Mock BigDecimal amount,
            @Mock Operation transfer,
            @Mock Register source,
            @Mock Register target) {
        // given
        String sourceId = "source name";
        String targetId = "target name";
        given(converter.createOperation(amount)).willReturn(transfer);
        given(registerRepository.findById(sourceId)).willReturn(Mono.just(source));
        given(registerRepository.findById(targetId)).willReturn(Mono.just(target));
        given(source.isActive()).willReturn(true);
        given(target.isActive()).willReturn(false);
        // when
        Mono<OperationResponse> result = service.transfer(sourceId, targetId, amount);
        // then
        StepVerifier.create(result)
                .verifyErrorSatisfies(thrown -> assertThat(thrown)
                        .isExactlyInstanceOf(RegisterNotFoundException.class)
                        .hasMessage(targetId + " register not found or not active")
                        .hasNoCause());
        then(converter).should().createOperation(amount);
        then(registerRepository).should().findById(sourceId);
        then(registerRepository).should().findById(targetId);
        then(converter).should(never()).updateSource(source, transfer);
        then(converter).should(never()).toResponse(transfer);
        then(registerRepository).should(never()).save(any(Register.class));
        then(operationRepository).shouldHaveNoInteractions();
        verifyNoMoreInteractions(converter, registerRepository);
    }
}
