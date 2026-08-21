package pl.mm.homebudget.api.ledger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.homebudget.api.dto.OperationType;
import pl.mm.homebudget.domain.LedgerHasher;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///ledgerapitest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.url=jdbc:h2:mem:ledgerapitest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        })
@AutoConfigureWebTestClient
class LedgerControllerIT {

    private static final LedgerHasher LEDGER_HASHER = new LedgerHasher();

    @Autowired
    private WebTestClient testClient;

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
    void verifyLedgerReturnsVerifiedGenesisStateForEmptyLedger() {
        testClient.get().uri("/ledger/verify")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .json("""
                        {
                          "status": "VERIFIED",
                          "operationCount": 0,
                          "verifiedThroughSequence": 0,
                          "latestSequenceNumber": 0,
                          "ledgerHeadHash": "%s",
                          "mismatch": null
                        }
                        """.formatted(LedgerHasher.GENESIS_HASH), JsonCompareMode.LENIENT)
                .jsonPath("$.checkedAt").exists();
    }

    @Test
    void verifyLedgerReturnsVerifiedChainAfterRechargeAndTransfer() {
        createRecharge();
        OperationResponse transfer = createTransfer();

        testClient.get().uri("/ledger/verify")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .json("""
                        {
                          "status": "VERIFIED",
                          "operationCount": 2,
                          "verifiedThroughSequence": 2,
                          "latestSequenceNumber": 2,
                          "ledgerHeadHash": "%s",
                          "mismatch": null
                        }
                        """.formatted(transfer.getOperationHash()), JsonCompareMode.LENIENT)
                .jsonPath("$.checkedAt").exists();
    }

    @Test
    void verifyLedgerReturnsInvalidAfterDirectTestOnlyDatabaseTamper() {
        createRecharge();
        OperationResponse transfer = createTransfer();
        tamperAmount(transfer.getId());

        testClient.get().uri("/ledger/verify")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .json("""
                        {
                          "status": "INVALID",
                          "operationCount": 2,
                          "verifiedThroughSequence": 1,
                          "latestSequenceNumber": 2,
                          "ledgerHeadHash": "%s",
                          "mismatch": {
                            "sequenceNumber": 2,
                            "operationId": %d,
                            "reason": "Stored payload hash does not match the recalculated payload hash."
                          }
                        }
                        """.formatted(transfer.getOperationHash(), transfer.getId()),
                        JsonCompareMode.LENIENT)
                .jsonPath("$.checkedAt").exists();
    }

    @Test
    void getOperationProofReturnsFullProofForValidOperation() {
        OperationResponse recharge = createRecharge();
        String canonicalTimestamp = recharge.getTimestamp().toInstant().toString();
        String payloadHash = LEDGER_HASHER.payloadHash(
                OperationType.RECHARGE,
                recharge.getTimestamp().toInstant(),
                recharge.getAmount(),
                null,
                "Wallet");
        String operationHash = LEDGER_HASHER.operationHash(1, LedgerHasher.GENESIS_HASH, payloadHash);

        testClient.get().uri("/operations/{operationId}/proof", recharge.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .json("""
                        {
                          "operationId": %d,
                          "operationType": "RECHARGE",
                          "amount": 2500.00,
                          "sourceRegisterId": null,
                          "targetRegisterId": "Wallet",
                          "sequenceNumber": 1,
                          "previousHash": "%s",
                          "payloadHash": "%s",
                          "operationHash": "%s",
                          "canonicalPayload": "ledgerVersion=1\\noperationType=RECHARGE\\ntimestamp=%s\\namount=2500.00\\nsourceRegisterId=null\\ntargetRegisterId=Wallet",
                          "canonicalOperationHashInput": "chainVersion=home-budget-ledger-v1\\nsequenceNumber=1\\npreviousHash=%s\\npayloadHash=%s",
                          "expectedPreviousHash": "%s",
                          "expectedPayloadHash": "%s",
                          "expectedOperationHash": "%s",
                          "previousHashValid": true,
                          "payloadHashValid": true,
                          "operationHashValid": true,
                          "valid": true
                        }
                        """.formatted(
                        recharge.getId(),
                        LedgerHasher.GENESIS_HASH,
                        payloadHash,
                        operationHash,
                        canonicalTimestamp,
                        LedgerHasher.GENESIS_HASH,
                        payloadHash,
                        LedgerHasher.GENESIS_HASH,
                        payloadHash,
                        operationHash), JsonCompareMode.LENIENT)
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void getOperationProofReturnsInvalidProofWhenPreviousOperationIsMissing() {
        OperationResponse recharge = createRecharge();
        OperationResponse transfer = createTransfer();
        deleteOperation(recharge.getId());
        String canonicalTimestamp = transfer.getTimestamp().toInstant().toString();
        String payloadHash = LEDGER_HASHER.payloadHash(
                OperationType.TRANSFER,
                transfer.getTimestamp().toInstant(),
                transfer.getAmount(),
                "Wallet",
                "Food expenses");

        testClient.get().uri("/operations/{operationId}/proof", transfer.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .json("""
                        {
                          "operationId": %d,
                          "operationType": "TRANSFER",
                          "amount": 1500.00,
                          "sourceRegisterId": "Wallet",
                          "targetRegisterId": "Food expenses",
                          "sequenceNumber": 2,
                          "canonicalPayload": "ledgerVersion=1\\noperationType=TRANSFER\\ntimestamp=%s\\namount=1500.00\\nsourceRegisterId=Wallet\\ntargetRegisterId=Food expenses",
                          "canonicalOperationHashInput": null,
                          "expectedPreviousHash": null,
                          "expectedPayloadHash": "%s",
                          "expectedOperationHash": null,
                          "previousHashValid": false,
                          "payloadHashValid": true,
                          "operationHashValid": false,
                          "valid": false
                        }
                        """.formatted(
                        transfer.getId(),
                        canonicalTimestamp,
                        payloadHash), JsonCompareMode.LENIENT)
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void getOperationProofReturnsNotFoundForUnknownOperation() {
        testClient.get().uri("/operations/{operationId}/proof", 999L)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .json("""
                        {
                          "title": "Not Found",
                          "status": 404,
                          "detail": "999 operation not found",
                          "instance": "/operations/999/proof"
                        }
                        """, JsonCompareMode.STRICT);
    }

    private OperationResponse createRecharge() {
        return testClient.post().uri("/operations/recharges")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"registerId\":\"Wallet\",\"amount\":2500}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OperationResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private OperationResponse createTransfer() {
        return testClient.post().uri("/operations/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Food expenses\",\"amount\":1500}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OperationResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private void tamperAmount(Long operationId) {
        databaseClient.sql("UPDATE OPERATIONS SET AMOUNT = 1501.00 WHERE ID = :operationId")
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
