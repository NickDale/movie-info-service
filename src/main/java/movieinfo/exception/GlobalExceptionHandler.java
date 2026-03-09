package movieinfo.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, InvalidProviderException.class, TypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleIllegalArgument(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(
                        ErrorResponse.of(ex.getMessage())
                );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity
                .badRequest()
                .body(
                        ErrorResponse.of("Missing required parameter: " + ex.getParameterName())
                );
    }

    @ExceptionHandler(MovieApiException.class)
    public ResponseEntity<ErrorResponse> handleMovieApiException(MovieApiException ex) {
        log.error("External API error [{}]: {}", ex.getApiName(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(
                        ErrorResponse.of("Error communicating with " + ex.getApiName() + " API: " + ex.getMessage())
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ErrorResponse.of("An unexpected error occurred")
                );
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorResponse(String error, String timestamp) {

        public static ErrorResponse of(String error) {
            return new ErrorResponse(error, LocalDateTime.now().toString());
        }

    }
}
