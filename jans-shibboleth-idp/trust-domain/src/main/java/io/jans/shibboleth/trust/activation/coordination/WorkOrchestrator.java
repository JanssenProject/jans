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
import io.jans.shibboleth.trust.activation.error.WorkItemTransitionNotAllowed;
import io.jans.shibboleth.trust.activation.error.WorkerNotAlive;
import io.jans.shibboleth.trust.activation.lease.Lease;
import io.jans.shibboleth.trust.activation.lease.LeaseGeneration;
import io.jans.shibboleth.trust.activation.model.ClaimOutcome;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemActivation;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.activation.repository.LeaseRepository;
import io.jans.shibboleth.trust.activation.repository.WorkItemRepository;
import io.jans.shibboleth.trust.activation.repository.WorkerRepository;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationStatus;

/**
 * Coordinates activation work across many workers. State is durable and shared through repositories; the
 * only in-memory state is the current-episode pointer per trust relationship. Assignment is a lease: to
 * claim (or take over) a work item, a worker atomically creates the next-generation lease, and the store's
 * identity uniqueness ensures at most one winner. A live lease is what makes an item {@code ASSIGNED};
 * lease expiry (implicit) makes it claimable again. Operations return a {@link WorkItemActivation} carrying
 * the work item together with its current lease.
 */
public final class WorkOrchestrator {

    private final TimeSource timeSource;
    private final Duration leaseTtl;
    private final Duration heartbeatTtl;
    private final ActivationEventSink events;
    private final FinalizeActivationPort finalizePort;

    private final WorkItemRepository workItems;
    private final LeaseRepository leases;
    private final WorkerRepository workers;
    private final Map<TrustRelationshipRef, WorkItemId> currentByTr = new HashMap<>();

    private WorkOrchestrator(TimeSource timeSource, Duration leaseTtl, Duration heartbeatTtl,
                             ActivationEventSink events, FinalizeActivationPort finalizePort,
                             WorkItemRepository workItems, LeaseRepository leases, WorkerRepository workers) {

        this.timeSource = timeSource;
        this.leaseTtl = leaseTtl;
        this.heartbeatTtl = heartbeatTtl;
        this.events = events;
        this.finalizePort = finalizePort;
        this.workItems = workItems;
        this.leases = leases;
        this.workers = workers;
    }

    public static Result<WorkOrchestrator> create(TimeSource timeSource, Duration leaseTtl, Duration heartbeatTtl,
                                                            ActivationEventSink events, FinalizeActivationPort finalizePort,
                                                            WorkItemRepository workItems, LeaseRepository leases,
                                                            WorkerRepository workers) {

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

        if (leases == null) {

            return Result.failure(RequiredValueMissing.forField("leases"));
        }

        if (workers == null) {

            return Result.failure(RequiredValueMissing.forField("workers"));
        }

        return Result.success(new WorkOrchestrator(timeSource, leaseTtl, heartbeatTtl, events, finalizePort,
            workItems, leases, workers));
    }

    public Result<WorkItemActivation> onActivationRequested(TrustRelationshipRef trustRelationshipId,
                                                            WorkItemType type) {

        Result<WorkItem> created = WorkItem.create(type, trustRelationshipId, timeSource.now());

        if (created.isFailure()) {

            return Result.failure(created.getError());
        }

        Result<WorkItem> saved = workItems.save(created.getValue());

        if (saved.isFailure()) {

            return Result.failure(saved.getError());
        }

        currentByTr.put(trustRelationshipId, saved.getValue().id());

        return Result.success(WorkItemActivation.unassigned(saved.getValue()));
    }

