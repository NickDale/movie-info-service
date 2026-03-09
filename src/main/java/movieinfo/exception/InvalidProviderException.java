package movieinfo.exception;

import lombok.Getter;

@Getter
public class InvalidProviderException extends RuntimeException {

    public InvalidProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
