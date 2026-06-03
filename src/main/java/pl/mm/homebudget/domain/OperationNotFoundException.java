package pl.mm.homebudget.domain;

public class OperationNotFoundException extends RuntimeException {

    public OperationNotFoundException(String message) {
        super(message);
    }
}
