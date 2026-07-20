package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.error.InvalidDurationSyntax;
import io.jans.shibboleth.trust.config.profile.Saml2AttributeQueryProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.dto.config.Saml2AttributeQueryProfileConfigurationRequest;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class TrustRelationshipSaml2AttributeQueryProfileMapperTests {

    @Test
    void shouldParseAssertionLifetimeDuration() {

        Saml2AttributeQueryProfileConfigurationRequest request =
            new Saml2AttributeQueryProfileConfigurationRequest();
        request.setAssertionLifetime("PT5M");

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2AttributeQueryProfileConfiguration(individual(), request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getSaml2AttributeQueryProfileConfiguration().getAssertionLifetime())
            .isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldFailWhenAssertionLifetimeIsMalformed() {

        Saml2AttributeQueryProfileConfigurationRequest request =
            new Saml2AttributeQueryProfileConfigurationRequest();
        request.setAssertionLifetime("five-minutes");

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2AttributeQueryProfileConfiguration(individual(), request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(InvalidDurationSyntax.class);
    }

    @Test
    void shouldOverrideOnlyTheProvidedField() {

        TrustRelationship existing = individual();
        Saml2AttributeQueryProfileConfiguration original =
            existing.getSaml2AttributeQueryProfileConfiguration();

        Saml2AttributeQueryProfileConfigurationRequest request =
            new Saml2AttributeQueryProfileConfigurationRequest();
        request.setFriendlyNameRandomizationPolicy(FriendlyNameRandomizationPolicy.RANDOMIZED);

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateSaml2AttributeQueryProfileConfiguration(existing, request);

        assertThat(result.isSuccess()).isTrue();
        Saml2AttributeQueryProfileConfiguration updated =
            result.getValue().getSaml2AttributeQueryProfileConfiguration();
        assertThat(updated.getFriendlyNameRandomizationPolicy()).isEqualTo(FriendlyNameRandomizationPolicy.RANDOMIZED);
        assertThat(updated.getAssertionLifetime()).isEqualTo(original.getAssertionLifetime());
        assertThat(updated.getStatus()).isEqualTo(original.getStatus());
    }

    @Test
    void shouldLeaveProfileAndVersionUnchangedForEmptyRequest() {

        TrustRelationship existing = individual();

        Result<TrustRelationship> result = TrustRelationshipMapper
            .updateSaml2AttributeQueryProfileConfiguration(
                existing, new Saml2AttributeQueryProfileConfigurationRequest());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getSaml2AttributeQueryProfileConfiguration())
            .isEqualTo(existing.getSaml2AttributeQueryProfileConfiguration());
        assertThat(result.getValue().getVersion()).isEqualTo(existing.getVersion());
    }

    private static TrustRelationship individual() {

        return TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("d"), TrustNature.INDIVIDUAL)
            .getValue();
    }
}
