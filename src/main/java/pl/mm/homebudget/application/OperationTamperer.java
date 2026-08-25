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

    private static final BigDecimal CENT = new BigDecimal("0.01");

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
        BigDecimal newValue = nextAmount(previousValue);
        return requireUpdated(
                operationTamperRepository.updateAmount(operation.getId(), previousValue, newValue),
                new LedgerTamperChange(
                        operation,
                        mode,
                        "AMOUNT",
                        previousValue.toPlainString(),
                        newValue.toPlainString()));
    }

    private Mono<LedgerTamperChange> tamperHash(
            Operation operation,
            TamperLedgerMode mode,
            String field,
            String previousValue,
            TriFunction<Long, String, String, Mono<Boolean>> updater) {
        String newValue = changeFirstHexDigit(previousValue);
        return requireUpdated(
                updater.apply(operation.getId(), previousValue, newValue),
                new LedgerTamperChange(operation, mode, field, previousValue, newValue));
    }

    private Mono<LedgerTamperChange> requireUpdated(
            Mono<Boolean> updateResult,
            LedgerTamperChange change) {
        return updateResult.flatMap(updated -> updated
                ? Mono.just(change)
                : Mono.error(new LedgerConflictException(
                "The selected operation changed before the tamper simulation could be applied.")));
    }

    private static BigDecimal nextAmount(BigDecimal previousValue) {
        BigDecimal increased = previousValue.add(CENT);
        return increased.precision() <= 19 ? increased : previousValue.subtract(CENT);
    }

    private static String changeFirstHexDigit(String value) {
        char replacement = value.charAt(0) == '0' ? '1' : '0';
        return replacement + value.substring(1);
    }
}
