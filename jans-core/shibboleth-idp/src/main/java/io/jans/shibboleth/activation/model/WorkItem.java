package io.jans.shibboleth.activation.model;

import java.time.Instant;

import io.jans.shibboleth.activation.error.RequiredValueMissing;
import io.jans.shibboleth.activation.util.ActivationResult;

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

    public static ActivationResult<WorkItem> create(WorkItemType type, TrustRelationshipRef trustRelationshipId, Instant now) {

        if (type == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("type"));
        }

        if (trustRelationshipId == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("trustRelationshipId"));
        }

        if (now == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("now"));
        }

        WorkItem item = new WorkItem(WorkItemId.generate(), type, trustRelationshipId,
            WorkItemState.PENDING, Lease.NONE, now, now);

        return ActivationResult.success(item);
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
