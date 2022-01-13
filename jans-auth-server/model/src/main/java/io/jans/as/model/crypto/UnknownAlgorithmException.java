package io.jans.as.model.crypto;

/**
 * @author Yuriy Zabrovarnyy
 */
public class UnknownAlgorithmException extends RuntimeException {

    public UnknownAlgorithmException() {
    }

    public UnknownAlgorithmException(String message) {
        super(message);
    }

    public UnknownAlgorithmException(String message, Throwable cause) {
        super(message, cause);
    }
}
