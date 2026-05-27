package com.solera.budgeting.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Getter
@Setter
@Schema(description = "Transfer request")
public class TransferRequest {

    @NotBlank
    @Schema(description = "The name of the source register", requiredMode = REQUIRED, example = "Wallet")
    private String sourceRegister;
    @NotBlank
    @Schema(description = "The name of the target register", requiredMode = REQUIRED, example = "Food expenses")
    private String targetRegister;
    @NotNull
    @Positive
    @Schema(description = "The amount to be transferred", requiredMode = REQUIRED, example = "1500")
    private BigDecimal amount;

}