    public Result<WorkItemActivation> find(WorkItemId id) {

        Instant now = timeSource.now();

        Result<WorkItem> found = workItems.findById(id);

        if (found.isFailure()) {

            return Result.failure(found.getError());
        }

        Lease current = currentLiveLease(id, now);

        return Result.success(current != null
            ? WorkItemActivation.assigned(found.getValue(), current)
            : WorkItemActivation.unassigned(found.getValue()));
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

    public Result<WorkItemActivation> claim(WorkItemId id, Worker worker) {

        Instant now = timeSource.now();

        Result<WorkItem> found = workItems.findById(id);

        if (found.isFailure()) {

            return Result.failure(found.getError());
        }

        if (!worker.isAlive(now, heartbeatTtl)) {

            return Result.failure(WorkerNotAlive.instance());
        }

        WorkItem item = found.getValue();

        if (item.isTerminal()) {

            return Result.failure(WorkItemTransitionNotAllowed.of("claim", item.state().name()));
        }

        Result<List<Lease>> existing = leases.findByWorkItem(id);

        if (existing.isFailure()) {

            return Result.failure(existing.getError());
        }

        Lease current = maxGeneration(existing.getValue());

        if (current != null && current.isLive(now)) {

            return Result.failure(WorkItemTransitionNotAllowed.of("claim", "ASSIGNED"));
        }

        LeaseGeneration nextGeneration = current == null ? LeaseGeneration.first() : current.generation().next();

        Result<Lease> granted = Lease.granted(id, nextGeneration, worker.id(), now, now.plus(leaseTtl));

        if (granted.isFailure()) {

            return Result.failure(granted.getError());
        }

        Result<Lease> created = leases.create(granted.getValue());

        if (created.isFailure()) {

            return Result.failure(created.getError());
        }

        events.emit(WorkItemAssigned.of(id, worker.id()));

        return Result.success(WorkItemActivation.assigned(item, created.getValue()));
    }

    /**
     * Atomically selects the oldest claimable work item of the given type and claims it for the worker.
     * Returns a {@link ClaimOutcome} that either carries the claimed activation or is empty when nothing is
     * claimable (not an error — a poll of an empty queue). Fails only when the worker is not alive or an
     * argument is missing. A candidate lost to another worker mid-poll is skipped and the next is tried.
     */
    public Result<ClaimOutcome> claimNext(WorkItemType type, Worker worker) {

        if (type == null) {

            return Result.failure(RequiredValueMissing.forField("type"));
        }

        if (worker == null) {

            return Result.failure(RequiredValueMissing.forField("worker"));
        }

        Instant now = timeSource.now();

        if (!worker.isAlive(now, heartbeatTtl)) {

            return Result.failure(WorkerNotAlive.instance());
        }

        Result<List<WorkItem>> candidates = workItems.findClaimableCandidates(type);

        if (candidates.isFailure()) {

            return Result.failure(candidates.getError());
        }

        for (WorkItem candidate : candidates.getValue()) {

            if (currentLiveLease(candidate.id(), now) != null) {

                continue;
            }

            Result<WorkItemActivation> claimed = claim(candidate.id(), worker);

            if (claimed.isSuccess()) {

                return Result.success(ClaimOutcome.of(claimed.getValue()));
            }
        }

        return Result.success(ClaimOutcome.none());
    }

    public Result<WorkItemActivation> heartbeat(WorkItemId id, Worker worker) {

        Instant now = timeSource.now();

        Result<WorkItem> found = workItems.findById(id);

        if (found.isFailure()) {

            return Result.failure(found.getError());
        }

        Result<List<Lease>> existing = leases.findByWorkItem(id);

        if (existing.isFailure()) {

            return Result.failure(existing.getError());
        }

        Lease current = maxGeneration(existing.getValue());

        if (current == null || !current.isLive(now) || !current.isHeldBy(worker.id())) {

            return Result.failure(NotLeaseHolder.instance());
        }

        Result<Lease> renewed = current.renew(now.plus(leaseTtl));

        if (renewed.isFailure()) {

            return Result.failure(renewed.getError());
        }

        Result<Lease> saved = leases.renew(renewed.getValue());

        if (saved.isFailure()) {

            return Result.failure(saved.getError());
        }

        return Result.success(WorkItemActivation.assigned(found.getValue(), saved.getValue()));
    }

    public void sweepExpiredLeases() {

        Instant now = timeSource.now();

        for (WorkItemType type : WorkItemType.values()) {

            Result<List<WorkItem>> candidates = workItems.findClaimableCandidates(type);

            if (candidates.isFailure()) {

                continue;
            }

            for (WorkItem item : candidates.getValue()) {

                Result<List<Lease>> leasesForItem = leases.findByWorkItem(item.id());

                if (leasesForItem.isFailure() || leasesForItem.getValue().isEmpty()) {

                    continue;
                }

                if (maxGeneration(leasesForItem.getValue()).isExpired(now)) {

                    for (Lease lease : leasesForItem.getValue()) {

                        leases.delete(lease);
                    }

                    events.emit(WorkItemLeaseExpired.of(item.id()));
                }
            }
        }
    }

    public Result<WorkItemActivation> report(WorkItemId id, ActivationDiagnostics diagnostics) {

        Instant now = timeSource.now();

        Result<WorkItem> found = workItems.findById(id);

        if (found.isFailure()) {

            return Result.failure(found.getError());
        }

        WorkItem item = found.getValue();

        if (!isCurrent(item)) {

            return Result.failure(StaleReport.instance());
        }

        if (item.isTerminal()) {

            return Result.failure(StaleReport.instance());
        }

        Result<WorkerId> reporter = WorkerId.of(diagnostics.getOrigin());

        if (reporter.isFailure()) {

            return Result.failure(reporter.getError());
        }

        Result<List<Lease>> existing = leases.findByWorkItem(id);

        if (existing.isFailure()) {

            return Result.failure(existing.getError());
        }

        Lease current = maxGeneration(existing.getValue());

        if (current == null || !current.isLive(now) || !current.isHeldBy(reporter.getValue())) {

            return Result.failure(NotLeaseHolder.instance());
        }

        finalizePort.finalizeActivation(item.trustRelationshipId(), diagnostics);

        if (diagnostics.getStatus() == ActivationStatus.NO_DATA) {

            return Result.success(WorkItemActivation.assigned(item, current));
        }

        Result<WorkItem> completed = item.complete(now);

        if (completed.isFailure()) {

            return Result.failure(completed.getError());
        }

        Result<WorkItem> saved = workItems.save(completed.getValue());

        if (saved.isFailure()) {

            return Result.failure(saved.getError());
        }

        leases.delete(current);

        return Result.success(WorkItemActivation.unassigned(saved.getValue()));
    }

    public Result<WorkItemActivation> onActivationCancelled(TrustRelationshipRef trustRelationshipId) {

        Instant now = timeSource.now();

        WorkItemId currentId = currentByTr.get(trustRelationshipId);

        if (currentId == null) {

            return Result.failure(WorkItemNotFound.instance());
        }

        Result<WorkItem> found = workItems.findById(currentId);

        if (found.isFailure()) {

            return Result.failure(found.getError());
        }

        Result<WorkItem> cancelled = found.getValue().cancel(now);

        if (cancelled.isFailure()) {

            return Result.failure(cancelled.getError());
        }

        Result<WorkItem> saved = workItems.save(cancelled.getValue());

        if (saved.isFailure()) {

            return Result.failure(saved.getError());
        }

        Result<List<Lease>> existing = leases.findByWorkItem(currentId);

        if (existing.isSuccess()) {

            for (Lease lease : existing.getValue()) {

                leases.delete(lease);
            }
        }

        currentByTr.remove(trustRelationshipId);

        return Result.success(WorkItemActivation.unassigned(saved.getValue()));
    }

    public boolean isCurrent(WorkItem item) {

        WorkItemId current = currentByTr.get(item.trustRelationshipId());

        return current != null && current.equals(item.id());
    }

    public boolean isCurrent(WorkItemActivation activation) {

        return isCurrent(activation.workItem());
    }

    private Lease currentLiveLease(WorkItemId id, Instant now) {

        Result<List<Lease>> existing = leases.findByWorkItem(id);

        if (existing.isFailure()) {

            return null;
        }

        Lease current = maxGeneration(existing.getValue());

        return current != null && current.isLive(now) ? current : null;
    }

    private static Lease maxGeneration(List<Lease> leasesForItem) {

        Lease max = null;

        for (Lease lease : leasesForItem) {

            if (max == null || lease.generation().isAfter(max.generation())) {

                max = lease;
            }
        }

        return max;
    }
}
