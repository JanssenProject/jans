package io.jans.shibboleth.trust.dto.mapper.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustRelationship;
import io.jans.shibboleth.trust.config.error.InvalidDurationSyntax;
import io.jans.shibboleth.trust.config.profile.ShibbolethSsoProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.common.AttributeStatementPolicy;
import io.jans.shibboleth.trust.dto.config.ShibbolethSsoProfileConfigurationRequest;
import io.jans.shibboleth.trust.shared.Result;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class TrustRelationshipShibbolethSsoProfileMapperTests {

    @Test
    void shouldParseBothDurations() {

        ShibbolethSsoProfileConfigurationRequest request = new ShibbolethSsoProfileConfigurationRequest();
        request.setMaxAuthenticationAge("PT1H");
        request.setAssertionLifetime("PT5M");

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateShibbolethSsoProfileConfiguration(individual(), request);

        assertThat(result.isSuccess()).isTrue();
        ShibbolethSsoProfileConfiguration updated = result.getValue().getShibbolethSsoProfileConfiguration();
        assertThat(updated.getMaxAuthenticationAge()).isEqualTo(Duration.ofHours(1));
        assertThat(updated.getAssertionLifetime()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldFailWhenMaxAuthenticationAgeIsMalformed() {

        ShibbolethSsoProfileConfigurationRequest request = new ShibbolethSsoProfileConfigurationRequest();
        request.setMaxAuthenticationAge("one-hour");

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateShibbolethSsoProfileConfiguration(individual(), request);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(InvalidDurationSyntax.class);
    }

    @Test
    void shouldSetNameIdFormatPrecedence() {

        ShibbolethSsoProfileConfigurationRequest request = new ShibbolethSsoProfileConfigurationRequest();
        request.setNameIdFormatPrecedence(List.of("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"));

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateShibbolethSsoProfileConfiguration(individual(), request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getShibbolethSsoProfileConfiguration()
                .getNameIdFormatPrecedence().getNameIdentifiers())
            .containsExactly("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
    }

    @Test
    void shouldOverrideOnlyTheProvidedField() {

        TrustRelationship existing = individual();
        ShibbolethSsoProfileConfiguration original = existing.getShibbolethSsoProfileConfiguration();

        ShibbolethSsoProfileConfigurationRequest request = new ShibbolethSsoProfileConfigurationRequest();
        request.setAttributeStatementPolicy(AttributeStatementPolicy.OMIT_ATTRIBUTE_STATEMENT);

        Result<TrustRelationship> result =
            TrustRelationshipMapper.updateShibbolethSsoProfileConfiguration(existing, request);

        assertThat(result.isSuccess()).isTrue();
        ShibbolethSsoProfileConfiguration updated = result.getValue().getShibbolethSsoProfileConfiguration();
        assertThat(updated.getAttributeStatementPolicy()).isEqualTo(AttributeStatementPolicy.OMIT_ATTRIBUTE_STATEMENT);
        assertThat(updated.getMaxAuthenticationAge()).isEqualTo(original.getMaxAuthenticationAge());
        assertThat(updated.getStatus()).isEqualTo(original.getStatus());
    }

    @Test
    void shouldLeaveProfileAndVersionUnchangedForEmptyRequest() {

        TrustRelationship existing = individual();

        Result<TrustRelationship> result = TrustRelationshipMapper
            .updateShibbolethSsoProfileConfiguration(existing, new ShibbolethSsoProfileConfigurationRequest());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getShibbolethSsoProfileConfiguration())
            .isEqualTo(existing.getShibbolethSsoProfileConfiguration());
        assertThat(result.getValue().getVersion()).isEqualTo(existing.getVersion());
    }

    private static TrustRelationship individual() {

        return TrustRelationship
            .create(DisplayName.of("Portal SP").getValue(), Description.of("d"), TrustNature.INDIVIDUAL)
            .getValue();
    }
}
