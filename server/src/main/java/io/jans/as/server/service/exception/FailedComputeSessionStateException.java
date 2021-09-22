package io.jans.as.server.service.exception;

/**
 * @author Yuriy Zabrovarnyy
 */
public class FailedComputeSessionStateException extends RuntimeException {

    public FailedComputeSessionStateException() {
    }

    public FailedComputeSessionStateException(String message) {
        super(message);
    }

    public FailedComputeSessionStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
