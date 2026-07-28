package io.jans.shibboleth.trust.persistence.activation;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;

import io.jans.shibboleth.trust.activation.error.LeaseAlreadyHeld;
import io.jans.shibboleth.trust.activation.error.LeaseNotPresent;
import io.jans.shibboleth.trust.activation.lease.Lease;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.repository.LeaseRepository;
import io.jans.shibboleth.trust.shared.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code jans-orm}-backed {@link LeaseRepository}. A lease's DN is {@code inum=<name-uuid>,<baseDn>}, where
 * the inum is the deterministic id of {@code (workItemId, generation)} ({@link LeaseEntryMapper#inumFor}).
 *
 * <p>{@link #create} is the claim lock: it {@code persist}s the lease, and a primary-key collision — surfaced
 * by the store as an {@link EntryPersistenceException} — means another worker already holds that generation,
 * i.e. the claim was lost ({@code LeaseAlreadyHeld}). jans-orm's SQL backend does not distinguish a duplicate
 * key from other persist failures, so any persist failure is read as a lost race; the caller (claimNext) then
 * simply tries the next candidate.
 */
public final class LeaseRepositoryImpl implements LeaseRepository {

    private final PersistenceEntryManager entryManager;
    private final String baseDn;

    public LeaseRepositoryImpl(PersistenceEntryManager entryManager, String baseDn) {

        this.entryManager = entryManager;
        this.baseDn = baseDn;
    }

    @Override
    public Result<Lease> create(Lease lease) {

        LeaseEntry entry = LeaseEntryMapper.toEntry(lease);
        entry.setDn(dnFor(entry.getInum()));

        try {

            entryManager.persist(entry);
        } catch (EntryPersistenceException collision) {

            return Result.failure(LeaseAlreadyHeld.instance());
        }

        return Result.success(lease);
    }

    @Override
    public Result<List<Lease>> findByWorkItem(WorkItemId workItemId) {

        Filter filter = Filter.createEqualityFilter("jansWorkItemRef", workItemId.value().toString());

        List<LeaseEntry> entries = entryManager.findEntries(baseDn, LeaseEntry.class, filter);

        List<Lease> leases = new ArrayList<>();

        for (LeaseEntry entry : entries) {

            Result<Lease> domain = LeaseEntryMapper.toDomain(entry);

            if (domain.isFailure()) {

                return Result.failure(domain.getError());
            }

            leases.add(domain.getValue());
        }

        return Result.success(leases);
    }

    @Override
    public Result<Lease> renew(Lease lease) {

        LeaseEntry entry = LeaseEntryMapper.toEntry(lease);
        entry.setDn(dnFor(entry.getInum()));

        try {

            entryManager.merge(entry);
        } catch (EntryPersistenceException absent) {

            return Result.failure(LeaseNotPresent.instance());
        }

        return Result.success(lease);
    }

    @Override
    public Result<Void> delete(Lease lease) {

        String inum = LeaseEntryMapper.inumFor(lease.workItemId().value(), lease.generation().getValue());

        try {

            entryManager.remove(dnFor(inum), LeaseEntry.class);
        } catch (EntryPersistenceException alreadyGone) {

            // idempotent GC: a lease already removed (e.g. by another sweep) is a success
        }

        return Result.success(null);
    }

    private String dnFor(String inum) {

        return "inum=" + inum + "," + baseDn;
    }
}
