package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;

import org.junit.jupiter.api.Test;

class Saml2LogoutProfileConfigurationRequestJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialisesFromSnakeCaseWithEnumsAndArrays() throws Exception {

        String body = "{\"status\":\"ACTIVE\",\"inbound_flows\":[\"flow-a\"],"
            + "\"message_signing_policy\":\"SIGN_BOTH\",\"nameid_encryption_policy\":\"ENCRYPT_NAMEIDS\"}";

        Saml2LogoutProfileConfigurationRequest request =
            mapper.readValue(body, Saml2LogoutProfileConfigurationRequest.class);

        assertThat(request.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
        assertThat(request.getInboundFlows()).containsExactly("flow-a");
        assertThat(request.getMessageSigningPolicy()).isEqualTo(MessageSigningPolicy.SIGN_BOTH);
    }

    @Test
    void leavesOmittedFieldsNull() throws Exception {

        Saml2LogoutProfileConfigurationRequest request =
            mapper.readValue("{\"status\":\"INACTIVE\"}", Saml2LogoutProfileConfigurationRequest.class);

        assertThat(request.getStatus()).isEqualTo(ProfileStatus.INACTIVE);
        assertThat(request.getMessageSigningPolicy()).isNull();
        assertThat(request.getInboundFlows()).isNull();
        assertThat(request.getOutboundFlows()).isNull();
    }

    @Test
    void rejectsUnknownField() {

        String body = "{\"status\":\"ACTIVE\",\"bogus\":\"x\"}";

        assertThatThrownBy(() -> mapper.readValue(body, Saml2LogoutProfileConfigurationRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }
}
