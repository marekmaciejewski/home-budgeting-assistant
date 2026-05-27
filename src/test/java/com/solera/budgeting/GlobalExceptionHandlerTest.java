package com.solera.budgeting;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handle_returnsResponseEntityWithNotFoundStatus_andExceptionMessageInBody() {
        // given
        String message = "sample message";
        RegisterNotFoundException e = new RegisterNotFoundException(message);
        // when
        ResponseEntity<String> responseEntity = exceptionHandler.handle(e);
        // then
        assertThat(responseEntity)
                .returns(HttpStatus.NOT_FOUND, from(ResponseEntity::getStatusCode))
                .returns(message, from(HttpEntity::getBody));
    }

    @Test
    void handle_returnsResponseEntityWithNotFoundStatus_andOperationExceptionMessageInBody() {
        // given
        String message = "sample message";
        OperationNotFoundException e = new OperationNotFoundException(message);
        // when
        ResponseEntity<String> responseEntity = exceptionHandler.handle(e);
        // then
        assertThat(responseEntity)
                .returns(HttpStatus.NOT_FOUND, from(ResponseEntity::getStatusCode))
                .returns(message, from(HttpEntity::getBody));
    }

    @Test
    void handle_returnsResponseEntityWithBadRequestStatus_andExceptionMessageInBody() {
        // given
        String message = "sample message";
        InvalidTransferException e = new InvalidTransferException(message);
        // when
        ResponseEntity<String> responseEntity = exceptionHandler.handle(e);
        // then
        assertThat(responseEntity)
                .returns(HttpStatus.BAD_REQUEST, from(ResponseEntity::getStatusCode))
                .returns(message, from(HttpEntity::getBody));
    }
}
