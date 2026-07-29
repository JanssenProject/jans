package io.jans.shibboleth.trust.persistence.activation;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;

import io.jans.shibboleth.trust.activation.error.WorkerNotFound;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.activation.repository.WorkerRepository;
import io.jans.shibboleth.trust.shared.Result;

/**
 * {@code jans-orm}-backed {@link WorkerRepository}. A worker's DN is {@code inum=<name-uuid>,<baseDn>}, the
 * inum being a deterministic name-based UUID of the worker's origin. Deriving the inum from the origin keeps a
 * direct DN lookup by worker id (no secondary search) while giving every entry a uniformly shaped id; the raw
 * origin travels in its own attribute.
 */
public final class WorkerRepositoryImpl implements WorkerRepository {

    private final PersistenceEntryManager entryManager;
    private final String baseDn;

    public WorkerRepositoryImpl(PersistenceEntryManager entryManager, String baseDn) {

        this.entryManager = entryManager;
        this.baseDn = baseDn;
    }

    @Override
    public Result<Worker> save(Worker worker) {

        WorkerEntry entry = WorkerEntryMapper.toEntry(worker);
        entry.setDn(dnFor(entry.getInum()));

        if (exists(entry.getDn())) {

            entryManager.merge(entry);
        } else {

            entryManager.persist(entry);
        }

        return Result.success(worker);
    }

    @Override
    public Result<Worker> findById(WorkerId id) {

        WorkerEntry entry = find(dnFor(WorkerEntryMapper.inumFor(id.origin().getValue())));

        if (entry == null) {

            return Result.failure(WorkerNotFound.instance());
        }

        return WorkerEntryMapper.toDomain(entry);
    }

    @Override
    public Result<Void> delete(WorkerId id) {

        entryManager.remove(dnFor(WorkerEntryMapper.inumFor(id.origin().getValue())), WorkerEntry.class);

        return Result.success(null);
    }

    private WorkerEntry find(String dn) {

        try {

            return entryManager.find(dn, WorkerEntry.class, null);
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
