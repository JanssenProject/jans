package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.config.profiles.common.ProfileType;
import io.jans.shibboleth.model.util.TrustResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.model.TrustRelationshipAssert.assertThat;



public class TrustRelationshipTest {
    

    @Nested
    @DisplayName("Creation")
    public class CreationTests {

        @Test
        @DisplayName("Given blank or null displayName " +
                     "WHEN creating trustrelationship " + 
                     "THEN creation fails with a DisplayNameError")
        public void shouldRejectNullOrEmptyDisplayName() {

            final TrustNature nature = TrustNature.INDIVIDUAL;
            TrustResult<TrustRelationship> result = TrustRelationship.create(null,"Jans TR",nature);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DisplayNameError.class);

            result = TrustRelationship.create(" ","JansRP",nature);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DisplayNameError.class);
        }

        @Test
        @DisplayName("Given null trust nature " + 
                     "WHEN creating trustrelationship " +
                     "THEN creation fails with a TrustNatureError ")
        public void shouldRejectNoTrustNature() {

            TrustResult<TrustRelationship> result = TrustRelationship.create("JansTR","Jans TR",null);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TrustNatureError.class);
        }

        @Test
        @DisplayName("Given null description " +
                     "WHEN creating trustrelationship " + 
                     "THEN it is created with a blank description")
        public void shouldCreateTrustRelationshipWithBlankDescription() {

            TrustResult<TrustRelationship> result = TrustRelationship.create("JansTR",null,TrustNature.INDIVIDUAL);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().getDescription()).isEqualTo(Description.of(""));
        }

        @Test
        @DisplayName("Given valid parameters " + 
                     "WHEN creating trustrelationship " +
                     "THEN it should create a trust relationship in DRAFT STATUS " +
                     "  with version 1 " +
                     "  with no metadata source configured yet " +
                     "  and using all default profile configurations ")
        public void shouldCreateTrustRelationshipInInitialDraftState() {
            
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
}