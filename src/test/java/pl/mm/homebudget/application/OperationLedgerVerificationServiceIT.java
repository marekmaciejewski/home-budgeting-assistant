package pl.mm.homebudget.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import pl.mm.homebudget.api.dto.LedgerMismatch;
import pl.mm.homebudget.api.dto.LedgerStatus;
import pl.mm.homebudget.api.dto.LedgerVerificationResponse;
import pl.mm.homebudget.api.dto.OperationProofResponse;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.OperationType;
import pl.mm.homebudget.domain.LedgerHasher;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///ledgerverificationserviceit;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.url=jdbc:h2:mem:ledgerverificationserviceit;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        })
class OperationLedgerVerificationServiceIT {

    private static final String WRONG_HASH = "f".repeat(64);

    @Autowired
    private OperationLedgerVerificationService service;

    @Autowired
    private RegisterService registerService;

    @Autowired
    private LedgerHasher ledgerHasher;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void resetState() {
        databaseClient.sql("DELETE FROM OPERATIONS")
                .fetch()
                .rowsUpdated()
                .then(databaseClient.sql("""
                        UPDATE REGISTERS
                        SET BALANCE = CASE ID
                            WHEN 'Wallet' THEN 1000.00
                            WHEN 'Savings' THEN 5000.00
                            WHEN 'Insurance policy' THEN 0.00
                            WHEN 'Food expenses' THEN 0.00
                            ELSE BALANCE
                        END
                        """)
                        .fetch()
                        .rowsUpdated())
                .block();
    }

    @Test
    void returnsVerifiedGenesisStateForEmptyLedger() {
        LedgerVerificationResponse response = service.verifyLedger().block();

        assertThat(response)
                .isNotNull()
                .returns(LedgerStatus.VERIFIED, from(LedgerVerificationResponse::getStatus))
                .returns(0L, from(LedgerVerificationResponse::getOperationCount))
                .returns(0L, from(LedgerVerificationResponse::getVerifiedThroughSequence))
                .returns(0L, from(LedgerVerificationResponse::getLatestSequenceNumber))
                .returns(LedgerHasher.GENESIS_HASH, from(LedgerVerificationResponse::getLedgerHeadHash))
                .returns(null, from(LedgerVerificationResponse::getMismatch));
        assertThat(response.getCheckedAt()).isNotNull();
    }

    @Test
    void verifiesValidMultiOperationChainThroughLatestSequence() {
        createRecharge();
        OperationResponse transfer = createTransfer();

        LedgerVerificationResponse response = service.verifyLedger().block();

        assertThat(response)
                .isNotNull()
                .returns(LedgerStatus.VERIFIED, from(LedgerVerificationResponse::getStatus))
                .returns(2L, from(LedgerVerificationResponse::getOperationCount))
                .returns(2L, from(LedgerVerificationResponse::getVerifiedThroughSequence))
                .returns(2L, from(LedgerVerificationResponse::getLatestSequenceNumber))
                .returns(transfer.getOperationHash(), from(LedgerVerificationResponse::getLedgerHeadHash))
                .returns(null, from(LedgerVerificationResponse::getMismatch));
    }

    @Test
    void reportsFirstSequenceGapAndContinuesObservingLedgerSummary() {
        createRecharge();
        OperationResponse transfer = createTransfer();
        createAdditionalRecharge();
        updateSequenceNumber(transfer.getId(), 4L);

        LedgerVerificationResponse response = service.verifyLedger().block();

        assertInvalid(
                response,
                3L,
                2L,
                null,
                "Missing operation for sequence 2.",
                1L,
                4L,
                transfer.getOperationHash());
    }

    @Test
    void reportsFirstPreviousHashMismatch() {
        createRecharge();
        OperationResponse transfer = createTransfer();
        updatePreviousHash(transfer.getId(), WRONG_HASH);

        LedgerVerificationResponse response = service.verifyLedger().block();

        assertInvalid(
                response,
                2L,
                2L,
                transfer.getId(),
                "Stored previous hash does not match the expected previous hash.",
                1L,
                2L,
                transfer.getOperationHash());
    }

    @Test
    void reportsFirstPayloadMismatchAfterTampering() {
        createRecharge();
        OperationResponse transfer = createTransfer();
        updateTargetRegister(transfer.getId(), "Savings");

        LedgerVerificationResponse response = service.verifyLedger().block();

        assertInvalid(
                response,
                2L,
                2L,
                transfer.getId(),
                "Stored payload hash does not match the recalculated payload hash.",
                1L,
                2L,
                transfer.getOperationHash());
    }

