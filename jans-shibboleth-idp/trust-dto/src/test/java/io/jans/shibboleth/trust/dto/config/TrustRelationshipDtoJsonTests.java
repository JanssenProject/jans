package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustStatus;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Verifies the wire contract: {@code snake_case} JSON keys and verbatim enum values.
 */
class TrustRelationshipDtoJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void summarySerialisesWithSnakeCaseKeysAndVerbatimEnums() throws Exception {

        UUID id = UUID.fromString("7f3a9c2e-4b1d-4c8a-9e2f-1a2b3c4d5e6f");
        TrustRelationshipSummary summary = new TrustRelationshipSummary();
        summary.setId(id);
        summary.setDisplayName("University Portal SP");
        summary.setDescription("SAML SP");
        summary.setNature(TrustNature.INDIVIDUAL);
        summary.setStatus(TrustStatus.DRAFT);
        summary.setVersion(1);

        JsonNode json = mapper.readTree(mapper.writeValueAsString(summary));

        assertThat(iterableFieldNames(json))
            .containsExactlyInAnyOrder("id", "display_name", "description", "nature", "status", "version");
        assertThat(json.get("id").asText()).isEqualTo(id.toString());
        assertThat(json.get("display_name").asText()).isEqualTo("University Portal SP");
        assertThat(json.get("nature").asText()).isEqualTo("INDIVIDUAL");
        assertThat(json.get("status").asText()).isEqualTo("DRAFT");
        assertThat(json.get("version").asInt()).isEqualTo(1);
    }

    @Test
    void createRequestDeserialisesFromSnakeCase() throws Exception {

        String body = "{\"display_name\":\"Portal SP\",\"description\":\"d\",\"nature\":\"AGGREGATE\"}";

        CreateTrustRelationshipRequest request = mapper.readValue(body, CreateTrustRelationshipRequest.class);

        assertThat(request.getDisplayName()).isEqualTo("Portal SP");
        assertThat(request.getDescription()).isEqualTo("d");
        assertThat(request.getNature()).isEqualTo(TrustNature.AGGREGATE);
    }

    @Test
    void createRequestRejectsUnknownField() {

        String body = "{\"display_name\":\"Portal SP\",\"nature\":\"INDIVIDUAL\",\"foo\":\"bar\"}";

        assertThatThrownBy(() -> mapper.readValue(body, CreateTrustRelationshipRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }

    @Test
    void updateBasicInfoRequestDeserialisesFromSnakeCase() throws Exception {

        String body = "{\"display_name\":\"New Name\",\"description\":\"d\"}";

        UpdateBasicInfoRequest request = mapper.readValue(body, UpdateBasicInfoRequest.class);

        assertThat(request.getDisplayName()).isEqualTo("New Name");
        assertThat(request.getDescription()).isEqualTo("d");
    }

    @Test
    void updateBasicInfoRequestLeavesDescriptionNullWhenOmitted() throws Exception {

        String body = "{\"display_name\":\"New Name\"}";

        UpdateBasicInfoRequest request = mapper.readValue(body, UpdateBasicInfoRequest.class);

        assertThat(request.getDisplayName()).isEqualTo("New Name");
        assertThat(request.getDescription()).isNull();
    }

    @Test
    void updateBasicInfoRequestRejectsUnknownField() {

        String body = "{\"display_name\":\"New Name\",\"bogus\":\"x\"}";

        assertThatThrownBy(() -> mapper.readValue(body, UpdateBasicInfoRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }

    private static Iterable<String> iterableFieldNames(JsonNode node) {

        return () -> node.fieldNames();
    }
}
