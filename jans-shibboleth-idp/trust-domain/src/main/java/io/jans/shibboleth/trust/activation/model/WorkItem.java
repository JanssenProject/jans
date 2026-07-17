package io.jans.shibboleth.trust.activation.model;

import java.time.Instant;

import io.jans.shibboleth.trust.activation.error.LeaseStillValid;
import io.jans.shibboleth.trust.activation.error.NotLeaseHolder;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.activation.error.WorkItemTransitionNotAllowed;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.activation.workers.WorkerId;

public final class WorkItem {

    private final WorkItemId id;
    private final WorkItemType type;
    private final TrustRelationshipRef trustRelationshipId;
    private final WorkItemState state;
    private final Lease lease;
    private final Instant createdAt;
    private final Instant lastTransitionAt;

    private WorkItem(WorkItemId id, WorkItemType type, TrustRelationshipRef trustRelationshipId,
                     WorkItemState state, Lease lease, Instant createdAt, Instant lastTransitionAt) {

        this.id = id;
        this.type = type;
        this.trustRelationshipId = trustRelationshipId;
        this.state = state;
        this.lease = lease;
        this.createdAt = createdAt;
        this.lastTransitionAt = lastTransitionAt;
    }

    public static Result<WorkItem> create(WorkItemType type, TrustRelationshipRef trustRelationshipId, Instant now) {

        if (type == null) {

            return Result.failure(RequiredValueMissing.forField("type"));
        }

        if (trustRelationshipId == null) {

            return Result.failure(RequiredValueMissing.forField("trustRelationshipId"));
        }

        if (now == null) {

            return Result.failure(RequiredValueMissing.forField("now"));
        }

        WorkItem item = new WorkItem(WorkItemId.generate(), type, trustRelationshipId,
            WorkItemState.PENDING, Lease.NONE, now, now);

        return Result.success(item);
    }

    public Result<WorkItem> claim(WorkerId worker, Instant now, Instant leaseExpiresAt) {

        if (state != WorkItemState.PENDING) {

            return Result.failure(WorkItemTransitionNotAllowed.of("claim", state.name()));
        }

        if (now == null) {

            return Result.failure(RequiredValueMissing.forField("now"));
        }

        Result<Lease> granted = Lease.granted(worker, now, leaseExpiresAt);

        if (granted.isFailure()) {

            return Result.failure(granted.getError());
        }

        return Result.success(with(WorkItemState.ASSIGNED, granted.getValue(), now));
    }

    public Result<WorkItem> complete(Instant now) {

        if (state != WorkItemState.ASSIGNED) {

            return Result.failure(WorkItemTransitionNotAllowed.of("complete", state.name()));
        }

        if (now == null) {

            return Result.failure(RequiredValueMissing.forField("now"));
        }

        return Result.success(with(WorkItemState.COMPLETED, lease, now));
    }

    public Result<WorkItem> cancel(Instant now) {

        if (state.isTerminal()) {

            return Result.failure(WorkItemTransitionNotAllowed.of("cancel", state.name()));
        }

        if (now == null) {

            return Result.failure(RequiredValueMissing.forField("now"));
        }

        return Result.success(with(WorkItemState.CANCELLED, Lease.NONE, now));
    }

    public Result<WorkItem> heartbeat(WorkerId worker, Instant now, Instant newExpiresAt) {

        if (state != WorkItemState.ASSIGNED) {

            return Result.failure(WorkItemTransitionNotAllowed.of("heartbeat", state.name()));
        }

        if (!lease.isHeldBy(worker)) {

            return Result.failure(NotLeaseHolder.instance());
        }

        if (now == null) {

            return Result.failure(RequiredValueMissing.forField("now"));
        }

        Result<Lease> renewed = lease.renew(newExpiresAt);

        if (renewed.isFailure()) {

            return Result.failure(renewed.getError());
        }

        return Result.success(with(WorkItemState.ASSIGNED, renewed.getValue(), now));
    }

    public Result<WorkItem> reclaim(Instant now) {

        if (state != WorkItemState.ASSIGNED) {

            return Result.failure(WorkItemTransitionNotAllowed.of("reclaim", state.name()));
        }

        if (now == null) {

            return Result.failure(RequiredValueMissing.forField("now"));
        }

        if (!lease.isExpired(now)) {

            return Result.failure(LeaseStillValid.instance());
        }

        return Result.success(with(WorkItemState.PENDING, Lease.NONE, now));
    }

    private WorkItem with(WorkItemState newState, Lease newLease, Instant transitionAt) {

        return new WorkItem(id, type, trustRelationshipId, newState, newLease, createdAt, transitionAt);
    }

    public WorkItemId id() {

        return id;
    }

    public WorkItemType type() {

        return type;
    }

    public TrustRelationshipRef trustRelationshipId() {

        return trustRelationshipId;
    }

    public WorkItemState state() {

        return state;
    }

    public Lease lease() {

        return lease;
    }

    public Instant createdAt() {

        return createdAt;
    }

    public Instant lastTransitionAt() {

        return lastTransitionAt;
    }
}
