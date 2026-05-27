package com.solera.budgeting;

import com.solera.budgeting.model.OperationResponse;
import com.solera.budgeting.model.RechargeCommand;
import com.solera.budgeting.model.RegisterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import java.net.URI;

@Tag(name = "registers")
@RestController
@RequestMapping(path = "/registers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService service;

    @Operation(summary = "Get all active registers.")
    @ApiResponse(responseCode = "200", description = "Success")
    @GetMapping
    public Flux<RegisterResponse> getRegisters() {
        return service.getRegisters();
    }

    @Operation(summary = "Get an active register by id.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Resource Not Found")
    @GetMapping("/{registerId}")
    public Mono<RegisterResponse> getRegister(@PathVariable String registerId) {
        return service.getRegister(registerId);
    }

    @Operation(summary = "Recharge a particular register with provided amount.")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "404", description = "Resource Not Found")
    @PostMapping(path = "/{registerId}/recharges", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<OperationResponse>> createRecharge(
            @PathVariable String registerId,
            @Valid @RequestBody RechargeCommand request) {
        return service.recharge(registerId, request.amount())
                .map(RegisterController::createdOperation);
    }

    static ResponseEntity<OperationResponse> createdOperation(OperationResponse response) {
        return ResponseEntity.created(URI.create("/operations/" + response.id()))
                .body(response);
    }
}
