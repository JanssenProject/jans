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

@DisplayName("INACTIVE State Transitions")
public class TrustRelationshipInactiveTransitionTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN an INACTIVE TrustRelationship with a real metadata source and at least one active profile " +
        "WHEN activate() is called " + 
        "THEN should transition to ACTIVATING state and increment version "
    )
    public void shouldTransitionToActivatingFromInactive_whenRequirementsMet(TrustRelationship tr) {

        assertThat(tr).isInInactiveStatus();
        assertThat(tr).hasRealMetadataSource();
        assertThat(tr).hasAtLeastOneActiveProfileConfiguration();

        TrustResult<TrustRelationship> result = tr.activate();

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInActivatingStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource({
        "io.jans.shibboleth.model.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNaturesWithNoRealMetadataSource",
        "io.jans.shibboleth.model.TrustRelationshipArguments#inactiveTrustRelationshipsOfAllNaturesWithNoActiveProfileConfiguration"
    })
    @DisplayName(
        "GIVEN an INACTIVE TrustRelationship with no real metadata source or no active profile " +
        "WHEN activate() is called " +
        "THEN should transition to DRAFT state and increment version "
    )
    public void shouldTransitionToDraft_whenActivateCalledFromInactiveButRequirementsNotMet(TrustRelationship tr) {

        assertThat(tr).isInInactiveStatus();
        assertThat(tr.hasNoRealMetadataSource() || tr.hasNoActiveProfileConfiguration()).isTrue();

        TrustResult<TrustRelationship> result = tr.activate();

        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();

        assertThat(updated).isInDraftStatus();
        assertThat(updated).isVersion(tr.getVersion().next());
    }

}
