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

@DisplayName("Cross-Cutting Invariants & Lifecycle")
public class TrustRelationshipInvariantsAndLifecycleTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#readyTrustRelationshipsWithActivationDiagnostics")
    @DisplayName(
        "GIVEN a READY TrustRelationship with previous activation diagnostics " +
        "WHEN activate() is called " +
        "THEN should clear previous diagnostics and transition to ACTIVATING "
    )
    public void shouldClearPreviousDiagnostics_whenActivateIsCalledFromReady(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();
        assertThat(tr).hasActivationDiagnostics();

        Result<TrustRelationship> result = tr.activate();

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).hasNoActivationDiagnostics();
        assertThat(updated).isInActivatingStatus();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activatingTrustRelationshipsWithSuccessActivationDiagnostics")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN finalizeActivation() is called with a successful ActivationContext containing diagnostics " +
        "THEN the resulting TrustRelationship should be in ACTIVE state and contain the activation diagnostics " 
    )
    public void shouldIncludeActivationDiagnosticsAfterSuccessfulFinalizeActivation(TrustRelationship tr,ActivationDiagnostics success_diagnostics) {

        assertThat(tr).isInActivatingStatus();
        assertThat(success_diagnostics.getStatus()).isEqualTo(ActivationStatus.SUCCEEDED);

        Result<TrustRelationship> result = tr.finalizeActivation(success_diagnostics);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInActiveStatus();
        assertThat(updated.getActivationDiagnostics()).isEqualTo(success_diagnostics);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activatingTrustRelationshipsWithFailedActivationDiagnostics")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN finalizeActivation() is called with a failed ActivationContext " +
        "THEN the resulting TrustRelationship should be in READY state and contain the activation diagnostics "
    )
    public void shouldIncludeActivationDiagnosticsAfterFailedFinalizeActivation(TrustRelationship tr, ActivationDiagnostics failed_diagnostics) {

        assertThat(tr).isInActivatingStatus();
        assertThat(failed_diagnostics.getStatus()).isEqualTo(ActivationStatus.FAILED);

        Result<TrustRelationship> result = tr.finalizeActivation(failed_diagnostics);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInReadyStatus();
        assertThat(updated.getActivationDiagnostics()).isEqualTo(failed_diagnostics);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#trustRelationshipsOfVariousStatuses")
    @DisplayName(
        "GIVEN a fully populated TrustRelationship in any state " +
        "WHEN it is rebuilt using the builder from persisted data (reconstruction scenario) " +
        "THEN all data, state, version and invariants should be preserved " 
    )
    public void shouldPreserveAllDataWhenRebuildingFromStorage(TrustRelationship tr) {

        Result<TrustRelationship> result  = TrustRelationship.builder()
            .withId(tr.getId())
            .withVersion(tr.getVersion())
            .withDisplayName(tr.getDisplayName())
            .withDescription(tr.getDescription())
            .withNature(tr.getNature())
            .withStatus(tr.getStatus())
            .withMetadataSource(tr.getMetadataSource())
            .withDiscoveredEntityIds(tr.getDiscoveredEntityIds())
            .withShibbolethSsoProfileConfiguration(tr.getShibbolethSsoProfileConfiguration())
            .withSaml2ArtifactResolutionProfileConfiguration(tr.getSaml2ArtifactResolutionProfileConfiguration())
            .withSaml2AttributeQueryProfileConfiguration(tr.getSaml2AttributeQueryProfileConfiguration())
            .withSaml2EcpProfileConfiguration(tr.getSaml2EcpProfileConfiguration())
            .withSaml2SsoProfileConfiguration(tr.getSaml2SsoProfileConfiguration())
            .withSaml2LogoutProfileConfiguration(tr.getSaml2LogoutProfileConfiguration())
            .withReleasedAttributes(tr.getReleasedAttributes())
            .withActivationDiagnostics(tr.getActivationDiagnostics())
            .build();
        
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(tr);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#trustRelationshipsWithIdempotentUpdateOperations")
    @DisplayName(
        "GIVEN a TrustRelationship in any valid state where the operation is allowed " +
        "WHEN any update method/operation is called with the same current value " +
        "THEN version should not be incremented and state should remain the same "
    )
    public void shouldMaintainVersionWhenNoActualChangeInAnyState(TrustRelationship tr, 
        Function<TrustRelationship,Result<TrustRelationship>> operation) {

        Result<TrustRelationship> result = operation.apply(tr);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getVersion()).isEqualTo(tr.getVersion());

    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN incorporateDiscoveredEntityIds() is called with null EntityIds " +
        "THEN should fail with appropriate error "
    )
    public void shouldFailIncorporateDiscoveredEntityIdsWhenEntityIdsIsNull(TrustRelationship tr) {

        assertThat(tr).isInActivatingStatus();

        Result<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
    }

    @Test
    @DisplayName(
        "GIVEN any trust relationship " +
        "WHEN a successful mutation is applied " +
        "THEN a new instance is returned and the original is left unchanged"
    )
    public void shouldReturnNewInstanceAndLeaveOriginalUnchanged_whenOperationSucceeds() {

        TrustRelationship original = sampleDraftIndividualTrustRelationship();
        io.jans.shibboleth.trust.config.DisplayName originalName = original.getDisplayName();
        Version originalVersion = original.getVersion();

        TrustRelationship updated = original.updateDisplayName(
            io.jans.shibboleth.trust.config.DisplayName.of("Renamed").getValue()).getValue();

        assertThat(updated).isNotEqualTo(original);
        assertThat(original.getDisplayName()).isEqualTo(originalName);
        assertThat(original.getVersion()).isEqualTo(originalVersion);
    }

    @Test
    @DisplayName(
        "GIVEN any trust relationship " +
        "WHEN a failing operation is attempted " +
        "THEN it fails and the original is left unchanged"
    )
    public void shouldLeaveOriginalUnchanged_whenOperationFails() {

        TrustRelationship original = sampleDraftIndividualTrustRelationship();
        Version originalVersion = original.getVersion();

        Result<TrustRelationship> result = original.activate();

        assertThat(result.isFailure()).isTrue();
        assertThat(original).isInDraftStatus();
        assertThat(original.getVersion()).isEqualTo(originalVersion);
    }

    @Test
    @DisplayName(
        "GIVEN two trust relationships built with identical fields " +
        "WHEN they are compared " +
        "THEN they are equal and share the same hashCode"
    )
    public void shouldBeEqual_whenAllFieldsMatch() {

        TrustRelationship a = sampleDraftIndividualTrustRelationship();
        TrustRelationship b = sampleDraftIndividualTrustRelationship();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName(
        "GIVEN two trust relationships differing in a single field " +
        "WHEN they are compared " +
        "THEN they are not equal"
    )
    public void shouldNotBeEqual_whenAnyFieldDiffers() {

        TrustRelationship a = sampleDraftIndividualTrustRelationship();
        TrustRelationship b = a.updateDescription(Description.of("A different description")).getValue();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName(
        "GIVEN a fresh trust relationship " +
        "WHEN a chain of effective mutations is applied " +
        "THEN the version never decreases and strictly increases on each effective change"
    )
    public void shouldNeverDecreaseVersion_acrossSuccessfulMutations() {

        TrustRelationship tr = sampleDraftIndividualTrustRelationship();
        Version v0 = tr.getVersion();

        TrustRelationship s1 = tr.updateDisplayName(
            io.jans.shibboleth.trust.config.DisplayName.of("A").getValue()).getValue();
        TrustRelationship s2 = s1.updateDescription(Description.of("B")).getValue();
        TrustRelationship s3 = s2.updateMetadataSource(sampleFileMetadataSource()).getValue();

        assertThat(s1.getVersion()).isEqualTo(v0.next());
        assertThat(s2.getVersion()).isEqualTo(s1.getVersion().next());
        assertThat(s3.getVersion()).isEqualTo(s2.getVersion().next());
    }

    @Test
    @DisplayName(
        "GIVEN a READY or INACTIVE trust relationship carrying diagnostics " +
        "WHEN activate() is called " +
        "THEN the activation diagnostics are cleared"
    )
    public void shouldClearDiagnostics_whenActivateCalled() {

        TrustRelationship readyWithDiagnostics = sampleActivatingIndividualTrustRelationship()
            .finalizeActivation(sampleActivationDiagnosticsForFailedActivation()).getValue();
        assertThat(readyWithDiagnostics).isInReadyStatus().hasActivationDiagnostics();
        assertThat(readyWithDiagnostics.activate().getValue())
            .isInActivatingStatus().hasNoActivationDiagnostics();

        TrustRelationship inactiveWithDiagnostics = sampleInactiveIndividualTrustRelationship();
        assertThat(inactiveWithDiagnostics).isInInactiveStatus().hasActivationDiagnostics();
        assertThat(inactiveWithDiagnostics.activate().getValue())
            .isInActivatingStatus().hasNoActivationDiagnostics();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#trustRelationshipsOfVariousStatuses")
    @DisplayName(
        "GIVEN a trust relationship in any state " +
        "WHEN updateDisplayName() is called with a different name " +
        "THEN it succeeds and remains in the same state"
    )
    public void shouldPreserveState_whenDisplayNameUpdatedFromAnyState(TrustRelationship tr) {

        TrustStatus before = tr.getStatus();

        Result<TrustRelationship> result = tr.updateDisplayName(
            io.jans.shibboleth.trust.config.DisplayName.of(tr.getDisplayName().getValue() + "_x").getValue());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInStatus(before);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#trustRelationshipsOfVariousStatuses")
    @DisplayName(
        "GIVEN a trust relationship in any state " +
        "WHEN updateDescription() is called with a different description " +
        "THEN it succeeds and remains in the same state"
    )
    public void shouldPreserveState_whenDescriptionUpdatedFromAnyState(TrustRelationship tr) {

        TrustStatus before = tr.getStatus();

        Result<TrustRelationship> result = tr.updateDescription(
            Description.of(tr.getDescription().getValue() + " changed"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInStatus(before);
    }

    @Test
    @DisplayName(
        "GIVEN a new individual trust relationship " +
        "WHEN it walks the full lifecycle from create through READY, ACTIVATING, ACTIVE, INACTIVE, and back to ACTIVATING " +
        "THEN every waypoint reaches the expected status and the version stays monotonic"
    )
    public void shouldReachEveryState_whenIndividualFollowsFullLifecycle() {

        TrustRelationship draft = sampleDraftIndividualTrustRelationship();
        assertThat(draft).isInDraftStatus();

        TrustRelationship ready = draft
            .updateShibbolethSsoProfileConfiguration(activeShibbolethSsoProfileConfiguration()).getValue()
            .updateMetadataSource(sampleFileMetadataSource()).getValue();
        assertThat(ready).isInReadyStatus();

        TrustRelationship activating = ready.activate().getValue();
        assertThat(activating).isInActivatingStatus();

        TrustRelationship active = activating
            .finalizeActivation(sampleActivationDiagnosticsForSuccessfulActivation()).getValue();
        assertThat(active).isInActiveStatus();

        TrustRelationship inactive = active.deactivate().getValue();
        assertThat(inactive).isInInactiveStatus();

        TrustRelationship reactivating = inactive.activate().getValue();
        assertThat(reactivating).isInActivatingStatus();

        assertThat(ready.getVersion().isGreaterThan(draft.getVersion())).isTrue();
        assertThat(activating.getVersion().isGreaterThan(ready.getVersion())).isTrue();
        assertThat(active.getVersion().isGreaterThan(activating.getVersion())).isTrue();
        assertThat(inactive.getVersion().isGreaterThan(active.getVersion())).isTrue();
        assertThat(reactivating.getVersion().isGreaterThan(inactive.getVersion())).isTrue();
    }

    @Test
    @DisplayName(
        "GIVEN a new aggregate trust relationship " +
        "WHEN it walks the full lifecycle and incorporates discovered entity IDs while ACTIVATING " +
        "THEN every waypoint reaches the expected status and the discovered IDs are present at ACTIVE"
    )
    public void shouldReachEveryState_whenAggregateFollowsFullLifecycleWithDiscovery() {

        TrustRelationship draft = sampleDraftAggregateTrustRelationship();
        assertThat(draft).isInDraftStatus();

        TrustRelationship ready = draft
            .updateSaml2SsoProfileConfiguration(activeSaml2SsoProfileConfiguration()).getValue()
            .updateMetadataSource(sampleMdqMetadataSource()).getValue();
        assertThat(ready).isInReadyStatus();

        TrustRelationship activating = ready.activate().getValue();
        assertThat(activating).isInActivatingStatus();

        TrustRelationship withIds = activating.incorporateDiscoveredEntityIds(sampleEntityIds()).getValue();
        assertThat(withIds).isInActivatingStatus().hasAnyDiscoveredEntityIds();

        TrustRelationship active = withIds
            .finalizeActivation(sampleActivationDiagnosticsForSuccessfulActivation()).getValue();
        assertThat(active).isInActiveStatus().hasAnyDiscoveredEntityIds();
    }

    @Test
    @DisplayName(
        "GIVEN a READY trust relationship " +
        "WHEN activation fails and is then retried successfully " +
        "THEN it returns to READY on failure and reaches ACTIVE on the retry"
    )
    public void shouldReturnToReadyThenRetry_whenActivationFails() {

        TrustRelationship ready = sampleReadyIndividualTrustRelationship();

        TrustRelationship activating = ready.activate().getValue();
        assertThat(activating).isInActivatingStatus();

        TrustRelationship backToReady = activating
            .finalizeActivation(sampleActivationDiagnosticsForFailedActivation()).getValue();
        assertThat(backToReady).isInReadyStatus();

        TrustRelationship activatingAgain = backToReady.activate().getValue();
        assertThat(activatingAgain).isInActivatingStatus();

        TrustRelationship active = activatingAgain
            .finalizeActivation(sampleActivationDiagnosticsForSuccessfulActivation()).getValue();
        assertThat(active).isInActiveStatus();
    }

}
