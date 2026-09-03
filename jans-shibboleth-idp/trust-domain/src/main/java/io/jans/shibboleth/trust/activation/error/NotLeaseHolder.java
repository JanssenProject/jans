package io.jans.shibboleth.trust.activation.error;

public class NotLeaseHolder extends ActivationError {

    private NotLeaseHolder(String message) {

        super(message);
    }

    public static NotLeaseHolder instance() {

        return new NotLeaseHolder("The operation must be performed by the current lease holder");
    }
}
