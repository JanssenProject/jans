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

@DisplayName("DRAFT State Transitions")
public class TrustRelationshipDraftTransitionTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsWithSupportedMetadataSources")
    @DisplayName (
        "GIVEN a DRAFT TrustRelationship with no active profiles " +
        "WHEN updateMetadataSource() is called " +
        "THEN the operation updates the metadata source, and maintains the DRAFT status"
    )
    public void shouldUpdateMetadataSourceAndStayInDraft_whenNoActiveProfiles(TrustRelationship tr,MetadataSource source) {

        assertThat(tr).isInDraftStatus();
        assertThat(tr).hasNoActiveProfileConfiguration();

        Result<TrustRelationship> result = tr.updateMetadataSource(source);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated_tr = result.getValue();
        assertThat(updated_tr.getMetadataSource()).isEqualTo(source);
        assertThat(updated_tr).isInDraftStatus();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsWithAnActiveProfileAndRealMetadataSource")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship  with at least one active profile " +
        "WHEN updateMetadataSource() is called with a no-NONE metadata source " +
        "THEN the TrustRelationship should transition to READY state AND increment version "
    )
    public void shouldTransitionToReady_whenRealMetadataSourceAddedWithActiveProfile(TrustRelationship tr,  MetadataSource source) {

        assertThat(tr).isInDraftStatus();
        assertThat(tr).hasAtLeastOneActiveProfileConfiguration();
        assertThat(source.getType()).isNotEqualTo(MetadataSourceType.NONE);

        Result<TrustRelationship> result = tr.updateMetadataSource(source);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInReadyStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsWithRealMetadataSourceAndActiveProfileConfigurationToUpdate")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship with a REAL(non-NONE) metadata source " +
        "WHEN updateXXXProfileConfiguration is called with an ACTIVE profile configuration " +
        "THEN the TrustRelationship should transition to READY status AND increment version "
    )
    public void shouldTransitionToReady_whenProfileConfigurationEnabledWithRealMetadataSource(TrustRelationship tr, Object profileconfig, ProfileConfigurationAccessor accessor) {

        assertThat(tr).isInDraftStatus();
        assertThat(tr).hasRealMetadataSource();
        assertThat(accessor.getStatus(profileconfig)).isEqualTo(ProfileStatus.ACTIVE);

        Result<TrustRelationship> result = accessor.update(tr,profileconfig);
        
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInReadyStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT trust relationship " +
        "WHEN a descriptive field (displayName or description) is updated " +
        "THEN it remains in DRAFT"
    )
    public void shouldRemainInDraft_whenDescriptiveFieldUpdated(TrustRelationship tr) {

        assertThat(tr).isInDraftStatus();

        TrustRelationship afterDisplayName = tr.updateDisplayName(
            io.jans.shibboleth.trust.config.DisplayName.of(tr.getDisplayName().getValue() + "_x").getValue()).getValue();
        assertThat(afterDisplayName).isInDraftStatus();

        TrustRelationship afterDescription = tr.updateDescription(Description.of("Changed")).getValue();
        assertThat(afterDescription).isInDraftStatus();
    }

    @Test
    @DisplayName(
        "GIVEN a DRAFT trust relationship with a real source " +
        "WHEN the metadata source is set to NONE " +
        "THEN it remains in DRAFT"
    )
    public void shouldRemainInDraft_whenMetadataSourceSetToNone() {

        TrustRelationship[] draftsWithRealSource = {
            sampleDraftIndividualTrustRelationshipWithRealMetadataSource(),
            sampleDraftAggregateTrustRelationshipWithRealMetadataSource()
        };

        for (TrustRelationship tr : draftsWithRealSource) {

            assertThat(tr).isInDraftStatus();
            assertThat(tr).hasRealMetadataSource();

            Result<TrustRelationship> result = tr.updateMetadataSource(NoMetadataSource.getInstance());

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isInDraftStatus().hasNoRealMetadataSource();
        }
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT trust relationship " +
        "WHEN activate() is called " +
        "THEN it fails with OperationForbiddenFromStatus and the original is unchanged"
    )
    public void shouldFailActivate_whenCalledFromDraft(TrustRelationship tr) {

        assertThat(tr).isInDraftStatus();

        Result<TrustRelationship> result = tr.activate();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(OperationForbiddenFromStatus.class);
        OperationForbiddenFromStatus cause = (OperationForbiddenFromStatus) error.getCause();
        assertThat(cause.getOperationName()).isEqualTo("activate");
        assertThat(cause.getForbiddenStatus()).isEqualTo(TrustStatus.DRAFT);
    }

}
