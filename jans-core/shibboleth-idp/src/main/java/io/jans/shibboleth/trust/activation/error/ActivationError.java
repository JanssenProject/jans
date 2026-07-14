package io.jans.shibboleth.trust.activation.error;

public class ActivationError {

    protected final String message;

    protected ActivationError(String message) {

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
