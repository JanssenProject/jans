package io.jans.shibboleth.model.error;


public class TrustError {

    protected String message;

    protected TrustError(String message) {

        this.message = message;
    }

    public final String getMessage() {

        return message;
    }

    @Override
    public String toString() {

        return message;
    }
}