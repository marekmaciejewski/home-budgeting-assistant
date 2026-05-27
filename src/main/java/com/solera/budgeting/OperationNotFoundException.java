package com.solera.budgeting;

class OperationNotFoundException extends RuntimeException {

    OperationNotFoundException(String message) {
        super(message);
    }
}
