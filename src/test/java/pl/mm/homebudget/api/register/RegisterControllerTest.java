package pl.mm.homebudget.api.register;

import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.RechargeCommand;
import pl.mm.homebudget.api.dto.RegisterResponse;
import pl.mm.homebudget.application.RegisterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RegisterControllerTest {

    @Mock
    private RegisterService service;
    @InjectMocks
    private RegisterController controller;

    @Test
    void getRegisters_invokesService() {
        // given
        RegisterResponse register = new RegisterResponse("Wallet", BigDecimal.TEN);
        given(service.getRegisters()).willReturn(Flux.just(register));
        // when
        Flux<RegisterResponse> result = controller.getRegisters();
        // then
        StepVerifier.create(result)
                .expectNext(register)
                .verifyComplete();
        then(service).should().getRegisters();
        then(service).shouldHaveNoMoreInteractions();
    }

    @Test
    void getRegister_invokesServiceWithGivenRegisterId() {
        // given
        String registerId = "Wallet";
        RegisterResponse register = new RegisterResponse(registerId, BigDecimal.TEN);
        given(service.getRegister(registerId)).willReturn(Mono.just(register));
        // when
        Mono<RegisterResponse> result = controller.getRegister(registerId);
        // then
        StepVerifier.create(result)
                .expectNext(register)
                .verifyComplete();
        then(service).should().getRegister(registerId);
        then(service).shouldHaveNoMoreInteractions();
    }

    @Test
    void createRecharge_invokesService_andReturnsCreatedOperation() {
        // given
        String registerId = "Wallet";
        BigDecimal amount = BigDecimal.TEN;
        RechargeCommand request = new RechargeCommand(amount);
        OperationResponse operation = new OperationResponse(1L, Instant.now(), amount, null, registerId);
        given(service.recharge(registerId, amount)).willReturn(Mono.just(operation));
        // when
        Mono<ResponseEntity<OperationResponse>> result = controller.createRecharge(registerId, request);
        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    assertThat(response.getHeaders().getLocation()).hasToString("/operations/1");
                    assertThat(response.getBody()).isSameAs(operation);
                })
                .verifyComplete();
        then(service).should().recharge(registerId, amount);
        then(service).shouldHaveNoMoreInteractions();
    }

}
