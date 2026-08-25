package pl.mm.homebudget.domain;

public class LedgerConflictException extends RuntimeException {

    public LedgerConflictException(String message) {
        super(message);
    }
}
