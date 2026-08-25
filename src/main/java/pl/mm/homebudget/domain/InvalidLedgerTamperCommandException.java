package pl.mm.homebudget.domain;

public class InvalidLedgerTamperCommandException extends RuntimeException {

    public InvalidLedgerTamperCommandException(String message) {
        super(message);
    }
}
