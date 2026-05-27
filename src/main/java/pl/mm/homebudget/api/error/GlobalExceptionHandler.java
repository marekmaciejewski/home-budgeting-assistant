package pl.mm.homebudget.api.error;

import pl.mm.homebudget.domain.InvalidTransferException;
import pl.mm.homebudget.domain.OperationNotFoundException;
import pl.mm.homebudget.domain.RegisterNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RegisterNotFoundException.class)
    public ResponseEntity<String> handle(RegisterNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(e.getMessage());
    }

    @ExceptionHandler(OperationNotFoundException.class)
    public ResponseEntity<String> handle(OperationNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(e.getMessage());
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<String> handle(InvalidTransferException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(e.getMessage());
    }
}
