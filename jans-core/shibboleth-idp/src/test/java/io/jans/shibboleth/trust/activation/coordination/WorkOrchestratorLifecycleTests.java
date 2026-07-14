package io.jans.shibboleth.trust.activation.coordination;

import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationStatus;
import io.jans.shibboleth.trust.shared.Origin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.trust.activation.model.WorkItemType.PROCESS_AGGREGATE_METADATA;

@DisplayName("Group 12 — Cross-Cutting: Delivery & Lifecycle")
public class WorkOrchestratorLifecycleTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration LEASE_TTL = Duration.ofSeconds(30);
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);

    private final AtomicReference<Instant> clock = new AtomicReference<>(NOW);
    private final List<ActivationEvent> emitted = new ArrayList<>();
    private final RecordingFinalizePort finalizePort = new RecordingFinalizePort();
    private final WorkOrchestrator orchestrator =
        WorkOrchestrator.create(clock::get, LEASE_TTL, HEARTBEAT_TTL, emitted::add, finalizePort).getValue();

    private static TrustRelationshipRef aTrustRelationship() {

        return TrustRelationshipRef.of(UUID.randomUUID()).getValue();
    }

    private static Worker worker(String origin, Instant registeredAt) {

        return Worker.register(WorkerId.of(Origin.of(origin)).getValue(), registeredAt).getValue();
    }

    private static ActivationDiagnostics diagnostics(String origin, ActivationStatus status) {

        return ActivationDiagnostics.of(status, Origin.of(origin), List.of(), Instant.EPOCH, Instant.EPOCH).getValue();
    }

    private long workItemAssignedCount() {

        return emitted.stream().filter(WorkItemAssigned.class::isInstance).count();
    }

    @Test
    @DisplayName("GIVEN a WorkItem whose first holder went silent WHEN it is reclaimed and reassigned THEN the same episode is processed more than once which is at-least-once delivery")
    public void shouldProcessAtLeastOnce_whenWorkerRetried() {

        WorkItem pending = orchestrator.onActivationRequested(aTrustRelationship(), PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.claim(pending.id(), worker("w1@host", NOW));

        clock.set(NOW.plusSeconds(31));
        orchestrator.sweepExpiredLeases();
        orchestrator.claim(pending.id(), worker("w2@host", NOW.plusSeconds(31)));

        assertThat(workItemAssignedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("GIVEN at-least-once processing of an episode WHEN more than one Worker completes work THEN finalizeActivation takes effect exactly once")
    public void shouldFinalizeEffectivelyOnce_despiteRetries() {

        WorkItem pending = orchestrator.onActivationRequested(aTrustRelationship(), PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.claim(pending.id(), worker("w1@host", NOW));

        clock.set(NOW.plusSeconds(31));
        orchestrator.sweepExpiredLeases();
        orchestrator.claim(pending.id(), worker("w2@host", NOW.plusSeconds(31)));

        orchestrator.report(pending.id(), diagnostics("w2@host", ActivationStatus.SUCCEEDED));
        orchestrator.report(pending.id(), diagnostics("w1@host", ActivationStatus.SUCCEEDED));

        assertThat(finalizePort.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN an ActivationRequested WHEN the WorkItem is created and claimed and heartbeated and reported successfully THEN it ends COMPLETED and finalizeActivation was invoked exactly once with success")
    public void shouldWalkFullActivationFlow_whenReportedSuccessfully() {

        Worker holder = worker("w@host", NOW);
        WorkItem pending = orchestrator.onActivationRequested(aTrustRelationship(), PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.claim(pending.id(), holder);
        orchestrator.heartbeat(pending.id(), holder);

        WorkItem completed = orchestrator.report(pending.id(), diagnostics("w@host", ActivationStatus.SUCCEEDED)).getValue();

        assertThat(completed.state()).isEqualTo(WorkItemState.COMPLETED);
        assertThat(finalizePort.count()).isEqualTo(1);
        assertThat(finalizePort.statuses).containsExactly(ActivationStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("GIVEN an ASSIGNED WorkItem whose first Worker crashes WHEN the lease expires and a second Worker claims and reports THEN the item ends COMPLETED with a single finalize")
    public void shouldReclaimThenComplete_whenFirstWorkerCrashes() {

        WorkItem pending = orchestrator.onActivationRequested(aTrustRelationship(), PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.claim(pending.id(), worker("w1@host", NOW));

        clock.set(NOW.plusSeconds(31));
        orchestrator.sweepExpiredLeases();
        orchestrator.claim(pending.id(), worker("w2@host", NOW.plusSeconds(31)));

        orchestrator.report(pending.id(), diagnostics("w2@host", ActivationStatus.SUCCEEDED));

        assertThat(orchestrator.find(pending.id()).getValue().state()).isEqualTo(WorkItemState.COMPLETED);
        assertThat(finalizePort.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN a reported failure for the current WorkItem WHEN a later episode succeeds THEN the second episode reaches a successful finalize")
    public void shouldReturnTrToReadyThenRetry_whenActivationFails() {

        TrustRelationshipRef tr = aTrustRelationship();
        WorkItem first = orchestrator.onActivationRequested(tr, PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.claim(first.id(), worker("w1@host", NOW));
        orchestrator.report(first.id(), diagnostics("w1@host", ActivationStatus.FAILED));

        WorkItem second = orchestrator.onActivationRequested(tr, PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.claim(second.id(), worker("w2@host", NOW));
        WorkItem completed = orchestrator.report(second.id(), diagnostics("w2@host", ActivationStatus.SUCCEEDED)).getValue();

        assertThat(completed.state()).isEqualTo(WorkItemState.COMPLETED);
        assertThat(finalizePort.statuses).containsExactly(ActivationStatus.FAILED, ActivationStatus.SUCCEEDED);
    }

    static final class RecordingFinalizePort implements FinalizeActivationPort {

        final List<ActivationStatus> statuses = new ArrayList<>();

        @Override
        public void finalizeActivation(TrustRelationshipRef trustRelationshipId, ActivationDiagnostics diagnostics) {

            statuses.add(diagnostics.getStatus());
        }

        int count() {

            return statuses.size();
        }
    }
}
