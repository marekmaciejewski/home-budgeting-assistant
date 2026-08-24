package pl.mm.homebudget.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import pl.mm.homebudget.api.dto.OperationType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class LedgerHasher {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";
    private static final String LEDGER_VERSION = "1";
    private static final String CHAIN_VERSION = "home-budget-ledger-v1";

    private final ObjectProvider<MessageDigest> messageDigestProvider;

    public String canonicalPayload(
            OperationType operationType,
            Instant timestamp,
            BigDecimal amount,
            String sourceRegisterId,
            String targetRegisterId) {
        return String.join("\n",
                "ledgerVersion=" + LEDGER_VERSION,
                "operationType=" + operationType.getValue(),
                "timestamp=" + timestamp.toString(),
                "amount=" + normalizeAmount(amount),
                "sourceRegisterId=" + sourceRegisterId,
                "targetRegisterId=" + targetRegisterId);
    }

    public String payloadHash(
            OperationType operationType,
            Instant timestamp,
            BigDecimal amount,
            String sourceRegisterId,
            String targetRegisterId) {
        return sha256(canonicalPayload(operationType, timestamp, amount, sourceRegisterId, targetRegisterId));
    }

    public String canonicalOperationHashInput(long sequenceNumber, String previousHash, String payloadHash) {
        return String.join("\n",
                "chainVersion=" + CHAIN_VERSION,
                "sequenceNumber=" + sequenceNumber,
                "previousHash=" + previousHash,
                "payloadHash=" + payloadHash);
    }

    public String operationHash(long sequenceNumber, String previousHash, String payloadHash) {
        return sha256(canonicalOperationHashInput(sequenceNumber, previousHash, payloadHash));
    }

    private static String normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private String sha256(String value) {
        MessageDigest digest = messageDigestProvider.getObject();
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
