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

@DisplayName("Profile Configuration Fundamentals")
public class TrustRelationshipProfileConfigurationTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#draftTrustRelationshipsWithProfileConfigurationsAndAccessors")
    @DisplayName(
        "GIVEN a DRAFT TrustRelationship with no metadatasources " + 
        "WHEN updateXXXProfileConfiguration() is called " +
        "THEN the operation updates the profile configuration and maintains the DRAFT status "
    )
    public void shouldUpdateProfileConfigurationAndStayInDraft_whenNoMetadataSource(TrustRelationship tr, Object profileconfig,ProfileConfigurationAccessor accessor) {


        assertThat(tr).isInDraftStatus();
        assertThat(tr).hasNoRealMetadataSource();

        TrustResult<TrustRelationship> result = accessor.update(tr, profileconfig);
        assertThat(result.isSuccess()).isTrue();
        TrustRelationship updated = result.getValue();
        assertThat(accessor.extract(updated)).isEqualTo(profileconfig);
        assertThat(updated).isInDraftStatus();
    }

}
