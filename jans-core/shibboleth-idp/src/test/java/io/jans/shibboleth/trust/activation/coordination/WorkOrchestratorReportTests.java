package io.jans.shibboleth.trust.activation.coordination;

import io.jans.shibboleth.trust.activation.error.NotLeaseHolder;
import io.jans.shibboleth.trust.activation.error.StaleReport;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.util.ActivationResult;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.config.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.config.diagnostics.ActivationStatus;
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
import static io.jans.shibboleth.trust.activation.model.WorkItemType.PROCESS_AGGREGATE_METADATA;

@DisplayName("Group 10 — Orchestrator: Report & Fencing")
public class WorkOrchestratorReportTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration LEASE_TTL = Duration.ofSeconds(30);
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);
    private static final ActivationEventSink NO_EVENTS = event -> { };

    private final AtomicReference<Instant> clock = new AtomicReference<>(NOW);
    private final RecordingFinalizePort finalizePort = new RecordingFinalizePort();
    private final WorkOrchestrator orchestrator =
        WorkOrchestrator.create(clock::get, LEASE_TTL, HEARTBEAT_TTL, NO_EVENTS, finalizePort).getValue();

    private static TrustRelationshipRef aTrustRelationship() {

        return TrustRelationshipRef.of(UUID.randomUUID()).getValue();
    }

    private static Worker worker(String origin, Instant registeredAt) {

        return Worker.register(WorkerId.of(Origin.of(origin)).getValue(), registeredAt).getValue();
    }

    private static ActivationDiagnostics diagnostics(ActivationStatus status, String origin) {

        return ActivationDiagnostics.of(status, Origin.of(origin), List.of(), Instant.EPOCH, Instant.EPOCH).getValue();
    }

    private WorkItem assignedItem(Worker holder) {

        WorkItem pending = orchestrator.onActivationRequested(aTrustRelationship(), PROCESS_AGGREGATE_METADATA).getValue();
        return orchestrator.claim(pending.id(), holder).getValue();
    }

    @Test
    @DisplayName("GIVEN a report for the TR's current WorkItem from its lease holder WHEN the orchestrator processes it THEN it invokes finalizeActivation and marks the WorkItem COMPLETED")
    public void shouldFinalizeAndComplete_whenReportForCurrentItemByHolder() {

        WorkItem assigned = assignedItem(worker("w@host", NOW));

        WorkItem completed = orchestrator.report(assigned.id(), diagnostics(ActivationStatus.SUCCEEDED, "w@host")).getValue();

        assertThat(completed.state()).isEqualTo(WorkItemState.COMPLETED);
        assertThat(finalizePort.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN an authoritative report carrying an opaque trustRelationshipId WHEN the orchestrator finalizes THEN it hands that opaque value to the boundary rather than a trust Id")
    public void shouldResolveOpaqueReferenceToTrustId_atBoundary() {

        WorkItem assigned = assignedItem(worker("w@host", NOW));

        orchestrator.report(assigned.id(), diagnostics(ActivationStatus.SUCCEEDED, "w@host"));

        assertThat(finalizePort.refs).containsExactly(assigned.trustRelationshipId());
    }

    @Test
    @DisplayName("GIVEN a report naming a WorkItem that is no longer the TR's current one WHEN the orchestrator processes it THEN the report is dropped and finalizeActivation is not called")
    public void shouldDropReport_whenForPriorEpisodeItem() {

        TrustRelationshipRef tr = aTrustRelationship();
        WorkItem first = orchestrator.onActivationRequested(tr, PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.claim(first.id(), worker("w1@host", NOW));
        orchestrator.onActivationRequested(tr, PROCESS_AGGREGATE_METADATA);

        ActivationResult<WorkItem> result = orchestrator.report(first.id(), diagnostics(ActivationStatus.SUCCEEDED, "w1@host"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(StaleReport.class);
        assertThat(finalizePort.count()).isZero();
    }

    @Test
    @DisplayName("GIVEN a TR that cycled into a second episode WHEN a slow report from the first episode arrives THEN it is discarded and does not finalize the second episode")
    public void shouldNotFinalizeSecondEpisode_whenSlowFirstEpisodeReportArrives() {

        TrustRelationshipRef tr = aTrustRelationship();
        WorkItem episodeOne = orchestrator.onActivationRequested(tr, PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.claim(episodeOne.id(), worker("w1@host", NOW));
        WorkItem episodeTwo = orchestrator.onActivationRequested(tr, PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.claim(episodeTwo.id(), worker("w2@host", NOW));

        ActivationResult<WorkItem> result = orchestrator.report(episodeOne.id(), diagnostics(ActivationStatus.SUCCEEDED, "w1@host"));

        assertThat(result.isFailure()).isTrue();
        assertThat(finalizePort.count()).isZero();
    }

    @Test
    @DisplayName("GIVEN a report from a Worker whose lease expired and was reassigned WHEN the orchestrator processes it THEN it is dropped by the lease-ownership check")
    public void shouldDropReport_whenReporterNoLongerHoldsLease() {

        TrustRelationshipRef tr = aTrustRelationship();
        WorkItem pending = orchestrator.onActivationRequested(tr, PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.claim(pending.id(), worker("w1@host", NOW));

        clock.set(NOW.plusSeconds(31));
        orchestrator.sweepExpiredLeases();
        orchestrator.claim(pending.id(), worker("w2@host", NOW.plusSeconds(31)));

        ActivationResult<WorkItem> result = orchestrator.report(pending.id(), diagnostics(ActivationStatus.SUCCEEDED, "w1@host"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(NotLeaseHolder.class);
        assertThat(finalizePort.count()).isZero();
    }

    @Test
    @DisplayName("GIVEN the current WorkItem was already finalized WHEN a late intra-episode report arrives THEN no second finalize takes effect")
    public void shouldNotDoubleFinalize_whenLateIntraEpisodeReportSlipsThrough() {

        WorkItem assigned = assignedItem(worker("w@host", NOW));
        orchestrator.report(assigned.id(), diagnostics(ActivationStatus.SUCCEEDED, "w@host")).getValue();

        orchestrator.report(assigned.id(), diagnostics(ActivationStatus.SUCCEEDED, "w@host"));

        assertThat(finalizePort.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN a report naming an already COMPLETED WorkItem WHEN the orchestrator processes it THEN it is dropped")
    public void shouldDropReport_whenWorkItemAlreadyCompleted() {

        WorkItem assigned = assignedItem(worker("w@host", NOW));
        orchestrator.report(assigned.id(), diagnostics(ActivationStatus.SUCCEEDED, "w@host")).getValue();

        ActivationResult<WorkItem> result = orchestrator.report(assigned.id(), diagnostics(ActivationStatus.SUCCEEDED, "w@host"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(StaleReport.class);
    }

    @Test
    @DisplayName("GIVEN duplicate reports for the same current WorkItem WHEN the orchestrator processes them THEN finalizeActivation takes effect exactly once")
    public void shouldFinalizeEffectivelyOnce_whenDuplicateReportsArrive() {

        WorkItem assigned = assignedItem(worker("w@host", NOW));

        orchestrator.report(assigned.id(), diagnostics(ActivationStatus.SUCCEEDED, "w@host"));
        orchestrator.report(assigned.id(), diagnostics(ActivationStatus.SUCCEEDED, "w@host"));

        assertThat(finalizePort.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN a report carrying NO_DATA WHEN the orchestrator processes it THEN finalizeActivation is invoked but the WorkItem is not marked COMPLETED")
    public void shouldNotComplete_whenReportIsNoData() {

        WorkItem assigned = assignedItem(worker("w@host", NOW));

        WorkItem after = orchestrator.report(assigned.id(), diagnostics(ActivationStatus.NO_DATA, "w@host")).getValue();

        assertThat(after.state()).isNotEqualTo(WorkItemState.COMPLETED);
        assertThat(finalizePort.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("GIVEN a NO_DATA report was processed WHEN the WorkItem is inspected THEN it is still workable rather than terminal")
    public void shouldKeepItemWorkable_whenNoData() {

        WorkItem assigned = assignedItem(worker("w@host", NOW));

        orchestrator.report(assigned.id(), diagnostics(ActivationStatus.NO_DATA, "w@host"));

        WorkItem after = orchestrator.find(assigned.id()).getValue();
        assertThat(after.state()).isEqualTo(WorkItemState.ASSIGNED);
    }

    static final class RecordingFinalizePort implements FinalizeActivationPort {

        final List<TrustRelationshipRef> refs = new ArrayList<>();
        final List<ActivationDiagnostics> diagnostics = new ArrayList<>();

        @Override
        public void finalizeActivation(TrustRelationshipRef trustRelationshipId, ActivationDiagnostics activationDiagnostics) {

            refs.add(trustRelationshipId);
            diagnostics.add(activationDiagnostics);
        }

        int count() {

            return refs.size();
        }
    }
}
