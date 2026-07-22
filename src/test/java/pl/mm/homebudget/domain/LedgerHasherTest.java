package pl.mm.homebudget.domain;

import org.junit.jupiter.api.Test;
import pl.mm.homebudget.api.dto.OperationType;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerHasherTest {

    private final LedgerHasher ledgerHasher = new LedgerHasher();

    @Test
    void hashesRechargeVector() {
        String payloadHash = ledgerHasher.payloadHash(
                OperationType.RECHARGE,
                Instant.parse("2026-06-01T10:15:30Z"),
                new BigDecimal("2500.00"),
                null,
                "Wallet");

        assertThat(payloadHash)
                .isEqualTo("296ec6d6025acbdc12da09bf833e445d2e4028c461d11b85cc3dc341c77dbc04");
        assertThat(ledgerHasher.operationHash(1, LedgerHasher.GENESIS_HASH, payloadHash))
                .isEqualTo("bd17cfa30eb03af0e742822e57be0fab4fba8c2e6082d557ebd40c855177474c");
    }

    @Test
    void canonicalizesTransferPayloadWithScaleTwoMoney() {
        String payload = ledgerHasher.canonicalPayload(
                OperationType.TRANSFER,
                Instant.parse("2026-06-01T10:15:30Z"),
                new BigDecimal("1500"),
                "Wallet",
                "Food expenses");

        assertThat(payload).isEqualTo("""
                ledgerVersion=1
                operationType=TRANSFER
                timestamp=2026-06-01T10:15:30Z
                amount=1500.00
                sourceRegisterId=Wallet
                targetRegisterId=Food expenses""");
    }
}
