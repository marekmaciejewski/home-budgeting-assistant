package pl.mm.homebudget.api.operation;

import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.application.RegisterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class OperationControllerTest {

    @Mock
    private RegisterService service;
    @InjectMocks
    private OperationController controller;

    @Test
    void getOperations_invokesService() {
        // given
        OperationResponse operation = new OperationResponse(1L, Instant.now(), BigDecimal.TEN, null, "Wallet");
        given(service.getOperations()).willReturn(Flux.just(operation));
        // when
        Flux<OperationResponse> result = controller.getOperations();
        // then
        StepVerifier.create(result)
                .expectNext(operation)
                .verifyComplete();
        then(service).should().getOperations();
        then(service).shouldHaveNoMoreInteractions();
    }

    @Test
    void getOperation_invokesServiceWithGivenOperationId() {
        // given
        long operationId = 1L;
        OperationResponse operation = new OperationResponse(operationId, Instant.now(), BigDecimal.TEN, null, "Wallet");
        given(service.getOperation(operationId)).willReturn(Mono.just(operation));
        // when
        Mono<OperationResponse> result = controller.getOperation(operationId);
        // then
        StepVerifier.create(result)
                .expectNext(operation)
                .verifyComplete();
        then(service).should().getOperation(operationId);
        then(service).shouldHaveNoMoreInteractions();
    }
}
