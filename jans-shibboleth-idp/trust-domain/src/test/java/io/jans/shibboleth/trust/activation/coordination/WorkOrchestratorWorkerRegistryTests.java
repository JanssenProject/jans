package io.jans.shibboleth.trust.activation.coordination;

import io.jans.shibboleth.trust.activation.error.WorkerNotFound;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import io.jans.shibboleth.trust.activation.support.FakeWorkItemRepository;
import io.jans.shibboleth.trust.activation.support.FakeWorkerRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.trust.activation.model.WorkItemType.PROCESS_AGGREGATE_METADATA;

@DisplayName("Orchestrator: Worker Registry & Liveness")
public class WorkOrchestratorWorkerRegistryTests {

    private static final Duration LEASE_TTL = Duration.ofSeconds(30);
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);
    private static final FinalizeActivationPort NO_FINALIZE = (ref, diagnostics) -> { };

    private Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final TimeSource clock = () -> now;
    private final List<ActivationEvent> emitted = new ArrayList<>();
    private final WorkOrchestrator orchestrator =
        WorkOrchestrator.create(clock, LEASE_TTL, HEARTBEAT_TTL, emitted::add, NO_FINALIZE, new FakeWorkItemRepository(), new FakeWorkerRepository()).getValue();

    private static WorkerId workerId(String origin) {

        return WorkerId.of(Origin.of(origin)).getValue();
    }

    @Test
    @DisplayName("GIVEN a registered worker WHEN it is looked up THEN it is found and alive")
    public void shouldRegisterAndFindWorker() {

        WorkerId id = workerId("w@host");

        assertThat(orchestrator.registerWorker(id).isSuccess()).isTrue();

        Result<Worker> found = orchestrator.findWorker(id);
        assertThat(found.isSuccess()).isTrue();
        assertThat(found.getValue().id()).isEqualTo(id);
        assertThat(found.getValue().isAlive(now, HEARTBEAT_TTL)).isTrue();
    }

    @Test
    @DisplayName("GIVEN no such worker WHEN looked up THEN it fails with WorkerNotFound")
    public void shouldFailFind_whenWorkerUnknown() {

        Result<Worker> found = orchestrator.findWorker(workerId("ghost@host"));

        assertThat(found.isFailure()).isTrue();
        assertThat(found.getError()).isInstanceOf(WorkerNotFound.class);
    }

    @Test
    @DisplayName("GIVEN no such worker WHEN it heartbeats THEN it fails with WorkerNotFound")
    public void shouldFailHeartbeat_whenWorkerUnknown() {

        Result<Worker> result = orchestrator.heartbeatWorker(workerId("ghost@host"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(WorkerNotFound.class);
    }

    @Test
    @DisplayName("GIVEN a worker whose liveness has lapsed WHEN it heartbeats THEN it is alive again")
    public void shouldRenewLivenessViaHeartbeat() {

        WorkerId id = workerId("w@host");
        orchestrator.registerWorker(id);

        now = now.plusSeconds(31);
        assertThat(orchestrator.findWorker(id).getValue().isAlive(now, HEARTBEAT_TTL)).isFalse();

        assertThat(orchestrator.heartbeatWorker(id).isSuccess()).isTrue();
        assertThat(orchestrator.findWorker(id).getValue().isAlive(now, HEARTBEAT_TTL)).isTrue();
    }

    @Test
    @DisplayName("GIVEN a registered worker WHEN it claims a PENDING item THEN the claim succeeds")
    public void shouldFeedClaimWithRegisteredWorker() {

        WorkerId id = workerId("w@host");
        orchestrator.registerWorker(id);
        WorkItem pending = orchestrator.onActivationRequested(
            TrustRelationshipRef.of(UUID.randomUUID()).getValue(), PROCESS_AGGREGATE_METADATA).getValue();

        Worker registered = orchestrator.findWorker(id).getValue();
        Result<WorkItem> assigned = orchestrator.claim(pending.id(), registered);

        assertThat(assigned.isSuccess()).isTrue();
        assertThat(assigned.getValue().state()).isEqualTo(WorkItemState.ASSIGNED);
    }

    @Test
    @DisplayName("GIVEN a null worker id WHEN registering THEN it fails with RequiredValueMissing")
    public void shouldFailRegister_whenIdNull() {

        Result<Worker> result = orchestrator.registerWorker(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }
}
