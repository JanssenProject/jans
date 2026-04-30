package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.metadata.MetadataSource;
import io.jans.shibboleth.model.metadata.NoMetadataSource;
import io.jans.shibboleth.model.config.profiles.common.ProfileType;
import io.jans.shibboleth.model.util.TrustResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.model.TrustRelationshipAssert.assertThat;



public class TrustRelationshipTest {
    

    @Nested
    @DisplayName("TrustRelationship Creation")
    public class CreationTests {

        @Test
        @DisplayName(
            "Given blank or null displayName " +
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
        @DisplayName(
            "Given null trust nature " + 
            "WHEN creating trustrelationship " +
            "THEN creation fails with a TrustNatureError ")
        public void shouldRejectNoTrustNature() {

            TrustResult<TrustRelationship> result = TrustRelationship.create("JansTR","Jans TR",null);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TrustNatureError.class);
        }

        @Test
        @DisplayName(
            "Given null description " +
            "WHEN creating trustrelationship " + 
            "THEN it is created with a blank description")
        public void shouldCreateTrustRelationshipWithBlankDescription() {

            TrustResult<TrustRelationship> result = TrustRelationship.create("JansTR",null,TrustNature.INDIVIDUAL);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().getDescription()).isEqualTo(Description.of(""));
        }

        @Test
        @DisplayName(
            "Given valid parameters " + 
            "WHEN creating trustrelationship " +
            "THEN it should create a trust relationship in DRAFT STATUS " +
            "  with version 1 " +
            "  with no metadata source configured yet " +
            "  and using all default profile configurations ")
        public void shouldCreateTrustRelationshipInInitialDraftState() {
            
            TrustResult<TrustRelationship> result = TrustRelationship.create("TestTR","Test TR",TrustNature.INDIVIDUAL);

            assertThat(result.isSuccess()).isTrue();

            TrustRelationship trustrelationship = result.getValue();
            var newDisplayName = io.jans.shibboleth.model.core.DisplayName.of("TestTR").getValue();

            assertThat(trustrelationship).isNew()
                .hasDisplayName(newDisplayName)
                .hasDescription(Description.of("Test TR"))
                .isOfNature(TrustNature.INDIVIDUAL)
                .hasStatus(TrustStatus.DRAFT)
                .isVersion(1)
                .hasNoMetadataSource()
                .hasNoDiscoveredEntityIds();
        
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

    @Nested
    @DisplayName("Basic Info Update")
    public class BasicInfoUpdateTests {


        @Test
        @DisplayName(
            "Given a null display name " + 
            "WHEN updating the display name of an existing TrustRelationship " +
            "THEN the update should fail with a display name required error"
        )
        public void shouldFailWithDisplayNameRequiredError() {

            //Given 
            TrustRelationship tr = createValidTrustRelationship();
            io.jans.shibboleth.model.core.DisplayName newDisplayName = null;

            //When
            TrustResult<Void> result = tr.updateDisplayName(null);

            //Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DisplayNameError.class);
        }

        @Test
        @DisplayName(
            "Given a null description " + 
            "WHEN updating the description of an existing TrustRelationship " +
            "THEN description is cleared and version is incremented")
        public void shouldAllowNullToClearDescription() {

            //Given
            TrustRelationship tr = createValidTrustRelationship();
            int originalVersion = tr.getVersion();
            Description description = null;

            //When
            TrustResult<Void> result = tr.updateDescription(description);

            //Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(tr).hasDescription(Description.of(""));
            assertThat(tr).isVersion(originalVersion+1);
        }

        @Test
        @DisplayName(
            "Given a valid new display name " +
            "WHEN updating the display name of an existing TrustRelationship " + 
            "THEN display name is changed successfully "+
            "status stays the same, " +
            "and version is incremented")
        public void shouldUpdateDisplayNameWhileKeepingStatusAndIncrementingVersion() {

            //Given
            TrustRelationship tr = createValidTrustRelationship();
            int originalVersion = tr.getVersion();
            TrustStatus originalStatus = tr.getStatus();

            //When
            var newDisplayName = io.jans.shibboleth.model.core.DisplayName.of("NewTR").getValue();
            TrustResult<Void> result = tr.updateDisplayName(newDisplayName);

            //Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(tr).hasDisplayName(newDisplayName);
            assertThat(tr).hasStatus(originalStatus);
            assertThat(tr).isVersion(originalVersion+1);
        }

        @Test
        @DisplayName(
            "Given a valid new description " + 
            "WHEN updating the description of an existing TrustRelationship " + 
            "THEN description is changed successfully " +
            "status stays the same, and version is incremented")
        public void shouldUpdateDescriptionWhileKeepingStatusAndIncrementingVersion() {

            //Given
            TrustRelationship tr = createValidTrustRelationship();
            int originalVersion = tr.getVersion();
            TrustStatus originalStatus = tr.getStatus();

            //When
            Description newDescription = Description.of("New Description");
            TrustResult<Void> result = tr.updateDescription(newDescription);

            //Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(tr).hasDescription(newDescription);
            assertThat(tr).hasStatus(originalStatus);
            assertThat(tr).isVersion(originalVersion+1);
        }
    }

    @Nested
    @DisplayName("Metadata Source Update")
    public class MetadataSourceUpdateTests {


        @Test
        @DisplayName(
            "Given a null metadatasource " +
            "WHEN updating the metadata source of an existing TrustRelationship " +
            "THEN the operation should fail with a missing metadatasource error" )
        public void shouldFailWhenMetadataSourceIsNull() {

            //Given
            MetadataSource metadataSource = null;
            TrustRelationship tr = createValidTrustRelationship();
    

            //When
            TrustResult<Void> result = tr.updateMetadataSource(metadataSource);

            //Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(MetadataSourceError.class);

        }

        @Test
        @DisplayName(
            "GIVEN an existing TrustRelationship using NoMetadataSource " +
            "WHEN the same NoMetadataSource is provided again " +
            "THEN the update succeeds without changing status or version ")
        public void shouldTreatSameNoMetadataSourceAsNoOp() {

            //Given
            MetadataSource metadataSource = NoMetadataSource.getInstance();
            TrustRelationship tr = createValidTrustRelationship();
            int originalVersion = tr.getVersion();
            TrustStatus originalStatus = tr.getStatus();

            //When
            TrustResult<Void> result = tr.updateMetadataSource(metadataSource);

            //Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(tr.getMetadataSource()).isInstanceOf(NoMetadataSource.class);
            assertThat(tr).isVersion(originalVersion);
            assertThat(tr).hasStatus(originalStatus);
        }

        @Disabled
        @Test
        @DisplayName(
            " GIVEN an existing TrustRelationship that currently has: " +
            "   - a configured metadata source " +
            "   - pending work items " +
            "   - discovered entity IDs " +
            " WHEN updateMetadataSource is called with NoMetadataSource " +
            " THEN the operation succeeds, status switches to DRAFT, " +
            "   pending work items and discovered entity IDs are cleared, " +
            "   and version is incremented ")
        public void shouldSwitchToDraftAndClearPendingWorkAndDiscoveredEntityIdsWhenSettingNoMetadataSource() {

           //TODO: Circle back to this once other features this depends on are implemented 
        }

    }

    private TrustRelationship createValidTrustRelationship() {

        return TrustRelationship.create("JansTR","Jans TR",TrustNature.INDIVIDUAL).getValue();
    }
}