package org.xdi.oxauth.model.exception;

/**
 * @author Javier Rojas Blum Date: 03.09.2012
 */
public class InvalidJwtException extends Exception {

    public InvalidJwtException(String message) {
        super(message);
    }

    public InvalidJwtException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidJwtException(Throwable cause) {
        super(cause);
    }
}