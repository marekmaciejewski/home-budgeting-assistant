package pl.mm.homebudget.api.showcase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.mm.homebudget.api.dto.OperationResponse;
import pl.mm.testsupport.FixedClockTestConfiguration;

import java.util.stream.Stream;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.r2dbc.url=r2dbc:h2:mem:///tamperledgerapitest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.liquibase.url=jdbc:h2:mem:tamperledgerapitest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        })
@AutoConfigureWebTestClient
@Import(FixedClockTestConfiguration.class)
class LedgerTamperSimulationIT {

    private static final String TAMPER_PATH = "/ledger/tamper-simulations";
    private static final String GENESIS_HASH = "0".repeat(64);
    private static final String RECHARGE_PAYLOAD_HASH =
            "296ec6d6025acbdc12da09bf833e445d2e4028c461d11b85cc3dc341c77dbc04";
    private static final String RECHARGE_OPERATION_HASH =
            "bd17cfa30eb03af0e742822e57be0fab4fba8c2e6082d557ebd40c855177474c";
    private static final String SEED_REGISTERS = """
            [
              {"id":"Wallet","balance":1000.00},
              {"id":"Savings","balance":5000.00},
              {"id":"Insurance policy","balance":0.00},
              {"id":"Food expenses","balance":0.00}
            ]
            """;

    @Autowired
    private WebTestClient testClient;

    @BeforeEach
    void resetDemo() {
        testClient.post().uri("/demo/reset")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .json(SEED_REGISTERS);
    }

    @Test
    void capabilities_reportEnabledEphemeralTamperAndReset() {
        testClient.get().uri("/capabilities")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json("""
                        {
                          "ephemeralStorage": true,
                          "ledgerTamperSimulationAvailable": true
                        }
                        """, JsonCompareMode.STRICT);
    }

    @Test
    void simulateLedgerTamper_rejectsEmptyLedger() {
        expectProblem(
                postTamper("{}"),
                409,
                "Conflict",
                "Ledger is empty. Create an operation before simulating a mismatch.",
                TAMPER_PATH);
    }

    @Test
    void simulateLedgerTamper_defaultsToLatestOperationPayloadHash() {
        OperationResponse operation = createRecharge();
        String newPayloadHash = changedHash(RECHARGE_PAYLOAD_HASH);

        postTamper("{}")
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.operationId").isEqualTo(operation.getId().intValue())
                .jsonPath("$.sequenceNumber").isEqualTo(1)
                .jsonPath("$.mode").isEqualTo("PAYLOAD_HASH")
                .jsonPath("$.field").isEqualTo("PAYLOAD_HASH")
                .jsonPath("$.previousValue").isEqualTo(RECHARGE_PAYLOAD_HASH)
                .jsonPath("$.newValue").isEqualTo(newPayloadHash)
                .jsonPath("$.verificationPath").isEqualTo("/ledger/verify")
                .jsonPath("$.resetPath").isEqualTo("/demo/reset");

        testClient.get().uri("/ledger/verify")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("INVALID")
                .jsonPath("$.mismatch.reason")
                .isEqualTo("Stored payload hash does not match the recalculated payload hash.");
    }

    @Test
    void simulateLedgerTamper_rejectsExplicitNullModeWithoutChangingLedger() {
        createRecharge();

        expectProblem(
                postTamper("{\"mode\":null}"),
                400,
                "Bad Request",
                "Request body is invalid",
                TAMPER_PATH);

        testClient.get().uri("/ledger/verify")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("VERIFIED")
                .jsonPath("$.operationCount").isEqualTo(1);
    }

