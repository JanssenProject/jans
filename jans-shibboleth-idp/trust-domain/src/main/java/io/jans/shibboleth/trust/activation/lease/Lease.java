package io.jans.shibboleth.trust.activation.lease;

import java.time.Instant;
import java.util.Objects;

import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

/**
 * A lease binds a work item to a single holder for a bounded time window, at a given fencing
 * {@link LeaseGeneration}. It is a <b>create-only</b> aggregate: a claim (or a takeover) is the creation of
 * a new lease, never an in-place mutation, and its absence — not a {@code NONE} sentinel — means the work
 * item is unassigned. The only permitted change is {@link #renew(Instant)} (a heartbeat extending the
 * window), which the holder applies to its own lease.
 *
 * <p>The lease carries no identity of its own: the persistence adapter derives a deterministic id from
 * {@code (workItemId, generation)} so that two workers racing for the same generation collide, and exactly
 * one wins.
 *
 * <p>{@link #granted} validates and constructs; it is used both to acquire a fresh lease and to rehydrate a
 * stored one, since both simply build a valid lease from its full field set.
 */
public final class Lease {

    private final WorkItemId workItemId;
    private final LeaseGeneration generation;
    private final WorkerId holder;
    private final Instant grantedAt;
    private final Instant expiresAt;

    private Lease(WorkItemId workItemId, LeaseGeneration generation, WorkerId holder,
                  Instant grantedAt, Instant expiresAt) {

        this.workItemId = workItemId;
        this.generation = generation;
        this.holder = holder;
        this.grantedAt = grantedAt;
        this.expiresAt = expiresAt;
    }

    public static Result<Lease> granted(WorkItemId workItemId, LeaseGeneration generation, WorkerId holder,
                                        Instant grantedAt, Instant expiresAt) {

        if (workItemId == null) {

            return Result.failure(RequiredValueMissing.forField("workItemId"));
        }

        if (generation == null) {

            return Result.failure(RequiredValueMissing.forField("generation"));
        }

        if (holder == null) {

            return Result.failure(RequiredValueMissing.forField("holder"));
        }

        if (grantedAt == null) {

            return Result.failure(RequiredValueMissing.forField("grantedAt"));
        }

        if (expiresAt == null) {

            return Result.failure(RequiredValueMissing.forField("expiresAt"));
        }

        return Result.success(new Lease(workItemId, generation, holder, grantedAt, expiresAt));
    }

    public Result<Lease> renew(Instant newExpiresAt) {

        if (newExpiresAt == null) {

            return Result.failure(RequiredValueMissing.forField("expiresAt"));
        }

        return Result.success(new Lease(workItemId, generation, holder, grantedAt, newExpiresAt));
    }

    public boolean isExpired(Instant now) {

        return now.isAfter(expiresAt);
    }

    public boolean isLive(Instant now) {

        return !isExpired(now);
    }

    public boolean isHeldBy(WorkerId candidate) {

        return Objects.equals(holder, candidate);
    }

    public WorkItemId workItemId() {

        return workItemId;
    }

    public LeaseGeneration generation() {

        return generation;
    }

    public WorkerId holder() {

        return holder;
    }

    public Instant grantedAt() {

        return grantedAt;
    }

    public Instant expiresAt() {

        return expiresAt;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lease lease = (Lease) o;

        return Objects.equals(workItemId, lease.workItemId)
            && Objects.equals(generation, lease.generation)
            && Objects.equals(holder, lease.holder)
            && Objects.equals(grantedAt, lease.grantedAt)
            && Objects.equals(expiresAt, lease.expiresAt);
    }

    @Override
    public int hashCode() {

        return Objects.hash(workItemId, generation, holder, grantedAt, expiresAt);
    }

    @Override
    public String toString() {

        return "Lease{workItemId=" + workItemId + ", generation=" + generation
            + ", holder=" + holder + ", grantedAt=" + grantedAt + ", expiresAt=" + expiresAt + "}";
    }
}
