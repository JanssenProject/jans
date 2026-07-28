package io.jans.shibboleth.trust.persistence.activation;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;

import io.jans.shibboleth.trust.activation.error.WorkItemNotFound;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.activation.repository.WorkItemRepository;
import io.jans.shibboleth.trust.shared.Result;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * {@code jans-orm}-backed {@link WorkItemRepository}. Work items map to/from {@link WorkItemEntry} via
 * {@link WorkItemEntryMapper}; the DN is {@code inum=<id>,<baseDn>}. Claimable candidates are the
 * non-terminal ({@code jansWorkItemStatus} absent) items of a type, returned oldest first.
 */
public final class WorkItemRepositoryImpl implements WorkItemRepository {

    private final PersistenceEntryManager entryManager;
    private final String baseDn;

    public WorkItemRepositoryImpl(PersistenceEntryManager entryManager, String baseDn) {

        this.entryManager = entryManager;
        this.baseDn = baseDn;
    }

    @Override
    public Result<WorkItem> save(WorkItem workItem) {

        WorkItemEntry entry = WorkItemEntryMapper.toEntry(workItem);
        entry.setDn(dnFor(entry.getInum()));

        if (exists(entry.getDn())) {

            entryManager.merge(entry);
        } else {

            entryManager.persist(entry);
        }

        return Result.success(workItem);
    }

    @Override
    public Result<WorkItem> findById(WorkItemId id) {

        WorkItemEntry entry = find(dnFor(id.value().toString()));

        if (entry == null) {

            return Result.failure(WorkItemNotFound.instance());
        }

        return WorkItemEntryMapper.toDomain(entry);
    }

    @Override
    public Result<Void> delete(WorkItemId id) {

        entryManager.remove(dnFor(id.value().toString()), WorkItemEntry.class);

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

    private WorkItemEntry find(String dn) {

        try {

            return entryManager.find(dn, WorkItemEntry.class, null);
        } catch (EntryPersistenceException notFound) {

            return null;
        }
    }

    private boolean exists(String dn) {

        return find(dn) != null;
    }

    private String dnFor(String inum) {

        return "inum=" + inum + "," + baseDn;
    }
}
