package pl.mm.homebudget.api.error;

import pl.mm.homebudget.api.dto.ValidationError;
import pl.mm.homebudget.domain.InvalidTransferException;
import pl.mm.homebudget.domain.OperationNotFoundException;
import pl.mm.homebudget.domain.RegisterNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({RegisterNotFoundException.class, OperationNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException e, ServerWebExchange exchange) {
        return problem(HttpStatus.NOT_FOUND, e.getMessage(), exchange);
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTransfer(
            InvalidTransferException e,
            ServerWebExchange exchange) {
        return problem(HttpStatus.BAD_REQUEST, e.getMessage(), exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            WebExchangeBindException e,
            ServerWebExchange exchange) {
        List<ValidationError> errors = e.getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> new ValidationError(error.getField(), message(error)))
                .toList();

        return validationProblem(errors, exchange);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ProblemDetail> handleInvalidInput(
            ServerWebInputException e,
            ServerWebExchange exchange) {
        return problem(HttpStatus.BAD_REQUEST, "Request body is invalid", exchange);
    }

    private static ResponseEntity<ProblemDetail> validationProblem(
            List<ValidationError> errors,
            ServerWebExchange exchange) {
        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, "Request validation failed", exchange);
        problem.setProperty("errors", errors);

        return response(HttpStatus.BAD_REQUEST, problem);
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String detail,
            ServerWebExchange exchange) {
        return response(status, createProblem(status, detail, exchange));
    }

    private static ProblemDetail createProblem(
            HttpStatus status,
            String detail,
            ServerWebExchange exchange) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(exchange.getRequest().getPath().pathWithinApplication().value()));
        return problem;
    }

    private static ResponseEntity<ProblemDetail> response(HttpStatus status, ProblemDetail problem) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static String message(FieldError error) {
        return Objects.toString(error.getDefaultMessage(), "Invalid value");
    }
}
