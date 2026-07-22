package io.jans.shibboleth.trust.persistence.config;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;

import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.error.TrustRelationshipNotFound;
import io.jans.shibboleth.trust.shared.Result;

import java.util.UUID;

/**
 * {@code jans-orm}-backed {@link TrustRelationshipRepository}. Whole-object operations map to/from the
 * domain aggregate via {@link TrustRelationshipEntryMapper}; {@link #list} projects the reduced summary
 * entry straight to the view DTO via {@link TrustRelationshipSummaries} (TP10/TP11). Failures are reported
 * through {@link Result}; only genuinely exceptional store errors propagate.
 */
public final class TrustRelationshipRepositoryImpl implements TrustRelationshipRepository {

    private final PersistenceEntryManager entryManager;
    private final String baseDn;

    public TrustRelationshipRepositoryImpl(PersistenceEntryManager entryManager, String baseDn) {

        this.entryManager = entryManager;
        this.baseDn = baseDn;
    }

    @Override
    public Result<TrustRelationship> save(TrustRelationship trustRelationship) {

        Id id = trustRelationship.getId();
        boolean insert = id.isNotAssigned();
        UUID uuid = insert ? UUID.randomUUID() : id.getValue().getValue();

        TrustRelationshipEntry entry = TrustRelationshipEntryMapper.toEntry(trustRelationship);
        entry.setInum(uuid.toString());
        entry.setDn(dnFor(uuid));

        if (insert) {

            entryManager.persist(entry);
        } else {

            entryManager.merge(entry);
        }

        return TrustRelationshipEntryMapper.toDomain(entry);
    }

    @Override
    public Result<TrustRelationship> findById(Id id) {

        Result<UUID> uuid = id.getValue();
        if (uuid.isFailure()) {

            return Result.failure(uuid.getError());
        }

        TrustRelationshipEntry entry;
        try {

            entry = entryManager.find(dnFor(uuid.getValue()), TrustRelationshipEntry.class, null);
        } catch (EntryPersistenceException notFound) {

            entry = null;
        }

        if (entry == null) {

            return Result.failure(TrustRelationshipNotFound.forId(uuid.getValue()));
        }

        return TrustRelationshipEntryMapper.toDomain(entry);
    }

    @Override
    public Result<TrustRelationshipSummaryPage> list(TrustRelationshipQuery query) {

        PagedResult<TrustRelationshipSummaryEntry> result = entryManager.findPagedEntries(
            baseDn,
            TrustRelationshipSummaryEntry.class,
            TrustRelationshipSummaries.toFilter(query),
            TrustRelationshipSummaries.SUMMARY_ATTRIBUTES,
            TrustRelationshipSummaries.SORT_BY,
            SortOrder.ASCENDING,
            TrustRelationshipSummaries.offset(query),
            query.getSize(),
            query.getSize());

        return Result.success(
            TrustRelationshipSummaries.toPage(result.getEntries(), result.getTotalEntriesCount()));
    }

    @Override
    public Result<Void> delete(Id id) {

        Result<UUID> uuid = id.getValue();
        if (uuid.isFailure()) {

            return Result.failure(uuid.getError());
        }

        entryManager.remove(dnFor(uuid.getValue()), TrustRelationshipEntry.class);
        return Result.success(null);
    }

    private String dnFor(UUID id) {

        return "inum=" + id + "," + baseDn;
    }
}
