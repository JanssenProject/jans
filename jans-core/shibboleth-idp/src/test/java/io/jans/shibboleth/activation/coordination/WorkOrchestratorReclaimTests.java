package io.jans.shibboleth.activation.coordination;

import io.jans.shibboleth.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.activation.model.WorkItem;
import io.jans.shibboleth.activation.model.WorkItemState;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.activation.model.WorkItemType.PROCESS_AGGREGATE_METADATA;

@DisplayName("Group 9 — Orchestrator: Heartbeat & Lease-Expiry Reclaim")
public class WorkOrchestratorReclaimTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration LEASE_TTL = Duration.ofSeconds(30);
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);

    private final AtomicReference<Instant> clock = new AtomicReference<>(NOW);
    private final List<ActivationEvent> emitted = new ArrayList<>();
    private final WorkOrchestrator orchestrator =
        WorkOrchestrator.create(clock::get, LEASE_TTL, HEARTBEAT_TTL, emitted::add).getValue();

    private static TrustRelationshipRef aTrustRelationship() {

        return TrustRelationshipRef.of(UUID.randomUUID()).getValue();
    }

    private static Worker worker(String origin, Instant registeredAt) {

        return Worker.register(WorkerId.of(Origin.of(origin)).getValue(), registeredAt).getValue();
    }

    private WorkItem assignedItem(Worker holder) {

        WorkItem pending = orchestrator.onActivationRequested(aTrustRelationship(), PROCESS_AGGREGATE_METADATA).getValue();
        return orchestrator.claim(pending.id(), holder).getValue();
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem WHEN its lease holder heartbeats before expiry THEN the lease is renewed and the item stays ASSIGNED")
    public void shouldRenewLease_whenHolderHeartbeats() {

        Worker holder = worker("w@host", NOW);
        WorkItem assigned = assignedItem(holder);

        clock.set(NOW.plusSeconds(10));
        WorkItem beat = orchestrator.heartbeat(assigned.id(), holder).getValue();

        assertThat(beat.state()).isEqualTo(WorkItemState.ASSIGNED);
        assertThat(beat.lease().expiresAt()).isEqualTo(NOW.plusSeconds(10).plus(LEASE_TTL));
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem whose holder went silent past the lease TTL WHEN the orchestrator sweeps THEN the item is reclaimed to PENDING with lease Lease.NONE and the same WorkItemId")
    public void shouldReclaimToPending_whenLeaseExpires() {

        WorkItem assigned = assignedItem(worker("w@host", NOW));

        clock.set(NOW.plusSeconds(31));
        orchestrator.sweepExpiredLeases();

        WorkItem reclaimed = orchestrator.find(assigned.id()).getValue();
        assertThat(reclaimed.state()).isEqualTo(WorkItemState.PENDING);
        assertThat(reclaimed.lease().isNone()).isTrue();
        assertThat(reclaimed.id()).isEqualTo(assigned.id());
    }

    @Test
    @DisplayName("GIVEN a WorkItem reclaimed due to lease expiry WHEN the reclaim completes THEN a WorkItemLeaseExpired event is emitted")
    public void shouldEmitLeaseExpired_whenReclaimed() {

        WorkItem assigned = assignedItem(worker("w@host", NOW));

        clock.set(NOW.plusSeconds(31));
        orchestrator.sweepExpiredLeases();

        assertThat(emitted).hasAtLeastOneElementOfType(WorkItemLeaseExpired.class);
        WorkItemLeaseExpired event = emitted.stream()
            .filter(e -> e instanceof WorkItemLeaseExpired)
            .map(e -> (WorkItemLeaseExpired) e)
            .findFirst()
            .orElseThrow();
        assertThat(event.workItemId()).isEqualTo(assigned.id());
    }

    @Test
    @DisplayName("GIVEN a reclaimed PENDING WorkItem WHEN another alive Worker claims it THEN it becomes ASSIGNED again under the same WorkItemId and same episode")
    public void shouldReassignAfterReclaim_whenAnotherWorkerClaims() {

        WorkItem assigned = assignedItem(worker("w1@host", NOW));

        clock.set(NOW.plusSeconds(31));
        orchestrator.sweepExpiredLeases();

        Worker second = worker("w2@host", NOW.plusSeconds(31));
        WorkItem reassigned = orchestrator.claim(assigned.id(), second).getValue();

        assertThat(reassigned.state()).isEqualTo(WorkItemState.ASSIGNED);
        assertThat(reassigned.id()).isEqualTo(assigned.id());
    }

    @Test
    @DisplayName("GIVEN a Worker holding several WorkItems that then goes silent past the TTL WHEN the orchestrator sweeps THEN every one of its items is reclaimed to PENDING")
    public void shouldReclaimAllHeldItems_whenWorkerExpires() {

        Worker holder = worker("w@host", NOW);
        WorkItem firstPending = orchestrator.onActivationRequested(aTrustRelationship(), PROCESS_AGGREGATE_METADATA).getValue();
        WorkItem secondPending = orchestrator.onActivationRequested(aTrustRelationship(), PROCESS_AGGREGATE_METADATA).getValue();
        WorkItem first = orchestrator.claim(firstPending.id(), holder).getValue();
        WorkItem second = orchestrator.claim(secondPending.id(), holder).getValue();

        clock.set(NOW.plusSeconds(31));
        orchestrator.sweepExpiredLeases();

        assertThat(orchestrator.find(first.id()).getValue().state()).isEqualTo(WorkItemState.PENDING);
        assertThat(orchestrator.find(second.id()).getValue().state()).isEqualTo(WorkItemState.PENDING);
    }
}
