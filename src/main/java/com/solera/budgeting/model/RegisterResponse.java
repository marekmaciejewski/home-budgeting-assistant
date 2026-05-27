package com.solera.budgeting.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Register")
public record RegisterResponse(
        @Schema(description = "The register identifier", example = "Wallet")
        String id,
        @Schema(description = "The current register balance", example = "1000.00")
        BigDecimal balance) {
}
