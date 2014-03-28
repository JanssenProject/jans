package org.xdi.oxauth.model.exception;

/**
 * @author Javier Rojas Blum Date: 12.06.2012
 */
public class InvalidJweException extends Exception {

    public InvalidJweException(String message) {
        super(message);
    }

    public InvalidJweException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidJweException(Throwable cause) {
        super(cause);
    }
}