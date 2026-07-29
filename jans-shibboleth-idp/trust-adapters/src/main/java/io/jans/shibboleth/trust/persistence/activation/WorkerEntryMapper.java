package io.jans.shibboleth.trust.persistence.activation;

import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.Result;

import java.util.Date;

/**
 * Translates between the {@link Worker} aggregate and its {@link WorkerEntry}. The worker's id is its
 * caller-supplied origin string, which is also the {@code inum}.
 */
public final class WorkerEntryMapper {

    private WorkerEntryMapper() {
    }

    public static WorkerEntry toEntry(Worker worker) {

        WorkerEntry entry = new WorkerEntry();
        entry.setInum(worker.id().origin().getValue());
        entry.setRegisteredAt(Date.from(worker.registeredAt()));
        entry.setLastHeartbeatAt(Date.from(worker.lastHeartbeatAt()));

        return entry;
    }

    public static Result<Worker> toDomain(WorkerEntry entry) {

        Result<WorkerId> id = WorkerId.of(Origin.of(entry.getInum()));

        if (id.isFailure()) {

            return Result.failure(id.getError());
        }

        return Worker.rehydrate(
            id.getValue(),
            entry.getRegisteredAt().toInstant(),
            entry.getLastHeartbeatAt().toInstant());
    }
}
