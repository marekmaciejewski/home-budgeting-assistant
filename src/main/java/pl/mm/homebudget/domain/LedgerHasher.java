package pl.mm.homebudget.domain;

import org.springframework.stereotype.Component;
import pl.mm.homebudget.api.dto.OperationType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class LedgerHasher {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private static final String LEDGER_VERSION = "1";
    private static final String CHAIN_VERSION = "home-budget-ledger-v1";

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

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
