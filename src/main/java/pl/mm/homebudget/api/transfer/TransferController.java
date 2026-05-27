package pl.mm.homebudget.api.transfer;

import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.TransferCommand;
import pl.mm.homebudget.application.RegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

@Tag(name = "transfers")
@RestController
@RequestMapping(path = "/transfers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TransferController {

    private final RegisterService service;

    @Operation(summary = "Transfer a provided amount from source to target register.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "404", description = "Resource Not Found")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<OperationResponse>> createTransfer(@Valid @RequestBody TransferCommand request) {
        return service.transfer(request.sourceRegisterId(), request.targetRegisterId(), request.amount())
                .map(TransferController::createdOperation);
    }

    private static ResponseEntity<OperationResponse> createdOperation(OperationResponse response) {
        return ResponseEntity.created(URI.create("/operations/" + response.id()))
                .body(response);
    }
}