    @Test
    void reportsFirstOperationHashMismatchAfterTampering() {
        createRecharge();
        OperationResponse transfer = createTransfer();
        updateOperationHash(transfer.getId(), WRONG_HASH);

        LedgerVerificationResponse response = service.verifyLedger().block();

        assertInvalid(
                response,
                2L,
                2L,
                transfer.getId(),
                "Stored operation hash does not match the recalculated operation hash.",
                1L,
                2L,
                WRONG_HASH);
    }

    @Test
    void returnsInvalidProofWhenPreviousSequenceIsMissing() {
        OperationResponse recharge = createRecharge();
        OperationResponse transfer = createTransfer();
        deleteOperation(recharge.getId());
        String expectedPayloadHash = ledgerHasher.payloadHash(
                OperationType.TRANSFER,
                transfer.getTimestamp().toInstant(),
                transfer.getAmount(),
                "Wallet",
                "Food expenses");

        OperationProofResponse proof = service.getOperationProof(transfer.getId()).block();

        assertThat(proof)
                .isNotNull()
                .returns(2L, from(OperationProofResponse::getSequenceNumber))
                .returns(null, from(OperationProofResponse::getExpectedPreviousHash))
                .returns(expectedPayloadHash, from(OperationProofResponse::getExpectedPayloadHash))
                .returns(null, from(OperationProofResponse::getCanonicalOperationHashInput))
                .returns(null, from(OperationProofResponse::getExpectedOperationHash))
                .returns(false, from(OperationProofResponse::getPreviousHashValid))
                .returns(true, from(OperationProofResponse::getPayloadHashValid))
                .returns(false, from(OperationProofResponse::getOperationHashValid))
                .returns(false, from(OperationProofResponse::getValid));
    }

    private void assertInvalid(
            LedgerVerificationResponse response,
            long operationCount,
            long mismatchSequenceNumber,
            Long mismatchOperationId,
            String mismatchReason,
            long verifiedThroughSequence,
            long latestSequenceNumber,
            String ledgerHeadHash) {
        assertThat(response)
                .isNotNull()
                .returns(LedgerStatus.INVALID, from(LedgerVerificationResponse::getStatus))
                .returns(operationCount, from(LedgerVerificationResponse::getOperationCount))
                .returns(verifiedThroughSequence, from(LedgerVerificationResponse::getVerifiedThroughSequence))
                .returns(latestSequenceNumber, from(LedgerVerificationResponse::getLatestSequenceNumber))
                .returns(ledgerHeadHash, from(LedgerVerificationResponse::getLedgerHeadHash));
        assertThat(response.getCheckedAt()).isNotNull();
        assertThat(response.getMismatch())
                .isNotNull()
                .returns(mismatchSequenceNumber, from(LedgerMismatch::getSequenceNumber))
                .returns(mismatchOperationId, from(LedgerMismatch::getOperationId))
                .returns(mismatchReason, from(LedgerMismatch::getReason));
    }

    private OperationResponse createRecharge() {
        return registerService.recharge("Wallet", new BigDecimal("2500.00")).block();
    }

    private OperationResponse createTransfer() {
        return registerService.transfer(
                "Wallet",
                "Food expenses",
                new BigDecimal("1500.00")).block();
    }

    private OperationResponse createAdditionalRecharge() {
        return registerService.recharge("Savings", new BigDecimal("100.00")).block();
    }

    private void updateSequenceNumber(Long operationId, long sequenceNumber) {
        databaseClient.sql("UPDATE OPERATIONS SET SEQUENCE_NUMBER = :sequenceNumber WHERE ID = :operationId")
                .bind("sequenceNumber", sequenceNumber)
                .bind("operationId", operationId)
                .fetch()
                .rowsUpdated()
                .block();
    }

    private void updatePreviousHash(Long operationId, String previousHash) {
        databaseClient.sql("UPDATE OPERATIONS SET PREVIOUS_HASH = :previousHash WHERE ID = :operationId")
                .bind("previousHash", previousHash)
                .bind("operationId", operationId)
                .fetch()
                .rowsUpdated()
                .block();
    }

    private void updateTargetRegister(Long operationId, String targetRegisterId) {
        databaseClient.sql("UPDATE OPERATIONS SET TARGET_REGISTER_ID = :targetRegisterId WHERE ID = :operationId")
                .bind("targetRegisterId", targetRegisterId)
                .bind("operationId", operationId)
                .fetch()
                .rowsUpdated()
                .block();
    }

    private void updateOperationHash(Long operationId, String operationHash) {
        databaseClient.sql("UPDATE OPERATIONS SET OPERATION_HASH = :operationHash WHERE ID = :operationId")
                .bind("operationHash", operationHash)
                .bind("operationId", operationId)
                .fetch()
                .rowsUpdated()
                .block();
    }

    private void deleteOperation(Long operationId) {
        databaseClient.sql("DELETE FROM OPERATIONS WHERE ID = :operationId")
                .bind("operationId", operationId)
                .fetch()
                .rowsUpdated()
                .block();
    }
}
