package pl.mm.homebudget.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Recharge command")
public record RechargeCommand(
        @NotNull
        @Positive
        @Schema(description = "The amount to recharge with", requiredMode = REQUIRED, example = "2500")
        BigDecimal amount) {
}
