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

@DisplayName("ACTIVATING: Operations & Finalization")
public class TrustRelationshipActivatingTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN cancelActivation() is called " + 
        "THEN should transition to READY state and increment version "
    )
    public void shouldTransitionToReady_whenCancelActivationCalledFromActivating(TrustRelationship tr) {

        assertThat(tr).isInActivatingStatus();
        TrustResult<TrustRelationship> result = tr.cancelActivation();

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInReadyStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN finalizeActivation() is called with null ActivationDiagnostics " +
        "THEN should fail with the appropriate error"
    )
    public void shouldFailWhenFinalizeActivationIsCalledWithNullActivationContext(TrustRelationship tr) {

        assertThat(tr).isInActivatingStatus();

        TrustResult<TrustRelationship> result = tr.finalizeActivation(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);

        CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
        assertThat(cause).isNotNull();
        assertThat(cause.getFieldName()).isEqualTo("activationDiagnostics");
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN finalizeActivation() is called with a successful ActivationDiagnostics " +
        "THEN should transition to ACTIVE state and increment version " 
    )
    public void shouldTransitionToActive_whenFinalizeActivationSucceeds(TrustRelationship tr) {

        ActivationDiagnostics diagnostics = TrustRelationshipFixtures.sampleActivationDiagnosticsForSuccessfulActivation();

        assertThat(tr).isInActivatingStatus();
        assertThat(diagnostics.getStatus()).isEqualTo(ActivationStatus.SUCCEEDED);

        TrustResult<TrustRelationship> result = tr.finalizeActivation(diagnostics);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInActiveStatus();
        assertThat(updated.getActivationDiagnostics()).isEqualTo(diagnostics);
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN finalizeActivation() is called with a failed ActivationDiagnostics " + 
        "THEN should transition to READY and increment version "
    )
    public void shouldTransitionToReady_whenFinalizeActivationFails(TrustRelationship tr) {

        ActivationDiagnostics diagnostics = TrustRelationshipFixtures.sampleActivationDiagnosticsForFailedActivation();

        assertThat(tr).isInActivatingStatus();
        assertThat(diagnostics.getStatus()).isEqualTo(ActivationStatus.FAILED);

        TrustResult<TrustRelationship> result = tr.finalizeActivation(diagnostics);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInReadyStatus();
        assertThat(updated.getActivationDiagnostics()).isEqualTo(diagnostics);
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @Test
    @DisplayName(
        "GIVEN an ACTIVATING AGGREGATE TrustRelationship " +
        "WHEN incorporateDiscoveredEntityIds() is called with new valid entity ids " +
        "THEN should update the discovered entityids and increment version (no state change) "
    )
    public void shouldIncorporateDiscoveredEntityIds_whenInActivatingStateForAggregate() {

        TrustRelationship tr = TrustRelationshipFixtures.sampleActivatingAggregateTrustRelationship();
        EntityIds ids = TrustRelationshipFixtures.sampleEntityIds();

        assertThat(tr).isInActivatingStatus();
        assertThat(tr.getDiscoveredEntityIds()).isNotEqualTo(ids);

        TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(ids);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated.getDiscoveredEntityIds()).isEqualTo(ids);
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @Test
    @DisplayName(
        "GIVEN an ACTIVATING AGGREGATE TrustRelationship with existing discovered entity ids " + 
        "WHEN incorporateDiscoveredEntityIds() is called with the exact same entity ids " +
        "THEN should be idempotent (no version change , no error)" 
    )
    public void shouldBeIdempotent_whenIncorporateDiscoveredEntityIdsWithSameValue() {

        TrustRelationship tr = TrustRelationshipFixtures.sampleActivatingAggregateTrustRelationshipWithDiscoveredEntityIds();

        assertThat(tr).isInActivatingStatus();
        assertThat(tr).hasAnyDiscoveredEntityIds();

        EntityIds ids = EntityIds.from(tr.getDiscoveredEntityIds()).build().getValue();
        assertThat(ids).isEqualTo(tr.getDiscoveredEntityIds());

        TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(ids);

        assertThat(result.isSuccess()).isTrue();

        TrustRelationship same = result.getValue();
        assertThat(same.getDiscoveredEntityIds()).isEqualTo(ids);
        assertThat(same).isVersion(tr.getVersion());

    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#aggregateTrustRelationshipsNotInActivatingState")
    @DisplayName(
        "GIVEN an AGGREGATE TrustRelationship that is not in ACTIVATING state " +
        "WHEN incorporateDiscoveredEntityIds() is called with valid entityIDs " +
        "THEN should fail with OperationForbiddenFromStatus error"
    )
    public void shouldRejectIncorporateDiscoveredEntityIds_whenAggregateNotInActivatingState(TrustRelationship tr) {

        assertThat(tr).isOfAggregateNature();
        assertThat(tr).doesNotHaveStatus(TrustStatus.ACTIVATING);

        EntityIds ids = TrustRelationshipFixtures.sampleEntityIds();
        TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(ids);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(OperationForbiddenFromStatus.class);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#individualTrustRelationshipsInMultipleStates")
    @DisplayName(
        "GIVEN an INDIVIDUAL TrustRelationship irrespective of state " +
        "WHEN incorporateDiscoveredEntityIds() is called with valid entityIDs " +
        "THEN should fail with OperationRestrictedToNature error "
    )
    public void shouldRejectIncorporateDiscoveredEntityIds_whenTrustIsIndividual(TrustRelationship tr) {

        assertThat(tr).isOfIndividualNature();

        EntityIds ids = TrustRelationshipFixtures.sampleEntityIds();
        TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(ids);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(OperationRestrictedToNature.class);
    }

    @Test
    @DisplayName(
        "GIVEN an ACTIVATING AGGREGATE TrustRelationship " +
        "WHEN incorporateDiscoveredEntityIds() is called with null entityIds " +
        "THEN should fail with the appropriate error"
    )
    public void shouldFailIncorporateDiscoveredEntityIdsWhenEntityIdsIsNull() {

        TrustRelationship tr = TrustRelationshipFixtures.sampleActivatingAggregateTrustRelationship();

        assertThat(tr).isInActivatingStatus();
        assertThat(tr).isOfAggregateNature();

        TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNaturesWithSupportedMetadataSources")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " + 
        "WHEN updateMetadataSource() is called " +
        "THEN should fail with OperationForbiddenFromStatus "
    )
    public void shouldFailWhenUpdateMetadataSourceCalledInActivatingState(TrustRelationship tr,MetadataSource source) {

        assertThat(tr).isInActivatingStatus();

        TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(OperationForbiddenFromStatus.class);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsWithProfileConfigurations")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN updateXXXProfileConfiguration() is called " +
        "THEN should fail with OperationForbiddenFromStatus " 
    )
    public void shouldFailWhenUpdateProfileConfigurationCalledInActivatingStatus(TrustRelationship tr,Object config, ProfileConfigurationAccessor accessor) {

        assertThat(tr).isInActivatingStatus();

        TrustResult<TrustRelationship> result = accessor.update(tr,config);
        
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(OperationForbiddenFromStatus.class);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#trustRelationshipsOfAllNaturesNotInActivatingState")
    @DisplayName(
        "GIVEN a TrustRelationship that is NOT in ACTIVATING state " +
        "WHEN finalizeActivation() is called " +
        "THEN should fail with OperationForbiddenFromStatus error "
    )
    public void shouldFailFinalizeActivation_whenNotInActivatingState(TrustRelationship tr) {

        assertThat(tr).doesNotHaveStatus(TrustStatus.ACTIVATING);

        ActivationDiagnostics diagnostics = TrustRelationshipFixtures.sampleActivationDiagnosticsForFailedActivation();
        TrustResult<TrustRelationship> result = tr.finalizeActivation(diagnostics);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(OperationForbiddenFromStatus.class);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#trustRelationshipsOfAllNaturesNotInActivatingState")
    @DisplayName(
        "GIVEN a TrustRelationship that is NOT in ACTIVATING state " +
        "WHEN cancelActivation() is called " + 
        "THEN should fail with OperationForbiddenFromStatus error "
    )
    public void shouldFailCancelActivation_whenNotInActivatingState(TrustRelationship tr) {

        assertThat(tr).doesNotHaveStatus(TrustStatus.ACTIVATING);
        TrustResult<TrustRelationship> result = tr.cancelActivation();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();

        assertThat(error.getCause()).isInstanceOf(OperationForbiddenFromStatus.class);
    }

    @Test
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipFixtures#sampleActivatingIndividualTrustRelationship")
    @DisplayName(
        "GIVEN an ACTIVATING INDIVIDUAL TrustRelationship " +
        "WHEN incorporateDiscoveredEntityIds() is called "  +
        "THEN should fail with OperationRestrictedToNature"
    )
    public void shouldRejectIncorporateDiscoveredEntityIds_whenTrustIsIndividual() {

        TrustRelationship tr = TrustRelationshipFixtures.sampleActivatingIndividualTrustRelationship();

        assertThat(tr).isOfIndividualNature();
        assertThat(tr).isInActivatingStatus();

        EntityIds ids = TrustRelationshipFixtures.sampleEntityIds();
        TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(ids);
        
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();

        assertThat(error.getCause()).isInstanceOf(OperationRestrictedToNature.class);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN activate() is called " + 
        "THEN should fail with OperationForbiddenFromStatus error"
    )
    public void shouldFailWhenActivateCalledFromActivatingState(TrustRelationship tr) {

        assertThat(tr).isInActivatingStatus();

        TrustResult<TrustRelationship> result = tr.activate();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(OperationForbiddenFromStatus.class);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN deactivate() is called " +
        "THEN should fail with OperationForbiddenFromStatus error"
    )
    public void shouldFailWhenDeactivateCalledFromActivatingStatus(TrustRelationship tr) {

        assertThat(tr).isInActivatingStatus();

        TrustResult<TrustRelationship> result = tr.deactivate();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(OperationForbiddenFromStatus.class);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN updateReleasedAttributes() is called " +
        "THEN it fails with OperationForbiddenFromStatus and the original is unchanged"
    )
    public void shouldFailUpdateReleasedAttributes_whenCalledFromActivating(TrustRelationship tr) {

        assertThat(tr).isInActivatingStatus();

        TrustResult<TrustRelationship> result = tr.updateReleasedAttributes(sampleReleasedAttributes());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(OperationForbiddenFromStatus.class);
        OperationForbiddenFromStatus cause = (OperationForbiddenFromStatus) error.getCause();
        assertThat(cause.getOperationName()).isEqualTo("updateReleasedAttributes");
        assertThat(cause.getForbiddenStatus()).isEqualTo(TrustStatus.ACTIVATING);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN finalizeActivation() is called with no activation data " +
        "THEN it remains in ACTIVATING"
    )
    public void shouldRemainInActivating_whenFinalizeActivationHasNoData(TrustRelationship tr) {

        assertThat(tr).isInActivatingStatus();
        ActivationDiagnostics noData = ActivationDiagnostics.none();
        assertThat(noData.getStatus()).isEqualTo(ActivationStatus.NO_DATA);

        TrustResult<TrustRelationship> result = tr.finalizeActivation(noData);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInActivatingStatus();
    }

    @Test
    @DisplayName(
        "GIVEN an ACTIVATING AGGREGATE TrustRelationship " +
        "WHEN incorporateDiscoveredEntityIds() is called " +
        "THEN it remains in ACTIVATING"
    )
    public void shouldRemainInActivating_whenIncorporateDiscoveredEntityIds() {

        TrustRelationship tr = sampleActivatingAggregateTrustRelationship();
        assertThat(tr).isInActivatingStatus();

        TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(sampleEntityIds());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInActivatingStatus();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN updateDisplayName() is called with a different name " +
        "THEN it succeeds, remains in ACTIVATING, and bumps the version"
    )
    public void shouldRemainInActivating_whenDisplayNameUpdated(TrustRelationship tr) {

        assertThat(tr).isInActivatingStatus();

        TrustResult<TrustRelationship> result = tr.updateDisplayName(
            io.jans.shibboleth.model.core.DisplayName.of(tr.getDisplayName().getValue() + "_x").getValue());

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInActivatingStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN updateDescription() is called with a different description " +
        "THEN it succeeds and remains in ACTIVATING"
    )
    public void shouldRemainInActivating_whenDescriptionUpdated(TrustRelationship tr) {

        assertThat(tr).isInActivatingStatus();

        TrustResult<TrustRelationship> result = tr.updateDescription(Description.of("Changed while activating"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInActivatingStatus();
    }

}
