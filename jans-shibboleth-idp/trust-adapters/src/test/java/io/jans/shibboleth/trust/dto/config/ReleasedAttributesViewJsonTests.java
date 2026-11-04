package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ReleasedAttributesViewJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serialisesAttributesArrayWithSnakeCase() throws Exception {

        ReleasedAttributesView view = new ReleasedAttributesView(List.of(
            new ReleasedAttributeDto(UUID.fromString("11111111-2222-3333-4444-555555555555"), "givenName")));

        JsonNode json = mapper.readTree(mapper.writeValueAsString(view));

        assertThat(json.get("attributes")).hasSize(1);
        JsonNode item = json.get("attributes").get(0);
        assertThat(item.get("id").asText()).isEqualTo("11111111-2222-3333-4444-555555555555");
        assertThat(item.get("display_name").asText()).isEqualTo("givenName");
    }

    @Test
    void serialisesEmptyArray() throws Exception {

        JsonNode json = mapper.readTree(mapper.writeValueAsString(new ReleasedAttributesView(List.of())));

        assertThat(json.get("attributes")).isEmpty();
    }
}
