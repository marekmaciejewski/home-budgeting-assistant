package pl.mm.homebudget.api.transfer;

import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.TransferCommand;
import pl.mm.homebudget.application.RegisterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;

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
        OperationResponse operation = new OperationResponse(
                1L, Instant.now(), amount, sourceRegisterId, targetRegisterId);
        given(service.transfer(sourceRegisterId, targetRegisterId, amount)).willReturn(Mono.just(operation));
        // when
        Mono<ResponseEntity<OperationResponse>> result = controller.createTransfer(request);
        // then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    assertThat(response.getHeaders().getLocation()).hasToString("/operations/1");
                    assertThat(response.getBody()).isSameAs(operation);
                })
                .verifyComplete();
        then(service).should().transfer(sourceRegisterId, targetRegisterId, amount);
        then(service).shouldHaveNoMoreInteractions();
    }
}
