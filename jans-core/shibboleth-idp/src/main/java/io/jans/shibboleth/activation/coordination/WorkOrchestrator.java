package io.jans.shibboleth.activation.coordination;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.jans.shibboleth.activation.error.RequiredValueMissing;
import io.jans.shibboleth.activation.error.WorkItemNotFound;
import io.jans.shibboleth.activation.error.WorkerNotAlive;
import io.jans.shibboleth.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.activation.model.WorkItem;
import io.jans.shibboleth.activation.model.WorkItemId;
import io.jans.shibboleth.activation.model.WorkItemType;
import io.jans.shibboleth.activation.util.ActivationResult;
import io.jans.shibboleth.activation.workers.Worker;

public final class WorkOrchestrator {

    private final TimeSource timeSource;
    private final Duration leaseTtl;
    private final Duration heartbeatTtl;
    private final ActivationEventSink events;

    private final Map<WorkItemId, WorkItem> items = new HashMap<>();
    private final Map<TrustRelationshipRef, WorkItemId> currentByTr = new HashMap<>();

    private WorkOrchestrator(TimeSource timeSource, Duration leaseTtl, Duration heartbeatTtl, ActivationEventSink events) {

        this.timeSource = timeSource;
        this.leaseTtl = leaseTtl;
        this.heartbeatTtl = heartbeatTtl;
        this.events = events;
    }

    public static ActivationResult<WorkOrchestrator> create(TimeSource timeSource, Duration leaseTtl, Duration heartbeatTtl, ActivationEventSink events) {

        if (timeSource == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("timeSource"));
        }

        if (leaseTtl == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("leaseTtl"));
        }

        if (heartbeatTtl == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("heartbeatTtl"));
        }

        if (events == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("events"));
        }

        return ActivationResult.success(new WorkOrchestrator(timeSource, leaseTtl, heartbeatTtl, events));
    }

    public ActivationResult<WorkItem> onActivationRequested(TrustRelationshipRef trustRelationshipId, WorkItemType type) {

        ActivationResult<WorkItem> created = WorkItem.create(type, trustRelationshipId, timeSource.now());

        if (created.isFailure()) {

            return created;
        }

        WorkItem item = created.getValue();
        items.put(item.id(), item);
        currentByTr.put(trustRelationshipId, item.id());

        return ActivationResult.success(item);
    }

    public ActivationResult<WorkItem> find(WorkItemId id) {

        WorkItem item = items.get(id);

        if (item == null) {

            return ActivationResult.failure(WorkItemNotFound.instance());
        }

        return ActivationResult.success(item);
    }

    public ActivationResult<WorkItem> claim(WorkItemId id, Worker worker) {

        Instant now = timeSource.now();

        ActivationResult<WorkItem> found = find(id);

        if (found.isFailure()) {

            return found;
        }

        if (!worker.isAlive(now, heartbeatTtl)) {

            return ActivationResult.failure(WorkerNotAlive.instance());
        }

        ActivationResult<WorkItem> assigned = found.getValue().claim(worker.id(), now, now.plus(leaseTtl));

        if (assigned.isFailure()) {

            return assigned;
        }

        items.put(id, assigned.getValue());
        events.emit(WorkItemAssigned.of(id, worker.id()));

        return assigned;
    }

    public ActivationResult<WorkItem> heartbeat(WorkItemId id, Worker worker) {

        Instant now = timeSource.now();

        ActivationResult<WorkItem> found = find(id);

        if (found.isFailure()) {

            return found;
        }

        ActivationResult<WorkItem> renewed = found.getValue().heartbeat(worker.id(), now, now.plus(leaseTtl));

        if (renewed.isFailure()) {

            return renewed;
        }

        items.put(id, renewed.getValue());

        return renewed;
    }

    public void sweepExpiredLeases() {

        Instant now = timeSource.now();

        for (WorkItem item : new ArrayList<>(items.values())) {

            ActivationResult<WorkItem> reclaimed = item.reclaim(now);

            if (reclaimed.isSuccess()) {

                items.put(item.id(), reclaimed.getValue());
                events.emit(WorkItemLeaseExpired.of(item.id()));
            }
        }
    }

    public boolean isCurrent(WorkItem item) {

        WorkItemId current = currentByTr.get(item.trustRelationshipId());

        return current != null && current.equals(item.id());
    }
}
