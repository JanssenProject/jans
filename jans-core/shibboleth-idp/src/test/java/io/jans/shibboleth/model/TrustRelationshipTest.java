package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.metadata.*;
import io.jans.shibboleth.model.config.profiles.common.ProfileType;
import io.jans.shibboleth.model.util.TrustResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;
import static io.jans.shibboleth.model.TrustRelationshipAssert.*;
import static io.jans.shibboleth.model.config.profiles.ProfileConfigurationAssert.*;



public class TrustRelationshipTest {
    

    
    @Test
    @DisplayName("Given blank or null display name WHEN creating TrustRelationship THEN it should fail")
    public void shouldRejectNullOrEmptyDisplayName() {

        final TrustNature nature = TrustNature.INDIVIDUAL;
        TrustResult<TrustRelationship> result = TrustRelationship.create(null,"Test TR",nature);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DisplayNameError.class);

        result = TrustRelationship.create(" ","Test TR",nature);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DisplayNameError.class);
    }

    @Test
    @DisplayName("Given null trust nature WHEN creating TrustRelationship THEN it should fail")
    public void shouldRejectNullTrustNature() {

        TrustResult<TrustRelationship> result = TrustRelationship.create("TestTR","Test TR",null);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(TrustNatureError.class);
    }

    @Test
    @DisplayName("Given null description WHEN creating TrustRelationship THEN it should succeed with a blank description")
    public void shouldCreateTrustRelationshipWithBlankDescription() {

        TrustResult<TrustRelationship> result = TrustRelationship.create("TestTR",null,TrustNature.INDIVIDUAL);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().getDescription()).isEqualTo(Description.of(""));
    }

    @Test
    @DisplayName("Given valid parameters WHEN creating TrustRelationshhip THEN it should succeed with trust relationship in a valid state")
    public void shouldCreateTrustRelationshipInValidState() {

        TrustResult<TrustRelationship> result = TrustRelationship.create("TestTR","Test TR",TrustNature.INDIVIDUAL);

        assertTrue(result.isSuccess());

        TrustRelationship trustrelationship = result.getValue();
        assertThat(trustrelationship).isNew()
            .hasDisplayName("TestTR")
            .hasDescription("Test TR")
            .isOfNature(TrustNature.INDIVIDUAL)
            .hasStatus(TrustStatus.DRAFT)
            .isVersion(1)
            .hasNoMetadataSource()
            .hasNoDiscoveredEntityIds()
            .hasNoRegisteredIdpInstances()
            .hasNoWorkItem();
        
        assertThat(trustrelationship).withProfileConfiguration(ProfileType.SHIBBOLETH_SSO)
            .isInactive()
            .usesDefaultConfiguration();
        
        assertThat(trustrelationship).withProfileConfiguration(ProfileType.SAML2_ATTRIBUTE_QUERY)
            .isInactive()
            .usesDefaultConfiguration();

        assertThat(trustrelationship).withProfileConfiguration(ProfileType.SAML2_ARTIFACT_RESOLUTION)
            .isInactive()
            .usesDefaultConfiguration();
        
        assertThat(trustrelationship).withProfileConfiguration(ProfileType.SAML2_ECP)
            .isInactive()
            .usesDefaultConfiguration();

        assertThat(trustrelationship).withProfileConfiguration(ProfileType.SAML2_LOGOUT)
            .isInactive()
            .usesDefaultConfiguration();
    }


}