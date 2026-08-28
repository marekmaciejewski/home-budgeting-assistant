package pl.mm.homebudget.application;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.stereotype.Component;
import pl.mm.homebudget.api.dto.TamperLedgerMode;
import pl.mm.homebudget.domain.LedgerConflictException;
import pl.mm.homebudget.persistence.OperationTamperRepository;
import pl.mm.homebudget.persistence.entity.Operation;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
class OperationTamperer {

    private static final BigDecimal MALICIOUS_VALUE = new BigDecimal("0.01");

    private final OperationTamperRepository operationTamperRepository;

    Mono<LedgerTamperChange> tamper(Operation operation, TamperLedgerMode mode) {
        return switch (mode) {
            case AMOUNT -> tamperAmount(operation, mode);
            case PREVIOUS_HASH -> tamperHash(
                    operation,
                    mode,
                    "PREVIOUS_HASH",
                    operation.getPreviousHash(),
                    operationTamperRepository::updatePreviousHash);
            case PAYLOAD_HASH -> tamperHash(
                    operation,
                    mode,
                    "PAYLOAD_HASH",
                    operation.getPayloadHash(),
                    operationTamperRepository::updatePayloadHash);
            case OPERATION_HASH -> tamperHash(
                    operation,
                    mode,
                    "OPERATION_HASH",
                    operation.getOperationHash(),
                    operationTamperRepository::updateOperationHash);
        };
    }

    private Mono<LedgerTamperChange> tamperAmount(Operation operation, TamperLedgerMode mode) {
        BigDecimal previousValue = operation.getAmount().setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal corruptedValue = nextAmount(previousValue);
        return requireExactlyOneRowUpdated(
                operationTamperRepository.updateAmount(operation.getId(), previousValue, corruptedValue),
                new LedgerTamperChange(
                        operation,
                        mode,
                        "AMOUNT",
                        previousValue.toPlainString(),
                        corruptedValue.toPlainString()));
    }

    private Mono<LedgerTamperChange> tamperHash(
            Operation operation,
            TamperLedgerMode mode,
            String field,
            String previousValue,
            TriFunction<Long, String, String, Mono<Long>> updater) {
        String newValue = changeFirstHexDigit(previousValue);
        return requireExactlyOneRowUpdated(
                updater.apply(operation.getId(), previousValue, newValue),
                new LedgerTamperChange(operation, mode, field, previousValue, newValue));
    }

    private Mono<LedgerTamperChange> requireExactlyOneRowUpdated(
            Mono<Long> rowsUpdated,
            LedgerTamperChange change) {
        return rowsUpdated
                .filter(updated -> updated == 1)
                .switchIfEmpty(Mono.error(new LedgerConflictException(
                        "The selected operation changed before the tamper simulation could be applied.")))
                .thenReturn(change);
    }

    private static BigDecimal nextAmount(BigDecimal previousValue) {
        BigDecimal increased = previousValue.add(MALICIOUS_VALUE);
        return increased.precision() <= 19 ? increased : previousValue.subtract(MALICIOUS_VALUE);
    }

    private static String changeFirstHexDigit(String value) {
        char replacement = value.charAt(0) == '0' ? '1' : '0';
        return replacement + value.substring(1);
    }
}
