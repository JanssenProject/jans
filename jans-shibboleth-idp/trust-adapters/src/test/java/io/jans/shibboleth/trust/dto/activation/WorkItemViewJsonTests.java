package io.jans.shibboleth.trust.dto.activation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.activation.model.WorkItemState;
import io.jans.shibboleth.trust.activation.model.WorkItemType;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class WorkItemViewJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serialisesWithSnakeCaseKeysAndVerbatimEnums() throws Exception {

        WorkItemView view = new WorkItemView(
            UUID.fromString("7f3a9c2e-4b1d-4c8a-9e2f-1a2b3c4d5e6f"),
            WorkItemType.PROCESS_INDIVIDUAL_METADATA,
            UUID.fromString("11111111-2222-3333-4444-555555555555"),
            WorkItemState.ASSIGNED,
            "2027-01-01T00:05:00Z");

        JsonNode json = mapper.readTree(mapper.writeValueAsString(view));

        assertThat(fieldNames(json)).containsExactlyInAnyOrder(
            "id", "type", "trust_relationship_ref", "state", "lease_expires_at");
        assertThat(json.get("type").asText()).isEqualTo("PROCESS_INDIVIDUAL_METADATA");
        assertThat(json.get("state").asText()).isEqualTo("ASSIGNED");
        assertThat(json.get("lease_expires_at").asText()).isEqualTo("2027-01-01T00:05:00Z");
    }

    @Test
    void omitsLeaseExpiresAtWhenAbsent() throws Exception {

        WorkItemView view = new WorkItemView(
            UUID.fromString("7f3a9c2e-4b1d-4c8a-9e2f-1a2b3c4d5e6f"),
            WorkItemType.PROCESS_AGGREGATE_METADATA,
            UUID.fromString("11111111-2222-3333-4444-555555555555"),
            WorkItemState.PENDING,
            null);

        JsonNode json = mapper.readTree(mapper.writeValueAsString(view));

        assertThat(json.has("lease_expires_at")).isFalse();
        assertThat(json.get("state").asText()).isEqualTo("PENDING");
    }

    private static Iterable<String> fieldNames(JsonNode node) {

        return () -> node.fieldNames();
    }
}
