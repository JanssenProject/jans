package io.jans.shibboleth.trust.persistence.activation;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.persistence.SqlEntryManagerExtension;

import io.jans.orm.sql.impl.SqlEntryManager;

import io.jans.shibboleth.trust.activation.error.LeaseAlreadyHeld;
import io.jans.shibboleth.trust.activation.lease.Lease;
import io.jans.shibboleth.trust.activation.lease.LeaseGeneration;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemId;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.activation.repository.LeaseRepository;
import io.jans.shibboleth.trust.activation.repository.WorkItemRepository;
import io.jans.shibboleth.trust.activation.repository.WorkerRepository;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration tests for the activation repositories against a real jans-orm SQL backend.
 *
 * <p><b>Gated:</b> skipped unless {@code -Dtrust.it.sql.uri} is set — jans-orm SQL has no embedded/H2 mode,
 * so these need a running Postgres whose schema already contains the {@code jansTrustActivationWorkItem} /
 * {@code jansTrustActivationLease} / {@code jansTrustActivationWorker} tables. The {@code docker-compose.yaml} in this
 * module starts such an instance, provisioning them from
 * {@code src/test/resources/init-scripts/01-activation-init.sql}.
 *
 * <p>Run e.g.:
 * <pre>
 * docker compose -f trust-adapters/docker-compose.yaml up -d
 * mvn -pl trust-adapters test -Dtest=ActivationRepositorySqlIntegrationTests \
 *   -Dtrust.it.sql.uri=jdbc:postgresql://localhost:5432/jansdb \
 *   -Dtrust.it.sql.schema=public -Dtrust.it.sql.user=jans \
 *   -Dtrust.it.sql.password='VWSAG/ixu14S7EDjDNH4cQ=='
 * </pre>
 */
