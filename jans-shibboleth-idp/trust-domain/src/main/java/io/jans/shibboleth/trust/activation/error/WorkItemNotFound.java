package io.jans.shibboleth.trust.activation.error;

public class WorkItemNotFound extends ActivationError {

    private WorkItemNotFound(String message) {

        super(message);
    }

    public static WorkItemNotFound instance() {

        return new WorkItemNotFound("No WorkItem was found for the given id");
    }
}
