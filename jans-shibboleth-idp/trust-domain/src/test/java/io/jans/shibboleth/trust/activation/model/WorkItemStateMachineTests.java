package io.jans.shibboleth.trust.activation.model;

import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.activation.error.WorkItemTransitionNotAllowed;
import io.jans.shibboleth.trust.shared.Result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.trust.activation.model.WorkItemType.PROCESS_AGGREGATE_METADATA;

/**
 * The work item's own transitions — the terminal ones, {@code complete} and {@code cancel}. Claiming,
 * heartbeating and reclaiming are lease operations owned by the orchestrator (see the orchestrator and
 * lease tests), not the work item, so they are absent here. The work item only guards that a terminal
 * transition is reached at most once; whether it was assigned is the orchestrator's concern.
 */
@DisplayName("Group 5 — WorkItem terminal transitions")
public class WorkItemStateMachineTests {

    private static final TrustRelationshipRef TR_REF = TrustRelationshipRef.of(UUID.randomUUID()).getValue();
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static WorkItem pending() {

        return WorkItem.create(PROCESS_AGGREGATE_METADATA, TR_REF, NOW).getValue();
    }

    private static WorkItem completed() {

        return pending().complete(NOW).getValue();
    }

    private static WorkItem cancelled() {

        return pending().cancel(NOW).getValue();
    }

    static Stream<WorkItem> terminalWorkItems() {

        return Stream.of(completed(), cancelled());
    }

    @Test
    @DisplayName("GIVEN a non-terminal WorkItem WHEN it is completed THEN it becomes COMPLETED which is terminal")
    public void shouldTransitionToCompleted() {

        WorkItem completed = pending().complete(NOW).getValue();

        assertThat(completed.state()).isEqualTo(WorkItemState.COMPLETED);
        assertThat(completed.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("GIVEN a non-terminal WorkItem WHEN it is cancelled THEN it becomes CANCELLED which is terminal")
    public void shouldTransitionToCancelled() {

        WorkItem cancelled = pending().cancel(NOW).getValue();

        assertThat(cancelled.state()).isEqualTo(WorkItemState.CANCELLED);
        assertThat(cancelled.isTerminal()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.activation.model.WorkItemStateMachineTests#terminalWorkItems")
    @DisplayName("GIVEN a terminal WorkItem WHEN completion is attempted THEN it fails and the item is unchanged")
    public void shouldFailComplete_whenTerminal(WorkItem terminal) {

        WorkItemState before = terminal.state();

        Result<WorkItem> result = terminal.complete(NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(WorkItemTransitionNotAllowed.class);
        assertThat(terminal.state()).isEqualTo(before);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.activation.model.WorkItemStateMachineTests#terminalWorkItems")
    @DisplayName("GIVEN a terminal WorkItem WHEN cancellation is attempted THEN it fails and the item is unchanged")
    public void shouldFailCancel_whenTerminal(WorkItem terminal) {

        WorkItemState before = terminal.state();

        Result<WorkItem> result = terminal.cancel(NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(WorkItemTransitionNotAllowed.class);
        assertThat(terminal.state()).isEqualTo(before);
    }

    @Test
    @DisplayName("GIVEN a WorkItem WHEN the same terminal transition is applied twice THEN the second fails so a terminal state is reached at most once")
    public void shouldReachTerminalAtMostOnce() {

        assertThat(completed().complete(NOW).isFailure()).isTrue();
        assertThat(cancelled().cancel(NOW).isFailure()).isTrue();
    }

    @Test
    @DisplayName("GIVEN a non-terminal WorkItem WHEN completion is attempted with a null instant THEN it fails and no transition occurs")
    public void shouldFailComplete_whenNowIsNull() {

        Result<WorkItem> result = pending().complete(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }

    @Test
    @DisplayName("GIVEN a non-terminal WorkItem WHEN cancellation is attempted with a null instant THEN it fails and no transition occurs")
    public void shouldFailCancel_whenNowIsNull() {

        Result<WorkItem> result = pending().cancel(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(RequiredValueMissing.class);
    }
}
