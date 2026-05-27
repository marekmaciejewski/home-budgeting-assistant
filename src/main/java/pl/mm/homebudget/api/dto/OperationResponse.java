package pl.mm.homebudget.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Balance operation")
public record OperationResponse(
        @Schema(description = "The operation identifier", example = "1")
        Long id,
        @Schema(description = "The operation timestamp")
        Instant timestamp,
        @Schema(description = "The operation amount", example = "1500.00")
        BigDecimal amount,
        @Schema(description = "The source register identifier", example = "Wallet")
        String sourceRegisterId,
        @Schema(description = "The target register identifier", example = "Food expenses")
        String targetRegisterId) {
}
