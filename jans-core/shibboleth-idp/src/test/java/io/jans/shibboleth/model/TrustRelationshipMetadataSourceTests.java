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

@DisplayName("Metadata Source: Nature Support & Validation")
public class TrustRelationshipMetadataSourceTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#trustRelationshipsOfAllNaturesWithIncompatibleMetadataSources")
    @DisplayName(
        "GIVEN a TrustRelationship NOT in ACTIVATING state " +
        "WHEN updateMetadataSource() is called with a metadatasource incompatible with its nature " +
        "THEN should fail with IncompatibleMetadataSourceForNature "
    )
    public void shouldFailWhenUsingIncompatibleMetadataSourceForTrustNature(TrustRelationship tr, MetadataSource source) {

        assertThat(tr).isNotInStatus(TrustStatus.ACTIVATING);
        assertThat(tr).doesNotSupportMetadataSource(source);

        TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
        
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(IncompatibleMetadataSourceForNature.class);
    }

}
