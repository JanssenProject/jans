package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class UpdateReleasedAttributesRequestJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialisesAttributesFromSnakeCase() throws Exception {

        String body = "{\"attributes\":[{\"id\":\"11111111-2222-3333-4444-555555555555\","
            + "\"display_name\":\"givenName\"}]}";

        UpdateReleasedAttributesRequest request = mapper.readValue(body, UpdateReleasedAttributesRequest.class);

        assertThat(request.getAttributes()).hasSize(1);
        assertThat(request.getAttributes().get(0).getId())
            .isEqualTo(UUID.fromString("11111111-2222-3333-4444-555555555555"));
        assertThat(request.getAttributes().get(0).getDisplayName()).isEqualTo("givenName");
    }

    @Test
    void deserialisesEmptyAttributeList() throws Exception {

        UpdateReleasedAttributesRequest request =
            mapper.readValue("{\"attributes\":[]}", UpdateReleasedAttributesRequest.class);

        assertThat(request.getAttributes()).isEmpty();
    }

    @Test
    void rejectsUnknownFieldOnAnItem() {

        String body = "{\"attributes\":[{\"id\":\"11111111-2222-3333-4444-555555555555\","
            + "\"display_name\":\"givenName\",\"bogus\":\"x\"}]}";

        assertThatThrownBy(() -> mapper.readValue(body, UpdateReleasedAttributesRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }
}
