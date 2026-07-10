package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.core.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.model.core.diagnostics.ActivationStatus;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.metadata.MetadataSource;
import io.jans.shibboleth.model.metadata.MetadataSourceType;
import io.jans.shibboleth.model.metadata.NoMetadataSource;
import io.jans.shibboleth.model.config.profiles.*;
import io.jans.shibboleth.model.config.profiles.common.*;
import io.jans.shibboleth.model.util.TrustResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.model.TrustRelationshipAssert.assertThat;
import static io.jans.shibboleth.model.config.profiles.ProfileConfigurationAssert.assertThat;
import static io.jans.shibboleth.model.TrustRelationshipFixtures.*;

@DisplayName("READY State Transitions")
public class TrustRelationshipReadyTransitionTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#readyTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a READY TrustRelationship " +
        "WHEN activate() is called " +
        "THEN should transition to ACTIVATING state , increment version and clear previous activation diagnostics "
    )
    public void shouldTransitionToActivating_whenActivateCalledFromReady(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();

        TrustResult<TrustRelationship> result = tr.activate();
        
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        
        assertThat(updated).isInActivatingStatus();
        assertThat(updated).hasNoActivationDiagnostics();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#readyTrustRelationshipsWithSingleActiveProfileConfiguration")
    @DisplayName(
        "GIVEN a READY TrustRelationship with at least one active profile configuration " + 
        "WHEN updateXXXProfileConfiguration() is called such that all profiles become disabled " +
        "THEN should transit to DRAFT and increment version "
    )
    public void shouldTransitionToDraft_whenAllProfilesDisabledFromReady(TrustRelationship tr,ProfileConfigurationAccessor accessor) {

        assertThat(tr).isInReadyStatus();
        assertThat(tr).hasRealMetadataSource();
        assertThat(tr).hasAtLeastOneActiveProfileConfiguration();
        assertThat(tr).hasActiveProfileConfigurationCount(1);

        TrustResult<TrustRelationship> result = accessor.updateStatus(tr,ProfileStatus.INACTIVE);
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInDraftStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#readyTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a READY TrustRelationship with a real metadata source " +
        "WHEN updateMetadataSource() is called with NoMetadataSource " +
        "THEN should transition to DRAFT and increment version " 
    )
    public void shouldTransitionToDraft_whenMetadataSourceSetToNoneFromReady(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();
        assertThat(tr).hasRealMetadataSource();
        assertThat(tr).hasAtLeastOneActiveProfileConfiguration();

        TrustResult<TrustRelationship> result = tr.updateMetadataSource(NoMetadataSource.getInstance());

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInDraftStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#readyTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a READY trust relationship " +
        "WHEN a descriptive field is updated " +
        "THEN it remains in READY"
    )
    public void shouldRemainInReady_whenDescriptiveFieldUpdated(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();

        TrustRelationship afterDisplayName = tr.updateDisplayName(
            io.jans.shibboleth.model.core.DisplayName.of(tr.getDisplayName().getValue() + "_x").getValue()).getValue();
        assertThat(afterDisplayName).isInReadyStatus();

        TrustRelationship afterDescription = tr.updateDescription(Description.of("Changed")).getValue();
        assertThat(afterDescription).isInReadyStatus();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#readyTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a READY trust relationship " +
        "WHEN the metadata source is changed to another real source " +
        "THEN it remains in READY and bumps the version"
    )
    public void shouldRemainInReady_whenMetadataSourceChangedToAnotherRealSource(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();
        MetadataSource newSource = sampleUriMetadataSource();
        assertThat(tr.getMetadataSource()).isNotEqualTo(newSource);

        TrustResult<TrustRelationship> result = tr.updateMetadataSource(newSource);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInReadyStatus().hasRealMetadataSource();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#readyTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a READY trust relationship " +
        "WHEN a second profile is enabled " +
        "THEN it remains in READY"
    )
    public void shouldRemainInReady_whenAnotherProfileEnabled(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();
        assertThat(tr).hasActiveProfileConfigurationCount(1);

        TrustResult<TrustRelationship> result =
            tr.updateSaml2LogoutProfileConfiguration(activeSaml2LogoutProfileConfiguration());

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInReadyStatus().hasActiveProfileConfigurationCount(2);
    }

}
