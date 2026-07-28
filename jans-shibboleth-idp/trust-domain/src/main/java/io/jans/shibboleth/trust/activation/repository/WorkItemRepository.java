package io.jans.shibboleth.trust.activation.repository;

import java.util.List;

import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.shared.Result;

/**
 * Outbound port for storing and retrieving work items. A domain contract — the adapter implements it over
 * the store; the orchestrator depends only on this interface.
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
}
