package pl.mm.homebudget.api.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.testsupport.FixedClockTestConfiguration;
import pl.mm.testsupport.LedgerHttpTestConfiguration;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///validledgerapitest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.url=jdbc:h2:mem:validledgerapitest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        })
@AutoConfigureWebTestClient
@Import({FixedClockTestConfiguration.class, LedgerHttpTestConfiguration.class})
@SqlConfig(dataSource = "sqlDataSource")
@Sql("/db/test-data/ledger/reset-ledger.sql")
class LedgerControllerIT {

    private static final String GENESIS_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";
    private static final String RECHARGE_PAYLOAD_HASH =
            "296ec6d6025acbdc12da09bf833e445d2e4028c461d11b85cc3dc341c77dbc04";
    private static final String RECHARGE_OPERATION_HASH =
            "bd17cfa30eb03af0e742822e57be0fab4fba8c2e6082d557ebd40c855177474c";
    private static final String TRANSFER_OPERATION_HASH =
            "e324da21bb55dc8730068ce1f132a61fb26b3d06afae0a7165aa09dbb6b7cb1e";

    @Autowired
    private WebTestClient testClient;

    @Test
    void verifyLedger_returnsVerifiedGenesisState_forEmptyLedger() {
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
                          "checkedAt": "2026-06-01T10:15:30Z",
                          "mismatch": null
                        }
                        """.formatted(GENESIS_HASH), JsonCompareMode.STRICT);
    }

    @Test
    void verifyLedger_returnsVerifiedChain_afterRechargeAndTransfer() {
        createRecharge();
        createTransfer();

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
                          "checkedAt": "2026-06-01T10:15:30Z",
                          "mismatch": null
                        }
                        """.formatted(TRANSFER_OPERATION_HASH), JsonCompareMode.STRICT);
    }

    @Test
    void getOperationProof_returnsFullProof_forValidOperation() {
        OperationResponse recharge = createRecharge();

        testClient.get().uri("/operations/{operationId}/proof", recharge.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .json("""
                        {
                          "operationId": %d,
                          "operationType": "RECHARGE",
                          "timestamp": "2026-06-01T10:15:30Z",
                          "amount": 2500.00,
                          "sourceRegisterId": null,
                          "targetRegisterId": "Wallet",
                          "sequenceNumber": 1,
                          "previousHash": "%2$s",
                          "payloadHash": "%3$s",
                          "operationHash": "%4$s",
                          "canonicalPayload": "ledgerVersion=1\\noperationType=RECHARGE\\ntimestamp=2026-06-01T10:15:30Z\\namount=2500.00\\nsourceRegisterId=null\\ntargetRegisterId=Wallet",
                          "canonicalOperationHashInput": "chainVersion=home-budget-ledger-v1\\nsequenceNumber=1\\npreviousHash=%2$s\\npayloadHash=%3$s",
                          "expectedPreviousHash": "%2$s",
                          "expectedPayloadHash": "%3$s",
                          "expectedOperationHash": "%4$s",
                          "previousHashValid": true,
                          "payloadHashValid": true,
                          "operationHashValid": true,
                          "valid": true
                        }
                        """.formatted(
                        recharge.getId(),
                        GENESIS_HASH,
                        RECHARGE_PAYLOAD_HASH,
                        RECHARGE_OPERATION_HASH), JsonCompareMode.STRICT);
    }

    @Test
    void getOperationProof_returnsNotFound_forUnknownOperation() {
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

    private void createTransfer() {
        testClient.post().uri("/operations/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"sourceRegisterId\":\"Wallet\",\"targetRegisterId\":\"Food expenses\",\"amount\":1500}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OperationResponse.class);
    }
}
