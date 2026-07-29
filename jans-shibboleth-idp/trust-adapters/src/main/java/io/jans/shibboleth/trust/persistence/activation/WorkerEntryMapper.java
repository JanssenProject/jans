package io.jans.shibboleth.trust.persistence.activation;

import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.Result;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Translates between the {@link Worker} aggregate and its {@link WorkerEntry}. The worker's domain id is its
 * caller-supplied origin string; its storage id ({@code inum}) is a <b>deterministic</b> name-based UUID of
 * that origin, so the id is uniformly shaped like every other entry yet still resolves to one DN per worker.
 * The raw origin is kept in {@code jansWorkerOrigin} and is what a read rebuilds the worker id from.
 */
public final class WorkerEntryMapper {

    private WorkerEntryMapper() {
    }

    /**
     * The deterministic storage id for the worker at {@code origin}. Same origin → same id on every node (a
     * name-based UUID), so a direct DN lookup by worker id needs no secondary search.
     */
    public static String inumFor(String origin) {

        String name = "worker|" + origin;

        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static WorkerEntry toEntry(Worker worker) {

        String origin = worker.id().origin().getValue();

        WorkerEntry entry = new WorkerEntry();
        entry.setInum(inumFor(origin));
        entry.setOrigin(origin);
        entry.setRegisteredAt(Date.from(worker.registeredAt()));
        entry.setLastHeartbeatAt(Date.from(worker.lastHeartbeatAt()));

        return entry;
    }

    public static Result<Worker> toDomain(WorkerEntry entry) {

        Result<WorkerId> id = WorkerId.of(Origin.of(entry.getOrigin()));

        if (id.isFailure()) {

            return Result.failure(id.getError());
        }

        return Worker.rehydrate(
            id.getValue(),
            entry.getRegisteredAt().toInstant(),
            entry.getLastHeartbeatAt().toInstant());
    }
}
