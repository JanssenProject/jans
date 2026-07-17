package io.jans.shibboleth.trust.activation.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.trust.activation.model.WorkItemState.*;

@DisplayName("Group 2 — WorkItem Enums: WorkItemState")
public class WorkItemStateTests {

    @Test
    @DisplayName("GIVEN WorkItemState WHEN its values are inspected THEN PENDING and ASSIGNED and COMPLETED and CANCELLED are all present")
    public void shouldExposeAllFourStates() {

        assertThat(WorkItemState.values())
            .contains(PENDING, ASSIGNED, COMPLETED, CANCELLED);
    }

    @Test
    @DisplayName("GIVEN the WorkItem states WHEN terminality is queried THEN COMPLETED and CANCELLED are terminal while PENDING and ASSIGNED are not")
    public void shouldClassifyTerminalStates() {

        assertThat(COMPLETED.isTerminal()).isTrue();
        assertThat(CANCELLED.isTerminal()).isTrue();
        assertThat(PENDING.isTerminal()).isFalse();
        assertThat(ASSIGNED.isTerminal()).isFalse();
    }
}
