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

@DisplayName("ACTIVE State Transitions")
public class TrustRelationshipActiveTransitionTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activeTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN deactivate() is called " +
        "THEN should transition to INACTIVE state and increment version "
    )
    public void shouldTransitionToInactive_whenDeactivateCalledFromActive(TrustRelationship tr) {

        assertThat(tr).isInActiveStatus();
        
        TrustResult<TrustRelationship> result = tr.deactivate();

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInInactiveStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activeTrustRelationshipsOfAllNaturesWithDifferentMetadataSources")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN updateMetadataSource() is called with a different real metadata source " +
        "THEN should transition to ACTIVATING state and increment version "
    )
    public void shouldTransitionToActivating_whenMetadataSourceUpdatedFromActive(TrustRelationship tr, MetadataSource source) {
    
        assertThat(tr).isInActiveStatus();
        assertThat(source).isNotEqualTo(NoMetadataSource.getInstance());
        assertThat(source).isNotEqualTo(tr.getMetadataSource());
    
        TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInActivatingStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activeTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN updateMetadataSource() is called with the *same* current metadata source " +
        "THEN should remain in ACTIVE state and version should not change (idempotent) "
    )
    public void shouldRemainInActive_whenMetadataSourceUpdateIsNoOp(TrustRelationship tr) {

        assertThat(tr).isInActiveStatus();
        MetadataSource source = tr.getMetadataSource();

        TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship same = result.getValue();
        assertThat(same).isInActiveStatus();
        assertThat(same).isVersion(tr.getVersion());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activeTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN updateMetadataSource() is called with a metadatasource of type `NONE` " +
        "THEN should transition to DRAFT state and increment version "
    )
    public void shouldTransitionToDraft_whenMetadataSourceSetToNoneFromActive(TrustRelationship tr) {

        assertThat(tr).isInActiveStatus();
        MetadataSource source = NoMetadataSource.getInstance();

        TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInDraftStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activeTrustRelationshipsOfAllNaturesWithDifferentProfileConfiguration")
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

        TrustResult<TrustRelationship> result = accessor.update(tr, profileconfig);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInActivatingStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activeTrustRelationshipsOfAllNaturesWithAllProfileAccessors")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN updateXXXProfileConfiguration() is called with the *same* configuration as the current one " +
        "THEN should remaine in ACTIVE state and version should not change (idempotent) "
    )
    public void shouldRemainInActive_whenProfileConfigurationIsNoOp(TrustRelationship tr, ProfileConfigurationAccessor accessor) {

        assertThat(tr).isInActiveStatus();
        
        TrustResult<TrustRelationship> result = accessor.update(tr,accessor.extract(tr));
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship same = result.getValue();

        assertThat(same).isInActiveStatus();
        assertThat(same).isVersion(tr.getVersion());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#activeTrustRelationshipsOfAllNaturesWithActiveProfileAccessor")
    @DisplayName(
        "GIVEN an ACTIVE TrustRelationship " +
        "WHEN updateXXXProfileConfiguration() is called such that all profiles become disabled " +
        "THEN should transition to DRAFT state and increment version "
    )
    public void shouldTransitionToDraft_whenAllProfilesDisabledFromActive(TrustRelationship tr,ProfileConfigurationAccessor accessor) {

        assertThat(tr).isInActiveStatus();
        assertThat(tr).hasActiveProfileConfigurationCount(1);

        TrustResult<TrustRelationship> result = accessor.updateStatus(tr,ProfileStatus.INACTIVE);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInDraftStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#trustRelationshipsOfAllStatusesExceptActive")
    @DisplayName(
        "GIVEN a TrustRelationship that is not in ACTIVE state " +
        "WHEN deactivate() is called " +
        "THEN should fail with OperationForbiddenFromStatus error"
    )
    public void shouldFailWhenDeactivateCalledFromNonActiveState(TrustRelationship tr) {

        assertThat(tr).isNotInStatus(TrustStatus.ACTIVE);

        TrustResult<TrustRelationship> result = tr.deactivate();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(OperationForbiddenFromStatus.class);
    }

}
