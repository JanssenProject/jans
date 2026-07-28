package io.jans.shibboleth.trust.activation.workers;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The from-store rehydration path for a worker: reconstruct it verbatim from its persisted registration and
 * last-heartbeat instants, so liveness (`isAlive`) still evaluates against the stored heartbeat.
 */
@DisplayName("Worker — rehydration from store")
public class WorkerRehydrationTests {

    private static final WorkerId ID = WorkerId.of(Origin.of("instance@host")).getValue();
    private static final Instant REGISTERED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LAST_HEARTBEAT_AT = REGISTERED_AT.plusSeconds(45);

    @Test
    @DisplayName("GIVEN persisted fields WHEN rehydrated THEN the id, registration and last-heartbeat instants are carried verbatim")
    public void carriesAllFields() {

        Worker worker = Worker.rehydrate(ID, REGISTERED_AT, LAST_HEARTBEAT_AT).getValue();

        assertThat(worker.id()).isEqualTo(ID);
        assertThat(worker.registeredAt()).isEqualTo(REGISTERED_AT);
        assertThat(worker.lastHeartbeatAt()).isEqualTo(LAST_HEARTBEAT_AT);
    }

    @Test
    @DisplayName("GIVEN a rehydrated worker WHEN liveness is checked THEN it uses the stored last-heartbeat instant")
    public void livenessUsesStoredHeartbeat() {

        Worker worker = Worker.rehydrate(ID, REGISTERED_AT, LAST_HEARTBEAT_AT).getValue();
        Duration ttl = Duration.ofSeconds(30);

        assertThat(worker.isAlive(LAST_HEARTBEAT_AT.plusSeconds(10), ttl)).isTrue();
        assertThat(worker.isAlive(LAST_HEARTBEAT_AT.plusSeconds(31), ttl)).isFalse();
    }

    @Test
    @DisplayName("GIVEN a null field WHEN rehydrated THEN it fails and no worker is produced")
    public void failsWhenAnyFieldIsNull() {

        assertThat(Worker.rehydrate(null, REGISTERED_AT, LAST_HEARTBEAT_AT).isFailure()).isTrue();
        assertThat(Worker.rehydrate(ID, null, LAST_HEARTBEAT_AT).isFailure()).isTrue();

        Result<Worker> result = Worker.rehydrate(ID, REGISTERED_AT, null);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }
}
