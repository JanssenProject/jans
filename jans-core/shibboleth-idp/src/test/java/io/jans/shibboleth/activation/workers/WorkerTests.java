package io.jans.shibboleth.activation.workers;

import io.jans.shibboleth.activation.error.RequiredValueMissing;
import io.jans.shibboleth.activation.util.ActivationResult;
import io.jans.shibboleth.shared.Origin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Group 6 — Worker Entity & Liveness")
public class WorkerTests {

    private static final WorkerId WORKER_ID = WorkerId.of(Origin.of("worker@host")).getValue();
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration TTL = Duration.ofSeconds(30);

    @Test
    @DisplayName("GIVEN a WorkerId and a time source WHEN a Worker registers THEN it carries that id and its registeredAt and lastHeartbeatAt are stamped from that time")
    public void shouldRegisterWithIdentityAndTimestamps() {

        Worker worker = Worker.register(WORKER_ID, NOW).getValue();

        assertThat(worker.id()).isEqualTo(WORKER_ID);
        assertThat(worker.registeredAt()).isEqualTo(NOW);
        assertThat(worker.lastHeartbeatAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("GIVEN a registered Worker WHEN a heartbeat is recorded at a later instant THEN its lastHeartbeatAt advances to that instant")
    public void shouldAdvanceLastHeartbeat_whenHeartbeatRecorded() {

        Worker worker = Worker.register(WORKER_ID, NOW).getValue();
        Instant later = NOW.plusSeconds(10);

        Worker beat = worker.heartbeat(later).getValue();

        assertThat(beat.lastHeartbeatAt()).isEqualTo(later);
        assertThat(beat.registeredAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("GIVEN a Worker whose last heartbeat is within the heartbeat TTL WHEN liveness is evaluated THEN it is alive")
    public void shouldBeAlive_whenWithinHeartbeatTtl() {

        Worker worker = Worker.register(WORKER_ID, NOW).getValue();

        assertThat(worker.isAlive(NOW.plusSeconds(10), TTL)).isTrue();
    }

    @Test
    @DisplayName("GIVEN a Worker whose last heartbeat is older than the heartbeat TTL WHEN liveness is evaluated THEN it is expired")
    public void shouldBeExpired_whenBeyondHeartbeatTtl() {

        Worker worker = Worker.register(WORKER_ID, NOW).getValue();

        assertThat(worker.isExpired(NOW.plusSeconds(31), TTL)).isTrue();
        assertThat(worker.isAlive(NOW.plusSeconds(31), TTL)).isFalse();
    }

    @Test
    @DisplayName("GIVEN a Worker whose last heartbeat is exactly the heartbeat TTL ago WHEN liveness is evaluated THEN it is alive because the boundary is inclusive")
    public void shouldBeAlive_whenExactlyAtTtlBoundary() {

        Worker worker = Worker.register(WORKER_ID, NOW).getValue();

        assertThat(worker.isAlive(NOW.plus(TTL), TTL)).isTrue();
    }

    @Test
    @DisplayName("GIVEN two different configured heartbeat TTLs WHEN the same heartbeat age is evaluated against each THEN liveness follows the configured value rather than a hard-coded one")
    public void shouldUseConfiguredTtl_notHardcoded() {

        Worker worker = Worker.register(WORKER_ID, NOW).getValue();
        Instant checkAt = NOW.plusSeconds(20);

        assertThat(worker.isAlive(checkAt, Duration.ofSeconds(30))).isTrue();
        assertThat(worker.isAlive(checkAt, Duration.ofSeconds(10))).isFalse();
    }

    @Test
    @DisplayName("GIVEN a null WorkerId WHEN a Worker registers THEN it fails and no Worker is produced")
    public void shouldFailRegister_whenIdIsNull() {

        ActivationResult<Worker> result = Worker.register(null, NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a null instant WHEN a Worker registers THEN it fails and no Worker is produced")
    public void shouldFailRegister_whenNowIsNull() {

        ActivationResult<Worker> result = Worker.register(WORKER_ID, null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a registered Worker WHEN a heartbeat is recorded with a null instant THEN it fails and no heartbeat is recorded")
    public void shouldFailHeartbeat_whenNowIsNull() {

        Worker worker = Worker.register(WORKER_ID, NOW).getValue();

        ActivationResult<Worker> result = worker.heartbeat(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }
}
