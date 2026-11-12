package io.jans.staging.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class ClaimResultJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serialisesSnakeCaseFields() throws Exception {

        ClaimResult result = new ClaimResult("/opt/shibboleth-idp/metadata/tok-1.xml", 19L, "text/xml", "9f86d081");

        JsonNode node = mapper.readTree(mapper.writeValueAsString(result));

        assertThat(node.get("handle").asText()).isEqualTo("/opt/shibboleth-idp/metadata/tok-1.xml");
        assertThat(node.get("size").asLong()).isEqualTo(19L);
        assertThat(node.get("content_type").asText()).isEqualTo("text/xml");
        assertThat(node.get("sha256").asText()).isEqualTo("9f86d081");
    }

    @Test
    void omitsAbsentContentType() throws Exception {

        ClaimResult result = new ClaimResult("/opt/shibboleth-idp/metadata/tok-1.bin", 19L, null, "9f86d081");

        JsonNode node = mapper.readTree(mapper.writeValueAsString(result));

        assertThat(node.has("content_type")).isFalse();
    }
}
