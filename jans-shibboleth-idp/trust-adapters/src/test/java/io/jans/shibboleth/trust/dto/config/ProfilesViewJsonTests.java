package io.jans.shibboleth.trust.dto.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;

import org.junit.jupiter.api.Test;

class ProfilesViewJsonTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serialisesOnlyPresentProfilesAsKeys() throws Exception {

        Saml2LogoutProfileConfigurationView logout = new Saml2LogoutProfileConfigurationView();
        logout.setStatus(ProfileStatus.ACTIVE);
        logout.setMessageSigningPolicy(MessageSigningPolicy.SIGN_BOTH);

        ProfilesView view = new ProfilesView();
        view.setSaml2Logout(logout);

        JsonNode json = mapper.readTree(mapper.writeValueAsString(view));

        assertThat(fieldNames(json)).containsExactly("saml2_logout");
        JsonNode l = json.get("saml2_logout");
        assertThat(l.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(l.get("message_signing_policy").asText()).isEqualTo("SIGN_BOTH");
    }

    @Test
    void serialisesDurationAsString() throws Exception {

        Saml2SsoProfileConfigurationView sso = new Saml2SsoProfileConfigurationView();
        sso.setAssertionLifetime("PT5M");

        ProfilesView view = new ProfilesView();
        view.setSaml2Sso(sso);

        JsonNode json = mapper.readTree(mapper.writeValueAsString(view));

        assertThat(json.get("saml2_sso").get("assertion_lifetime").asText()).isEqualTo("PT5M");
    }

    private static Iterable<String> fieldNames(JsonNode node) {

        return () -> node.fieldNames();
    }
}
