package org.xdi.oxd.server;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 30/08/2016
 */

public class ShutdownException extends RuntimeException {

    public ShutdownException() {
    }

    public ShutdownException(String message) {
        super(message);
    }

    public ShutdownException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShutdownException(Throwable cause) {
        super(cause);
    }

    public ShutdownException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
