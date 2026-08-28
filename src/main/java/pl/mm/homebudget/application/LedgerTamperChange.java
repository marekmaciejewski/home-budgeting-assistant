package pl.mm.homebudget.application;

import pl.mm.homebudget.api.dto.TamperLedgerMode;
import pl.mm.homebudget.persistence.entity.Operation;

record LedgerTamperChange(
        Operation operation,
        TamperLedgerMode mode,
        String field,
        String previousValue,
        String newValue) {
}
