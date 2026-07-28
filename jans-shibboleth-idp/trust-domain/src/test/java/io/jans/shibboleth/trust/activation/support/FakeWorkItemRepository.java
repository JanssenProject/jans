package io.jans.shibboleth.trust.activation.support;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.jans.shibboleth.trust.activation.error.WorkItemNotFound;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.activation.repository.WorkItemRepository;
import io.jans.shibboleth.trust.shared.Result;

/**
 * In-memory {@link WorkItemRepository} for domain tests. {@code findClaimableCandidates} filters to
 * non-terminal items of the type and orders them oldest first, mirroring the store query the adapter runs.
 */
public final class FakeWorkItemRepository implements WorkItemRepository {

    private final Map<WorkItemId, WorkItem> items = new LinkedHashMap<>();
    private final Map<TrustRelationshipRef, WorkItemId> currentByTr = new LinkedHashMap<>();

    @Override
    public Result<WorkItem> save(WorkItem workItem) {

        items.put(workItem.id(), workItem);

        return Result.success(workItem);
    }

    @Override
    public Result<WorkItem> findById(WorkItemId id) {

        WorkItem item = items.get(id);

        if (item == null) {

            return Result.failure(WorkItemNotFound.instance());
        }

        return Result.success(item);
    }

    @Override
    public Result<Void> delete(WorkItemId id) {

        items.remove(id);

        return Result.success(null);
    }

    @Override
    public Result<List<WorkItem>> findClaimableCandidates(WorkItemType type) {

        List<WorkItem> candidates = new ArrayList<>();

        for (WorkItem item : items.values()) {

            if (item.type() == type && !item.state().isTerminal()) {

                candidates.add(item);
            }
        }

        candidates.sort(Comparator.comparing(WorkItem::createdAt));

        return Result.success(candidates);
    }

    @Override
    public Result<Void> assignCurrentEpisode(TrustRelationshipRef trustRelationshipId, WorkItemId workItemId) {

        currentByTr.put(trustRelationshipId, workItemId);

        return Result.success(null);
    }

    @Override
    public Result<WorkItemId> currentEpisode(TrustRelationshipRef trustRelationshipId) {

        WorkItemId current = currentByTr.get(trustRelationshipId);

        if (current == null) {

            return Result.failure(WorkItemNotFound.instance());
        }

        return Result.success(current);
    }

    @Override
    public Result<Void> clearCurrentEpisode(TrustRelationshipRef trustRelationshipId) {

        currentByTr.remove(trustRelationshipId);

        return Result.success(null);
    }
}
