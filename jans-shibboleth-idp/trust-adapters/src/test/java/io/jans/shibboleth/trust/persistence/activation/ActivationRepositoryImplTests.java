package io.jans.shibboleth.trust.persistence.activation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;

import io.jans.shibboleth.trust.activation.error.LeaseAlreadyHeld;
import io.jans.shibboleth.trust.activation.error.LeaseNotPresent;
import io.jans.shibboleth.trust.activation.error.WorkItemNotFound;
import io.jans.shibboleth.trust.activation.error.WorkerNotFound;
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
import io.jans.shibboleth.trust.shared.Result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Repository behaviour against a mocked {@link PersistenceEntryManager} (no DB): insert-vs-update, find /
 * rehydrate, not-found, the claim lock (create → collision → lost race), lease find/renew/GC, and the
 * claimable-candidate query.
 */
@DisplayName("Activation repositories — mocked entry manager")
public class ActivationRepositoryImplTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String WORK_ITEM_BASE = "ou=trustActivationWorkItems,o=jans";
    private static final String CURRENT_EPISODE_BASE = "ou=trustActivationEpisodes,o=jans";
    private static final String LEASE_BASE = "ou=trustActivationLeases,o=jans";
    private static final String WORKER_BASE = "ou=trustActivationWorkers,o=jans";

    private final PersistenceEntryManager entryManager = mock(PersistenceEntryManager.class);

    private final WorkItemRepositoryImpl workItems =
        new WorkItemRepositoryImpl(entryManager, WORK_ITEM_BASE, CURRENT_EPISODE_BASE);
    private final LeaseRepositoryImpl leases = new LeaseRepositoryImpl(entryManager, LEASE_BASE);
    private final WorkerRepositoryImpl workers = new WorkerRepositoryImpl(entryManager, WORKER_BASE);

    private static WorkItem workItem(WorkItemState state, Instant createdAt) {

        return WorkItem.rehydrate(WorkItemId.of(UUID.randomUUID()).getValue(),
            WorkItemType.PROCESS_AGGREGATE_METADATA, TrustRelationshipRef.of(UUID.randomUUID()).getValue(),
            state, createdAt, createdAt).getValue();
    }

    private static Lease lease(WorkItemId workItemId, LeaseGeneration generation) {

        return Lease.granted(workItemId, generation, WorkerId.of(Origin.of("w@host")).getValue(),
            NOW, NOW.plusSeconds(30)).getValue();
    }

    // ---- WorkItem ----

    @Test
    @DisplayName("GIVEN an absent work item WHEN saved THEN it is persisted, not merged")
    public void workItemSaveInserts() {

        workItems.save(workItem(WorkItemState.PENDING, NOW));

        verify(entryManager).persist(any(WorkItemEntry.class));
        verify(entryManager, never()).merge(any());
    }

    @Test
    @DisplayName("GIVEN an existing work item WHEN saved THEN it is merged, not persisted")
    public void workItemSaveUpdates() {

        WorkItem item = workItem(WorkItemState.COMPLETED, NOW);
        String dn = "inum=" + item.id().value() + "," + WORK_ITEM_BASE;
        when(entryManager.find(eq(dn), eq(WorkItemEntry.class), nullable(String[].class)))
            .thenReturn(WorkItemEntryMapper.toEntry(item));

        workItems.save(item);

        verify(entryManager).merge(any(WorkItemEntry.class));
        verify(entryManager, never()).persist(any());
    }

    @Test
    @DisplayName("GIVEN a stored work item WHEN found by id THEN it is rehydrated")
    public void workItemFindByIdRehydrates() {

        WorkItem item = workItem(WorkItemState.PENDING, NOW);
        String dn = "inum=" + item.id().value() + "," + WORK_ITEM_BASE;
        when(entryManager.find(eq(dn), eq(WorkItemEntry.class), nullable(String[].class)))
            .thenReturn(WorkItemEntryMapper.toEntry(item));

        Result<WorkItem> found = workItems.findById(item.id());

        assertThat(found.isSuccess()).isTrue();
        assertThat(found.getValue().id()).isEqualTo(item.id());
    }

    @Test
    @DisplayName("GIVEN no stored work item WHEN found by id THEN it fails with WorkItemNotFound")
    public void workItemFindByIdNotFound() {

        Result<WorkItem> found = workItems.findById(WorkItemId.of(UUID.randomUUID()).getValue());

        assertThat(found.isFailure()).isTrue();
        assertThat(found.getError()).isInstanceOf(WorkItemNotFound.class);
    }

    @Test
    @DisplayName("GIVEN an id WHEN deleted THEN the entry at its DN is removed")
    public void workItemDelete() {

        WorkItemId id = WorkItemId.of(UUID.randomUUID()).getValue();

        workItems.delete(id);

        verify(entryManager).remove("inum=" + id.value() + "," + WORK_ITEM_BASE, WorkItemEntry.class);
    }

    @Test
    @DisplayName("GIVEN claimable candidates out of order WHEN queried THEN they are returned oldest first")
    public void workItemClaimableCandidatesSorted() {

        WorkItem newer = workItem(WorkItemState.PENDING, NOW.plusSeconds(60));
        WorkItem older = workItem(WorkItemState.PENDING, NOW);
        when(entryManager.findEntries(eq(WORK_ITEM_BASE), eq(WorkItemEntry.class), any(Filter.class)))
            .thenReturn(List.of(WorkItemEntryMapper.toEntry(newer), WorkItemEntryMapper.toEntry(older)));

        Result<List<WorkItem>> result = workItems.findClaimableCandidates(WorkItemType.PROCESS_AGGREGATE_METADATA);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).extracting(WorkItem::id).containsExactly(older.id(), newer.id());
    }

    // ---- Lease ----

    @Test
    @DisplayName("GIVEN a free generation WHEN a lease is created THEN it is persisted and succeeds")
    public void leaseCreateWins() {

        Result<Lease> result = leases.create(lease(WorkItemId.of(UUID.randomUUID()).getValue(), LeaseGeneration.first()));

        assertThat(result.isSuccess()).isTrue();
        verify(entryManager).persist(any(LeaseEntry.class));
    }

    @Test
    @DisplayName("GIVEN a taken generation WHEN a lease is created THEN the identity collision is read as a lost claim")
    public void leaseCreateCollisionIsLost() {

        doThrow(new EntryPersistenceException("duplicate")).when(entryManager).persist(any(LeaseEntry.class));

        Result<Lease> result = leases.create(lease(WorkItemId.of(UUID.randomUUID()).getValue(), LeaseGeneration.first()));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(LeaseAlreadyHeld.class);
    }

    @Test
    @DisplayName("GIVEN stored leases for a work item WHEN queried THEN all are mapped back")
    public void leaseFindByWorkItem() {

        WorkItemId workItemId = WorkItemId.of(UUID.randomUUID()).getValue();
        when(entryManager.findEntries(eq(LEASE_BASE), eq(LeaseEntry.class), any(Filter.class)))
            .thenReturn(List.of(
                LeaseEntryMapper.toEntry(lease(workItemId, LeaseGeneration.first())),
                LeaseEntryMapper.toEntry(lease(workItemId, LeaseGeneration.first().next()))));

        Result<List<Lease>> result = leases.findByWorkItem(workItemId);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("GIVEN an existing lease WHEN renewed THEN it is merged")
    public void leaseRenewMerges() {

        Result<Lease> result = leases.renew(lease(WorkItemId.of(UUID.randomUUID()).getValue(), LeaseGeneration.first()));

        assertThat(result.isSuccess()).isTrue();
        verify(entryManager).merge(any(LeaseEntry.class));
    }

    @Test
    @DisplayName("GIVEN no matching lease WHEN renewed THEN it fails with LeaseNotPresent")
    public void leaseRenewAbsentFails() {

        doThrow(new EntryPersistenceException("absent")).when(entryManager).merge(any(LeaseEntry.class));

        Result<Lease> result = leases.renew(lease(WorkItemId.of(UUID.randomUUID()).getValue(), LeaseGeneration.first()));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(LeaseNotPresent.class);
    }

    @Test
    @DisplayName("GIVEN a lease WHEN deleted THEN it is removed, and a delete of an already-gone lease still succeeds")
    public void leaseDeleteIsIdempotent() {

        Lease lease = lease(WorkItemId.of(UUID.randomUUID()).getValue(), LeaseGeneration.first());
        String dn = "inum=" + LeaseEntryMapper.inumFor(lease.workItemId().value(), 1) + "," + LEASE_BASE;

        assertThat(leases.delete(lease).isSuccess()).isTrue();
        verify(entryManager).remove(dn, LeaseEntry.class);

        doThrow(new EntryPersistenceException("gone")).when(entryManager).remove(dn, LeaseEntry.class);
        assertThat(leases.delete(lease).isSuccess()).isTrue();
    }

    // ---- Worker ----

    @Test
    @DisplayName("GIVEN an absent worker WHEN saved THEN it is persisted, not merged")
    public void workerSaveInserts() {

        workers.save(Worker.register(WorkerId.of(Origin.of("w@host")).getValue(), NOW).getValue());

        verify(entryManager).persist(any(WorkerEntry.class));
        verify(entryManager, never()).merge(any());
    }

    @Test
    @DisplayName("GIVEN a stored worker WHEN found by id THEN it is rehydrated")
    public void workerFindByIdRehydrates() {

        WorkerId id = WorkerId.of(Origin.of("w@host")).getValue();
        Worker worker = Worker.register(id, NOW).getValue();
        String dn = "inum=" + WorkerEntryMapper.inumFor("w@host") + "," + WORKER_BASE;
        when(entryManager.find(eq(dn), eq(WorkerEntry.class), nullable(String[].class)))
            .thenReturn(WorkerEntryMapper.toEntry(worker));

        Result<Worker> found = workers.findById(id);

        assertThat(found.isSuccess()).isTrue();
        assertThat(found.getValue().id()).isEqualTo(id);
    }

    @Test
    @DisplayName("GIVEN no stored worker WHEN found by id THEN it fails with WorkerNotFound")
    public void workerFindByIdNotFound() {

        Result<Worker> found = workers.findById(WorkerId.of(Origin.of("ghost@host")).getValue());

        assertThat(found.isFailure()).isTrue();
        assertThat(found.getError()).isInstanceOf(WorkerNotFound.class);
    }

    // ---- Current episode pointer ----

    @Test
    @DisplayName("GIVEN no pointer WHEN a current episode is assigned THEN it is persisted")
    public void assignCurrentEpisodeInserts() {

        workItems.assignCurrentEpisode(TrustRelationshipRef.of(UUID.randomUUID()).getValue(),
            WorkItemId.of(UUID.randomUUID()).getValue());

        verify(entryManager).persist(any(CurrentEpisodeEntry.class));
        verify(entryManager, never()).merge(any());
    }

    @Test
    @DisplayName("GIVEN a stored pointer WHEN the current episode is queried THEN the referenced work-item id is returned")
    public void currentEpisodeFound() {

        TrustRelationshipRef tr = TrustRelationshipRef.of(UUID.randomUUID()).getValue();
        WorkItemId workItemId = WorkItemId.of(UUID.randomUUID()).getValue();
        CurrentEpisodeEntry entry = new CurrentEpisodeEntry();
        entry.setInum(tr.value().toString());
        entry.setWorkItemRef(workItemId.value().toString());
        String dn = "inum=" + tr.value() + "," + CURRENT_EPISODE_BASE;
        when(entryManager.find(eq(dn), eq(CurrentEpisodeEntry.class), nullable(String[].class)))
            .thenReturn(entry);

        Result<WorkItemId> current = workItems.currentEpisode(tr);

        assertThat(current.isSuccess()).isTrue();
        assertThat(current.getValue()).isEqualTo(workItemId);
    }

    @Test
    @DisplayName("GIVEN no pointer WHEN the current episode is queried THEN it fails with WorkItemNotFound")
    public void currentEpisodeAbsent() {

        Result<WorkItemId> current = workItems.currentEpisode(TrustRelationshipRef.of(UUID.randomUUID()).getValue());

        assertThat(current.isFailure()).isTrue();
        assertThat(current.getError()).isInstanceOf(WorkItemNotFound.class);
    }

    @Test
    @DisplayName("GIVEN a trust relationship WHEN its current episode is cleared THEN the pointer entry is removed")
    public void clearCurrentEpisodeRemoves() {

        TrustRelationshipRef tr = TrustRelationshipRef.of(UUID.randomUUID()).getValue();

        assertThat(workItems.clearCurrentEpisode(tr).isSuccess()).isTrue();

        verify(entryManager).remove("inum=" + tr.value() + "," + CURRENT_EPISODE_BASE, CurrentEpisodeEntry.class);
    }
}
