package com.solera.budgeting;

import com.solera.budgeting.model.RechargeRequest;
import com.solera.budgeting.model.TransferRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RegisterControllerTest {

    @Mock
    private RegisterService service;
    @InjectMocks
    private RegisterController controller;

    @Test
    void recharge_invokesServiceWithGivenRequest(@Mock RechargeRequest request) {
        // given
        given(service.recharge(request)).willReturn(Mono.empty());
        // when
        Mono<Void> result = controller.recharge(request);
        // then
        StepVerifier.create(result)
                .verifyComplete();
        then(service).should().recharge(request);
        then(service).shouldHaveNoMoreInteractions();
    }

    @Test
    void transfer_invokesServiceWithGivenRequest(@Mock TransferRequest request) {
        // given
        given(service.transfer(request)).willReturn(Mono.empty());
        // when
        Mono<Void> result = controller.transfer(request);
        // then
        StepVerifier.create(result)
                .verifyComplete();
        then(service).should().transfer(request);
        then(service).shouldHaveNoMoreInteractions();
    }

    @Test
    void getBalances_InvokesService() {
        // given
        String balance = "test balance";
        given(service.getBalances()).willReturn(Flux.just(balance));
        // when
        Flux<String> result = controller.getBalances();
        // then
        StepVerifier.create(result)
                .expectNext(balance)
                .verifyComplete();
        then(service).should().getBalances();
        then(service).shouldHaveNoMoreInteractions();
    }
}
