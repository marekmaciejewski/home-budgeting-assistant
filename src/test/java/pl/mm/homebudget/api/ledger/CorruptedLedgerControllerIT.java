package pl.mm.homebudget.api.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.mm.testsupport.FixedClockTestConfiguration;
import pl.mm.testsupport.LedgerHttpTestConfiguration;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///corruptedledgerapitest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.url=jdbc:h2:mem:corruptedledgerapitest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        })
@AutoConfigureWebTestClient
@Import({FixedClockTestConfiguration.class, LedgerHttpTestConfiguration.class})
@SqlConfig(dataSource = "sqlDataSource")
class CorruptedLedgerControllerIT {

    private static final String RESET_LEDGER = "/db/test-data/ledger/reset-ledger.sql";
    private static final String VALID_TWO_OPERATION_LEDGER =
            "/db/test-data/ledger/valid-two-operation-ledger.sql";
    private static final String WRONG_HASH = "f".repeat(64);
    private static final String FIRST_OPERATION_HASH =
            "bd17cfa30eb03af0e742822e57be0fab4fba8c2e6082d557ebd40c855177474c";
    private static final String SECOND_OPERATION_HASH =
            "e324da21bb55dc8730068ce1f132a61fb26b3d06afae0a7165aa09dbb6b7cb1e";
    private static final String SECOND_PAYLOAD_HASH =
            "4468247f24e9a9010afd19069ed85440990c8b92a8d68ed243c64fdfa24debca";

    @Autowired
    private WebTestClient testClient;

    @Test
    @Sql(
            scripts = {RESET_LEDGER, VALID_TWO_OPERATION_LEDGER},
            statements = "UPDATE OPERATIONS SET SEQUENCE_NUMBER = 3 WHERE ID = 102")
    void verifyLedger_reportsSequenceGap() {
        expectInvalidLedger(
                null,
                "Missing operation for sequence 2.",
                2,
                1,
                3,
                SECOND_OPERATION_HASH);
    }

    @Test
    @Sql(
            scripts = {RESET_LEDGER, VALID_TWO_OPERATION_LEDGER},
            statements = "UPDATE OPERATIONS SET SEQUENCE_NUMBER = 0 WHERE ID = 101")
    void verifyLedger_reportsInvalidFirstSequenceAndContinuesScanning() {
        expectInvalidLedger(
                101L,
                "Ledger sequence must start at 1.",
                1,
                0,
                2,
                SECOND_OPERATION_HASH);
    }

    @Test
    @Sql(
            scripts = {RESET_LEDGER, VALID_TWO_OPERATION_LEDGER},
            statements = "UPDATE OPERATIONS SET PREVIOUS_HASH = 'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff' WHERE ID = 102")
    void verifyLedger_reportsFirstPreviousHashMismatch() {
        expectInvalidLedger(
                102L,
                "Stored previous hash does not match the expected previous hash.",
                2,
                1,
                2,
                SECOND_OPERATION_HASH);
    }

    @Test
    @Sql(
            scripts = {RESET_LEDGER, VALID_TWO_OPERATION_LEDGER},
            statements = "UPDATE OPERATIONS SET TARGET_REGISTER_ID = 'Savings' WHERE ID = 102")
    void verifyLedger_reportsFirstPayloadHashMismatch() {
        expectInvalidLedger(
                102L,
                "Stored payload hash does not match the recalculated payload hash.",
                2,
                1,
                2,
                SECOND_OPERATION_HASH);

        expectInvalidProof(false);
    }

    @Test
    @Sql(
            scripts = {RESET_LEDGER, VALID_TWO_OPERATION_LEDGER},
            statements = "UPDATE OPERATIONS SET OPERATION_HASH = 'ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff' WHERE ID = 102")
    void verifyLedger_reportsFirstOperationHashMismatch() {
        expectInvalidLedger(
                102L,
                "Stored operation hash does not match the recalculated operation hash.",
                2,
                1,
                2,
                WRONG_HASH);

        expectInvalidProof(true);
    }

    @Test
    @Sql(
            scripts = {RESET_LEDGER, VALID_TWO_OPERATION_LEDGER},
            statements = "DELETE FROM OPERATIONS WHERE ID = 101")
    void getOperationProof_returnsInvalidProof_whenPreviousOperationIsMissing() {
        testClient.get().uri("/operations/{operationId}/proof", 102)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .json("""
                        {
                          "operationId": 102,
                          "operationType": "TRANSFER",
                          "timestamp": "2026-06-01T10:15:30Z",
                          "amount": 1500.00,
                          "sourceRegisterId": "Wallet",
                          "targetRegisterId": "Food expenses",
                          "sequenceNumber": 2,
                          "previousHash": "%s",
                          "payloadHash": "%s",
                          "operationHash": "%s",
                          "canonicalPayload": "ledgerVersion=1\\noperationType=TRANSFER\\ntimestamp=2026-06-01T10:15:30Z\\namount=1500.00\\nsourceRegisterId=Wallet\\ntargetRegisterId=Food expenses",
                          "canonicalOperationHashInput": null,
                          "expectedPreviousHash": null,
                          "expectedPayloadHash": "%2$s",
                          "expectedOperationHash": null,
                          "previousHashValid": false,
                          "payloadHashValid": true,
                          "operationHashValid": false,
                          "valid": false
                        }
                        """.formatted(
                        FIRST_OPERATION_HASH,
                        SECOND_PAYLOAD_HASH,
                        SECOND_OPERATION_HASH), JsonCompareMode.STRICT);
    }

    private void expectInvalidLedger(
            Long mismatchOperationId,
            String mismatchReason,
            long mismatchSequenceNumber,
            long verifiedThroughSequence,
            long latestSequenceNumber,
            String ledgerHeadHash) {
        String operationIdJson = mismatchOperationId == null ? "null" : mismatchOperationId.toString();

        testClient.get().uri("/ledger/verify")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .json("""
                        {
                          "status": "INVALID",
                          "operationCount": 2,
                          "verifiedThroughSequence": %d,
                          "latestSequenceNumber": %d,
                          "ledgerHeadHash": "%s",
                          "checkedAt": "2026-06-01T10:15:30Z",
                          "mismatch": {
                            "sequenceNumber": %d,
                            "operationId": %s,
                            "reason": "%s"
                          }
                        }
                        """.formatted(
                        verifiedThroughSequence,
                        latestSequenceNumber,
                        ledgerHeadHash,
                        mismatchSequenceNumber,
                        operationIdJson,
                        mismatchReason), JsonCompareMode.STRICT);
    }

    private void expectInvalidProof(boolean payloadHashValid) {
        testClient.get().uri("/operations/102/proof")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.previousHashValid").isEqualTo(true)
                .jsonPath("$.payloadHashValid").isEqualTo(payloadHashValid)
                .jsonPath("$.operationHashValid").isEqualTo(false)
                .jsonPath("$.valid").isEqualTo(false);
    }
}
