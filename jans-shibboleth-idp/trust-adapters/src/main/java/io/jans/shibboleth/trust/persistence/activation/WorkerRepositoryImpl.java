package io.jans.shibboleth.trust.persistence.activation;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;

import io.jans.shibboleth.trust.activation.error.WorkerNotFound;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.activation.repository.WorkerRepository;
import io.jans.shibboleth.trust.shared.Result;

/**
 * {@code jans-orm}-backed {@link WorkerRepository}. A worker's DN is {@code inum=<origin>,<baseDn>}, the
 * origin being its id. The origin is caller-supplied and must be DN-safe (AP7); sanitising it is the
 * responsibility of the registration boundary, not this adapter.
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

        WorkerEntry entry = find(dnFor(id.origin().getValue()));

        if (entry == null) {

            return Result.failure(WorkerNotFound.instance());
        }

        return WorkerEntryMapper.toDomain(entry);
    }

    @Override
    public Result<Void> delete(WorkerId id) {

        entryManager.remove(dnFor(id.origin().getValue()), WorkerEntry.class);

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
