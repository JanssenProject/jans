package io.jans.shibboleth.trust.activation.repository;

import java.util.List;

import io.jans.shibboleth.trust.activation.lease.Lease;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.shared.Result;

/**
 * Outbound port for lease acquisition and lifecycle. A lease's identity is {@code (workItemId, generation)};
 * the adapter folds that into a deterministic id so that {@link #create} is an atomic, at-most-one-winner
 * operation — the store's identity uniqueness <em>is</em> the lock.
 */
public interface LeaseRepository {

    /**
     * Atomically create a lease. Succeeds for the single winner; fails with {@code LeaseAlreadyHeld} when a
     * lease for the same {@code (workItemId, generation)} already exists — i.e. another worker won the race.
     */
    Result<Lease> create(Lease lease);

    /**
     * All lease rows for a work item, across generations (there may be several awaiting garbage collection).
     * The caller reduces them to the current holder (the highest generation) and checks liveness. An empty
     * list means the work item is unassigned.
     */
    Result<List<Lease>> findByWorkItem(WorkItemId workItemId);

    /**
     * Extend the window of an existing lease (a heartbeat). Fails with {@code LeaseNotPresent} when no lease
     * with that identity exists — the holder has lost it.
     */
    Result<Lease> renew(Lease lease);

    /** Remove a lease by identity. Idempotent: deleting an absent lease still succeeds (race-safe GC). */
    Result<Void> delete(Lease lease);
}
