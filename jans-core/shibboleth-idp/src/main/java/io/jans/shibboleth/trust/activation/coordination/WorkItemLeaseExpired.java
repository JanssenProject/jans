package io.jans.shibboleth.trust.activation.coordination;

import io.jans.shibboleth.trust.activation.model.WorkItemId;

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
