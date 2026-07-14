package io.jans.shibboleth.trust.activation.coordination;

import io.jans.shibboleth.trust.activation.error.StaleReport;
import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.util.ActivationResult;
import io.jans.shibboleth.trust.activation.workers.Worker;
import io.jans.shibboleth.trust.activation.workers.WorkerId;
import io.jans.shibboleth.trust.config.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.config.diagnostics.ActivationStatus;
import io.jans.shibboleth.trust.shared.Origin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.trust.activation.model.WorkItemType.PROCESS_AGGREGATE_METADATA;

@DisplayName("Group 11 — Orchestrator: Cancellation")
public class WorkOrchestratorCancellationTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final TimeSource CLOCK = () -> NOW;
    private static final Duration LEASE_TTL = Duration.ofSeconds(30);
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);
    private static final ActivationEventSink NO_EVENTS = event -> { };

    private int finalizeCount = 0;
    private final WorkOrchestrator orchestrator =
        WorkOrchestrator.create(CLOCK, LEASE_TTL, HEARTBEAT_TTL, NO_EVENTS, (ref, diagnostics) -> finalizeCount++).getValue();

    private static TrustRelationshipRef aTrustRelationship() {

        return TrustRelationshipRef.of(UUID.randomUUID()).getValue();
    }

    private static Worker worker(String origin) {

        return Worker.register(WorkerId.of(Origin.of(origin)).getValue(), NOW).getValue();
    }

    private static ActivationDiagnostics diagnostics(String origin) {

        return ActivationDiagnostics.of(ActivationStatus.SUCCEEDED, Origin.of(origin), List.of(), Instant.EPOCH, Instant.EPOCH).getValue();
    }

    @Test
    @DisplayName("GIVEN a TR that left ACTIVATING via cancelActivation WHEN ActivationCancelled is handled THEN the current WorkItem is marked CANCELLED and cleared as current")
    public void shouldCancelCurrentWorkItem_whenActivationCancelled() {

        TrustRelationshipRef tr = aTrustRelationship();
        WorkItem pending = orchestrator.onActivationRequested(tr, PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.claim(pending.id(), worker("w@host"));

        WorkItem cancelled = orchestrator.onActivationCancelled(tr).getValue();

        assertThat(cancelled.state()).isEqualTo(WorkItemState.CANCELLED);
        assertThat(orchestrator.isCurrent(cancelled)).isFalse();
    }

    @Test
    @DisplayName("GIVEN a WorkItem cancelled after a Worker was busy WHEN that Worker's eventual report arrives THEN it is discarded by the identity fence")
    public void shouldDiscardLateReport_afterCancellation() {

        TrustRelationshipRef tr = aTrustRelationship();
        WorkItem pending = orchestrator.onActivationRequested(tr, PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.claim(pending.id(), worker("w@host"));
        orchestrator.onActivationCancelled(tr);

        ActivationResult<WorkItem> result = orchestrator.report(pending.id(), diagnostics("w@host"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(StaleReport.class);
        assertThat(finalizeCount).isZero();
    }

    @Test
    @DisplayName("GIVEN a cancelled activation WHEN the TR is activated again THEN a new WorkItem with a new WorkItemId begins a fresh episode")
    public void shouldStartFreshEpisode_whenActivatedAgainAfterCancel() {

        TrustRelationshipRef tr = aTrustRelationship();
        WorkItem first = orchestrator.onActivationRequested(tr, PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.onActivationCancelled(tr);

        WorkItem second = orchestrator.onActivationRequested(tr, PROCESS_AGGREGATE_METADATA).getValue();

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(orchestrator.isCurrent(second)).isTrue();
    }
}