@DisplayName("Activation repositories — SQL integration (gated by -Dtrust.it.sql.uri)")
@ExtendWith(SqlEntryManagerExtension.class)
public class ActivationRepositorySqlIntegrationTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static WorkItemRepository workItems;
    private static LeaseRepository leases;
    private static WorkerRepository workers;

    @BeforeAll
    static void connect(SqlEntryManager entryManager) {

        workItems = new WorkItemRepositoryImpl(entryManager,
            System.getProperty("trust.it.sql.workItemsDn", "ou=trustActivationWorkItems,o=jans"),
            System.getProperty("trust.it.sql.currentEpisodesDn", "ou=trustActivationEpisodes,o=jans"));
        leases = new LeaseRepositoryImpl(entryManager,
            System.getProperty("trust.it.sql.leasesDn", "ou=trustActivationLeases,o=jans"));
        workers = new WorkerRepositoryImpl(entryManager,
            System.getProperty("trust.it.sql.workersDn", "ou=trustActivationWorkers,o=jans"));
    }

    private static WorkItem pending() {

        return WorkItem.create(WorkItemType.PROCESS_AGGREGATE_METADATA,
            TrustRelationshipRef.of(UUID.randomUUID()).getValue(), NOW).getValue();
    }

    private static WorkerId workerId(String origin) {

        return WorkerId.of(Origin.of(origin)).getValue();
    }

    @Test
    @DisplayName("save assigns a work item; findById rehydrates it; delete removes it")
    public void workItemSaveFindDelete() {

        WorkItem item = pending();

        Result<WorkItem> saved = workItems.save(item);
        assertThat(saved.isSuccess()).isTrue();

        try {

            Result<WorkItem> found = workItems.findById(item.id());
            assertThat(found.isSuccess()).isTrue();
            assertThat(found.getValue().id()).isEqualTo(item.id());
            assertThat(found.getValue().state()).isEqualTo(WorkItemState.PENDING);
        } finally {

            workItems.delete(item.id());
        }

        assertThat(workItems.findById(item.id()).isFailure()).isTrue();
    }

    @Test
    @DisplayName("create, find, renew and delete a lease round-trips through the store")
    public void leaseLifecycle() {

        WorkItemId workItemId = WorkItemId.of(UUID.randomUUID()).getValue();
        Lease lease = Lease.granted(workItemId, LeaseGeneration.first(), workerId("w@host"),
            NOW, NOW.plusSeconds(30)).getValue();

        try {

            assertThat(leases.create(lease).isSuccess()).isTrue();

            List<Lease> found = leases.findByWorkItem(workItemId).getValue();
            assertThat(found).hasSize(1);

            Lease renewed = lease.renew(NOW.plusSeconds(60)).getValue();
            assertThat(leases.renew(renewed).isSuccess()).isTrue();
            assertThat(leases.findByWorkItem(workItemId).getValue().get(0).expiresAt())
                .isEqualTo(NOW.plusSeconds(60));
        } finally {

            leases.delete(lease);
        }

        assertThat(leases.findByWorkItem(workItemId).getValue()).isEmpty();
    }

    @Test
    @DisplayName("two workers racing for the same (workItem, generation) collide on the primary key; exactly one wins")
    public void twoWorkersRaceForSameGenerationHaveOneWinner() {

        WorkItemId workItemId = WorkItemId.of(UUID.randomUUID()).getValue();
        Lease byA = Lease.granted(workItemId, LeaseGeneration.first(), workerId("a@host"),
            NOW, NOW.plusSeconds(30)).getValue();
        Lease byB = Lease.granted(workItemId, LeaseGeneration.first(), workerId("b@host"),
            NOW, NOW.plusSeconds(30)).getValue();

        try {

            Result<Lease> first = leases.create(byA);
            Result<Lease> second = leases.create(byB);

            assertThat(first.isSuccess()).isTrue();
            assertThat(second.isFailure()).isTrue();
            assertThat(second.getError()).isInstanceOf(LeaseAlreadyHeld.class);

            List<Lease> stored = leases.findByWorkItem(workItemId).getValue();
            assertThat(stored).hasSize(1);
            assertThat(stored.get(0).isHeldBy(workerId("a@host"))).isTrue();
        } finally {

            leases.delete(byA);
        }
    }

    @Test
    @DisplayName("takeover at the next generation succeeds once the current lease is a distinct identity")
    public void takeoverAtNextGenerationSucceeds() {

        WorkItemId workItemId = WorkItemId.of(UUID.randomUUID()).getValue();
        Lease gen1 = Lease.granted(workItemId, LeaseGeneration.first(), workerId("a@host"),
            NOW, NOW.plusSeconds(30)).getValue();
        Lease gen2 = Lease.granted(workItemId, LeaseGeneration.first().next(), workerId("b@host"),
            NOW, NOW.plusSeconds(30)).getValue();

        try {

            assertThat(leases.create(gen1).isSuccess()).isTrue();
            assertThat(leases.create(gen2).isSuccess()).isTrue();
            assertThat(leases.findByWorkItem(workItemId).getValue()).hasSize(2);
        } finally {

            leases.delete(gen1);
            leases.delete(gen2);
        }
    }

    @Test
    @DisplayName("the current-episode pointer is upserted, read back, and cleared")
    public void currentEpisodePointerRoundTrips() {

        TrustRelationshipRef tr = TrustRelationshipRef.of(UUID.randomUUID()).getValue();
        WorkItemId first = WorkItemId.of(UUID.randomUUID()).getValue();
        WorkItemId second = WorkItemId.of(UUID.randomUUID()).getValue();

        try {

            assertThat(workItems.assignCurrentEpisode(tr, first).isSuccess()).isTrue();
            assertThat(workItems.currentEpisode(tr).getValue()).isEqualTo(first);

            assertThat(workItems.assignCurrentEpisode(tr, second).isSuccess()).isTrue();
            assertThat(workItems.currentEpisode(tr).getValue()).isEqualTo(second);
        } finally {

            workItems.clearCurrentEpisode(tr);
        }

        assertThat(workItems.currentEpisode(tr).isFailure()).isTrue();
    }

    @Test
    @DisplayName("save a worker; findById rehydrates it")
    public void workerSaveFind() {

        WorkerId id = workerId("worker-" + UUID.randomUUID() + "@host");
        Worker worker = Worker.register(id, NOW).getValue();

        try {

            assertThat(workers.save(worker).isSuccess()).isTrue();

            Result<Worker> found = workers.findById(id);
            assertThat(found.isSuccess()).isTrue();
            assertThat(found.getValue().id()).isEqualTo(id);
        } finally {

            workers.delete(id);
        }
    }
}
