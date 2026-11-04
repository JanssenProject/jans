package io.jans.shibboleth.trust.activation.model;

import java.time.Instant;

import io.jans.shibboleth.trust.activation.lease.Lease;
import io.jans.shibboleth.trust.activation.workers.WorkerId;

/**
 * The activation picture of a work item at a point in time: the {@link WorkItem} together with its current
 * live lease, if any. This is the orchestrator's return shape — one object carrying the whole state a caller
 * (or the read view) needs. Assignment is decided by the orchestrator when it builds this (a live lease at
 * "now"), not re-derived by callers.
 *
 * <p>Absence of a lease (a pending or terminal item) is modelled by {@link #unassigned(WorkItem)} rather than
 * a null lease; the lease-dependent accessors are guarded behind {@link #isAssigned()}.
 */
public final class WorkItemActivation {

    private final WorkItem workItem;
    private final Lease lease;
    private final boolean assigned;

    private WorkItemActivation(WorkItem workItem, Lease lease, boolean assigned) {

        this.workItem = workItem;
        this.lease = lease;
        this.assigned = assigned;
    }

    public static WorkItemActivation assigned(WorkItem workItem, Lease lease) {

        return new WorkItemActivation(workItem, lease, true);
    }

    public static WorkItemActivation unassigned(WorkItem workItem) {

        return new WorkItemActivation(workItem, null, false);
    }

    public WorkItem workItem() {

        return workItem;
    }

    public WorkItemId id() {

        return workItem.id();
    }

    public WorkItemType type() {

        return workItem.type();
    }

    public TrustRelationshipRef trustRelationshipId() {

        return workItem.trustRelationshipId();
    }

    /** The fully-resolved state: {@code ASSIGNED} when a live lease is held, else the work item's own state. */
    public WorkItemState state() {

        return workItem.state(assigned);
    }

    public boolean isAssigned() {

        return assigned;
    }

    public boolean isHeldBy(WorkerId candidate) {

        return assigned && lease.isHeldBy(candidate);
    }

    public WorkerId heldBy() {

        requireAssigned();

        return lease.holder();
    }

    public Instant leaseExpiresAt() {

        requireAssigned();

        return lease.expiresAt();
    }

    private void requireAssigned() {

        if (!assigned) {

            throw new IllegalStateException("no active lease; check isAssigned() first");
        }
    }
}
