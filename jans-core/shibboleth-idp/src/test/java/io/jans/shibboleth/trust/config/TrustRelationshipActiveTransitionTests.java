package io.jans.shibboleth.trust.config;

import io.jans.shibboleth.trust.config.*;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationStatus;
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

@DisplayName("ACTIVE State Transitions")
public class TrustRelationshipActiveTransitionTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activeTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN deactivate() is called " +
        "THEN should transition to INACTIVE state and increment version "
    )
    public void shouldTransitionToInactive_whenDeactivateCalledFromActive(TrustRelationship tr) {

        assertThat(tr).isInActiveStatus();
        
        Result<TrustRelationship> result = tr.deactivate();

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInInactiveStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activeTrustRelationshipsOfAllNaturesWithDifferentMetadataSources")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN updateMetadataSource() is called with a different real metadata source " +
        "THEN should transition to ACTIVATING state and increment version "
    )
    public void shouldTransitionToActivating_whenMetadataSourceUpdatedFromActive(TrustRelationship tr, MetadataSource source) {
    
        assertThat(tr).isInActiveStatus();
        assertThat(source).isNotEqualTo(NoMetadataSource.getInstance());
        assertThat(source).isNotEqualTo(tr.getMetadataSource());
    
        Result<TrustRelationship> result = tr.updateMetadataSource(source);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInActivatingStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activeTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN updateMetadataSource() is called with the *same* current metadata source " +
        "THEN should remain in ACTIVE state and version should not change (idempotent) "
    )
    public void shouldRemainInActive_whenMetadataSourceUpdateIsNoOp(TrustRelationship tr) {

        assertThat(tr).isInActiveStatus();
        MetadataSource source = tr.getMetadataSource();

        Result<TrustRelationship> result = tr.updateMetadataSource(source);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship same = result.getValue();
        assertThat(same).isInActiveStatus();
        assertThat(same).isVersion(tr.getVersion());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activeTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN updateMetadataSource() is called with a metadatasource of type `NONE` " +
        "THEN should transition to DRAFT state and increment version "
    )
    public void shouldTransitionToDraft_whenMetadataSourceSetToNoneFromActive(TrustRelationship tr) {

        assertThat(tr).isInActiveStatus();
        MetadataSource source = NoMetadataSource.getInstance();

        Result<TrustRelationship> result = tr.updateMetadataSource(source);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInDraftStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activeTrustRelationshipsOfAllNaturesWithDifferentProfileConfiguration")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN updateXXXProfileConfiguration() is called with a *different* configuration than the current one " +
        "     that keeps at one active profile " +
        "THEN should transition to ACTIVATING state and increment version "
    )
    public void shouldTransitionToActivating_whenProfileConfigurationActuallyChangedFromActive(
        TrustRelationship tr,Object profileconfig, ProfileConfigurationAccessor accessor) {

        assertThat(tr).isInActiveStatus();
        assertThat(accessor.extract(tr)).isNotEqualTo(profileconfig);

        Result<TrustRelationship> result = accessor.update(tr, profileconfig);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInActivatingStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activeTrustRelationshipsOfAllNaturesWithAllProfileAccessors")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN updateXXXProfileConfiguration() is called with the *same* configuration as the current one " +
        "THEN should remaine in ACTIVE state and version should not change (idempotent) "
    )
    public void shouldRemainInActive_whenProfileConfigurationIsNoOp(TrustRelationship tr, ProfileConfigurationAccessor accessor) {

        assertThat(tr).isInActiveStatus();
        
        Result<TrustRelationship> result = accessor.update(tr,accessor.extract(tr));
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship same = result.getValue();

        assertThat(same).isInActiveStatus();
        assertThat(same).isVersion(tr.getVersion());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activeTrustRelationshipsOfAllNaturesWithActiveProfileAccessor")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN updateXXXProfileConfiguration() is called such that all profiles become disabled " +
        "THEN should transition to DRAFT state and increment version "
    )
    public void shouldTransitionToDraft_whenAllProfilesDisabledFromActive(TrustRelationship tr,ProfileConfigurationAccessor accessor) {

        assertThat(tr).isInActiveStatus();
        assertThat(tr).hasActiveProfileConfigurationCount(1);

        Result<TrustRelationship> result = accessor.updateStatus(tr,ProfileStatus.INACTIVE);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInDraftStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#trustRelationshipsOfAllStatusesExceptActive")
    @DisplayName(
        "GIVEN a TrustRelationship that is not in ACTIVE state " +
        "WHEN deactivate() is called " +
        "THEN should fail with OperationForbiddenFromStatus error"
    )
    public void shouldFailWhenDeactivateCalledFromNonActiveState(TrustRelationship tr) {

        assertThat(tr).isNotInStatus(TrustStatus.ACTIVE);

        Result<TrustRelationship> result = tr.deactivate();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(OperationForbiddenFromStatus.class);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activeTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVE trust relationship " +
        "WHEN a descriptive field is updated " +
        "THEN it remains in ACTIVE"
    )
    public void shouldRemainInActive_whenDescriptiveFieldUpdated(TrustRelationship tr) {

        assertThat(tr).isInActiveStatus();

        TrustRelationship afterDisplayName = tr.updateDisplayName(
            io.jans.shibboleth.trust.config.DisplayName.of(tr.getDisplayName().getValue() + "_x").getValue()).getValue();
        assertThat(afterDisplayName).isInActiveStatus();

        TrustRelationship afterDescription = tr.updateDescription(Description.of("Changed")).getValue();
        assertThat(afterDescription).isInActiveStatus();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activeTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVE trust relationship " +
        "WHEN updateReleasedAttributes() is called " +
        "THEN it remains in ACTIVE"
    )
    public void shouldRemainInActive_whenReleasedAttributesUpdated(TrustRelationship tr) {

        assertThat(tr).isInActiveStatus();

        Result<TrustRelationship> result = tr.updateReleasedAttributes(sampleReleasedAttributes());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInActiveStatus();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#activeTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVE trust relationship " +
        "WHEN activate() is called " +
        "THEN it fails with OperationForbiddenFromStatus and the original is unchanged"
    )
    public void shouldFailActivate_whenCalledFromActive(TrustRelationship tr) {

        assertThat(tr).isInActiveStatus();

        Result<TrustRelationship> result = tr.activate();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(OperationForbiddenFromStatus.class);
        OperationForbiddenFromStatus cause = (OperationForbiddenFromStatus) error.getCause();
        assertThat(cause.getOperationName()).isEqualTo("activate");
        assertThat(cause.getForbiddenStatus()).isEqualTo(TrustStatus.ACTIVE);
    }

}
