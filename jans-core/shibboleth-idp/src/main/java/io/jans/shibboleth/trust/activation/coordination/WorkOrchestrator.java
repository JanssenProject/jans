package io.jans.shibboleth.trust.activation.coordination;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.jans.shibboleth.trust.activation.error.NotLeaseHolder;
import io.jans.shibboleth.trust.activation.error.RequiredValueMissing;
import io.jans.shibboleth.trust.activation.error.StaleReport;
import io.jans.shibboleth.trust.activation.error.WorkItemNotFound;
import io.jans.shibboleth.trust.activation.error.WorkerNotAlive;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
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

    public static Result<WorkOrchestrator> create(TimeSource timeSource, Duration leaseTtl, Duration heartbeatTtl,
                                                            ActivationEventSink events, FinalizeActivationPort finalizePort) {

        if (timeSource == null) {

            return Result.failure(RequiredValueMissing.forField("timeSource"));
        }

        if (leaseTtl == null) {

            return Result.failure(RequiredValueMissing.forField("leaseTtl"));
        }

        if (heartbeatTtl == null) {

            return Result.failure(RequiredValueMissing.forField("heartbeatTtl"));
        }

        if (events == null) {

            return Result.failure(RequiredValueMissing.forField("events"));
        }

        if (finalizePort == null) {

            return Result.failure(RequiredValueMissing.forField("finalizePort"));
        }

        return Result.success(new WorkOrchestrator(timeSource, leaseTtl, heartbeatTtl, events, finalizePort));
    }

    public Result<WorkItem> onActivationRequested(TrustRelationshipRef trustRelationshipId, WorkItemType type) {

        Result<WorkItem> created = WorkItem.create(type, trustRelationshipId, timeSource.now());

        if (created.isFailure()) {

            return created;
        }

        WorkItem item = created.getValue();
        items.put(item.id(), item);
        currentByTr.put(trustRelationshipId, item.id());

        return Result.success(item);
    }

    public Result<WorkItem> find(WorkItemId id) {

        WorkItem item = items.get(id);

        if (item == null) {

            return Result.failure(WorkItemNotFound.instance());
        }

        return Result.success(item);
    }

    public Result<WorkItem> claim(WorkItemId id, Worker worker) {

        Instant now = timeSource.now();

        Result<WorkItem> found = find(id);

        if (found.isFailure()) {

            return found;
        }

        if (!worker.isAlive(now, heartbeatTtl)) {

            return Result.failure(WorkerNotAlive.instance());
        }

        Result<WorkItem> assigned = found.getValue().claim(worker.id(), now, now.plus(leaseTtl));

        if (assigned.isFailure()) {

            return assigned;
        }

        items.put(id, assigned.getValue());
        events.emit(WorkItemAssigned.of(id, worker.id()));

        return assigned;
    }

    public Result<WorkItem> heartbeat(WorkItemId id, Worker worker) {

        Instant now = timeSource.now();

        Result<WorkItem> found = find(id);

        if (found.isFailure()) {

            return found;
        }

        Result<WorkItem> renewed = found.getValue().heartbeat(worker.id(), now, now.plus(leaseTtl));

        if (renewed.isFailure()) {

            return renewed;
        }

        items.put(id, renewed.getValue());

        return renewed;
    }

    public void sweepExpiredLeases() {

        Instant now = timeSource.now();

        for (WorkItem item : new ArrayList<>(items.values())) {

            Result<WorkItem> reclaimed = item.reclaim(now);

            if (reclaimed.isSuccess()) {

                items.put(item.id(), reclaimed.getValue());
                events.emit(WorkItemLeaseExpired.of(item.id()));
            }
        }
    }

    public Result<WorkItem> report(WorkItemId id, ActivationDiagnostics diagnostics) {

        Instant now = timeSource.now();

        Result<WorkItem> found = find(id);

        if (found.isFailure()) {

            return found;
        }

        WorkItem item = found.getValue();

        if (!isCurrent(item)) {

            return Result.failure(StaleReport.instance());
        }

        if (item.state().isTerminal()) {

            return Result.failure(StaleReport.instance());
        }

        Result<WorkerId> reporter = WorkerId.of(diagnostics.getOrigin());

        if (reporter.isFailure()) {

            return Result.failure(reporter.getError());
        }

        if (!item.lease().isHeldBy(reporter.getValue())) {

            return Result.failure(NotLeaseHolder.instance());
        }

        finalizePort.finalizeActivation(item.trustRelationshipId(), diagnostics);

        if (diagnostics.getStatus() == ActivationStatus.NO_DATA) {

            return Result.success(item);
        }

        Result<WorkItem> completed = item.complete(now);

        if (completed.isSuccess()) {

            items.put(id, completed.getValue());
        }

        return completed;
    }

    public Result<WorkItem> onActivationCancelled(TrustRelationshipRef trustRelationshipId) {

        Instant now = timeSource.now();

        WorkItemId currentId = currentByTr.get(trustRelationshipId);

        if (currentId == null) {

            return Result.failure(WorkItemNotFound.instance());
        }

        Result<WorkItem> found = find(currentId);

        if (found.isFailure()) {

            return found;
        }

        Result<WorkItem> cancelled = found.getValue().cancel(now);

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
