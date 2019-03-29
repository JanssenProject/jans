package org.xdi.oxauth.model.exception;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 16/06/2015
 */

public class AcrChangedException extends Exception {

    private boolean forceReAuthentication;

    public AcrChangedException() {
        forceReAuthentication = true;
    }

    public AcrChangedException(boolean forceReAuthentication) {
        this.forceReAuthentication = forceReAuthentication;
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

    public boolean isForceReAuthentication() {
        return forceReAuthentication;
    }

    public void setForceReAuthentication(boolean forceReAuthentication) {
        this.forceReAuthentication = forceReAuthentication;
    }

}
