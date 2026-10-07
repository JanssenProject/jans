package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;

import org.junit.jupiter.api.Test;

class Saml2SsoProfileConfigurationRequestJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialisesAllThreeDurationsAndAuthenticationFields() throws Exception {

        String body = "{\"max_authentication_age\":\"PT1H\",\"assertion_lifetime\":\"PT5M\","
            + "\"maximum_sp_session_lifetime\":\"PT8H\",\"post_authentication_flows\":[\"mfa\"],"
            + "\"authentication_result_reuse_policy\":\"DISALLOW_REUSE\"}";

        Saml2SsoProfileConfigurationRequest request =
            mapper.readValue(body, Saml2SsoProfileConfigurationRequest.class);

        assertThat(request.getMaxAuthenticationAge()).isEqualTo("PT1H");
        assertThat(request.getAssertionLifetime()).isEqualTo("PT5M");
        assertThat(request.getMaximumSpSessionLifetime()).isEqualTo("PT8H");
        assertThat(request.getPostAuthenticationFlows()).containsExactly("mfa");
        assertThat(request.getAuthenticationResultReusePolicy()).isEqualTo(AuthenticationResultReusePolicy.DISALLOW_REUSE);
    }

    @Test
    void leavesOmittedFieldsNull() throws Exception {

        Saml2SsoProfileConfigurationRequest request = mapper.readValue(
            "{\"status\":\"ACTIVE\"}", Saml2SsoProfileConfigurationRequest.class);

        assertThat(request.getMaxAuthenticationAge()).isNull();
        assertThat(request.getPostAuthenticationFlows()).isNull();
        assertThat(request.getRequestSigningRequirement()).isNull();
    }

    @Test
    void rejectsUnknownField() {

        String body = "{\"status\":\"ACTIVE\",\"bogus\":\"x\"}";

        assertThatThrownBy(() -> mapper.readValue(body, Saml2SsoProfileConfigurationRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }
}
