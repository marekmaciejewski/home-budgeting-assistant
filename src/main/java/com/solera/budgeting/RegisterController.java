package com.solera.budgeting;

import com.solera.budgeting.model.RechargeRequest;
import com.solera.budgeting.model.TransferRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@Tag(name = "registers")
@RestController
@RequestMapping(path = "/registers", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RegisterController {

    @Operation(summary = "Recharge a particular register with provided amount.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "404", description = "Resource Not Found")
    @PostMapping("/recharge")
    public Mono<Void> recharge(@Valid @RequestBody RechargeRequest request) {
        return Mono.empty();
    }

    @Operation(summary = "Transfer a provided amount from source to target register.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "400", description = "Bad Request")
    @ApiResponse(responseCode = "404", description = "Resource Not Found")
    @PostMapping("/transfer")
    public Mono<Void> transfer(@Valid @RequestBody TransferRequest request) {
        return Mono.empty();
    }

    @Operation(summary = "Print the list of all registers accompanied by their balance.")
    @ApiResponse(responseCode = "200", description = "Success")
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getBalances() {
        return Flux.empty();
    }
}
