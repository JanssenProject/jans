package io.jans.shibboleth.trust.persistence.activation;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.activation.lease.Lease;
import io.jans.shibboleth.trust.activation.lease.LeaseGeneration;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Origin;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Round-trips for the activation aggregate ⇄ entry mappers (no DB), plus the deterministic lease-inum
 * property (AP9): the identity is a name-based UUID of (workItemId, generation), so it is reproducible and
 * generation-distinct — that is what makes the store's primary key double as the claim lock.
 */
@DisplayName("Activation entry mappers — round-trips and deterministic lease inum")
public class ActivationEntryMapperTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final WorkItemId WORK_ITEM = WorkItemId.of(UUID.randomUUID()).getValue();
    private static final WorkerId WORKER = WorkerId.of(Origin.of("instance@host")).getValue();

    private static WorkItem pending() {

        return WorkItem.create(WorkItemType.PROCESS_AGGREGATE_METADATA,
            TrustRelationshipRef.of(UUID.randomUUID()).getValue(), NOW).getValue();
    }

    @Test
    @DisplayName("GIVEN a non-terminal work item WHEN round-tripped THEN every field survives and status is stored null")
    public void workItemPendingRoundTrip() {

        WorkItem pending = pending();

        WorkItemEntry entry = WorkItemEntryMapper.toEntry(pending);
        assertThat(entry.getStatus()).isNull();

        WorkItem back = WorkItemEntryMapper.toDomain(entry).getValue();

        assertThat(back.id()).isEqualTo(pending.id());
        assertThat(back.type()).isEqualTo(pending.type());
        assertThat(back.trustRelationshipId()).isEqualTo(pending.trustRelationshipId());
        assertThat(back.state()).isEqualTo(WorkItemState.PENDING);
        assertThat(back.createdAt()).isEqualTo(pending.createdAt());
        assertThat(back.lastTransitionAt()).isEqualTo(pending.lastTransitionAt());
    }

    @Test
    @DisplayName("GIVEN a terminal work item WHEN round-tripped THEN the terminal flag is stored and restored")
    public void workItemTerminalRoundTrip() {

        WorkItem completed = pending().complete(NOW.plusSeconds(5)).getValue();

        WorkItemEntry entry = WorkItemEntryMapper.toEntry(completed);
        assertThat(entry.getStatus()).isEqualTo("COMPLETED");

        WorkItem back = WorkItemEntryMapper.toDomain(entry).getValue();
        assertThat(back.state()).isEqualTo(WorkItemState.COMPLETED);
        assertThat(back.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("GIVEN a lease WHEN round-tripped THEN it is value-equal to the original")
    public void leaseRoundTrip() {

        Lease lease = Lease.granted(WORK_ITEM, LeaseGeneration.first(), WORKER, NOW, NOW.plusSeconds(30)).getValue();

        Lease back = LeaseEntryMapper.toDomain(LeaseEntryMapper.toEntry(lease)).getValue();

        assertThat(back).isEqualTo(lease);
    }

    @Test
    @DisplayName("GIVEN a worker WHEN round-tripped THEN its id and timestamps survive")
    public void workerRoundTrip() {

        Worker worker = Worker.register(WORKER, NOW).getValue();

        Worker back = WorkerEntryMapper.toDomain(WorkerEntryMapper.toEntry(worker)).getValue();

        assertThat(back.id()).isEqualTo(worker.id());
        assertThat(back.registeredAt()).isEqualTo(worker.registeredAt());
        assertThat(back.lastHeartbeatAt()).isEqualTo(worker.lastHeartbeatAt());
    }

    @Test
    @DisplayName("GIVEN a worker WHEN mapped to an entry THEN its inum is the deterministic id for its origin and the origin is stored")
    public void workerEntryInumIsDeterministicFromOrigin() {

        Worker worker = Worker.register(WORKER, NOW).getValue();
        String origin = WORKER.origin().getValue();

        WorkerEntry entry = WorkerEntryMapper.toEntry(worker);

        assertThat(entry.getInum()).isEqualTo(WorkerEntryMapper.inumFor(origin));
        assertThat(entry.getOrigin()).isEqualTo(origin);
    }

    @Test
    @DisplayName("GIVEN the same (workItem, generation) WHEN the lease inum is derived THEN it is reproducible and distinct across generations and work items")
    public void leaseInumIsDeterministicAndDistinct() {

        UUID other = UUID.randomUUID();

        assertThat(LeaseEntryMapper.inumFor(WORK_ITEM.value(), 1))
            .isEqualTo(LeaseEntryMapper.inumFor(WORK_ITEM.value(), 1));
        assertThat(LeaseEntryMapper.inumFor(WORK_ITEM.value(), 1))
            .isNotEqualTo(LeaseEntryMapper.inumFor(WORK_ITEM.value(), 2));
        assertThat(LeaseEntryMapper.inumFor(WORK_ITEM.value(), 1))
            .isNotEqualTo(LeaseEntryMapper.inumFor(other, 1));
    }

    @Test
    @DisplayName("GIVEN a lease WHEN mapped to an entry THEN its inum is the deterministic id for its identity")
    public void leaseEntryInumMatchesDerivation() {

        Lease lease = Lease.granted(WORK_ITEM, LeaseGeneration.of(3), WORKER, NOW, NOW.plusSeconds(30)).getValue();

        assertThat(LeaseEntryMapper.toEntry(lease).getInum())
            .isEqualTo(LeaseEntryMapper.inumFor(WORK_ITEM.value(), 3));
    }
}
