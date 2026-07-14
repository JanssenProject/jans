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

@DisplayName("Metadata Source: Nature Support & Validation")
public class TrustRelationshipMetadataSourceTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#trustRelationshipsOfAllNaturesWithIncompatibleMetadataSources")
    @DisplayName(
        "GIVEN a TrustRelationship NOT in ACTIVATING state " +
        "WHEN updateMetadataSource() is called with a metadatasource incompatible with its nature " +
        "THEN should fail with IncompatibleMetadataSourceForNature "
    )
    public void shouldFailWhenUsingIncompatibleMetadataSourceForTrustNature(TrustRelationship tr, MetadataSource source) {

        assertThat(tr).isNotInStatus(TrustStatus.ACTIVATING);
        assertThat(tr).doesNotSupportMetadataSource(source);

        Result<TrustRelationship> result = tr.updateMetadataSource(source);
        
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(IncompatibleMetadataSourceForNature.class);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT trust relationship with a NONE source " +
        "WHEN updateMetadataSource() is called with a real source " +
        "THEN it succeeds and the version is incremented")
    public void shouldIncrementVersion_whenRealMetadataSourceSetFromNone(TrustRelationship tr) {

        assertThat(tr).hasNoRealMetadataSource();

        Result<TrustRelationship> result = tr.updateMetadataSource(sampleFileMetadataSource());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue())
            .hasRealMetadataSource()
            .isVersion(tr.getVersion().next());
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a DRAFT trust relationship with a NONE source " +
        "WHEN updateMetadataSource() is called with NONE " +
        "THEN it succeeds and the version is unchanged")
    public void shouldMaintainVersion_whenMetadataSetToNoneOnNoneSource(TrustRelationship tr) {

        assertThat(tr).hasNoRealMetadataSource();

        Result<TrustRelationship> result = tr.updateMetadataSource(NoMetadataSource.getInstance());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue())
            .hasNoRealMetadataSource()
            .isVersion(tr.getVersion());
    }

}
