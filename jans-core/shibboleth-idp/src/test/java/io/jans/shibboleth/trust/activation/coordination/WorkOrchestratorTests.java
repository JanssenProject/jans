package io.jans.shibboleth.trust.activation.coordination;

import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.activation.model.WorkItem;
import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.model.WorkItemType;
import io.jans.shibboleth.trust.activation.error.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Group 7 — Orchestrator: Activation Demand to WorkItem")
public class WorkOrchestratorTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final TimeSource CLOCK = () -> NOW;
    private static final Duration LEASE_TTL = Duration.ofSeconds(30);
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);
    private static final ActivationEventSink NO_EVENTS = event -> { };
    private static final FinalizeActivationPort NO_FINALIZE = (ref, diagnostics) -> { };

    private static TrustRelationshipRef aTrustRelationship() {

        return TrustRelationshipRef.of(UUID.randomUUID()).getValue();
    }

    @Test
    @DisplayName("GIVEN an ActivationRequested for a TR WHEN the orchestrator handles it THEN it creates a PENDING WorkItem for that trustRelationshipId")
    public void shouldCreatePendingWorkItem_whenActivationRequested() {

        WorkOrchestrator orchestrator = WorkOrchestrator.create(CLOCK, LEASE_TTL, HEARTBEAT_TTL, NO_EVENTS, NO_FINALIZE).getValue();
        TrustRelationshipRef tr = aTrustRelationship();

        WorkItem item = orchestrator.onActivationRequested(tr, WorkItemType.PROCESS_AGGREGATE_METADATA).getValue();

        assertThat(item.state()).isEqualTo(WorkItemState.PENDING);
        assertThat(item.trustRelationshipId()).isEqualTo(tr);
    }

    @Test
    @DisplayName("GIVEN an ActivationRequested for a TR whose metadata source is an aggregate WHEN the WorkItem is created THEN its type is PROCESS_AGGREGATE_METADATA")
    public void shouldSelectAggregateType_whenTrMetadataIsAggregate() {

        WorkOrchestrator orchestrator = WorkOrchestrator.create(CLOCK, LEASE_TTL, HEARTBEAT_TTL, NO_EVENTS, NO_FINALIZE).getValue();

        WorkItem item = orchestrator.onActivationRequested(aTrustRelationship(), WorkItemType.PROCESS_AGGREGATE_METADATA).getValue();

        assertThat(item.type()).isEqualTo(WorkItemType.PROCESS_AGGREGATE_METADATA);
    }

    @Test
    @DisplayName("GIVEN an ActivationRequested for a TR whose metadata is a single entity WHEN the WorkItem is created THEN its type is PROCESS_INDIVIDUAL_METADATA")
    public void shouldSelectIndividualType_whenTrMetadataIsIndividual() {

        WorkOrchestrator orchestrator = WorkOrchestrator.create(CLOCK, LEASE_TTL, HEARTBEAT_TTL, NO_EVENTS, NO_FINALIZE).getValue();

        WorkItem item = orchestrator.onActivationRequested(aTrustRelationship(), WorkItemType.PROCESS_INDIVIDUAL_METADATA).getValue();

        assertThat(item.type()).isEqualTo(WorkItemType.PROCESS_INDIVIDUAL_METADATA);
    }

    @Test
    @DisplayName("GIVEN an ActivationRequested WHEN the WorkItem is created THEN it becomes the TR's current WorkItem")
    public void shouldSetCreatedItemAsCurrentForTr() {

        WorkOrchestrator orchestrator = WorkOrchestrator.create(CLOCK, LEASE_TTL, HEARTBEAT_TTL, NO_EVENTS, NO_FINALIZE).getValue();

        WorkItem item = orchestrator.onActivationRequested(aTrustRelationship(), WorkItemType.PROCESS_AGGREGATE_METADATA).getValue();

        assertThat(orchestrator.isCurrent(item)).isTrue();
    }

    @Test
    @DisplayName("GIVEN a TR that already had an episode WHEN a further ActivationRequested arrives THEN a new WorkItem with a new WorkItemId is created and becomes current")
    public void shouldPointToNewWorkItem_whenNewEpisodeRequested() {

        WorkOrchestrator orchestrator = WorkOrchestrator.create(CLOCK, LEASE_TTL, HEARTBEAT_TTL, NO_EVENTS, NO_FINALIZE).getValue();
        TrustRelationshipRef tr = aTrustRelationship();

        WorkItem first = orchestrator.onActivationRequested(tr, WorkItemType.PROCESS_AGGREGATE_METADATA).getValue();
        WorkItem second = orchestrator.onActivationRequested(tr, WorkItemType.PROCESS_AGGREGATE_METADATA).getValue();

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(orchestrator.isCurrent(second)).isTrue();
    }

    @Test
    @DisplayName("GIVEN a new episode has started for a TR WHEN the previous episode's WorkItem is checked against the current pointer THEN it is no longer current")
    public void shouldTreatPriorWorkItemAsNotCurrent_afterNewEpisode() {

        WorkOrchestrator orchestrator = WorkOrchestrator.create(CLOCK, LEASE_TTL, HEARTBEAT_TTL, NO_EVENTS, NO_FINALIZE).getValue();
        TrustRelationshipRef tr = aTrustRelationship();

        WorkItem first = orchestrator.onActivationRequested(tr, WorkItemType.PROCESS_AGGREGATE_METADATA).getValue();
        orchestrator.onActivationRequested(tr, WorkItemType.PROCESS_AGGREGATE_METADATA);

        assertThat(orchestrator.isCurrent(first)).isFalse();
    }

    @Test
    @DisplayName("GIVEN a null TimeSource WHEN a WorkOrchestrator is created THEN it fails and no orchestrator is produced")
    public void shouldFailCreation_whenTimeSourceIsNull() {

        Result<WorkOrchestrator> result = WorkOrchestrator.create(null, LEASE_TTL, HEARTBEAT_TTL, NO_EVENTS, NO_FINALIZE);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a null lease TTL WHEN a WorkOrchestrator is created THEN it fails and no orchestrator is produced")
    public void shouldFailCreation_whenLeaseTtlIsNull() {

        Result<WorkOrchestrator> result = WorkOrchestrator.create(CLOCK, null, HEARTBEAT_TTL, NO_EVENTS, NO_FINALIZE);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a null heartbeat TTL WHEN a WorkOrchestrator is created THEN it fails and no orchestrator is produced")
    public void shouldFailCreation_whenHeartbeatTtlIsNull() {

        Result<WorkOrchestrator> result = WorkOrchestrator.create(CLOCK, LEASE_TTL, null, NO_EVENTS, NO_FINALIZE);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a null event sink WHEN a WorkOrchestrator is created THEN it fails and no orchestrator is produced")
    public void shouldFailCreation_whenEventSinkIsNull() {

        Result<WorkOrchestrator> result = WorkOrchestrator.create(CLOCK, LEASE_TTL, HEARTBEAT_TTL, null, NO_FINALIZE);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a null finalize port WHEN a WorkOrchestrator is created THEN it fails and no orchestrator is produced")
    public void shouldFailCreation_whenFinalizePortIsNull() {

        Result<WorkOrchestrator> result = WorkOrchestrator.create(CLOCK, LEASE_TTL, HEARTBEAT_TTL, NO_EVENTS, null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }
}
