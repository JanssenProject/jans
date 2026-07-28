package io.jans.shibboleth.trust.activation.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The from-store rehydration path: reconstruct a work item verbatim from its persisted identity and
 * lifecycle fields. A work item's persisted form carries no lease — assignment is derived separately — so
 * rehydration takes no lease and the reconstructed item reports state via {@code state(hasLiveLease)}.
 */
@DisplayName("WorkItem — rehydration from store")
public class WorkItemRehydrationTests {

    private static final WorkItemId ID = WorkItemId.of(UUID.randomUUID()).getValue();
    private static final TrustRelationshipRef TR = TrustRelationshipRef.of(UUID.randomUUID()).getValue();
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant TRANSITION_AT = CREATED_AT.plusSeconds(120);

    private static Result<WorkItem> rehydrate(WorkItemState state) {

        return WorkItem.rehydrate(ID, WorkItemType.PROCESS_AGGREGATE_METADATA, TR, state,
            CREATED_AT, TRANSITION_AT);
    }

    @Test
    @DisplayName("GIVEN persisted fields WHEN rehydrated THEN every identity and lifecycle field is carried verbatim")
    public void carriesAllFields() {

        WorkItem item = rehydrate(WorkItemState.PENDING).getValue();

        assertThat(item.id()).isEqualTo(ID);
        assertThat(item.type()).isEqualTo(WorkItemType.PROCESS_AGGREGATE_METADATA);
        assertThat(item.trustRelationshipId()).isEqualTo(TR);
        assertThat(item.state()).isEqualTo(WorkItemState.PENDING);
        assertThat(item.createdAt()).isEqualTo(CREATED_AT);
        assertThat(item.lastTransitionAt()).isEqualTo(TRANSITION_AT);
    }

    @Test
    @DisplayName("GIVEN a non-terminal rehydrated item WHEN state is derived THEN lease presence decides, not the embedded lease")
    public void nonTerminalDerivesFromLeasePresence() {

        WorkItem item = rehydrate(WorkItemState.PENDING).getValue();

        assertThat(item.state(false)).isEqualTo(WorkItemState.PENDING);
        assertThat(item.state(true)).isEqualTo(WorkItemState.ASSIGNED);
    }

    @Test
    @DisplayName("GIVEN a terminal rehydrated item WHEN state is derived THEN it stays terminal regardless of lease presence")
    public void terminalStaysTerminal() {

        WorkItem completed = rehydrate(WorkItemState.COMPLETED).getValue();

        assertThat(completed.state(true)).isEqualTo(WorkItemState.COMPLETED);
        assertThat(completed.state(false)).isEqualTo(WorkItemState.COMPLETED);
    }

    @Test
    @DisplayName("GIVEN a null field WHEN rehydrated THEN it fails and no work item is produced")
    public void failsWhenAnyFieldIsNull() {

        assertThat(WorkItem.rehydrate(null, WorkItemType.PROCESS_AGGREGATE_METADATA, TR,
            WorkItemState.PENDING, CREATED_AT, TRANSITION_AT).isFailure()).isTrue();
        assertThat(WorkItem.rehydrate(ID, null, TR,
            WorkItemState.PENDING, CREATED_AT, TRANSITION_AT).isFailure()).isTrue();
        assertThat(WorkItem.rehydrate(ID, WorkItemType.PROCESS_AGGREGATE_METADATA, null,
            WorkItemState.PENDING, CREATED_AT, TRANSITION_AT).isFailure()).isTrue();
        assertThat(WorkItem.rehydrate(ID, WorkItemType.PROCESS_AGGREGATE_METADATA, TR,
            null, CREATED_AT, TRANSITION_AT).isFailure()).isTrue();
        assertThat(WorkItem.rehydrate(ID, WorkItemType.PROCESS_AGGREGATE_METADATA, TR,
            WorkItemState.PENDING, null, TRANSITION_AT).isFailure()).isTrue();

        Result<WorkItem> result = WorkItem.rehydrate(ID, WorkItemType.PROCESS_AGGREGATE_METADATA, TR,
            WorkItemState.PENDING, CREATED_AT, null);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }
}
