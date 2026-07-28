package io.jans.shibboleth.trust.activation.support;

import java.util.LinkedHashMap;
import java.util.Map;

import io.jans.shibboleth.trust.activation.error.WorkerNotFound;
import io.jans.shibboleth.trust.activation.repository.WorkerRepository;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Result;

/**
 * In-memory {@link WorkerRepository} for domain tests.
 */
public final class FakeWorkerRepository implements WorkerRepository {

    private final Map<WorkerId, Worker> workers = new LinkedHashMap<>();

    @Override
    public Result<Worker> save(Worker worker) {

        workers.put(worker.id(), worker);

        return Result.success(worker);
    }

    @Override
    public Result<Worker> findById(WorkerId id) {

        Worker worker = workers.get(id);

        if (worker == null) {

            return Result.failure(WorkerNotFound.instance());
        }

        return Result.success(worker);
    }

    @Override
    public Result<Void> delete(WorkerId id) {

        workers.remove(id);

        return Result.success(null);
    }
}
