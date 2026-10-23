package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.config.metadata.MetadataSourceType;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationStatus;
import io.jans.shibboleth.trust.shared.diagnostics.LogLevel;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Verifies the detail wire contract: snake_case keys throughout the nested graph, verbatim enums,
 * and ISO-8601 date-time strings (no Java-time Jackson module required).
 */
class TrustRelationshipDetailJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void detailSerialisesWithSnakeCaseKeysAndNestedStructures() throws Exception {

        TrustRelationshipDetail detail = new TrustRelationshipDetail();
        detail.setId(UUID.fromString("7f3a9c2e-4b1d-4c8a-9e2f-1a2b3c4d5e6f"));
        detail.setDisplayName("University Portal SP");
        detail.setDescription("SAML SP");
        detail.setNature(TrustNature.AGGREGATE);
        detail.setStatus(TrustStatus.ACTIVE);
        detail.setVersion(3);
        detail.setMetadataSource(new MetadataSourceSummary(MetadataSourceType.URI));
        detail.setProfiles(List.of(new ProfileSummary(ProfileType.SAML2_SSO, ProfileStatus.ACTIVE)));
        detail.setReleasedAttributes(List.of(
            new ReleasedAttributeDto(UUID.fromString("11111111-2222-3333-4444-555555555555"), "givenName")));
        detail.setActivationDiagnostics(new ActivationDiagnosticsDto(
            ActivationStatus.SUCCEEDED, "worker-1@host", "2026-07-19T10:15:00Z", "2026-07-19T10:15:30Z",
            List.of(new ActivationLogEntryDto("2026-07-19T10:15:10Z", LogLevel.INFO, "metadata processed"))));
        detail.setDiscoveredEntityIds(List.of("https://sp.example.org/shibboleth"));

        JsonNode json = mapper.readTree(mapper.writeValueAsString(detail));

        assertThat(fieldNames(json)).containsExactlyInAnyOrder(
            "id", "display_name", "description", "nature", "status", "version",
            "metadata_source", "profiles", "released_attributes", "activation_diagnostics",
            "discovered_entity_ids");

        assertThat(json.get("nature").asText()).isEqualTo("AGGREGATE");
        assertThat(json.get("metadata_source").get("type").asText()).isEqualTo("URI");

        JsonNode profile = json.get("profiles").get(0);
        assertThat(profile.get("type").asText()).isEqualTo("SAML2_SSO");
        assertThat(profile.get("status").asText()).isEqualTo("ACTIVE");

        JsonNode attribute = json.get("released_attributes").get(0);
        assertThat(fieldNames(attribute)).containsExactlyInAnyOrder("id", "display_name");
        assertThat(attribute.get("display_name").asText()).isEqualTo("givenName");

        JsonNode diagnostics = json.get("activation_diagnostics");
        assertThat(fieldNames(diagnostics)).containsExactlyInAnyOrder(
            "status", "origin", "started_at", "completed_at", "log_entries");
        assertThat(diagnostics.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(diagnostics.get("started_at").asText()).isEqualTo("2026-07-19T10:15:00Z");

        JsonNode logEntry = diagnostics.get("log_entries").get(0);
        assertThat(fieldNames(logEntry)).containsExactlyInAnyOrder("timestamp", "level", "message");
        assertThat(logEntry.get("level").asText()).isEqualTo("INFO");

        assertThat(json.get("discovered_entity_ids").get(0).asText())
            .isEqualTo("https://sp.example.org/shibboleth");
    }

    private static Iterable<String> fieldNames(JsonNode node) {

        return () -> node.fieldNames();
    }
}
