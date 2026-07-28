package io.jans.shibboleth.trust.persistence.activation;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;

import io.jans.shibboleth.trust.activation.error.WorkItemNotFound;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.activation.repository.WorkItemRepository;
import io.jans.shibboleth.trust.shared.Result;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * {@code jans-orm}-backed {@link WorkItemRepository}. Work items map to/from {@link WorkItemEntry} via
 * {@link WorkItemEntryMapper}; the DN is {@code inum=<id>,<baseDn>}. Claimable candidates are the
 * non-terminal ({@code jansWorkItemStatus} absent) items of a type, returned oldest first.
 *
 * <p>The current-episode pointer is a separate {@link CurrentEpisodeEntry} under {@code currentEpisodeBaseDn},
 * keyed by the trust-relationship id — one per trust relationship, upserted on request and cleared on cancel.
 */
public final class WorkItemRepositoryImpl implements WorkItemRepository {

    private final PersistenceEntryManager entryManager;
    private final String baseDn;
    private final String currentEpisodeBaseDn;

    public WorkItemRepositoryImpl(PersistenceEntryManager entryManager, String baseDn,
                                  String currentEpisodeBaseDn) {

        this.entryManager = entryManager;
        this.baseDn = baseDn;
        this.currentEpisodeBaseDn = currentEpisodeBaseDn;
    }

    @Override
    public Result<WorkItem> save(WorkItem workItem) {

        WorkItemEntry entry = WorkItemEntryMapper.toEntry(workItem);
        entry.setDn(dnFor(baseDn, entry.getInum()));

        if (find(entry.getDn(), WorkItemEntry.class) != null) {

            entryManager.merge(entry);
        } else {

            entryManager.persist(entry);
        }

        return Result.success(workItem);
    }

    @Override
    public Result<WorkItem> findById(WorkItemId id) {

        WorkItemEntry entry = find(dnFor(baseDn, id.value().toString()), WorkItemEntry.class);

        if (entry == null) {

            return Result.failure(WorkItemNotFound.instance());
        }

        return WorkItemEntryMapper.toDomain(entry);
    }

    @Override
    public Result<Void> delete(WorkItemId id) {

        entryManager.remove(dnFor(baseDn, id.value().toString()), WorkItemEntry.class);

        return Result.success(null);
    }

    @Override
    public Result<List<WorkItem>> findClaimableCandidates(WorkItemType type) {

        Filter filter = Filter.createANDFilter(
            Filter.createEqualityFilter("jansWorkItemType", type.name()),
            Filter.createNOTFilter(Filter.createPresenceFilter("jansWorkItemStatus")));

        List<WorkItemEntry> entries = entryManager.findEntries(baseDn, WorkItemEntry.class, filter);

        List<WorkItem> candidates = new ArrayList<>();

        for (WorkItemEntry entry : entries) {

            Result<WorkItem> domain = WorkItemEntryMapper.toDomain(entry);

            if (domain.isFailure()) {

                return Result.failure(domain.getError());
            }

            candidates.add(domain.getValue());
        }

        candidates.sort(Comparator.comparing(WorkItem::createdAt));

        return Result.success(candidates);
    }

    @Override
    public Result<Void> assignCurrentEpisode(TrustRelationshipRef trustRelationshipId, WorkItemId workItemId) {

        CurrentEpisodeEntry entry = new CurrentEpisodeEntry();
        entry.setInum(trustRelationshipId.value().toString());
        entry.setWorkItemRef(workItemId.value().toString());
        entry.setDn(dnFor(currentEpisodeBaseDn, entry.getInum()));

        if (find(entry.getDn(), CurrentEpisodeEntry.class) != null) {

            entryManager.merge(entry);
        } else {

            entryManager.persist(entry);
        }

        return Result.success(null);
    }

    @Override
    public Result<WorkItemId> currentEpisode(TrustRelationshipRef trustRelationshipId) {

        CurrentEpisodeEntry entry =
            find(dnFor(currentEpisodeBaseDn, trustRelationshipId.value().toString()), CurrentEpisodeEntry.class);

        if (entry == null) {

            return Result.failure(WorkItemNotFound.instance());
        }

        return WorkItemId.of(UUID.fromString(entry.getWorkItemRef()));
    }

    @Override
    public Result<Void> clearCurrentEpisode(TrustRelationshipRef trustRelationshipId) {

        try {

            entryManager.remove(dnFor(currentEpisodeBaseDn, trustRelationshipId.value().toString()),
                CurrentEpisodeEntry.class);
        } catch (EntryPersistenceException alreadyGone) {

            // idempotent: clearing an absent pointer is a success
        }

        return Result.success(null);
    }

    private <T> T find(String dn, Class<T> entryClass) {

        try {

            return entryManager.find(dn, entryClass, null);
        } catch (EntryPersistenceException notFound) {

            return null;
        }
    }

    private static String dnFor(String base, String inum) {

        return "inum=" + inum + "," + base;
    }
}
