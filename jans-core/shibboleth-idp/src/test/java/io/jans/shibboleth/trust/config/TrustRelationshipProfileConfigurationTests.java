package io.jans.shibboleth.trust.config;

import io.jans.shibboleth.trust.config.*;
import io.jans.shibboleth.trust.config.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.config.diagnostics.ActivationStatus;
import io.jans.shibboleth.trust.config.error.*;
import io.jans.shibboleth.trust.config.metadata.MetadataSource;
import io.jans.shibboleth.trust.config.metadata.MetadataSourceType;
import io.jans.shibboleth.trust.config.metadata.NoMetadataSource;
import io.jans.shibboleth.trust.config.profile.*;
import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.shared.Result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.trust.config.TrustRelationshipAssert.assertThat;
import static io.jans.shibboleth.trust.config.profile.ProfileConfigurationAssert.assertThat;
import static io.jans.shibboleth.trust.config.TrustRelationshipFixtures.*;

@DisplayName("Profile Configuration Fundamentals")
public class TrustRelationshipProfileConfigurationTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsWithProfileConfigurationsAndAccessors")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship with no metadatasources " + 
        "WHEN updateXXXProfileConfiguration() is called " +
        "THEN the operation updates the profile configuration and maintains the DRAFT status "
    )
    public void shouldUpdateProfileConfigurationAndStayInDraft_whenNoMetadataSource(TrustRelationship tr, Object profileconfig,ProfileConfigurationAccessor accessor) {


        assertThat(tr).isInDraftStatus();
        assertThat(tr).hasNoRealMetadataSource();

        Result<TrustRelationship> result = accessor.update(tr, profileconfig);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(accessor.extract(updated)).isEqualTo(profileconfig);
        assertThat(updated).isInDraftStatus();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsWithProfileConfigurationsAndAccessors")
    @DisplayName(
        "GIVEN a DRAFT trust relationship with all profiles disabled " +
        "WHEN a profile configuration is enabled " +
        "THEN that profile becomes ACTIVE and at least one profile is active"
    )
    public void shouldMarkProfileActive_whenProfileEnabled(TrustRelationship tr, Object activeConfig, ProfileConfigurationAccessor accessor) {

        assertThat(tr).hasNoActiveProfileConfiguration();

        Result<TrustRelationship> result = accessor.update(tr, activeConfig);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(accessor.getStatus(updated)).isEqualTo(ProfileStatus.ACTIVE);
        assertThat(updated).hasAtLeastOneActiveProfileConfiguration();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsWithProfileConfigurationsAndAccessors")
    @DisplayName(
        "GIVEN a DRAFT trust relationship " +
        "WHEN a profile configuration is enabled " +
        "THEN the version is incremented"
    )
    public void shouldIncrementVersion_whenProfileEnabled(TrustRelationship tr, Object activeConfig, ProfileConfigurationAccessor accessor) {

        Result<TrustRelationship> result = accessor.update(tr, activeConfig);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getVersion()).isEqualTo(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsAndAccessors")
    @DisplayName(
        "GIVEN a DRAFT trust relationship with one active profile " +
        "WHEN that profile configuration is disabled " +
        "THEN the profile becomes INACTIVE"
    )
    public void shouldMarkProfileInactive_whenProfileDisabled(TrustRelationship tr, ProfileConfigurationAccessor accessor, String fieldName) {

        TrustRelationship withActive = accessor.updateStatus(tr, ProfileStatus.ACTIVE).getValue();
        assertThat(accessor.getStatus(withActive)).isEqualTo(ProfileStatus.ACTIVE);

        Result<TrustRelationship> result = accessor.updateStatus(withActive, ProfileStatus.INACTIVE);

        assertThat(result.isSuccess()).isTrue();
        assertThat(accessor.getStatus(result.getValue())).isEqualTo(ProfileStatus.INACTIVE);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsAndAccessors")
    @DisplayName(
        "GIVEN a DRAFT trust relationship " +
        "WHEN a profile configuration is updated with the same value " +
        "THEN the version is unchanged"
    )
    public void shouldMaintainVersion_whenProfileUnchanged(TrustRelationship tr, ProfileConfigurationAccessor accessor, String fieldName) {

        Object sameConfig = accessor.extract(tr);

        Result<TrustRelationship> result = accessor.update(tr, sameConfig);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getVersion()).isEqualTo(tr.getVersion());
        assertThat(result.getValue()).isEqualTo(tr);
    }

    @Test
    @DisplayName(
        "GIVEN a DRAFT trust relationship " +
        "WHEN several distinct profiles are enabled " +
        "THEN the active profile count reflects them"
    )
    public void shouldReflectActiveCount_whenSeveralProfilesEnabled() {

        TrustRelationship tr = sampleDraftIndividualTrustRelationship()
            .updateShibbolethSsoProfileConfiguration(activeShibbolethSsoProfileConfiguration()).getValue()
            .updateSaml2SsoProfileConfiguration(activeSaml2SsoProfileConfiguration()).getValue()
            .updateSaml2LogoutProfileConfiguration(activeSaml2LogoutProfileConfiguration()).getValue();

        assertThat(tr).hasActiveProfileConfigurationCount(3);
    }

    @Test
    @DisplayName(
        "GIVEN a DRAFT trust relationship with active profiles " +
        "WHEN all profiles are disabled " +
        "THEN no profile is active"
    )
    public void shouldHaveNoActiveProfile_whenAllProfilesDisabled() {

        TrustRelationship withActive = sampleDraftIndividualTrustRelationship()
            .updateShibbolethSsoProfileConfiguration(activeShibbolethSsoProfileConfiguration()).getValue()
            .updateSaml2SsoProfileConfiguration(activeSaml2SsoProfileConfiguration()).getValue();
        assertThat(withActive).hasAtLeastOneActiveProfileConfiguration();

        TrustRelationship allDisabled = withActive
            .updateShibbolethSsoProfileConfiguration(inactiveShibbolethSsoProfileConfiguration()).getValue()
            .updateSaml2SsoProfileConfiguration(inactiveSaml2SsoProfileConfiguration()).getValue();

        assertThat(allDisabled).hasNoActiveProfileConfiguration();
    }

}
