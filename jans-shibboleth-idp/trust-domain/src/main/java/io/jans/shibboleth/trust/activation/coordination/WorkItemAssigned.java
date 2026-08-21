package io.jans.shibboleth.trust.activation.coordination;

import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.workers.WorkerId;

public final class WorkItemAssigned implements ActivationEvent {

    private final WorkItemId workItemId;
    private final WorkerId workerId;

    private WorkItemAssigned(WorkItemId workItemId, WorkerId workerId) {

        this.workItemId = workItemId;
        this.workerId = workerId;
    }

    public static WorkItemAssigned of(WorkItemId workItemId, WorkerId workerId) {

        return new WorkItemAssigned(workItemId, workerId);
    }

    public WorkItemId workItemId() {

        return workItemId;
    }

    public WorkerId workerId() {

        return workerId;
    }
}
