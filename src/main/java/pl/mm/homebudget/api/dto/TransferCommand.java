package pl.mm.homebudget.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Transfer command")
public record TransferCommand(
        @NotBlank
        @Schema(description = "The source register identifier", requiredMode = REQUIRED, example = "Wallet")
        String sourceRegisterId,
        @NotBlank
        @Schema(description = "The target register identifier", requiredMode = REQUIRED, example = "Food expenses")
        String targetRegisterId,
        @NotNull
        @Positive
        @Schema(description = "The amount to transfer", requiredMode = REQUIRED, example = "1500")
        BigDecimal amount) {
}
