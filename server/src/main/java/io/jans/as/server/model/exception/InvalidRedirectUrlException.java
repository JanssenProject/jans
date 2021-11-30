package io.jans.as.server.model.exception;

/**
 * @author Javier Rojas Blum
 * @version November 23, 2021
 */
public class InvalidRedirectUrlException extends RuntimeException {

    private static final long serialVersionUID = -2240920149302056837L;

    public InvalidRedirectUrlException() {
    }

    public InvalidRedirectUrlException(String message) {
        super(message);
    }

    public InvalidRedirectUrlException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidRedirectUrlException(Throwable cause) {
        super(cause);
    }

    public InvalidRedirectUrlException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