    @ParameterizedTest(name = "{0} changes {1} and produces the expected proof flags")
    @MethodSource("tamperModes")
    void simulateLedgerTamper_supportsFixedModes(
            String mode,
            String field,
            String previousValue,
            String newValue,
            String mismatchReason,
            boolean previousHashValid,
            boolean payloadHashValid,
            boolean operationHashValid) {
        OperationResponse operation = createRecharge();

        postTamper("{\"sequenceNumber\":1,\"mode\":\"%s\"}".formatted(mode))
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.field").isEqualTo(field)
                .jsonPath("$.previousValue").isEqualTo(previousValue)
                .jsonPath("$.newValue").isEqualTo(newValue);

        testClient.get().uri("/ledger/verify")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("INVALID")
                .jsonPath("$.operationCount").isEqualTo(1)
                .jsonPath("$.verifiedThroughSequence").isEqualTo(0)
                .jsonPath("$.latestSequenceNumber").isEqualTo(1)
                .jsonPath("$.mismatch.sequenceNumber").isEqualTo(1)
                .jsonPath("$.mismatch.operationId").isEqualTo(operation.getId().intValue())
                .jsonPath("$.mismatch.reason").isEqualTo(mismatchReason);

        testClient.get().uri("/operations/{operationId}/proof", operation.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.previousHashValid").isEqualTo(previousHashValid)
                .jsonPath("$.payloadHashValid").isEqualTo(payloadHashValid)
                .jsonPath("$.operationHashValid").isEqualTo(operationHashValid)
                .jsonPath("$.valid").isEqualTo(false);
    }

    @Test
    void simulateAmountTamper_subtractsAtDecimalLimit() {
        OperationResponse operation = createRecharge("Insurance policy", "99999999999999999.99");

        postTamper("{\"mode\":\"AMOUNT\"}")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.operationId").isEqualTo(operation.getId().intValue())
                .jsonPath("$.field").isEqualTo("AMOUNT")
                .jsonPath("$.previousValue").isEqualTo("99999999999999999.99")
                .jsonPath("$.newValue").isEqualTo("99999999999999999.98");

        testClient.get().uri("/ledger/verify")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("INVALID")
                .jsonPath("$.mismatch.operationId").isEqualTo(operation.getId().intValue())
                .jsonPath("$.mismatch.reason")
                .isEqualTo("Stored payload hash does not match the recalculated payload hash.");
    }

    @Test
    void simulateLedgerTamper_rejectsNonPositiveSequenceNumber() {
        postTamper("{\"sequenceNumber\":0}")
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Request validation failed")
                .jsonPath("$.instance").isEqualTo(TAMPER_PATH)
                .jsonPath("$.errors.length()").isEqualTo(1)
                .jsonPath("$.errors[0].field").isEqualTo("sequenceNumber")
                .jsonPath("$.errors[0].message").isEqualTo("must be greater than or equal to 1");
    }

    @Test
    void simulateLedgerTamper_returnsNotFoundForUnknownSequence() {
        createRecharge();

        expectProblem(
                postTamper("{\"sequenceNumber\":999}"),
                404,
                "Not Found",
                "999 ledger sequence not found",
                TAMPER_PATH);
    }

    @Test
    void simulateLedgerTamper_canTargetHistoricalSequence() {
        createRecharge();
        createRecharge();

        postTamper("{\"sequenceNumber\":1,\"mode\":\"OPERATION_HASH\"}")
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.sequenceNumber").isEqualTo(1)
                .jsonPath("$.mode").isEqualTo("OPERATION_HASH");

        testClient.get().uri("/ledger/verify")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("INVALID")
                .jsonPath("$.operationCount").isEqualTo(2)
                .jsonPath("$.latestSequenceNumber").isEqualTo(2)
                .jsonPath("$.mismatch.sequenceNumber").isEqualTo(1);
    }

    @Test
    void invalidLedger_blocksFurtherTamperingAndNewOperationsUntilDemoReset() {
        createRecharge();
        postTamper("{}").expectStatus().isOk();

        expectProblem(
                postTamper("{}"),
                409,
                "Conflict",
                "Ledger is already invalid. Repair or reset it before simulating another mismatch.",
                TAMPER_PATH);

        expectProblem(
                testClient.post().uri("/operations/recharges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{\"registerId\":\"Wallet\",\"amount\":1}")
                        .exchange(),
                409,
                "Conflict",
                "Ledger is invalid. Repair or reset it before creating new operations.",
                "/operations/recharges");

        resetDemo();
        testClient.get().uri("/ledger/verify")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("VERIFIED")
                .jsonPath("$.operationCount").isEqualTo(0);

        testClient.get().uri("/registers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .json(SEED_REGISTERS);
    }

    private static Stream<Arguments> tamperModes() {
        return Stream.of(
                Arguments.of(
                        "AMOUNT",
                        "AMOUNT",
                        "2500.00",
                        "2500.01",
                        "Stored payload hash does not match the recalculated payload hash.",
                        true,
                        false,
                        false),
                Arguments.of(
                        "PREVIOUS_HASH",
                        "PREVIOUS_HASH",
                        GENESIS_HASH,
                        changedHash(GENESIS_HASH),
                        "Stored previous hash does not match the expected previous hash.",
                        false,
                        true,
                        true),
                Arguments.of(
                        "PAYLOAD_HASH",
                        "PAYLOAD_HASH",
                        RECHARGE_PAYLOAD_HASH,
                        changedHash(RECHARGE_PAYLOAD_HASH),
                        "Stored payload hash does not match the recalculated payload hash.",
                        true,
                        false,
                        true),
                Arguments.of(
                        "OPERATION_HASH",
                        "OPERATION_HASH",
                        RECHARGE_OPERATION_HASH,
                        changedHash(RECHARGE_OPERATION_HASH),
                        "Stored operation hash does not match the recalculated operation hash.",
                        true,
                        true,
                        false));
    }

    private OperationResponse createRecharge() {
        return createRecharge("Wallet", "2500");
    }

    private OperationResponse createRecharge(String registerId, String amount) {
        return testClient.post().uri("/operations/recharges")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"registerId\":\"%s\",\"amount\":%s}".formatted(registerId, amount))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OperationResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private WebTestClient.ResponseSpec postTamper(String body) {
        return testClient.post().uri(TAMPER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    private static void expectProblem(
            WebTestClient.ResponseSpec response,
            int status,
            String title,
            String detail,
            String instance) {
        response.expectStatus().isEqualTo(status)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .json("""
                        {
                          "title": "%s",
                          "status": %d,
                          "detail": "%s",
                          "instance": "%s"
                        }
                        """.formatted(title, status, detail, instance), JsonCompareMode.STRICT);
    }

    private static String changedHash(String hash) {
        return (hash.charAt(0) == '0' ? "1" : "0") + hash.substring(1);
    }
}
