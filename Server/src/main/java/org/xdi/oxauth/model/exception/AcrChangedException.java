package org.xdi.oxauth.model.exception;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 16/06/2015
 */

public class AcrChangedException extends Exception {

    public AcrChangedException() {
    }

    public AcrChangedException(Throwable cause) {
        super(cause);
    }

    public AcrChangedException(String message) {
        super(message);
    }

    public AcrChangedException(String message, Throwable cause) {
        super(message, cause);
    }
}
