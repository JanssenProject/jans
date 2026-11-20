package io.jans.shibboleth.trust.activation.error;

public class WorkerNotAlive extends ActivationError {

    private WorkerNotAlive(String message) {

        super(message);
    }

    public static WorkerNotAlive instance() {

        return new WorkerNotAlive("The claiming Worker is not alive");
    }
}
