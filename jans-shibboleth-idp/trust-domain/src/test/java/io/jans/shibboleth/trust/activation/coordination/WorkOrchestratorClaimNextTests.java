package io.jans.shibboleth.trust.activation.coordination;

import io.jans.shibboleth.trust.activation.error.WorkerNotAlive;
import io.jans.shibboleth.trust.activation.model.ClaimOutcome;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
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
import static io.jans.shibboleth.trust.activation.model.WorkItemType.PROCESS_INDIVIDUAL_METADATA;

@DisplayName("Orchestrator: Claim-Next (atomic find-and-claim)")
public class WorkOrchestratorClaimNextTests {

    private static final Duration LEASE_TTL = Duration.ofSeconds(30);
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);
    private static final FinalizeActivationPort NO_FINALIZE = (ref, diagnostics) -> { };

    private Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final TimeSource clock = () -> now;
    private final List<ActivationEvent> emitted = new ArrayList<>();
    private final WorkOrchestrator orchestrator =
        WorkOrchestrator.create(clock, LEASE_TTL, HEARTBEAT_TTL, emitted::add, NO_FINALIZE, new FakeWorkItemRepository(), new FakeWorkerRepository()).getValue();

    private Worker aliveWorker(String origin) {

        WorkerId id = WorkerId.of(Origin.of(origin)).getValue();
        orchestrator.registerWorker(id);
        return orchestrator.findWorker(id).getValue();
    }

    private WorkItem pending(io.jans.shibboleth.trust.activation.model.WorkItemType type) {

        return orchestrator.onActivationRequested(
            TrustRelationshipRef.of(UUID.randomUUID()).getValue(), type).getValue();
    }

    @Test
    @DisplayName("GIVEN two PENDING items of a type WHEN claim-next THEN the oldest is claimed (FIFO)")
    public void shouldClaimOldestPendingOfType() {

        WorkItem first = pending(PROCESS_AGGREGATE_METADATA);
        now = now.plusSeconds(1);
        pending(PROCESS_AGGREGATE_METADATA);
        Worker worker = aliveWorker("w@host");

        Result<ClaimOutcome> result = orchestrator.claimNext(PROCESS_AGGREGATE_METADATA, worker);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().isClaimed()).isTrue();
        assertThat(result.getValue().workItem().id()).isEqualTo(first.id());
        assertThat(result.getValue().workItem().state()).isEqualTo(WorkItemState.ASSIGNED);
    }

    @Test
    @DisplayName("GIVEN a claim-next WHEN an item is claimed THEN a WorkItemAssigned event is emitted")
    public void shouldEmitAssignedEvent() {

        pending(PROCESS_AGGREGATE_METADATA);
        Worker worker = aliveWorker("w@host");

        orchestrator.claimNext(PROCESS_AGGREGATE_METADATA, worker);

        assertThat(emitted).anyMatch(e -> e instanceof WorkItemAssigned);
    }

    @Test
    @DisplayName("GIVEN no PENDING item of the requested type WHEN claim-next THEN nothing is claimed (empty)")
    public void shouldReturnEmpty_whenNothingPending() {

        Worker worker = aliveWorker("w@host");

        Result<ClaimOutcome> result = orchestrator.claimNext(PROCESS_AGGREGATE_METADATA, worker);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("GIVEN only a different-type PENDING item WHEN claim-next THEN nothing is claimed (empty)")
    public void shouldIgnoreOtherTypes() {

        pending(PROCESS_INDIVIDUAL_METADATA);
        Worker worker = aliveWorker("w@host");

        Result<ClaimOutcome> result = orchestrator.claimNext(PROCESS_AGGREGATE_METADATA, worker);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("GIVEN the only item of the type is already ASSIGNED WHEN claim-next THEN nothing is claimed")
    public void shouldIgnoreNonPending() {

        WorkItem item = pending(PROCESS_AGGREGATE_METADATA);
        Worker worker = aliveWorker("w@host");
        orchestrator.claim(item.id(), worker);

        Result<ClaimOutcome> result = orchestrator.claimNext(PROCESS_AGGREGATE_METADATA, worker);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("GIVEN a worker whose liveness has lapsed WHEN claim-next THEN it fails with WorkerNotAlive")
    public void shouldFail_whenWorkerNotAlive() {

        pending(PROCESS_AGGREGATE_METADATA);
        Worker stale = Worker.register(WorkerId.of(Origin.of("w@host")).getValue(), now).getValue();
        now = now.plusSeconds(31);

        Result<ClaimOutcome> result = orchestrator.claimNext(PROCESS_AGGREGATE_METADATA, stale);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(WorkerNotAlive.class);
    }

    @Test
    @DisplayName("GIVEN a null type WHEN claim-next THEN it fails with RequiredValueMissing")
    public void shouldFail_whenTypeNull() {

        Worker worker = aliveWorker("w@host");

        Result<ClaimOutcome> result = orchestrator.claimNext(null, worker);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a null worker WHEN claim-next THEN it fails with RequiredValueMissing")
    public void shouldFail_whenWorkerNull() {

        pending(PROCESS_AGGREGATE_METADATA);

        Result<ClaimOutcome> result = orchestrator.claimNext(PROCESS_AGGREGATE_METADATA, null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }
}
