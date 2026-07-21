package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.error.InvalidDurationSyntax;
import io.jans.shibboleth.trust.config.profile.Saml2EcpProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.common.EndpointValidationPolicy;
import io.jans.shibboleth.trust.config.profile.common.RequestSigningRequirement;
import io.jans.shibboleth.trust.dto.config.Saml2EcpProfileConfigurationRequest;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class TrustRelationshipSaml2EcpProfileMapperTests {

    @Test
    void shouldParseBothDurations() {

        Saml2EcpProfileConfigurationRequest request = new Saml2EcpProfileConfigurationRequest();
        request.setAssertionLifetime("PT5M");
        request.setMaximumSpSessionLifetime("PT8H");

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2EcpProfileConfiguration(individual(), request);

        assertThat(result.isSuccess()).isTrue();
        Saml2EcpProfileConfiguration updated = result.getValue().getSaml2EcpProfileConfiguration();
        assertThat(updated.getAssertionLifetime()).isEqualTo(Duration.ofMinutes(5));
        assertThat(updated.getMaximumSPSessionLifetime()).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void shouldFailWhenSessionLifetimeIsMalformed() {

        Saml2EcpProfileConfigurationRequest request = new Saml2EcpProfileConfigurationRequest();
        request.setMaximumSpSessionLifetime("eight-hours");

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2EcpProfileConfiguration(individual(), request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(InvalidDurationSyntax.class);
    }

    @Test
    void shouldSetSaml2SsoCapabilityFields() {

        Saml2EcpProfileConfigurationRequest request = new Saml2EcpProfileConfigurationRequest();
        request.setEndpointValidationPolicy(EndpointValidationPolicy.ALWAYS_VALIDATE_ENDPOINT);
        request.setRequestSigningRequirement(RequestSigningRequirement.REQUIRE_SIGNED_REQUESTS);

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2EcpProfileConfiguration(individual(), request);

        assertThat(result.isSuccess()).isTrue();
        Saml2EcpProfileConfiguration updated = result.getValue().getSaml2EcpProfileConfiguration();
        assertThat(updated.getEndpointValidationPolicy()).isEqualTo(EndpointValidationPolicy.ALWAYS_VALIDATE_ENDPOINT);
        assertThat(updated.getRequestSigningRequirement()).isEqualTo(RequestSigningRequirement.REQUIRE_SIGNED_REQUESTS);
    }

    @Test
    void shouldOverrideOnlyTheProvidedField() {

        TrustRelationship existing = individual();
        Saml2EcpProfileConfiguration original = existing.getSaml2EcpProfileConfiguration();

        Saml2EcpProfileConfigurationRequest request = new Saml2EcpProfileConfigurationRequest();
        request.setRequestSigningRequirement(RequestSigningRequirement.ALLOW_UNSIGNED_REQUESTS);

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2EcpProfileConfiguration(existing, request);

        assertThat(result.isSuccess()).isTrue();
        Saml2EcpProfileConfiguration updated = result.getValue().getSaml2EcpProfileConfiguration();
        assertThat(updated.getRequestSigningRequirement()).isEqualTo(RequestSigningRequirement.ALLOW_UNSIGNED_REQUESTS);
        assertThat(updated.getMaximumSPSessionLifetime()).isEqualTo(original.getMaximumSPSessionLifetime());
        assertThat(updated.getStatus()).isEqualTo(original.getStatus());
    }

    @Test
    void shouldLeaveProfileAndVersionUnchangedForEmptyRequest() {

        TrustRelationship existing = individual();

        Result<TrustRelationship> result = TrustRelationshipMapper
            .updateSaml2EcpProfileConfiguration(existing, new Saml2EcpProfileConfigurationRequest());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getSaml2EcpProfileConfiguration())
            .isEqualTo(existing.getSaml2EcpProfileConfiguration());
        assertThat(result.getValue().getVersion()).isEqualTo(existing.getVersion());
    }

    private static TrustRelationship individual() {

        return TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("d"), TrustNature.INDIVIDUAL)
            .getValue();
    }
}
