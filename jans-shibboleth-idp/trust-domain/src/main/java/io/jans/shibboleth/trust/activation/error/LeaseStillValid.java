package io.jans.shibboleth.trust.activation.error;

public class LeaseStillValid extends ActivationError {

    private LeaseStillValid(String message) {

        super(message);
    }

    public static LeaseStillValid instance() {

        return new LeaseStillValid("The lease has not expired; the WorkItem cannot be reclaimed");
    }
}
