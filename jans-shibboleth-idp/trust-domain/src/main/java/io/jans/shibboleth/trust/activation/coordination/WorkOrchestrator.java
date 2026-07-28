package io.jans.shibboleth.trust.activation.coordination;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.jans.shibboleth.trust.activation.error.NotLeaseHolder;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.activation.error.StaleReport;
import io.jans.shibboleth.trust.activation.error.WorkItemNotFound;
import io.jans.shibboleth.trust.activation.error.WorkerNotAlive;
import io.jans.shibboleth.trust.activation.model.ClaimOutcome;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.activation.repository.WorkItemRepository;
import io.jans.shibboleth.trust.activation.repository.WorkerRepository;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationStatus;

public final class WorkOrchestrator {

    private final TimeSource timeSource;
    private final Duration leaseTtl;
    private final Duration heartbeatTtl;
    private final ActivationEventSink events;
    private final FinalizeActivationPort finalizePort;

    private final WorkItemRepository workItems;
    private final WorkerRepository workers;
    private final Map<TrustRelationshipRef, WorkItemId> currentByTr = new HashMap<>();

    private WorkOrchestrator(TimeSource timeSource, Duration leaseTtl, Duration heartbeatTtl,
                             ActivationEventSink events, FinalizeActivationPort finalizePort,
                             WorkItemRepository workItems, WorkerRepository workers) {

        this.timeSource = timeSource;
        this.leaseTtl = leaseTtl;
        this.heartbeatTtl = heartbeatTtl;
        this.events = events;
        this.finalizePort = finalizePort;
        this.workItems = workItems;
        this.workers = workers;
    }

    public static Result<WorkOrchestrator> create(TimeSource timeSource, Duration leaseTtl, Duration heartbeatTtl,
                                                            ActivationEventSink events, FinalizeActivationPort finalizePort,
                                                            WorkItemRepository workItems, WorkerRepository workers) {

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

        if (workItems == null) {

            return Result.failure(RequiredValueMissing.forField("workItems"));
        }

        if (workers == null) {

            return Result.failure(RequiredValueMissing.forField("workers"));
        }

        return Result.success(new WorkOrchestrator(timeSource, leaseTtl, heartbeatTtl, events, finalizePort,
            workItems, workers));
    }

    public Result<WorkItem> onActivationRequested(TrustRelationshipRef trustRelationshipId, WorkItemType type) {

        Result<WorkItem> created = WorkItem.create(type, trustRelationshipId, timeSource.now());

        if (created.isFailure()) {

            return created;
        }

        Result<WorkItem> saved = workItems.save(created.getValue());

        if (saved.isFailure()) {

            return saved;
        }

        currentByTr.put(trustRelationshipId, saved.getValue().id());

        return saved;
    }

    public Result<WorkItem> find(WorkItemId id) {

        return workItems.findById(id);
    }

    public Result<Worker> registerWorker(WorkerId id) {

        Result<Worker> registered = Worker.register(id, timeSource.now());

        if (registered.isFailure()) {

            return registered;
        }

        return workers.save(registered.getValue());
    }

    public Result<Worker> heartbeatWorker(WorkerId id) {

        Result<Worker> found = workers.findById(id);

        if (found.isFailure()) {

            return found;
        }

        Result<Worker> renewed = found.getValue().heartbeat(timeSource.now());

        if (renewed.isFailure()) {

            return renewed;
        }

        return workers.save(renewed.getValue());
    }

    public Result<Worker> findWorker(WorkerId id) {

        return workers.findById(id);
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

        Result<WorkItem> saved = workItems.save(assigned.getValue());

        if (saved.isFailure()) {

            return saved;
        }

        events.emit(WorkItemAssigned.of(id, worker.id()));

        return saved;
    }

    /**
     * Atomically selects the oldest {@code PENDING} work item of the given type and claims it for the
     * worker. Returns a {@link ClaimOutcome} that either carries the claimed item or is empty when
     * nothing is claimable (not an error — a poll of an empty queue). Fails only when the worker is not
     * alive or an argument is missing.
     */
    public Result<ClaimOutcome> claimNext(WorkItemType type, Worker worker) {

        if (type == null) {

            return Result.failure(RequiredValueMissing.forField("type"));
        }

        if (worker == null) {

            return Result.failure(RequiredValueMissing.forField("worker"));
        }

        if (!worker.isAlive(timeSource.now(), heartbeatTtl)) {

            return Result.failure(WorkerNotAlive.instance());
        }

        Result<List<WorkItem>> candidates = workItems.findClaimableCandidates(type);

        if (candidates.isFailure()) {

            return Result.failure(candidates.getError());
        }

        WorkItem candidate = null;

        for (WorkItem item : candidates.getValue()) {

            if (item.state() == WorkItemState.PENDING) {

                candidate = item;
                break;
            }
        }

        if (candidate == null) {

            return Result.success(ClaimOutcome.none());
        }

        Result<WorkItem> assigned = claim(candidate.id(), worker);

        if (assigned.isFailure()) {

            return Result.failure(assigned.getError());
        }

        return Result.success(ClaimOutcome.of(assigned.getValue()));
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

        return workItems.save(renewed.getValue());
    }

    public void sweepExpiredLeases() {

        Instant now = timeSource.now();

        for (WorkItemType type : WorkItemType.values()) {

            Result<List<WorkItem>> candidates = workItems.findClaimableCandidates(type);

            if (candidates.isFailure()) {

                continue;
            }

            for (WorkItem item : candidates.getValue()) {

                Result<WorkItem> reclaimed = item.reclaim(now);

                if (reclaimed.isSuccess()) {

                    workItems.save(reclaimed.getValue());
                    events.emit(WorkItemLeaseExpired.of(item.id()));
                }
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

            workItems.save(completed.getValue());
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

        Result<WorkItem> saved = workItems.save(cancelled.getValue());

        if (saved.isFailure()) {

            return saved;
        }

        currentByTr.remove(trustRelationshipId);

        return saved;
    }

    public boolean isCurrent(WorkItem item) {

        WorkItemId current = currentByTr.get(item.trustRelationshipId());

        return current != null && current.equals(item.id());
    }
}
