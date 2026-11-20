package io.jans.shibboleth.trust.activation.coordination;

import io.jans.shibboleth.trust.activation.model.WorkItemId;

public record WorkItemLeaseExpired(WorkItemId workItemId) implements ActivationEvent {

    public static WorkItemLeaseExpired of(WorkItemId workItemId) {

        return new WorkItemLeaseExpired(workItemId);
    }
}
