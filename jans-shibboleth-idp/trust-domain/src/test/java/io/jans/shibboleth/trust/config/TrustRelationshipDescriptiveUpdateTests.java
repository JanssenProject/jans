package io.jans.shibboleth.trust.config;

import io.jans.shibboleth.trust.shared.Version;


import io.jans.shibboleth.trust.shared.Result;
import io.jans.shibboleth.trust.config.error.DomainObjectUpdateFailed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.trust.config.TrustRelationshipAssert.assertThat;

import static io.jans.shibboleth.trust.config.TrustRelationshipFixtures.*;

@DisplayName("Descriptive Updates & Version Semantics")
public class TrustRelationshipDescriptiveUpdateTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship with an existing display name " +
        "WHEN updateDisplayName() is called with a different name " +
        "THEN the operation succeeds, updates the display name and increments the version"
    )
    public void shouldUpdateDisplayNameAndIncrementVersion_whenDifferentNameProvided(TrustRelationship tr) {

        var newDisplayName = io.jans.shibboleth.trust.config.DisplayName.of(tr.getDisplayName().getValue() + "_updated").getValue();

        assertThat(tr.getDisplayName()).isNotNull();
        assertThat(tr.getDisplayName()).isNotEqualTo(newDisplayName);

        Result<TrustRelationship> result = tr.updateDisplayName(newDisplayName);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated_tr = result.getValue();
        assertThat(updated_tr.getDisplayName()).isEqualTo(newDisplayName);
        assertThat(updated_tr.getVersion()).isEqualTo(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship with an existing display name " +
        "WHEN updateDisplayName() is called with the same current name " +
        "THEN the operation should be idempotent (it should not change the TrustRelationship)"
    )
    public void shouldNotChangeState_whenUpdateDisplayNameWithSameName(TrustRelationship tr) {

        var sameDisplayName = io.jans.shibboleth.trust.config.DisplayName.of(tr.getDisplayName().getValue()).getValue();
        assertThat(tr.getDisplayName()).isEqualTo(sameDisplayName);

        Result<TrustRelationship> result = tr.updateDisplayName(sameDisplayName);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship same_tr = result.getValue();
        assertThat(same_tr).isEqualTo(tr);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship with an existing description " + 
        "WHEN updateDescription() is called with a different description " + 
        "THEN the operation succeeds, updates the description and increments the version "
    )
    public void shouldUpdateDescriptionAndIncrementVersion_whenDifferentDescriptionProvided(TrustRelationship tr) {

        Description newDescription = Description.of(tr.getDescription().getValue()+" Updated");
        assertThat(tr.getDescription()).isNotEqualTo(newDescription);

        Result<TrustRelationship> result = tr.updateDescription(newDescription);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated_tr = result.getValue();

        assertThat(updated_tr.getDescription()).isEqualTo(newDescription);
        assertThat(updated_tr.getVersion()).isEqualTo(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship with an existing description " +
        "WHEN updateDescription() is called with the same current description " +
        "THEN the operation is idempotent (no changes to the TrustRelationship) " 
    )
    public void shouldNotChangeState_whenUpdateDescriptionWithSameDescription(TrustRelationship tr) {

        Description sameDescription = Description.of(tr.getDescription().getValue());
        assertThat(tr.getDescription()).isEqualTo(sameDescription);

        Result<TrustRelationship> result = tr.updateDescription(sameDescription);
        assertThat(result.isSuccess()).isTrue();

        TrustRelationship same_tr = result.getValue();
        assertThat(same_tr).isEqualTo(tr);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship " +
        "WHEN updateDescription() is called with a blank description " +
        "THEN it succeeds because a blank description is allowed"
    )
    public void shouldUpdateDescription_whenNewDescriptionIsBlank(TrustRelationship tr) {

        Description blankDescription = Description.of("   ");

        Result<TrustRelationship> result = tr.updateDescription(blankDescription);

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
                io.jans.shibboleth.trust.config.DisplayName.of("Renamed").getValue())
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

    @Test
    @DisplayName(
        "GIVEN a DRAFT trust relationship " +
        "WHEN updateBasicInfo() is called with a different display name and description " +
        "THEN both are updated and it remains in DRAFT"
    )
    public void shouldUpdateBothFieldsAndRemainInDraft_whenBasicInfoChanged() {

        TrustRelationship draft = sampleDraftIndividualTrustRelationship();
        var newName = io.jans.shibboleth.trust.config.DisplayName.of(draft.getDisplayName().getValue() + "_updated").getValue();
        Description newDescription = Description.of(draft.getDescription().getValue() + " Updated");

        Result<TrustRelationship> result = draft.updateBasicInfo(newName, newDescription);

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(updated.getDisplayName()).isEqualTo(newName);
        assertThat(updated.getDescription()).isEqualTo(newDescription);
        assertThat(updated.getStatus()).isEqualTo(TrustStatus.DRAFT);
    }

    @Test
    @DisplayName(
        "GIVEN a DRAFT trust relationship " +
        "WHEN updateBasicInfo() changes both the display name and the description " +
        "THEN the version is incremented by exactly one"
    )
    public void shouldIncrementVersionExactlyOnce_whenBothBasicInfoFieldsChanged() {

        TrustRelationship draft = sampleDraftIndividualTrustRelationship();
        Version expected = draft.getVersion().next();

        var newName = io.jans.shibboleth.trust.config.DisplayName.of("Renamed").getValue();
        Description newDescription = Description.of("Changed");

        Result<TrustRelationship> result = draft.updateBasicInfo(newName, newDescription);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getVersion()).isEqualTo(expected);
    }

    @Test
    @DisplayName(
        "GIVEN a DRAFT trust relationship " +
        "WHEN updateBasicInfo() changes only the description and leaves the display name unchanged " +
        "THEN the version is incremented by exactly one"
    )
    public void shouldIncrementVersionExactlyOnce_whenOnlyOneBasicInfoFieldChanged() {

        TrustRelationship draft = sampleDraftIndividualTrustRelationship();
        Version expected = draft.getVersion().next();

        var sameName = io.jans.shibboleth.trust.config.DisplayName.of(draft.getDisplayName().getValue()).getValue();
        Description newDescription = Description.of(draft.getDescription().getValue() + " changed");

        Result<TrustRelationship> result = draft.updateBasicInfo(sameName, newDescription);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getVersion()).isEqualTo(expected);
    }

    @Test
    @DisplayName(
        "GIVEN a DRAFT trust relationship " +
        "WHEN updateBasicInfo() is called with the same display name and description " +
        "THEN neither the state nor the version changes"
    )
    public void shouldNotChangeStateOrVersion_whenBasicInfoUnchanged() {

        TrustRelationship draft = sampleDraftIndividualTrustRelationship();

        var sameName = io.jans.shibboleth.trust.config.DisplayName.of(draft.getDisplayName().getValue()).getValue();
        Description sameDescription = Description.of(draft.getDescription().getValue());

        Result<TrustRelationship> result = draft.updateBasicInfo(sameName, sameDescription);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(draft);
    }

    @Test
    @DisplayName(
        "GIVEN a DRAFT trust relationship " +
        "WHEN updateBasicInfo() is called with a null display name " +
        "THEN it fails with DomainObjectUpdateFailed and the original is unchanged"
    )
    public void shouldFail_whenBasicInfoDisplayNameIsNull() {

        TrustRelationship draft = sampleDraftIndividualTrustRelationship();

        Result<TrustRelationship> result = draft.updateBasicInfo(null, Description.of("anything"));

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
    }

    @Test
    @DisplayName(
        "GIVEN a DRAFT trust relationship " +
        "WHEN updateBasicInfo() is called with a null description " +
        "THEN it succeeds and the description becomes empty"
    )
    public void shouldNormaliseDescriptionToEmpty_whenBasicInfoDescriptionIsNull() {

        TrustRelationship draft = sampleDraftIndividualTrustRelationship();
        var sameName = io.jans.shibboleth.trust.config.DisplayName.of(draft.getDisplayName().getValue()).getValue();

        Result<TrustRelationship> result = draft.updateBasicInfo(sameName, Description.of(null));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getDescription().getValue()).isEmpty();
    }

}
