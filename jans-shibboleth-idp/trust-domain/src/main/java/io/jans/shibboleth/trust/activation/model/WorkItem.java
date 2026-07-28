package io.jans.shibboleth.trust.activation.model;

import java.time.Instant;

import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.activation.error.WorkItemTransitionNotAllowed;
import io.jans.shibboleth.trust.shared.Result;

/**
 * A unit of activation work. The work item owns only its identity, type, trust-relationship reference and
 * lifecycle timestamps plus a stored lifecycle marker; <b>assignment is not part of the work item</b> — it
 * lives in a separate lease aggregate. The stored marker is therefore only ever {@code PENDING} (active) or
 * a terminal {@code COMPLETED}/{@code CANCELLED}; the fully-resolved {@code ASSIGNED}/{@code PENDING} state
 * is derived from whether a live lease exists via {@link #state(boolean)}.
 *
 * <p>The work item's own transitions are the terminal ones — {@link #complete(Instant)} and
 * {@link #cancel(Instant)} — each permitted at most once. Claiming, heartbeating and reclaiming are lease
 * operations owned by the orchestrator, not the work item.
 */
public final class WorkItem {

    private final WorkItemId id;
    private final WorkItemType type;
    private final TrustRelationshipRef trustRelationshipId;
    private final WorkItemState state;
    private final Instant createdAt;
    private final Instant lastTransitionAt;

    private WorkItem(WorkItemId id, WorkItemType type, TrustRelationshipRef trustRelationshipId,
                     WorkItemState state, Instant createdAt, Instant lastTransitionAt) {

        this.id = id;
        this.type = type;
        this.trustRelationshipId = trustRelationshipId;
        this.state = state;
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

        return Result.success(new WorkItem(WorkItemId.generate(), type, trustRelationshipId,
            WorkItemState.PENDING, now, now));
    }

    /**
     * Reconstruct a work item verbatim from its persisted identity, stored lifecycle marker and timestamps.
     * A work item's persisted form carries no lease — assignment is a separate aggregate whose presence
     * drives {@link #state(boolean)}.
     */
    public static Result<WorkItem> rehydrate(WorkItemId id, WorkItemType type,
                                             TrustRelationshipRef trustRelationshipId, WorkItemState state,
                                             Instant createdAt, Instant lastTransitionAt) {

        if (id == null) {

            return Result.failure(RequiredValueMissing.forField("id"));
        }

        if (type == null) {

            return Result.failure(RequiredValueMissing.forField("type"));
        }

        if (trustRelationshipId == null) {

            return Result.failure(RequiredValueMissing.forField("trustRelationshipId"));
        }

        if (state == null) {

            return Result.failure(RequiredValueMissing.forField("state"));
        }

        if (createdAt == null) {

            return Result.failure(RequiredValueMissing.forField("createdAt"));
        }

        if (lastTransitionAt == null) {

            return Result.failure(RequiredValueMissing.forField("lastTransitionAt"));
        }

        return Result.success(new WorkItem(id, type, trustRelationshipId, state, createdAt, lastTransitionAt));
    }

    public Result<WorkItem> complete(Instant now) {

        if (state.isTerminal()) {

            return Result.failure(WorkItemTransitionNotAllowed.of("complete", state.name()));
        }

        if (now == null) {

            return Result.failure(RequiredValueMissing.forField("now"));
        }

        return Result.success(with(WorkItemState.COMPLETED, now));
    }

    public Result<WorkItem> cancel(Instant now) {

        if (state.isTerminal()) {

            return Result.failure(WorkItemTransitionNotAllowed.of("cancel", state.name()));
        }

        if (now == null) {

            return Result.failure(RequiredValueMissing.forField("now"));
        }

        return Result.success(with(WorkItemState.CANCELLED, now));
    }

    private WorkItem with(WorkItemState newState, Instant transitionAt) {

        return new WorkItem(id, type, trustRelationshipId, newState, createdAt, transitionAt);
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

    /**
     * The stored lifecycle marker: {@code PENDING} while active, or the terminal {@code COMPLETED}/
     * {@code CANCELLED}. This never reports {@code ASSIGNED} — assignment is derived from lease presence via
     * {@link #state(boolean)}.
     */
    public WorkItemState state() {

        return state;
    }

    /**
     * The fully-resolved state: {@code COMPLETED}/{@code CANCELLED} are terminal and authoritative regardless
     * of any lease, while a non-terminal item is {@code ASSIGNED} exactly when a live lease exists for it and
     * {@code PENDING} otherwise.
     */
    public WorkItemState state(boolean hasLiveLease) {

        if (state.isTerminal()) {

            return state;
        }

        return hasLiveLease ? WorkItemState.ASSIGNED : WorkItemState.PENDING;
    }

    public boolean isTerminal() {

        return state.isTerminal();
    }

    public Instant createdAt() {

        return createdAt;
    }

    public Instant lastTransitionAt() {

        return lastTransitionAt;
    }
}
