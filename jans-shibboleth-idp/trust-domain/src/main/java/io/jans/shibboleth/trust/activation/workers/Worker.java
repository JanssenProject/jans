package io.jans.shibboleth.trust.activation.workers;

import java.time.Duration;
import java.time.Instant;

import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

public final class Worker {

    private final WorkerId id;
    private final Instant registeredAt;
    private final Instant lastHeartbeatAt;

    private Worker(WorkerId id, Instant registeredAt, Instant lastHeartbeatAt) {

        this.id = id;
        this.registeredAt = registeredAt;
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public static Result<Worker> register(WorkerId id, Instant now) {

        if (id == null) {

            return Result.failure(RequiredValueMissing.forField("id"));
        }

        if (now == null) {

            return Result.failure(RequiredValueMissing.forField("now"));
        }

        return Result.success(new Worker(id, now, now));
    }

    /**
     * Reconstruct a worker verbatim from its persisted registration and last-heartbeat instants, so liveness
     * still evaluates against the stored heartbeat.
     */
    public static Result<Worker> rehydrate(WorkerId id, Instant registeredAt, Instant lastHeartbeatAt) {

        if (id == null) {

            return Result.failure(RequiredValueMissing.forField("id"));
        }

        if (registeredAt == null) {

            return Result.failure(RequiredValueMissing.forField("registeredAt"));
        }

        if (lastHeartbeatAt == null) {

            return Result.failure(RequiredValueMissing.forField("lastHeartbeatAt"));
        }

        return Result.success(new Worker(id, registeredAt, lastHeartbeatAt));
    }

    public Result<Worker> heartbeat(Instant now) {

        if (now == null) {

            return Result.failure(RequiredValueMissing.forField("now"));
        }

        return Result.success(new Worker(id, registeredAt, now));
    }

    public boolean isAlive(Instant now, Duration heartbeatTtl) {

        return !now.isAfter(lastHeartbeatAt.plus(heartbeatTtl));
    }

    public boolean isExpired(Instant now, Duration heartbeatTtl) {

        return !isAlive(now, heartbeatTtl);
    }

    public WorkerId id() {

        return id;
    }

    public Instant registeredAt() {

        return registeredAt;
    }

    public Instant lastHeartbeatAt() {

        return lastHeartbeatAt;
    }
}
