package io.jans.shibboleth.activation.error;

public class LeaseNotPresent extends ActivationError {

    private LeaseNotPresent(String message) {

        super(message);
    }

    public static LeaseNotPresent forRenewal() {

        return new LeaseNotPresent("An absent lease (Lease.NONE) cannot be renewed");
    }
}
