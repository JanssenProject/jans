package io.jans.shibboleth.trust.config;


import io.jans.shibboleth.trust.config.metadata.MetadataSource;
import io.jans.shibboleth.trust.config.metadata.NoMetadataSource;
import io.jans.shibboleth.trust.config.profile.*;
import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.shared.Result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.trust.config.TrustRelationshipAssert.assertThat;
import static io.jans.shibboleth.trust.config.TrustRelationshipFixtures.*;

@DisplayName("READY State Transitions")
public class TrustRelationshipReadyTransitionTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#readyTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a READY TrustRelationship " +
        "WHEN activate() is called " +
        "THEN should transition to ACTIVATING state , increment version and clear previous activation diagnostics "
    )
    public void shouldTransitionToActivating_whenActivateCalledFromReady(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();

        Result<TrustRelationship> result = tr.activate();
        
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        
        assertThat(updated).isInActivatingStatus();
        assertThat(updated).hasNoActivationDiagnostics();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#readyTrustRelationshipsWithSingleActiveProfileConfiguration")
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

        Result<TrustRelationship> result = accessor.updateStatus(tr,ProfileStatus.INACTIVE);
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInDraftStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#readyTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a READY TrustRelationship with a real metadata source " +
        "WHEN updateMetadataSource() is called with NoMetadataSource " +
        "THEN should transition to DRAFT and increment version " 
    )
    public void shouldTransitionToDraft_whenMetadataSourceSetToNoneFromReady(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();
        assertThat(tr).hasRealMetadataSource();
        assertThat(tr).hasAtLeastOneActiveProfileConfiguration();

        Result<TrustRelationship> result = tr.updateMetadataSource(NoMetadataSource.getInstance());

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInDraftStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#readyTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a READY trust relationship " +
        "WHEN a descriptive field is updated " +
        "THEN it remains in READY"
    )
    public void shouldRemainInReady_whenDescriptiveFieldUpdated(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();

        TrustRelationship afterDisplayName = tr.updateDisplayName(
            io.jans.shibboleth.trust.config.DisplayName.of(tr.getDisplayName().getValue() + "_x").getValue()).getValue();
        assertThat(afterDisplayName).isInReadyStatus();

        TrustRelationship afterDescription = tr.updateDescription(Description.of("Changed")).getValue();
        assertThat(afterDescription).isInReadyStatus();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#readyTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a READY trust relationship " +
        "WHEN the metadata source is changed to another real source " +
        "THEN it remains in READY and bumps the version"
    )
    public void shouldRemainInReady_whenMetadataSourceChangedToAnotherRealSource(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();
        MetadataSource newSource = sampleUriMetadataSource();
        assertThat(tr.getMetadataSource()).isNotEqualTo(newSource);

        Result<TrustRelationship> result = tr.updateMetadataSource(newSource);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInReadyStatus().hasRealMetadataSource();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#readyTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a READY trust relationship " +
        "WHEN a second profile is enabled " +
        "THEN it remains in READY"
    )
    public void shouldRemainInReady_whenAnotherProfileEnabled(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();
        assertThat(tr).hasActiveProfileConfigurationCount(1);

        Result<TrustRelationship> result =
            tr.updateSaml2LogoutProfileConfiguration(activeSaml2LogoutProfileConfiguration());

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInReadyStatus().hasActiveProfileConfigurationCount(2);
    }

}
