package pl.mm.homebudget.api.operation;

import pl.mm.homebudget.api.error.GlobalExceptionHandler;
import pl.mm.homebudget.application.RegisterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@MockitoBean(types = RegisterService.class)
@WebFluxTest(OperationController.class)
@Import(GlobalExceptionHandler.class)
class OperationControllerIT {

    private static final String RECHARGES_PATH = "/operations/recharges";
    private static final String TRANSFERS_PATH = "/operations/transfers";

    @Autowired
    private WebTestClient testClient;

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, nullValues = "NULL", textBlock = """
            registerId, amount,             expectedField, expectedMessage,                                                 secondExpectedField, secondExpectedMessage
            NULL,       2500,               registerId,    must not be null,                                               registerId,          must not be blank
            '',         2500,               registerId,    must not be blank,                                              NULL,                NULL
            '   ',      2500,               registerId,    must not be blank,                                              NULL,                NULL
            Wallet,     NULL,               amount,        must not be null,                                               NULL,                NULL
            Wallet,     0,                  amount,        must be greater than 0,                                         NULL,                NULL
            Wallet,     -2500,              amount,        must be greater than 0,                                         NULL,                NULL
            Wallet,     1.001,              amount,        must have no more than 17 integer digits and 2 decimal places,  NULL,                NULL
            Wallet,     1.230,              amount,        must have no more than 17 integer digits and 2 decimal places,  NULL,                NULL
            Wallet,     100000000000000000, amount,        must have no more than 17 integer digits and 2 decimal places,  NULL,                NULL
            """)
    void createRecharge_returnsBadRequest(
            String registerId,
            String amount,
            String expectedField,
            String expectedMessage,
            String secondExpectedField,
            String secondExpectedMessage) {
        String payload = rechargePayload(registerId, amount);
        String expectedProblem = validationProblem(
                RECHARGES_PATH,
                expectedField,
                expectedMessage,
                secondExpectedField,
                secondExpectedMessage);

        testClient
                .post().uri(RECHARGES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .json(expectedProblem);
    }

    @ParameterizedTest
    @CsvSource(useHeadersInDisplayName = true, nullValues = "NULL", textBlock = """
            sourceRegisterId, targetRegisterId, amount,             expectedField,   expectedMessage,                                                 secondExpectedField, secondExpectedMessage
            NULL,             Food expenses,    1500,               sourceRegisterId, must not be null,                                               sourceRegisterId,    must not be blank
            '',               Food expenses,    1500,               sourceRegisterId, must not be blank,                                              NULL,                NULL
            Wallet,           NULL,             1500,               targetRegisterId, must not be null,                                               targetRegisterId,    must not be blank
            Wallet,           '',               1500,               targetRegisterId, must not be blank,                                              NULL,                NULL
            Wallet,           Food expenses,    NULL,               amount,           must not be null,                                               NULL,                NULL
            Wallet,           Food expenses,    0,                  amount,           must be greater than 0,                                         NULL,                NULL
            Wallet,           Food expenses,    -1500,              amount,           must be greater than 0,                                         NULL,                NULL
            Wallet,           Food expenses,    1.001,              amount,           must have no more than 17 integer digits and 2 decimal places,  NULL,                NULL
            Wallet,           Food expenses,    1.230,              amount,           must have no more than 17 integer digits and 2 decimal places,  NULL,                NULL
            Wallet,           Food expenses,    100000000000000000, amount,           must have no more than 17 integer digits and 2 decimal places,  NULL,                NULL
            """)
    void createTransfer_returnsBadRequest(
            String sourceRegisterId,
            String targetRegisterId,
            String amount,
            String expectedField,
            String expectedMessage,
            String secondExpectedField,
            String secondExpectedMessage) {
        String payload = transferPayload(sourceRegisterId, targetRegisterId, amount);
        String expectedProblem = validationProblem(
                TRANSFERS_PATH,
                expectedField,
                expectedMessage,
                secondExpectedField,
                secondExpectedMessage);

        testClient
                .post().uri(TRANSFERS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .json(expectedProblem);
    }

    @Test
    void createTransfer_returnsBadRequest_whenBodyIsMalformed() {
        String payload = "{";
        String expectedProblem = """
                {
                  "title": "Bad Request",
                  "status": 400,
                  "detail": "Request body is invalid",
                  "instance": "/operations/transfers"
                }
                """;

        testClient
                .post().uri(TRANSFERS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .json(expectedProblem, JsonCompareMode.STRICT);
    }

    private static String rechargePayload(String registerId, String amount) {
        return "{\"registerId\":%s,\"amount\":%s}".formatted(jsonValue(registerId), amount);
    }

    private static String transferPayload(String sourceRegisterId, String targetRegisterId, String amount) {
        return "{\"sourceRegisterId\":%s,\"targetRegisterId\":%s,\"amount\":%s}"
                .formatted(jsonValue(sourceRegisterId), jsonValue(targetRegisterId), amount);
    }

    private static String validationProblem(
            String instance,
            String expectedField,
            String expectedMessage,
            String secondExpectedField,
            String secondExpectedMessage) {
        String errors = error(expectedField, expectedMessage);
        if (secondExpectedField != null) {
            errors += ",\n    " + error(secondExpectedField, secondExpectedMessage);
        }

        return """
                {
                  "title": "Bad Request",
                  "status": 400,
                  "detail": "Request validation failed",
                  "instance": "%s",
                  "errors": [
                    %s
                  ]
                }
                """.formatted(instance, errors);
    }

    private static String error(String field, String message) {
        return "{\"field\": \"%s\", \"message\": \"%s\"}".formatted(field, message);
    }

    private static String jsonValue(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
