package org.xdi.oxauth.model.exception;

/**
 * @author Javier Rojas Blum Date: 11.12.2012
 */
public class SignatureException extends Exception {

    public SignatureException(String message) {
        super(message);
    }

    public SignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignatureException(Throwable cause) {
        super(cause);
    }
}