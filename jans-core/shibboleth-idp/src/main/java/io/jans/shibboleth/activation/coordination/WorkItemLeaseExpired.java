package io.jans.shibboleth.activation.coordination;

import io.jans.shibboleth.activation.model.WorkItemId;

public final class WorkItemLeaseExpired implements ActivationEvent {

    private final WorkItemId workItemId;

    private WorkItemLeaseExpired(WorkItemId workItemId) {

        this.workItemId = workItemId;
    }

    public static WorkItemLeaseExpired of(WorkItemId workItemId) {

        return new WorkItemLeaseExpired(workItemId);
    }

    public WorkItemId workItemId() {

        return workItemId;
    }
}
