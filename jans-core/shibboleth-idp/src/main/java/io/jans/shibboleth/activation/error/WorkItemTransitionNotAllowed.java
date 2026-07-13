package io.jans.shibboleth.activation.error;

public class WorkItemTransitionNotAllowed extends ActivationError {

    private WorkItemTransitionNotAllowed(String message) {

        super(message);
    }

    public static WorkItemTransitionNotAllowed of(String operation, String fromState) {

        return new WorkItemTransitionNotAllowed(
            String.format("Operation '%s' is not allowed from state '%s'", operation, fromState));
    }
}
