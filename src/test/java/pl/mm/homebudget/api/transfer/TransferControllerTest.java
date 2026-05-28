package pl.mm.homebudget.api.transfer;

import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.TransferCommand;
import pl.mm.homebudget.application.RegisterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private RegisterService service;
    @InjectMocks
    private TransferController controller;

    @Test
    void createTransfer_invokesService_andReturnsCreatedOperation() {
        // given
        String sourceRegisterId = "Wallet";
        String targetRegisterId = "Savings";
        BigDecimal amount = BigDecimal.TEN;
        TransferCommand request = new TransferCommand(sourceRegisterId, targetRegisterId, amount);
        OperationResponse operation = new OperationResponse(1L, OffsetDateTime.now(), amount)
                .sourceRegisterId(sourceRegisterId)
                .targetRegisterId(targetRegisterId);
        given(service.transfer(sourceRegisterId, targetRegisterId, amount)).willReturn(Mono.just(operation));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/transfers").build());
        // when
        Mono<OperationResponse> result = controller.createTransfer(Mono.just(request), exchange);
        // then
        StepVerifier.create(result)
                .expectNext(operation)
                .verifyComplete();
        assertThat(exchange.getResponse().getHeaders().getLocation()).hasToString("/operations/1");
        then(service).should().transfer(sourceRegisterId, targetRegisterId, amount);
        then(service).shouldHaveNoMoreInteractions();
    }
}
