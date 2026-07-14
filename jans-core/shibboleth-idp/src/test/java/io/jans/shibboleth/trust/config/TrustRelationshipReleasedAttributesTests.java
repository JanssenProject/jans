package io.jans.shibboleth.trust.config;


import io.jans.shibboleth.trust.shared.Result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.trust.config.TrustRelationshipAssert.assertThat;
import static io.jans.shibboleth.trust.config.TrustRelationshipFixtures.*;

@DisplayName("Released Attributes")
public class TrustRelationshipReleasedAttributesTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsAndReleasedAttributes")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship " +
        "WHEN updateReleasedAttributes is called with a valid parameter " +
        "THEN the operation updates the released attributes and maintains the DRAFT status"
    )
    public void shouldUpdateReleasedAttributesAndStayInDraft(TrustRelationship tr, ReleasedAttributes attributes) {

        assertThat(tr).isInDraftStatus();
        assertThat(attributes).isNotNull();

        Result<TrustRelationship> result = tr.updateReleasedAttributes(attributes);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship same_or_updated_tr = result.getValue();

        assertThat(same_or_updated_tr).isInDraftStatus();
        assertThat(same_or_updated_tr.getReleasedAttributes()).isEqualTo(attributes);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#readyTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a READY trust relationship " +
        "WHEN updateReleasedAttributes() is called " +
        "THEN it succeeds and remains in READY"
    )
    public void shouldRemainInReady_whenReleasedAttributesUpdated(TrustRelationship tr) {

        assertThat(tr).isInReadyStatus();

        Result<TrustRelationship> result = tr.updateReleasedAttributes(sampleReleasedAttributes());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInReadyStatus();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an INACTIVE trust relationship " +
        "WHEN updateReleasedAttributes() is called " +
        "THEN it succeeds and remains in INACTIVE"
    )
    public void shouldRemainInInactive_whenReleasedAttributesUpdated(TrustRelationship tr) {

        assertThat(tr).isInInactiveStatus();

        Result<TrustRelationship> result = tr.updateReleasedAttributes(sampleReleasedAttributes());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInInactiveStatus();
    }

    @Test
    @DisplayName(
        "GIVEN a trust relationship at a given version " +
        "WHEN updateReleasedAttributes() changes the attributes " +
        "THEN the version is incremented"
    )
    public void shouldIncrementVersion_whenReleasedAttributesChanged() {

        TrustRelationship tr = sampleDraftIndividualTrustRelationship();
        assertThat(tr).hasNoReleasedAttributes();

        Result<TrustRelationship> result = tr.updateReleasedAttributes(sampleReleasedAttributes());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getVersion()).isEqualTo(tr.getVersion().next());
    }

}
