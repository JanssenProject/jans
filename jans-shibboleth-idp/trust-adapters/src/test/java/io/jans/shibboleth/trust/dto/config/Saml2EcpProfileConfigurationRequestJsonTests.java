package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.config.profile.common.EndpointValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.RequestSigningRequirement;

import org.junit.jupiter.api.Test;

class Saml2EcpProfileConfigurationRequestJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialisesDurationsAndSaml2SsoEnums() throws Exception {

        String body = "{\"assertion_lifetime\":\"PT5M\",\"maximum_sp_session_lifetime\":\"PT8H\","
            + "\"endpoint_validation_policy\":\"ALWAYS_VALIDATE_ENDPOINT\","
            + "\"request_signing_requirement\":\"REQUIRE_SIGNED_REQUESTS\","
            + "\"nameid_format_precedence\":[\"urn:a\"]}";

        Saml2EcpProfileConfigurationRequest request =
            mapper.readValue(body, Saml2EcpProfileConfigurationRequest.class);

        assertThat(request.getAssertionLifetime()).isEqualTo("PT5M");
        assertThat(request.getMaximumSpSessionLifetime()).isEqualTo("PT8H");
        assertThat(request.getEndpointValidationPolicy()).isEqualTo(EndpointValidationPolicy.ALWAYS_VALIDATE_ENDPOINT);
        assertThat(request.getRequestSigningRequirement()).isEqualTo(RequestSigningRequirement.REQUIRE_SIGNED_REQUESTS);
        assertThat(request.getNameIdFormatPrecedence()).containsExactly("urn:a");
    }

    @Test
    void leavesOmittedFieldsNull() throws Exception {

        Saml2EcpProfileConfigurationRequest request = mapper.readValue(
            "{\"status\":\"ACTIVE\"}", Saml2EcpProfileConfigurationRequest.class);

        assertThat(request.getMaximumSpSessionLifetime()).isNull();
        assertThat(request.getEndpointValidationPolicy()).isNull();
        assertThat(request.getRequestSigningRequirement()).isNull();
    }

    @Test
    void rejectsUnknownField() {

        String body = "{\"status\":\"ACTIVE\",\"bogus\":\"x\"}";

        assertThatThrownBy(() -> mapper.readValue(body, Saml2EcpProfileConfigurationRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }
}
