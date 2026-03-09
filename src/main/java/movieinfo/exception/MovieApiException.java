package movieinfo.exception;

import lombok.Getter;

@Getter
public class MovieApiException extends RuntimeException {

    private final String apiName;

    public MovieApiException(String apiName, String message, Throwable cause) {
        super(message, cause);
        this.apiName = apiName;
    }

}
