package io.jans.shibboleth.activation.coordination;

import java.util.HashMap;
import java.util.Map;

import io.jans.shibboleth.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.activation.model.WorkItem;
import io.jans.shibboleth.activation.model.WorkItemType;
import io.jans.shibboleth.activation.util.ActivationResult;

public final class WorkOrchestrator {

    private final TimeSource timeSource;
    private final Map<TrustRelationshipRef, WorkItem> currentByTr = new HashMap<>();

    public WorkOrchestrator(TimeSource timeSource) {

        this.timeSource = timeSource;
    }

    public ActivationResult<WorkItem> onActivationRequested(TrustRelationshipRef trustRelationshipId, WorkItemType type) {

        ActivationResult<WorkItem> created = WorkItem.create(type, trustRelationshipId, timeSource.now());

        if (created.isFailure()) {

            return created;
        }

        WorkItem item = created.getValue();
        currentByTr.put(trustRelationshipId, item);

        return ActivationResult.success(item);
    }

    public boolean isCurrent(WorkItem item) {

        WorkItem current = currentByTr.get(item.trustRelationshipId());

        return current != null && current.id().equals(item.id());
    }
}
