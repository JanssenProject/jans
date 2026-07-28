package io.jans.shibboleth.trust.activation.coordination;

import io.jans.shibboleth.trust.activation.error.WorkItemNotFound;
import io.jans.shibboleth.trust.activation.error.WorkerNotAlive;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItemActivation;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Origin;

import io.jans.shibboleth.trust.activation.support.FakeLeaseRepository;
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

@DisplayName("Group 8 — Orchestrator: Claim & Assignment")
public class WorkOrchestratorClaimTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final TimeSource CLOCK = () -> NOW;
    private static final Duration LEASE_TTL = Duration.ofSeconds(30);
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);
    private static final FinalizeActivationPort NO_FINALIZE = (ref, diagnostics) -> { };

    private final List<ActivationEvent> emitted = new ArrayList<>();
    private final WorkOrchestrator orchestrator = WorkOrchestrator.create(CLOCK, LEASE_TTL, HEARTBEAT_TTL, emitted::add, NO_FINALIZE, new FakeWorkItemRepository(), new FakeLeaseRepository(), new FakeWorkerRepository()).getValue();

    private static TrustRelationshipRef aTrustRelationship() {

        return TrustRelationshipRef.of(UUID.randomUUID()).getValue();
    }

    private static Worker aliveWorker(String origin) {

        return Worker.register(WorkerId.of(Origin.of(origin)).getValue(), NOW).getValue();
    }

    private static Worker expiredWorker(String origin) {

        return Worker.register(WorkerId.of(Origin.of(origin)).getValue(), NOW.minusSeconds(31)).getValue();
    }

    private WorkItemActivation pendingItem() {

        return orchestrator.onActivationRequested(aTrustRelationship(), PROCESS_AGGREGATE_METADATA).getValue();
    }

    @Test
    @DisplayName("GIVEN a PENDING WorkItem and an alive Worker WHEN the Worker claims it THEN the item becomes ASSIGNED holding a lease for that Worker")
    public void shouldAssignItemToAliveWorker_whenClaimed() {

        WorkItemActivation pending = pendingItem();
        Worker worker = aliveWorker("w@host");

        WorkItemActivation assigned = orchestrator.claim(pending.id(), worker).getValue();

        assertThat(assigned.state()).isEqualTo(WorkItemState.ASSIGNED);
        assertThat(assigned.isHeldBy(worker.id())).isTrue();
    }

    @Test
    @DisplayName("GIVEN a PENDING WorkItem and an expired Worker WHEN the Worker attempts to claim it THEN the claim is rejected and the item stays PENDING")
    public void shouldRejectClaim_whenWorkerNotAlive() {

        WorkItemActivation pending = pendingItem();
        Worker expired = expiredWorker("w@host");

        Result<WorkItemActivation> result = orchestrator.claim(pending.id(), expired);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(WorkerNotAlive.class);
    }

    @Test
    @DisplayName("GIVEN a successful claim WHEN it completes THEN a WorkItemAssigned event is emitted")
    public void shouldEmitWorkItemAssigned_whenClaimSucceeds() {

        WorkItemActivation pending = pendingItem();
        Worker worker = aliveWorker("w@host");

        WorkItemActivation assigned = orchestrator.claim(pending.id(), worker).getValue();

        assertThat(emitted).hasSize(1);
        WorkItemAssigned event = (WorkItemAssigned) emitted.get(0);
        assertThat(event.workItemId()).isEqualTo(assigned.id());
        assertThat(event.workerId()).isEqualTo(worker.id());
    }

    @Test
    @DisplayName("GIVEN an unknown WorkItem id WHEN a claim is attempted THEN it fails")
    public void shouldFailClaim_whenWorkItemNotFound() {

        Result<WorkItemActivation> result = orchestrator.claim(WorkItemId.generate(), aliveWorker("w@host"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(WorkItemNotFound.class);
    }

    @Test
    @DisplayName("GIVEN two alive Workers attempting to claim the same PENDING WorkItem WHEN both perform the claim THEN exactly one succeeds and the other is rejected")
    public void shouldLetOnlyOneWorkerWin_whenTwoClaimSameItem() {

        WorkItemActivation pending = pendingItem();

        Result<WorkItemActivation> first = orchestrator.claim(pending.id(), aliveWorker("w1@host"));
        Result<WorkItemActivation> second = orchestrator.claim(pending.id(), aliveWorker("w2@host"));

        assertThat(first.isSuccess()).isTrue();
        assertThat(second.isFailure()).isTrue();
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN its lease is inspected THEN it is held by exactly one worker")
    public void shouldKeepAtMostOneActiveLease_whenAssigned() {

        WorkItemActivation pending = pendingItem();
        Worker holder = aliveWorker("holder@host");
        Worker other = aliveWorker("other@host");

        WorkItemActivation assigned = orchestrator.claim(pending.id(), holder).getValue();

        assertThat(assigned.isAssigned()).isTrue();
        assertThat(assigned.isHeldBy(holder.id())).isTrue();
        assertThat(assigned.isHeldBy(other.id())).isFalse();
    }

    @Test
    @DisplayName("GIVEN one alive Worker WHEN it claims several distinct PENDING WorkItems THEN it holds all of them concurrently")
    public void shouldAllowWorkerToHoldManyItems() {

        Worker worker = aliveWorker("w@host");
        WorkItemActivation first = pendingItem();
        WorkItemActivation second = pendingItem();

        WorkItemActivation assignedFirst = orchestrator.claim(first.id(), worker).getValue();
        WorkItemActivation assignedSecond = orchestrator.claim(second.id(), worker).getValue();

        assertThat(assignedFirst.isHeldBy(worker.id())).isTrue();
        assertThat(assignedSecond.isHeldBy(worker.id())).isTrue();
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN its lease is inspected THEN it names exactly one workerId")
    public void shouldNameSingleWorkerPerItem() {

        WorkItemActivation pending = pendingItem();
        Worker holder = aliveWorker("holder@host");
        Worker other = aliveWorker("other@host");

        WorkItemActivation assigned = orchestrator.claim(pending.id(), holder).getValue();

        assertThat(assigned.isHeldBy(holder.id())).isTrue();
        assertThat(assigned.isHeldBy(other.id())).isFalse();
    }
}
