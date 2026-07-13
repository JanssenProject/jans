package io.jans.shibboleth.activation.workers;

import java.time.Duration;
import java.time.Instant;

import io.jans.shibboleth.activation.error.RequiredValueMissing;
import io.jans.shibboleth.activation.util.ActivationResult;

public final class Worker {

    private final WorkerId id;
    private final Instant registeredAt;
    private final Instant lastHeartbeatAt;

    private Worker(WorkerId id, Instant registeredAt, Instant lastHeartbeatAt) {

        this.id = id;
        this.registeredAt = registeredAt;
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public static ActivationResult<Worker> register(WorkerId id, Instant now) {

        if (id == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("id"));
        }

        if (now == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("now"));
        }

        return ActivationResult.success(new Worker(id, now, now));
    }

    public ActivationResult<Worker> heartbeat(Instant now) {

        if (now == null) {

            return ActivationResult.failure(RequiredValueMissing.forField("now"));
        }

        return ActivationResult.success(new Worker(id, registeredAt, now));
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
