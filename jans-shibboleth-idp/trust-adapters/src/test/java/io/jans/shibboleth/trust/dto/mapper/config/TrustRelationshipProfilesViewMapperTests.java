package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;
import io.jans.shibboleth.trust.dto.config.ProfilesView;
import io.jans.shibboleth.trust.dto.config.Saml2SsoProfileConfigurationRequest;

import java.util.Set;

import org.junit.jupiter.api.Test;

class TrustRelationshipProfilesViewMapperTests {

    @Test
    void shouldIncludeAllProfilesWhenNoFilter() {

        ProfilesView view = TrustRelationshipMapper.toProfilesView(individual(), null);

        assertThat(view.getShibbolethSso()).isNotNull();
        assertThat(view.getSaml2Sso()).isNotNull();
        assertThat(view.getSaml2ArtifactResolution()).isNotNull();
        assertThat(view.getSaml2AttributeQuery()).isNotNull();
        assertThat(view.getSaml2Ecp()).isNotNull();
        assertThat(view.getSaml2Logout()).isNotNull();
    }

    @Test
    void shouldIncludeOnlyRequestedProfiles() {

        ProfilesView view = TrustRelationshipMapper.toProfilesView(
            individual(), Set.of(ProfileType.SAML2_SSO, ProfileType.SAML2_LOGOUT));

        assertThat(view.getSaml2Sso()).isNotNull();
        assertThat(view.getSaml2Logout()).isNotNull();
        assertThat(view.getShibbolethSso()).isNull();
        assertThat(view.getSaml2Ecp()).isNull();
        assertThat(view.getSaml2ArtifactResolution()).isNull();
        assertThat(view.getSaml2AttributeQuery()).isNull();
    }

    @Test
    void shouldReflectConfiguredValues() {

        Saml2SsoProfileConfigurationRequest request = new Saml2SsoProfileConfigurationRequest();
        request.setStatus(ProfileStatus.ACTIVE);
        request.setAssertionLifetime("PT5M");
        TrustRelationship tr =
            TrustRelationshipMapper.updateSaml2SsoProfileConfiguration(individual(), request).getValue();

        ProfilesView view = TrustRelationshipMapper.toProfilesView(tr, Set.of(ProfileType.SAML2_SSO));

        assertThat(view.getSaml2Sso().getStatus()).isEqualTo(ProfileStatus.ACTIVE);
        assertThat(view.getSaml2Sso().getAssertionLifetime()).isEqualTo("PT5M");
    }

    @Test
    void shouldExposeDefaultDurationsAsIsoStrings() {

        ProfilesView view = TrustRelationshipMapper.toProfilesView(individual(), Set.of(ProfileType.SHIBBOLETH_SSO));

        assertThat(view.getShibbolethSso().getMaxAuthenticationAge()).startsWith("PT");
        assertThat(view.getShibbolethSso().getAssertionLifetime()).startsWith("PT");
    }

    private static TrustRelationship individual() {

        return TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("d"), TrustNature.INDIVIDUAL)
            .getValue();
    }
}
