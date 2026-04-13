package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.metadata.*;
import io.jans.shibboleth.model.util.TrustResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;


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
        assertThat(trustrelationship.isNew()).isTrue();
        assertThat(trustrelationship.getDisplayName()).isEqualTo(io.jans.shibboleth.model.core.DisplayName.of("TestTR").getValue());
        assertThat(trustrelationship.getDescription()).isEqualTo(Description.of("Test TR"));
        
        assertThat(trustrelationship.getNature()).isEqualTo(TrustNature.INDIVIDUAL);
        assertThat(trustrelationship.getStatus()).isEqualTo(TrustStatus.DRAFT);
        assertThat(trustrelationship.getVersion()).isEqualTo(1);
        
        assertThat(trustrelationship.hasNoMetadataSource()).isTrue();
        assertThat(trustrelationship.hasAnyDiscoveredEntityIds()).isFalse();
        assertThat(trustrelationship.hasAnyRegisteredIdpInstances()).isFalse();
        assertThat(trustrelationship.hasAnyWorkItem()).isFalse();

        assertThat(trustrelationship.hasNoEnabledProfiles()).isTrue();
        assertThat(trustrelationship.isUsingDefaultProfileConfigurationFor(ProfileType.SAML2_SSO)).isTrue();
        assertThat(trustrelationship.isUsingDefaultProfileConfigurationFor(ProfileType.SAML2_ECP)).isTrue();
        assertThat(trustrelationship.isUsingDefaultProfileConfigurationFor(ProfileType.SAML2_ATTRIBUTE_QUERY)).isTrue();
        assertThat(trustrelationship.isUsingDefaultProfileConfigurationFor(ProfileType.SAML2_ATTRIBUTE_RESOLUTION)).isTrue();
        assertThat(trustrelationship.isUsingDefaultProfileConfigurationFor(ProfileType.SHIBBOLETH_SSO)).isTrue();
        assertThat(trustrelationship.isUsingDefaultProfileConfigurationFor(ProfileType.SAML2_LOGOUT)).isTrue();    
    }


}