package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.profile.Saml2LogoutProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.common.MessageSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.dto.config.Saml2LogoutProfileConfigurationRequest;
import io.jans.shibboleth.trust.shared.Result;

import java.util.List;

import org.junit.jupiter.api.Test;

class TrustRelationshipSaml2LogoutProfileMapperTests {

    @Test
    void shouldOverrideOnlyTheProvidedField() {

        TrustRelationship existing = individual();
        Saml2LogoutProfileConfiguration original = existing.getSaml2LogoutProfileConfiguration();

        Saml2LogoutProfileConfigurationRequest request = new Saml2LogoutProfileConfigurationRequest();
        request.setStatus(ProfileStatus.ACTIVE);

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2LogoutProfileConfiguration(existing, request);

        assertThat(result.isSuccess()).isTrue();
        Saml2LogoutProfileConfiguration updated = result.getValue().getSaml2LogoutProfileConfiguration();
        assertThat(updated.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
        assertThat(updated.getMessageSigningPolicy()).isEqualTo(original.getMessageSigningPolicy());
    }

    @Test
    void shouldSetMessageSigningPolicy() {

        Saml2LogoutProfileConfigurationRequest request = new Saml2LogoutProfileConfigurationRequest();
        request.setMessageSigningPolicy(MessageSigningPolicy.SIGN_BOTH);

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2LogoutProfileConfiguration(individual(), request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getSaml2LogoutProfileConfiguration().getMessageSigningPolicy())
            .isEqualTo(MessageSigningPolicy.SIGN_BOTH);
    }

    @Test
    void shouldSetInboundFlows() {

        Saml2LogoutProfileConfigurationRequest request = new Saml2LogoutProfileConfigurationRequest();
        request.setInboundFlows(List.of("flow-a", "flow-b"));

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2LogoutProfileConfiguration(individual(), request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getSaml2LogoutProfileConfiguration().getInboundFlows().getFlows())
            .containsExactly("flow-a", "flow-b");
    }

    @Test
    void shouldLeaveProfileAndVersionUnchangedForEmptyRequest() {

        TrustRelationship existing = individual();

        Result<TrustRelationship> result = TrustRelationshipMapper
            .updateSaml2LogoutProfileConfiguration(existing, new Saml2LogoutProfileConfigurationRequest());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getSaml2LogoutProfileConfiguration())
            .isEqualTo(existing.getSaml2LogoutProfileConfiguration());
        assertThat(result.getValue().getVersion()).isEqualTo(existing.getVersion());
    }

    private static TrustRelationship individual() {

        return TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("d"), TrustNature.INDIVIDUAL)
            .getValue();
    }
}
