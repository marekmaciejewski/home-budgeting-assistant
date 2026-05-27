package com.solera.budgeting;

class InvalidTransferException extends RuntimeException {

    InvalidTransferException(String message) {
        super(message);
    }
}
