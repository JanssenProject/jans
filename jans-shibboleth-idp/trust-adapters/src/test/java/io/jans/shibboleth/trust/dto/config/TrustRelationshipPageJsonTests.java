package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.dto.shared.PageMetadata;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Verifies the list wire contract: {@code items} + a nested {@code page} metadata object, all in
 * snake_case, and summary items in snake_case.
 */
class TrustRelationshipPageJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void pageSerialisesWithItemsAndNestedPageMetadata() throws Exception {

        TrustRelationshipSummary summary = new TrustRelationshipSummary();
        summary.setId(UUID.fromString("7f3a9c2e-4b1d-4c8a-9e2f-1a2b3c4d5e6f"));
        summary.setDisplayName("University Portal SP");
        summary.setDescription("SAML SP");
        summary.setNature(TrustNature.INDIVIDUAL);
        summary.setStatus(TrustStatus.DRAFT);
        summary.setVersion(1);

        TrustRelationshipPage page = new TrustRelationshipPage(
            List.of(summary), new PageMetadata(20, 1, 145, 8, 1));

        JsonNode json = mapper.readTree(mapper.writeValueAsString(page));

        assertThat(fieldNames(json)).containsExactlyInAnyOrder("items", "page");

        JsonNode meta = json.get("page");
        assertThat(fieldNames(meta)).containsExactlyInAnyOrder(
            "size", "number", "total_elements", "total_pages", "number_of_elements");
        assertThat(meta.get("number").asInt()).isEqualTo(1);
        assertThat(meta.get("total_elements").asLong()).isEqualTo(145);
        assertThat(meta.get("total_pages").asInt()).isEqualTo(8);
        assertThat(meta.get("number_of_elements").asInt()).isEqualTo(1);

        JsonNode item = json.get("items").get(0);
        assertThat(fieldNames(item)).containsExactlyInAnyOrder(
            "id", "display_name", "description", "nature", "status", "version");
        assertThat(item.get("display_name").asText()).isEqualTo("University Portal SP");
    }

    private static Iterable<String> fieldNames(JsonNode node) {

        return () -> node.fieldNames();
    }
}
