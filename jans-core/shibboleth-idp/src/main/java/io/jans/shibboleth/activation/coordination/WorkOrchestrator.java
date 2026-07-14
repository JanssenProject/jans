package io.jans.shibboleth.activation.coordination;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.jans.shibboleth.activation.error.NotLeaseHolder;
import io.jans.shibboleth.activation.error.RequiredValueMissing;
import io.jans.shibboleth.activation.error.StaleReport;
import io.jans.shibboleth.activation.error.WorkItemNotFound;
import io.jans.shibboleth.activation.error.WorkerNotAlive;
import io.jans.shibboleth.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.activation.model.WorkItem;
import io.jans.shibboleth.activation.model.WorkItemId;
import io.jans.shibboleth.activation.model.WorkItemType;
import io.jans.shibboleth.activation.util.ActivationResult;
import io.jans.shibboleth.activation.workers.Worker;
import io.jans.shibboleth.activation.workers.WorkerId;
import io.jans.shibboleth.trust.config.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.config.diagnostics.ActivationStatus;

public final class WorkOrchestrator {

    private final TimeSource timeSource;
    private final Duration leaseTtl;
    private final Duration heartbeatTtl;
    private final ActivationEventSink events;
    private final FinalizeActivationPort finalizePort;

    private final Map<WorkItemId, WorkItem> items = new HashMap<>();
    private final Map<TrustRelationshipRef, WorkItemId> currentByTr = new HashMap<>();

    private WorkOrchestrator(TimeSource timeSource, Duration leaseTtl, Duration heartbeatTtl,
                             ActivationEventSink events, FinalizeActivationPort finalizePort) {

        this.timeSource = timeSource;
        this.leaseTtl = leaseTtl;
        this.heartbeatTtl = heartbeatTtl;
        this.events = events;
        this.finalizePort = finalizePort;
    }

    public static ActivationResult<WorkOrchestrator> create(TimeSource timeSource, Duration leaseTtl, Duration heartbeatTtl,
                                                            ActivationEventSink events, FinalizeActivationPort finalizePort) {

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

        if (finalizePort == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("finalizePort"));
        }

        return ActivationResult.success(new WorkOrchestrator(timeSource, leaseTtl, heartbeatTtl, events, finalizePort));
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

    public ActivationResult<WorkItem> report(WorkItemId id, ActivationDiagnostics diagnostics) {

        Instant now = timeSource.now();

        ActivationResult<WorkItem> found = find(id);

        if (found.isFailure()) {

            return found;
        }

        WorkItem item = found.getValue();

        if (!isCurrent(item)) {

            return ActivationResult.failure(StaleReport.instance());
        }

        if (item.state().isTerminal()) {

            return ActivationResult.failure(StaleReport.instance());
        }

        ActivationResult<WorkerId> reporter = WorkerId.of(diagnostics.getOrigin());

        if (reporter.isFailure()) {

            return ActivationResult.failure(reporter.getError());
        }

        if (!item.lease().isHeldBy(reporter.getValue())) {

            return ActivationResult.failure(NotLeaseHolder.instance());
        }

        finalizePort.finalizeActivation(item.trustRelationshipId(), diagnostics);

        if (diagnostics.getStatus() == ActivationStatus.NO_DATA) {

            return ActivationResult.success(item);
        }

        ActivationResult<WorkItem> completed = item.complete(now);

        if (completed.isSuccess()) {

            items.put(id, completed.getValue());
        }

        return completed;
    }

    public ActivationResult<WorkItem> onActivationCancelled(TrustRelationshipRef trustRelationshipId) {

        Instant now = timeSource.now();

        WorkItemId currentId = currentByTr.get(trustRelationshipId);

        if (currentId == null) {

            return ActivationResult.failure(WorkItemNotFound.instance());
        }

        ActivationResult<WorkItem> found = find(currentId);

        if (found.isFailure()) {

            return found;
        }

        ActivationResult<WorkItem> cancelled = found.getValue().cancel(now);

        if (cancelled.isFailure()) {

            return cancelled;
        }

        items.put(currentId, cancelled.getValue());
        currentByTr.remove(trustRelationshipId);

        return cancelled;
    }

    public boolean isCurrent(WorkItem item) {

        WorkItemId current = currentByTr.get(item.trustRelationshipId());

        return current != null && current.equals(item.id());
    }
}
