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

@DisplayName("Descriptive Updates & Version Semantics")
public class TrustRelationshipDescriptiveUpdateTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship with an existing display name " +
        "WHEN updateDisplayName() is called with a different name " +
        "THEN the operation succeeds, updates the display name and increments the version"
    )
    public void shouldUpdateDisplayNameAndIncrementVersion_whenDifferentNameProvided(TrustRelationship tr) {

        var newDisplayName = io.jans.shibboleth.model.core.DisplayName.of(tr.getDisplayName().getValue() + "_updated").getValue();

        assertThat(tr.getDisplayName()).isNotNull();
        assertThat(tr.getDisplayName()).isNotEqualTo(newDisplayName);

        TrustResult<TrustRelationship> result = tr.updateDisplayName(newDisplayName);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated_tr = result.getValue();
        assertThat(updated_tr.getDisplayName()).isEqualTo(newDisplayName);
        assertThat(updated_tr.getVersion()).isEqualTo(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship with an existing display name " +
        "WHEN updateDisplayName() is called with the same current name " +
        "THEN the operation should be idempotent (it should not change the TrustRelationship)"
    )
    public void shouldNotChangeState_whenUpdateDisplayNameWithSameName(TrustRelationship tr) {

        var sameDisplayName = io.jans.shibboleth.model.core.DisplayName.of(tr.getDisplayName().getValue()).getValue();
        assertThat(tr.getDisplayName()).isEqualTo(sameDisplayName);

        TrustResult<TrustRelationship> result = tr.updateDisplayName(sameDisplayName);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship same_tr = result.getValue();
        assertThat(same_tr).isEqualTo(tr);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship with an existing description " + 
        "WHEN updateDescription() is called with a different description " + 
        "THEN the operation succeeds, updates the description and increments the version "
    )
    public void shouldUpdateDescriptionAndIncrementVersion_whenDifferentDescriptionProvided(TrustRelationship tr) {

        Description newDescription = Description.of(tr.getDescription().getValue()+" Updated");
        assertThat(tr.getDescription()).isNotEqualTo(newDescription);

        TrustResult<TrustRelationship> result = tr.updateDescription(newDescription);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated_tr = result.getValue();

        assertThat(updated_tr.getDescription()).isEqualTo(newDescription);
        assertThat(updated_tr.getVersion()).isEqualTo(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship with an existing description " +
        "WHEN updateDescription() is called with the same current description " +
        "THEN the operation is idempotent (no changes to the TrustRelationship) " 
    )
    public void shouldNotChangeState_whenUpdateDescriptionWithSameDescription(TrustRelationship tr) {

        Description sameDescription = Description.of(tr.getDescription().getValue());
        assertThat(tr.getDescription()).isEqualTo(sameDescription);

        TrustResult<TrustRelationship> result = tr.updateDescription(sameDescription);
        assertThat(result.isSuccess()).isTrue();

        TrustRelationship same_tr = result.getValue();
        assertThat(same_tr).isEqualTo(tr);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship " +
        "WHEN updateDescription() is called with a blank description " +
        "THEN it succeeds because a blank description is allowed"
    )
    public void shouldUpdateDescription_whenNewDescriptionIsBlank(TrustRelationship tr) {

        Description blankDescription = Description.of("   ");

        TrustResult<TrustRelationship> result = tr.updateDescription(blankDescription);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated_tr = result.getValue();
        assertThat(updated_tr.getDescription()).isEqualTo(blankDescription);
        assertThat(updated_tr.getDescription().getValue()).isEmpty();
    }

    @Test
    @DisplayName(
        "GIVEN a trust relationship at a given version " +
        "WHEN any field is effectively changed " +
        "THEN the version is incremented exactly once"
    )
    public void shouldIncrementVersionByOne_whenEffectivelyModified() {

        TrustRelationship draft = sampleDraftIndividualTrustRelationship();
        Version expected = draft.getVersion().next();

        assertThat(draft.updateDisplayName(
                io.jans.shibboleth.model.core.DisplayName.of("Renamed").getValue())
            .getValue().getVersion()).isEqualTo(expected);

        assertThat(draft.updateDescription(Description.of("Changed"))
            .getValue().getVersion()).isEqualTo(expected);

        assertThat(draft.updateMetadataSource(sampleFileMetadataSource())
            .getValue().getVersion()).isEqualTo(expected);

        assertThat(draft.updateReleasedAttributes(sampleReleasedAttributes())
            .getValue().getVersion()).isEqualTo(expected);

        assertThat(draft.updateShibbolethSsoProfileConfiguration(activeShibbolethSsoProfileConfiguration())
            .getValue().getVersion()).isEqualTo(expected);
    }

}
