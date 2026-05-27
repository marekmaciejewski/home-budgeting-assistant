package pl.mm.homebudget.api.operation;

import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.application.RegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Tag(name = "operations")
@RestController
@RequestMapping(path = "/operations", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class OperationController {

    private final RegisterService service;

    @Operation(summary = "Get all balance operations.")
    @ApiResponse(responseCode = "200", description = "Success")
    @GetMapping
    public Flux<OperationResponse> getOperations() {
        return service.getOperations();
    }

    @Operation(summary = "Get a balance operation by id.")
    @ApiResponse(responseCode = "200", description = "Success")
    @ApiResponse(responseCode = "404", description = "Resource Not Found")
    @GetMapping("/{operationId}")
    public Mono<OperationResponse> getOperation(@PathVariable long operationId) {
        return service.getOperation(operationId);
    }
}
