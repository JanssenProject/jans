package io.jans.shibboleth.trust.config.error;


public class TrustError {

    protected String message;

    protected TrustError(String message) {

        this.message = message;
    }

    public String getMessage() {

        return message;
    }

    @Override
    public String toString() {

        return message;
    }
}