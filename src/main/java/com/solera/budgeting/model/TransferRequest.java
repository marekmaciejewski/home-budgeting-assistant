package com.solera.budgeting.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Transfer request")
public class TransferRequest {

    @NotBlank
    @Schema(description = "The name of the source register", required = true, example = "Wallet")
    private String sourceRegister;
    @NotBlank
    @Schema(description = "The name of the target register", required = true, example = "Food expenses")
    private String targetRegister;
    @Positive
    @Schema(description = "The amount to be transferred", required = true, example = "1500")
    private BigDecimal amount;

}
