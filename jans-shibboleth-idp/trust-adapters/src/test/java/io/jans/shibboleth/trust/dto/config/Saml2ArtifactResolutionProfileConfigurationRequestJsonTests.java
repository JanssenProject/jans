package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.config.profile.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;

import org.junit.jupiter.api.Test;

class Saml2ArtifactResolutionProfileConfigurationRequestJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserialisesFromSnakeCaseWithOwnEnums() throws Exception {

        String body = "{\"status\":\"ACTIVE\",\"assertion_signing_policy\":\"SIGN_ASSERTIONS\","
            + "\"assertion_encryption_policy\":\"ENCRYPT_ASSERTIONS\"}";

        Saml2ArtifactResolutionProfileConfigurationRequest request =
            mapper.readValue(body, Saml2ArtifactResolutionProfileConfigurationRequest.class);

        assertThat(request.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
        assertThat(request.getAssertionSigningPolicy()).isEqualTo(AssertionSigningPolicy.SIGN_ASSERTIONS);
        assertThat(request.getAssertionEncryptionPolicy()).isEqualTo(AssertionEncryptionPolicy.ENCRYPT_ASSERTIONS);
    }

    @Test
    void leavesOmittedFieldsNull() throws Exception {

        Saml2ArtifactResolutionProfileConfigurationRequest request = mapper.readValue(
            "{\"status\":\"INACTIVE\"}", Saml2ArtifactResolutionProfileConfigurationRequest.class);

        assertThat(request.getStatus()).isEqualTo(ProfileStatus.INACTIVE);
        assertThat(request.getAssertionSigningPolicy()).isNull();
        assertThat(request.getAttributeEncryptionPolicy()).isNull();
    }

    @Test
    void rejectsUnknownField() {

        String body = "{\"status\":\"ACTIVE\",\"bogus\":\"x\"}";

        assertThatThrownBy(() -> mapper.readValue(body, Saml2ArtifactResolutionProfileConfigurationRequest.class))
            .isInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }
}
