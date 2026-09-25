package io.jans.staging.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class StagedFileViewJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serialisesSnakeCaseFields() throws Exception {

        StagedFileView view = new StagedFileView("tok-1", 19L, "application/samlmetadata+xml",
            "9f86d081", "2026-01-01T00:10:00Z");

        JsonNode node = mapper.readTree(mapper.writeValueAsString(view));

        assertThat(node.get("token").asText()).isEqualTo("tok-1");
        assertThat(node.get("size").asLong()).isEqualTo(19L);
        assertThat(node.get("content_type").asText()).isEqualTo("application/samlmetadata+xml");
        assertThat(node.get("sha256").asText()).isEqualTo("9f86d081");
        assertThat(node.get("expires_at").asText()).isEqualTo("2026-01-01T00:10:00Z");
    }

    @Test
    void omitsAbsentContentType() throws Exception {

        StagedFileView view = new StagedFileView("tok-1", 19L, null, "9f86d081", "2026-01-01T00:10:00Z");

        JsonNode node = mapper.readTree(mapper.writeValueAsString(view));

        assertThat(node.has("content_type")).isFalse();
    }
}
