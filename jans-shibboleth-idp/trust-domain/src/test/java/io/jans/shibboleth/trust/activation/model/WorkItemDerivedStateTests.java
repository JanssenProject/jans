package io.jans.shibboleth.trust.activation.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The derived-state accessor {@code state(hasLiveLease)}: terminal states win over any lease; a non-terminal
 * item is {@code ASSIGNED} exactly when a live lease exists, {@code PENDING} otherwise.
 */
@DisplayName("WorkItem — state derived from live-lease presence")
public class WorkItemDerivedStateTests {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static WorkItem pending() {

        return WorkItem.create(WorkItemType.PROCESS_AGGREGATE_METADATA,
            TrustRelationshipRef.of(UUID.randomUUID()).getValue(), NOW).getValue();
    }

    @Test
    @DisplayName("GIVEN a non-terminal item WHEN a live lease exists THEN its derived state is ASSIGNED")
    public void nonTerminalWithLiveLeaseIsAssigned() {

        assertThat(pending().state(true)).isEqualTo(WorkItemState.ASSIGNED);
    }

    @Test
    @DisplayName("GIVEN a non-terminal item WHEN no live lease exists THEN its derived state is PENDING")
    public void nonTerminalWithoutLiveLeaseIsPending() {

        assertThat(pending().state(false)).isEqualTo(WorkItemState.PENDING);
    }

    @Test
    @DisplayName("GIVEN a COMPLETED item WHEN state is derived THEN it stays COMPLETED regardless of lease presence")
    public void completedIsTerminalRegardlessOfLease() {

        WorkItem completed = pending().complete(NOW).getValue();

        assertThat(completed.state(true)).isEqualTo(WorkItemState.COMPLETED);
        assertThat(completed.state(false)).isEqualTo(WorkItemState.COMPLETED);
    }

    @Test
    @DisplayName("GIVEN a CANCELLED item WHEN state is derived THEN it stays CANCELLED regardless of lease presence")
    public void cancelledIsTerminalRegardlessOfLease() {

        WorkItem cancelled = pending().cancel(NOW).getValue();

        assertThat(cancelled.state(true)).isEqualTo(WorkItemState.CANCELLED);
        assertThat(cancelled.state(false)).isEqualTo(WorkItemState.CANCELLED);
    }
}
