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

@DisplayName("DRAFT State Transitions")
public class TrustRelationshipDraftTransitionTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#draftTrustRelationshipsWithSupportedMetadataSources")
    @DisplayName (
        "GIVEN a DRAFT TrustRelationship with no active profiles " +
        "WHEN updateMetadataSource() is called " +
        "THEN the operation updates the metadata source, and maintains the DRAFT status"
    )
    public void shouldUpdateMetadataSourceAndStayInDraft_whenNoActiveProfiles(TrustRelationship tr,MetadataSource source) {

        assertThat(tr).isInDraftStatus();
        assertThat(tr).hasNoActiveProfileConfiguration();

        TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated_tr = result.getValue();
        assertThat(updated_tr.getMetadataSource()).isEqualTo(source);
        assertThat(updated_tr).isInDraftStatus();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#draftTrustRelationshipsWithAnActiveProfileAndRealMetadataSource")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship  with at least one active profile " +
        "WHEN updateMetadataSource() is called with a no-NONE metadata source " +
        "THEN the TrustRelationship should transition to READY state AND increment version "
    )
    public void shouldTransitionToReady_whenRealMetadataSourceAddedWithActiveProfile(TrustRelationship tr,  MetadataSource source) {

        assertThat(tr).isInDraftStatus();
        assertThat(tr).hasAtLeastOneActiveProfileConfiguration();
        assertThat(source.getType()).isNotEqualTo(MetadataSourceType.NONE);

        TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInReadyStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#draftTrustRelationshipsWithRealMetadataSourceAndActiveProfileConfigurationToUpdate")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship with a REAL(non-NONE) metadata source " +
        "WHEN updateXXXProfileConfiguration is called with an ACTIVE profile configuration " +
        "THEN the TrustRelationship should transition to READY status AND increment version "
    )
    public void shouldTransitionToReady_whenProfileConfigurationEnabledWithRealMetadataSource(TrustRelationship tr, Object profileconfig, ProfileConfigurationAccessor accessor) {

        assertThat(tr).isInDraftStatus();
        assertThat(tr).hasRealMetadataSource();
        assertThat(accessor.getStatus(profileconfig)).isEqualTo(ProfileStatus.ACTIVE);

        TrustResult<TrustRelationship> result = accessor.update(tr,profileconfig);
        
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated).isInReadyStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

}
