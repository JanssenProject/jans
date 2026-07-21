package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.profile.Saml2ArtifactResolutionProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.common.AssertionSigningPolicy;
import io.jans.shibboleth.trust.config.profile.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.dto.config.Saml2ArtifactResolutionProfileConfigurationRequest;
import io.jans.shibboleth.trust.shared.Result;

import org.junit.jupiter.api.Test;

class TrustRelationshipSaml2ArtifactResolutionProfileMapperTests {

    @Test
    void shouldOverrideOnlyTheProvidedField() {

        TrustRelationship existing = individual();
        Saml2ArtifactResolutionProfileConfiguration original =
            existing.getSaml2ArtifactResolutionProfileConfiguration();

        Saml2ArtifactResolutionProfileConfigurationRequest request =
            new Saml2ArtifactResolutionProfileConfigurationRequest();
        request.setAssertionSigningPolicy(AssertionSigningPolicy.SIGN_ASSERTIONS);

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2ArtifactResolutionProfileConfiguration(existing, request);

        assertThat(result.isSuccess()).isTrue();
        Saml2ArtifactResolutionProfileConfiguration updated =
            result.getValue().getSaml2ArtifactResolutionProfileConfiguration();
        assertThat(updated.getAssertionSigningPolicy()).isEqualTo(AssertionSigningPolicy.SIGN_ASSERTIONS);
        assertThat(updated.getStatus()).isEqualTo(original.getStatus());
        assertThat(updated.getAttributeEncryptionPolicy()).isEqualTo(original.getAttributeEncryptionPolicy());
    }

    @Test
    void shouldSetMultipleFields() {

        Saml2ArtifactResolutionProfileConfigurationRequest request =
            new Saml2ArtifactResolutionProfileConfigurationRequest();
        request.setStatus(ProfileStatus.ACTIVE);
        request.setAttributeEncryptionPolicy(AttributeEncryptionPolicy.ENCRYPT_ATTRIBUTES);

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2ArtifactResolutionProfileConfiguration(individual(), request);

        assertThat(result.isSuccess()).isTrue();
        Saml2ArtifactResolutionProfileConfiguration updated =
            result.getValue().getSaml2ArtifactResolutionProfileConfiguration();
        assertThat(updated.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
        assertThat(updated.getAttributeEncryptionPolicy()).isEqualTo(AttributeEncryptionPolicy.ENCRYPT_ATTRIBUTES);
    }

    @Test
    void shouldLeaveProfileAndVersionUnchangedForEmptyRequest() {

        TrustRelationship existing = individual();

        Result<TrustRelationship> result = TrustRelationshipMapper
            .updateSaml2ArtifactResolutionProfileConfiguration(
                existing, new Saml2ArtifactResolutionProfileConfigurationRequest());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getSaml2ArtifactResolutionProfileConfiguration())
            .isEqualTo(existing.getSaml2ArtifactResolutionProfileConfiguration());
        assertThat(result.getValue().getVersion()).isEqualTo(existing.getVersion());
    }

    private static TrustRelationship individual() {

        return TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("d"), TrustNature.INDIVIDUAL)
            .getValue();
    }
}
