package io.jans.staging.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class ClaimRequestJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialisesDestination() throws Exception {

        ClaimRequest request =
            mapper.readValue("{\"destination\":\"/opt/shibboleth-idp/metadata/\"}", ClaimRequest.class);

        assertThat(request.getDestination()).isEqualTo("/opt/shibboleth-idp/metadata/");
    }

    @Test
    void rejectsUnknownField() {

        assertThatThrownBy(() ->
            mapper.readValue("{\"destination\":\"/x/\",\"bogus\":1}", ClaimRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }
}
