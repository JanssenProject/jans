package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.error.InvalidDurationSyntax;
import io.jans.shibboleth.trust.config.profile.Saml2SsoProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.trust.dto.config.Saml2SsoProfileConfigurationRequest;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class TrustRelationshipSaml2SsoProfileMapperTests {

    @Test
    void shouldParseAllThreeDurations() {

        Saml2SsoProfileConfigurationRequest request = new Saml2SsoProfileConfigurationRequest();
        request.setMaxAuthenticationAge("PT1H");
        request.setAssertionLifetime("PT5M");
        request.setMaximumSpSessionLifetime("PT8H");

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2SsoProfileConfiguration(individual(), request);

        assertThat(result.isSuccess()).isTrue();
        Saml2SsoProfileConfiguration updated = result.getValue().getSaml2SsoProfileConfiguration();
        assertThat(updated.getMaxAuthenticationAge()).isEqualTo(Duration.ofHours(1));
        assertThat(updated.getAssertionLifetime()).isEqualTo(Duration.ofMinutes(5));
        assertThat(updated.getMaximumSPSessionLifetime()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void shouldFailWhenAssertionLifetimeIsMalformed() {

        Saml2SsoProfileConfigurationRequest request = new Saml2SsoProfileConfigurationRequest();
        request.setAssertionLifetime("five-minutes");

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2SsoProfileConfiguration(individual(), request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(InvalidDurationSyntax.class);
    }

    @Test
    void shouldSetAuthenticationCapabilityField() {

        Saml2SsoProfileConfigurationRequest request = new Saml2SsoProfileConfigurationRequest();
        request.setAuthenticationResultReusePolicy(AuthenticationResultReusePolicy.DISALLOW_REUSE);

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2SsoProfileConfiguration(individual(), request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getSaml2SsoProfileConfiguration().getAuthenticationResultReusePolicy())
            .isEqualTo(AuthenticationResultReusePolicy.DISALLOW_REUSE);
    }

    @Test
    void shouldOverrideOnlyTheProvidedField() {

        TrustRelationship existing = individual();
        Saml2SsoProfileConfiguration original = existing.getSaml2SsoProfileConfiguration();

        Saml2SsoProfileConfigurationRequest request = new Saml2SsoProfileConfigurationRequest();
        request.setMaxAuthenticationAge("PT2H");

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2SsoProfileConfiguration(existing, request);

        assertThat(result.isSuccess()).isTrue();
        Saml2SsoProfileConfiguration updated = result.getValue().getSaml2SsoProfileConfiguration();
        assertThat(updated.getMaxAuthenticationAge()).isEqualTo(Duration.ofHours(2));
        assertThat(updated.getAssertionLifetime()).isEqualTo(original.getAssertionLifetime());
        assertThat(updated.getStatus()).isEqualTo(original.getStatus());
    }

    @Test
    void shouldLeaveProfileAndVersionUnchangedForEmptyRequest() {

        TrustRelationship existing = individual();

        Result<TrustRelationship> result = TrustRelationshipMapper
            .updateSaml2SsoProfileConfiguration(existing, new Saml2SsoProfileConfigurationRequest());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getSaml2SsoProfileConfiguration())
            .isEqualTo(existing.getSaml2SsoProfileConfiguration());
        assertThat(result.getValue().getVersion()).isEqualTo(existing.getVersion());
    }

    private static TrustRelationship individual() {

        return TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("d"), TrustNature.INDIVIDUAL)
            .getValue();
    }
}
