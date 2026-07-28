package io.jans.shibboleth.trust.activation.repository;

import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Result;

/**
 * Outbound port for storing and retrieving workers. A domain contract — the adapter implements it over the
 * store; the orchestrator depends only on this interface.
 */
public interface WorkerRepository {

    /** Insert a new worker or update an existing one (e.g. its last-heartbeat), keyed by its id. */
    Result<Worker> save(Worker worker);

    /** Find a worker by id, or fail with {@code WorkerNotFound} when absent. */
    Result<Worker> findById(WorkerId id);

    Result<Void> delete(WorkerId id);
}
