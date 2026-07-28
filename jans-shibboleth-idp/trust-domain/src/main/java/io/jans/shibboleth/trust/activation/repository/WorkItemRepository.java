package io.jans.shibboleth.trust.activation.repository;

import java.util.List;

import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.shared.Result;

/**
 * Outbound port for storing and retrieving work items. A domain contract — the adapter implements it over
 * the store; the orchestrator depends only on this interface.
 *
 * <p>It also holds the durable <b>current-episode pointer</b>: at most one work item is the current episode
 * per trust relationship. The pointer survives restart and is shared across nodes, so the orchestrator keeps
 * no in-memory episode state.
 */
public interface WorkItemRepository {

    /** Insert a new work item or update an existing one, keyed by its id. */
    Result<WorkItem> save(WorkItem workItem);

    /** Find a work item by id, or fail with {@code WorkItemNotFound} when absent. */
    Result<WorkItem> findById(WorkItemId id);

    Result<Void> delete(WorkItemId id);

    /**
     * The non-terminal work items of the given type, oldest first (by creation). These are <em>candidates</em>
     * for claiming: whether a candidate is actually free is decided against the leases, not here, since a
     * work item's assignment lives in a separate aggregate.
     */
    Result<List<WorkItem>> findClaimableCandidates(WorkItemType type);

    /** Point the trust relationship's current episode at this work item (upsert, one per trust relationship). */
    Result<Void> assignCurrentEpisode(TrustRelationshipRef trustRelationshipId, WorkItemId workItemId);

    /** The current-episode work item for a trust relationship, or {@code WorkItemNotFound} when there is none. */
    Result<WorkItemId> currentEpisode(TrustRelationshipRef trustRelationshipId);

    /** Clear the current-episode pointer for a trust relationship. Idempotent. */
    Result<Void> clearCurrentEpisode(TrustRelationshipRef trustRelationshipId);
}
