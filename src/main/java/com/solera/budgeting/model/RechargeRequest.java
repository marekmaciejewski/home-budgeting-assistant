package com.solera.budgeting.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Recharge request")
public class RechargeRequest {

    @NotBlank
    @Schema(description = "The name of the register to recharge", required = true, example = "Wallet")
    private String registerName;
    @Positive
    @Schema(description = "The amount to recharge with", required = true, example = "2500")
    private BigDecimal amount;

}
