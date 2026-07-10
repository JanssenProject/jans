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

@DisplayName("Cross-Cutting Invariants & Lifecycle")
public class TrustRelationshipInvariantsAndLifecycleTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#readyTrustRelationshipsWithActivationDiagnostics")
    @DisplayName(
        "GIVEN a READY TrustRelationship with previous activation diagnostics " +
        "WHEN activate() is called " +
        "THEN should clear previous diagnostics and transition to ACTIVATING "
    )
    public void shouldClearPreviousDiagnostics_whenActivateIsCalledFromReady(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();
        assertThat(tr).hasActivationDiagnostics();

        TrustResult<TrustRelationship> result = tr.activate();

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).hasNoActivationDiagnostics();
        assertThat(updated).isInActivatingStatus();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsWithSuccessActivationDiagnostics")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN finalizeActivation() is called with a successful ActivationContext containing diagnostics " +
        "THEN the resulting TrustRelationship should be in ACTIVE state and contain the activation diagnostics " 
    )
    public void shouldIncludeActivationDiagnosticsAfterSuccessfulFinalizeActivation(TrustRelationship tr,ActivationDiagnostics success_diagnostics) {

        assertThat(tr).isInActivatingStatus();
        assertThat(success_diagnostics.getStatus()).isEqualTo(ActivationStatus.SUCCEEDED);

        TrustResult<TrustRelationship> result = tr.finalizeActivation(success_diagnostics);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInActiveStatus();
        assertThat(updated.getActivationDiagnostics()).isEqualTo(success_diagnostics);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsWithFailedActivationDiagnostics")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN finalizeActivation() is called with a failed ActivationContext " +
        "THEN the resulting TrustRelationship should be in READY state and contain the activation diagnostics "
    )
    public void shouldIncludeActivationDiagnosticsAfterFailedFinalizeActivation(TrustRelationship tr, ActivationDiagnostics failed_diagnostics) {

        assertThat(tr).isInActivatingStatus();
        assertThat(failed_diagnostics.getStatus()).isEqualTo(ActivationStatus.FAILED);

        TrustResult<TrustRelationship> result = tr.finalizeActivation(failed_diagnostics);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInReadyStatus();
        assertThat(updated.getActivationDiagnostics()).isEqualTo(failed_diagnostics);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#trustRelationshipsOfVariousStatuses")
    @DisplayName(
        "GIVEN a fully populated TrustRelationship in any state " +
        "WHEN it is rebuilt using the builder from persisted data (reconstruction scenario) " +
        "THEN all data, state, version and invariants should be preserved " 
    )
    public void shouldPreserveAllDataWhenRebuildingFromStorage(TrustRelationship tr) {

        TrustResult<TrustRelationship> result  = TrustRelationship.builder()
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
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#trustRelationshipsWithIdempotentUpdateOperations")
    @DisplayName(
        "GIVEN a TrustRelationship in any valid state where the operation is allowed " +
        "WHEN any update method/operation is called with the same current value " +
        "THEN version should not be incremented and state should remain the same "
    )
    public void shouldMaintainVersionWhenNoActualChangeInAnyState(TrustRelationship tr, 
        Function<TrustRelationship,TrustResult<TrustRelationship>> operation) {

        TrustResult<TrustRelationship> result = operation.apply(tr);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getVersion()).isEqualTo(tr.getVersion());

    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activatingTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVATING TrustRelationship " +
        "WHEN incorporateDiscoveredEntityIds() is called with null EntityIds " +
        "THEN should fail with appropriate error "
    )
    public void shouldFailIncorporateDiscoveredEntityIdsWhenEntityIdsIsNull(TrustRelationship tr) {

        assertThat(tr).isInActivatingStatus();

        TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
    }

}
