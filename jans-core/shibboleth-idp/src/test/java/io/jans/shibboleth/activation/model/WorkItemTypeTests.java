package io.jans.shibboleth.activation.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.activation.model.WorkItemType.*;

@DisplayName("Group 2 — WorkItem Enums: WorkItemType")
public class WorkItemTypeTests {

    @Test
    @DisplayName("GIVEN WorkItemType WHEN its values are inspected THEN both PROCESS_AGGREGATE_METADATA and PROCESS_INDIVIDUAL_METADATA are present")
    public void shouldExposeAggregateAndIndividualTypes() {

        assertThat(WorkItemType.values())
            .contains(PROCESS_AGGREGATE_METADATA, PROCESS_INDIVIDUAL_METADATA);
    }
}
