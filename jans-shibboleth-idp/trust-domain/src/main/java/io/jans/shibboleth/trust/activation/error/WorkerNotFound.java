package io.jans.shibboleth.trust.activation.error;

public class WorkerNotFound extends ActivationError {

    private WorkerNotFound(String message) {

        super(message);
    }

    public static WorkerNotFound instance() {

        return new WorkerNotFound("No Worker was found for the given id");
    }
}
