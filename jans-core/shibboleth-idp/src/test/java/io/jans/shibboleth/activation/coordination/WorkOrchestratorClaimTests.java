package io.jans.shibboleth.activation.coordination;

import io.jans.shibboleth.activation.error.WorkItemNotFound;
import io.jans.shibboleth.activation.error.WorkerNotAlive;
import io.jans.shibboleth.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.activation.model.WorkItem;
import io.jans.shibboleth.activation.model.WorkItemId;
import io.jans.shibboleth.activation.model.WorkItemState;
import io.jans.shibboleth.activation.util.ActivationResult;
import io.jans.shibboleth.activation.workers.Worker;
import io.jans.shibboleth.activation.workers.WorkerId;
import io.jans.shibboleth.shared.Origin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.activation.model.WorkItemType.PROCESS_AGGREGATE_METADATA;

@DisplayName("Group 8 — Orchestrator: Claim & Assignment")
public class WorkOrchestratorClaimTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final TimeSource CLOCK = () -> NOW;
    private static final Duration LEASE_TTL = Duration.ofSeconds(30);
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);

    private final List<ActivationEvent> emitted = new ArrayList<>();
    private final WorkOrchestrator orchestrator = WorkOrchestrator.create(CLOCK, LEASE_TTL, HEARTBEAT_TTL, emitted::add).getValue();

    private static TrustRelationshipRef aTrustRelationship() {

        return TrustRelationshipRef.of(UUID.randomUUID()).getValue();
    }

    private static Worker aliveWorker(String origin) {

        return Worker.register(WorkerId.of(Origin.of(origin)).getValue(), NOW).getValue();
    }

    private static Worker expiredWorker(String origin) {

        return Worker.register(WorkerId.of(Origin.of(origin)).getValue(), NOW.minusSeconds(31)).getValue();
    }

    private WorkItem pendingItem() {

        return orchestrator.onActivationRequested(aTrustRelationship(), PROCESS_AGGREGATE_METADATA).getValue();
    }

    @Test
    @DisplayName("GIVEN a PENDING WorkItem and an alive Worker WHEN the Worker claims it THEN the item becomes ASSIGNED holding a lease for that Worker")
    public void shouldAssignItemToAliveWorker_whenClaimed() {

        WorkItem pending = pendingItem();
        Worker worker = aliveWorker("w@host");

        WorkItem assigned = orchestrator.claim(pending.id(), worker).getValue();

        assertThat(assigned.state()).isEqualTo(WorkItemState.ASSIGNED);
        assertThat(assigned.lease().isHeldBy(worker.id())).isTrue();
    }

    @Test
    @DisplayName("GIVEN a PENDING WorkItem and an expired Worker WHEN the Worker attempts to claim it THEN the claim is rejected and the item stays PENDING")
    public void shouldRejectClaim_whenWorkerNotAlive() {

        WorkItem pending = pendingItem();
        Worker expired = expiredWorker("w@host");

        ActivationResult<WorkItem> result = orchestrator.claim(pending.id(), expired);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(WorkerNotAlive.class);
    }

    @Test
    @DisplayName("GIVEN a successful claim WHEN it completes THEN a WorkItemAssigned event is emitted")
    public void shouldEmitWorkItemAssigned_whenClaimSucceeds() {

        WorkItem pending = pendingItem();
        Worker worker = aliveWorker("w@host");

        WorkItem assigned = orchestrator.claim(pending.id(), worker).getValue();

        assertThat(emitted).hasSize(1);
        WorkItemAssigned event = (WorkItemAssigned) emitted.get(0);
        assertThat(event.workItemId()).isEqualTo(assigned.id());
        assertThat(event.workerId()).isEqualTo(worker.id());
    }

    @Test
    @DisplayName("GIVEN an unknown WorkItem id WHEN a claim is attempted THEN it fails")
    public void shouldFailClaim_whenWorkItemNotFound() {

        ActivationResult<WorkItem> result = orchestrator.claim(WorkItemId.generate(), aliveWorker("w@host"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(WorkItemNotFound.class);
    }

    @Test
    @DisplayName("GIVEN two alive Workers attempting to claim the same PENDING WorkItem WHEN both perform the claim THEN exactly one succeeds and the other is rejected")
    public void shouldLetOnlyOneWorkerWin_whenTwoClaimSameItem() {

        WorkItem pending = pendingItem();

        ActivationResult<WorkItem> first = orchestrator.claim(pending.id(), aliveWorker("w1@host"));
        ActivationResult<WorkItem> second = orchestrator.claim(pending.id(), aliveWorker("w2@host"));

        assertThat(first.isSuccess()).isTrue();
        assertThat(second.isFailure()).isTrue();
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN its lease is inspected THEN it has at most one active lease")
    public void shouldKeepAtMostOneActiveLease_whenAssigned() {

        WorkItem pending = pendingItem();
        Worker holder = aliveWorker("holder@host");
        Worker other = aliveWorker("other@host");

        WorkItem assigned = orchestrator.claim(pending.id(), holder).getValue();

        assertThat(assigned.lease().isPresent()).isTrue();
        assertThat(assigned.lease().isHeldBy(holder.id())).isTrue();
        assertThat(assigned.lease().isHeldBy(other.id())).isFalse();
    }

    @Test
    @DisplayName("GIVEN one alive Worker WHEN it claims several distinct PENDING WorkItems THEN it holds all of them concurrently")
    public void shouldAllowWorkerToHoldManyItems() {

        Worker worker = aliveWorker("w@host");
        WorkItem first = pendingItem();
        WorkItem second = pendingItem();

        WorkItem assignedFirst = orchestrator.claim(first.id(), worker).getValue();
        WorkItem assignedSecond = orchestrator.claim(second.id(), worker).getValue();

        assertThat(assignedFirst.lease().isHeldBy(worker.id())).isTrue();
        assertThat(assignedSecond.lease().isHeldBy(worker.id())).isTrue();
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN its lease is inspected THEN it names exactly one workerId")
    public void shouldNameSingleWorkerPerItem() {

        WorkItem pending = pendingItem();
        Worker holder = aliveWorker("holder@host");
        Worker other = aliveWorker("other@host");

        WorkItem assigned = orchestrator.claim(pending.id(), holder).getValue();

        assertThat(assigned.lease().isHeldBy(holder.id())).isTrue();
        assertThat(assigned.lease().isHeldBy(other.id())).isFalse();
    }
}
