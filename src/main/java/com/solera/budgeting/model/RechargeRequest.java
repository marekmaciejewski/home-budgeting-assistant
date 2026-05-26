package com.solera.budgeting.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Getter
@Setter
@Schema(description = "Recharge request")
public class RechargeRequest {

    @NotBlank
    @Schema(description = "The name of the register to recharge", requiredMode = REQUIRED, example = "Wallet")
    private String registerName;
    @Positive
    @Schema(description = "The amount to recharge with", requiredMode = REQUIRED, example = "2500")
    private BigDecimal amount;

}
