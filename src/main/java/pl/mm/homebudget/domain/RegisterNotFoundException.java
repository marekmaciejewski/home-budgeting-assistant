package pl.mm.homebudget.domain;

public class RegisterNotFoundException extends RuntimeException {

    public RegisterNotFoundException(String message) {
        super(message);
    }
}
