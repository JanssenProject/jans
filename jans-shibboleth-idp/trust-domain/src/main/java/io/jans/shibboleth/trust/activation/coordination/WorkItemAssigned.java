package io.jans.shibboleth.trust.activation.coordination;

import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.workers.WorkerId;

public record WorkItemAssigned(WorkItemId workItemId, WorkerId workerId) implements ActivationEvent {

    public static WorkItemAssigned of(WorkItemId workItemId, WorkerId workerId) {

        return new WorkItemAssigned(workItemId, workerId);
    }
}
