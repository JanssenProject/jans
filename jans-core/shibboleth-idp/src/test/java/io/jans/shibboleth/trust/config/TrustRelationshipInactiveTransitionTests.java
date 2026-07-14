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

@DisplayName("INACTIVE State Transitions")
public class TrustRelationshipInactiveTransitionTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an INACTIVE TrustRelationship with a real metadata source and at least one active profile " +
        "WHEN activate() is called " + 
        "THEN should transition to ACTIVATING state and increment version "
    )
    public void shouldTransitionToActivatingFromInactive_whenRequirementsMet(TrustRelationship tr) {

        assertThat(tr).isInInactiveStatus();
        assertThat(tr).hasRealMetadataSource();
        assertThat(tr).hasAtLeastOneActiveProfileConfiguration();

        Result<TrustRelationship> result = tr.activate();

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInActivatingStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource({
        "io.jans.shibboleth.trust.config.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNaturesWithNoRealMetadataSource",
        "io.jans.shibboleth.trust.config.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNaturesWithNoActiveProfileConfiguration"
    })
    @DisplayName(
        "GIVEN an INACTIVE TrustRelationship with no real metadata source or no active profile " +
        "WHEN activate() is called " +
        "THEN should transition to DRAFT state and increment version "
    )
    public void shouldTransitionToDraft_whenActivateCalledFromInactiveButRequirementsNotMet(TrustRelationship tr) {

        assertThat(tr).isInInactiveStatus();
        assertThat(tr.hasNoRealMetadataSource() || tr.hasNoActiveProfileConfiguration()).isTrue();

        Result<TrustRelationship> result = tr.activate();

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInDraftStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an INACTIVE trust relationship with a real source " +
        "WHEN the metadata source is changed to another real source " +
        "THEN it remains in INACTIVE and bumps the version"
    )
    public void shouldRemainInInactive_whenMetadataSourceChangedToAnotherRealSource(TrustRelationship tr) {

        assertThat(tr).isInInactiveStatus();
        MetadataSource newSource = sampleUriMetadataSource();
        assertThat(tr.getMetadataSource()).isNotEqualTo(newSource);

        Result<TrustRelationship> result = tr.updateMetadataSource(newSource);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInInactiveStatus().hasRealMetadataSource();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an INACTIVE trust relationship " +
        "WHEN the metadata source is set to NONE " +
        "THEN it remains in INACTIVE and bumps the version"
    )
    public void shouldRemainInInactive_whenMetadataSourceSetToNone(TrustRelationship tr) {

        assertThat(tr).isInInactiveStatus();
        assertThat(tr).hasRealMetadataSource();

        Result<TrustRelationship> result = tr.updateMetadataSource(NoMetadataSource.getInstance());

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInInactiveStatus().hasNoRealMetadataSource();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an INACTIVE trust relationship " +
        "WHEN a profile configuration is changed " +
        "THEN it remains in INACTIVE and bumps the version"
    )
    public void shouldRemainInInactive_whenProfileConfigurationChanged(TrustRelationship tr) {

        assertThat(tr).isInInactiveStatus();

        Result<TrustRelationship> result =
            tr.updateSaml2LogoutProfileConfiguration(activeSaml2LogoutProfileConfiguration());

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInInactiveStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @Test
    @DisplayName(
        "GIVEN an INACTIVE trust relationship " +
        "WHEN all profiles are disabled " +
        "THEN it remains in INACTIVE"
    )
    public void shouldRemainInInactive_whenAllProfilesDisabled() {

        TrustRelationship individual = sampleInactiveIndividualTrustRelationship();
        assertThat(individual).hasAtLeastOneActiveProfileConfiguration();
        TrustRelationship individualDisabled = individual
            .updateShibbolethSsoProfileConfiguration(inactiveShibbolethSsoProfileConfiguration()).getValue();
        assertThat(individualDisabled).isInInactiveStatus().hasNoActiveProfileConfiguration();

        TrustRelationship aggregate = sampleInactiveAggregateTrustRelationship();
        assertThat(aggregate).hasAtLeastOneActiveProfileConfiguration();
        TrustRelationship aggregateDisabled = aggregate
            .updateSaml2SsoProfileConfiguration(inactiveSaml2SsoProfileConfiguration()).getValue();
        assertThat(aggregateDisabled).isInInactiveStatus().hasNoActiveProfileConfiguration();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an INACTIVE trust relationship carrying diagnostics " +
        "WHEN activate() is called " +
        "THEN the diagnostics are cleared and the version is incremented"
    )
    public void shouldClearDiagnosticsAndIncrementVersion_whenActivateCalledFromInactive(TrustRelationship tr) {

        assertThat(tr).isInInactiveStatus();
        assertThat(tr).hasActivationDiagnostics();
        assertThat(tr).hasRealMetadataSource();
        assertThat(tr).hasAtLeastOneActiveProfileConfiguration();

        Result<TrustRelationship> result = tr.activate();

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInActivatingStatus();
        assertThat(updated).hasNoActivationDiagnostics();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an INACTIVE trust relationship " +
        "WHEN updateMetadataSource() is called with the same source " +
        "THEN the version is unchanged"
    )
    public void shouldMaintainVersion_whenMetadataSourceUnchangedFromInactive(TrustRelationship tr) {

        assertThat(tr).isInInactiveStatus();

        Result<TrustRelationship> result = tr.updateMetadataSource(tr.getMetadataSource());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInInactiveStatus();
        assertThat(result.getValue()).isVersion(tr.getVersion());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an INACTIVE trust relationship " +
        "WHEN a profile configuration is updated with the same value " +
        "THEN the version is unchanged"
    )
    public void shouldMaintainVersion_whenProfileConfigurationUnchangedFromInactive(TrustRelationship tr) {

        assertThat(tr).isInInactiveStatus();

        Result<TrustRelationship> result =
            tr.updateSaml2LogoutProfileConfiguration(tr.getSaml2LogoutProfileConfiguration());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInInactiveStatus();
        assertThat(result.getValue()).isVersion(tr.getVersion());
    }

}
