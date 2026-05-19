package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.metadata.FileMetadataSource;
import io.jans.shibboleth.model.metadata.MetadataSource;
import io.jans.shibboleth.model.metadata.MetadataSourceType;
import io.jans.shibboleth.model.metadata.NoMetadataSource;
import io.jans.shibboleth.model.metadata.UpstreamMetadataSource;
import io.jans.shibboleth.model.config.profiles.common.ProfileType;
import io.jans.shibboleth.model.util.TrustResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static io.jans.shibboleth.model.TrustRelationshipAssert.assertThat;



public class TrustRelationshipTest {
    

    private static final TrustRelationship draftIndividualTrustRelationship() {
        
        return TrustRelationship.create("IndividualTR","Individual TR",TrustNature.INDIVIDUAL).getValue();
    }

    private static final TrustRelationship draftAggregateTrustRelationship() {

        return TrustRelationship.create("AggregateTR", "Aggregate TR", TrustNature.AGGREGATE).getValue();
    }

    private static final TrustRelationship activatingIndividualTrustRelationship() {

        /*TrustRelationship tr = createDraftIndividualTrustRelationship();

        tr.updateMetadataSource(validFileMetadataSource());
        tr.enableSamlProfileForTesting(ProfileType.SAML2_SSO);
        */
        return null;
    }

    public static final Stream<TrustRelationship> draftTrustRelationshipsByNature() {

        return Stream.of(draftIndividualTrustRelationship(),draftAggregateTrustRelationship());
    }

    private static final Stream<TrustRelationship> activatingTrustRelationshipsByNature() {

        return null;
    }

    public static final Stream<MetadataSource> sourcesNotAllowedForAggregateTrustRelationship() {

        MetadataSource upstream = mock(MetadataSource.class);
        when(upstream.getType()).thenReturn(MetadataSourceType.UPSTREAM);

        MetadataSource manual = mock(MetadataSource.class);
        when(manual.getType()).thenReturn(MetadataSourceType.MANUAL);

        return Stream.of(upstream,manual);
    }

    public static final Stream<MetadataSource> sourcesNotAllowedForIndividualTrustRelationship() {

        MetadataSource mdq = mock(MetadataSource.class);
        when(mdq.getType()).thenReturn(MetadataSourceType.MDQ);

        return Stream.of(mdq);
    }

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

        @ParameterizedTest
        @EnumSource(TrustNature.class)
        @DisplayName(
            "Given valid parameters " + 
            "WHEN creating trustrelationship " +
            "THEN it should create a trust relationship in DRAFT STATUS " +
            "  with version 1 " +
            "  with no metadata source configured yet " +
            "  and using all default profile configurations ")
        public void shouldCreateTrustRelationshipInInitialDraftState(TrustNature nature) {
            
            TrustResult<TrustRelationship> result = TrustRelationship.create("TestTR","Test TR",nature);

            assertThat(result.isSuccess()).isTrue();

            TrustRelationship trustrelationship = result.getValue();
            var newDisplayName = io.jans.shibboleth.model.core.DisplayName.of("TestTR").getValue();

            assertThat(trustrelationship).isNew()
                .hasDisplayName(newDisplayName)
                .hasDescription(Description.of("Test TR"))
                .isOfNature(nature)
                .hasStatus(TrustStatus.DRAFT)
                .isVersion(1)
                .hasNoMetadataSource()
                .hasNoDiscoveredEntityIds();
        
            assertThat(trustrelationship).withProfileConfiguration(ProfileType.SHIBBOLETH_SSO)
                .usesDefaultConfiguration();
        
            assertThat(trustrelationship).withProfileConfiguration(ProfileType.SAML2_ATTRIBUTE_QUERY)
                .usesDefaultConfiguration();

            assertThat(trustrelationship).withProfileConfiguration(ProfileType.SAML2_ARTIFACT_RESOLUTION)
                .usesDefaultConfiguration();
        
            assertThat(trustrelationship).withProfileConfiguration(ProfileType.SAML2_ECP)
                .usesDefaultConfiguration();

            assertThat(trustrelationship).withProfileConfiguration(ProfileType.SAML2_LOGOUT)
                .usesDefaultConfiguration();
        }
    }

    @Nested
    @DisplayName("Basic Info Update")
    public class BasicInfoUpdateTests {


        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "Given a null display name " + 
            "WHEN updating the display name of an existing TrustRelationship " +
            "THEN the update should fail with a display name required error"
        )
        public void shouldFailWithDisplayNameRequiredError(TrustRelationship tr) {

            //Given
            io.jans.shibboleth.model.core.DisplayName newDisplayName = null;

            //When
            TrustResult<Void> result = tr.updateDisplayName(null);

            //Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DisplayNameError.class);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "Given a null description " + 
            "WHEN updating the description of an existing TrustRelationship " +
            "THEN description is cleared and version is incremented")
        public void shouldAllowNullToClearDescription(TrustRelationship tr) {

            //Given
            int originalVersion = tr.getVersion();
            Description description = null;

            //When
            TrustResult<Void> result = tr.updateDescription(description);

            //Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(tr).hasDescription(Description.of(""));
            assertThat(tr).isVersion(originalVersion+1);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "Given a valid new display name " +
            "WHEN updating the display name of an existing TrustRelationship " + 
            "THEN display name is changed successfully "+
            "status stays the same, " +
            "and version is incremented")
        public void shouldUpdateDisplayNameWhileKeepingStatusAndIncrementingVersion(TrustRelationship tr) {

            //Given
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

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "Given a valid new description " + 
            "WHEN updating the description of an existing TrustRelationship " + 
            "THEN description is changed successfully " +
            "status stays the same, and version is incremented")
        public void shouldUpdateDescriptionWhileKeepingStatusAndIncrementingVersion(TrustRelationship tr) {

            //Given
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
    @DisplayName("Metadata Source Update Tests")
    public class MetadataSourceUpdateTests {

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "GIVEN an existing TrustRelationship  " +
            "WHEN updateMetadataSource(null) is called " +
            "THEN return with missing metadata source error")
        public void shouldRejectNullMetadataSource(TrustRelationship tr) {

            TrustResult<Void> result = tr.updateMetadataSource(null);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(CannotBeNullOrBlank.class);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#sourcesNotAllowedForAggregateTrustRelationship")
        @DisplayName(
            "GIVEN an existing TrustRelationship of AGGREGATE nature " +
            "WHEN updateMetadataSource is called with an unsupported source type " +
            "THEN return with operation restricted to nature error")
        public void shouldRejectUnsupportedMetadataTypeForAggregate(MetadataSource source) {

            TrustRelationship tr = draftAggregateTrustRelationship();
            TrustResult<Void> result = tr.updateMetadataSource(source);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(OperationRestrictedToNature.class);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#sourcesNotAllowedForIndividualTrustRelationship")
        @DisplayName(
            "GIVEN an existing TrustRelationship of INDIVIDUAL Nature " +
            "WHEN updateMetadataSource is called with an unsupported source type " +
            "THEN return with operation restricted to nature error")
        public void shouldRejectUnsupportedMetadataTypeForInvididual(MetadataSource source) {

            TrustRelationship tr = draftIndividualTrustRelationship();
            TrustResult<Void> result = tr.updateMetadataSource(source);
            
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(OperationRestrictedToNature.class);
        }

        @Disabled
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#activatingTrustRelationshipsByNature")
        @DisplayName(
            "GIVEN an existing TrustRelationship in ACTIVATING state " +
            "WHEN updateMetadataSource is called with a  supported and valid metadata source type " +
            "THEN return with operation restricted by status "
        )
        public void shouldRejectAnyMetadataChangeWhileActivating(TrustRelationship tr) {

            assertThat(tr).hasStatus(TrustStatus.ACTIVATING);

            TrustResult<Void> result = tr.updateMetadataSource(null);
        }
    }

    @Nested
    @DisplayName("Discovered EntityIds Incorporation Tests")
    public class DiscoveredEntityIdsIncorporationTests {

        @Test
        @DisplayName(
            "GIVEN an existing TrustRelationship that is not of nature AGGREGATE " + 
            "WHEN incorporateDiscoveredEntityIds is called with a non-null parameter "  +
            "THEN the operation should fail, with an error of operationrestrictedtonature " )
        public void shouldFailIfTrustRelationshipNatureIsNotAggregate() {

            TrustRelationship tr = draftIndividualTrustRelationship();
            assertThat(tr).isOfIndividualNature();

            EntityIds entityIds = EntityIds.empty();
            TrustResult<Void> result = tr.incorporateDiscoveredEntityIds(entityIds);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(OperationRestrictedToNature.class);
        }

        @Test
        @DisplayName(
            "GIVEN an existing TrustRelationship that is of nature AGGREGATE " +
            "WHEN incorporateDiscoveredEntityIds is called with a null entityIds " +
            "THEN the operation should fail, with an error of cannotbenullorblank " )
        public void shouldFailIfEntityIdsIsNull() {

            TrustRelationship tr = draftAggregateTrustRelationship();
           
            EntityIds entityIds = null;
            TrustResult<Void> result = tr.incorporateDiscoveredEntityIds(entityIds);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(CannotBeNullOrBlank.class);
        }

        @Test
        @DisplayName( 
            "GIVEN an existing TrustRelationship that: " +
            " - is of nature aggregate " +
            " - is of any status *but* ACTIVATING " + 
            "WHEN  incorporateDiscoveredEntityIds is called with a non-null parameter " +
            "THEN the operation should fail, with an error of invalidstatusforoperation " )
        public void shouldFailIfTrustRelationshipIsInInvalidStatus() {
            
            TrustRelationship tr = draftAggregateTrustRelationship();
            assertThat(tr).isOfAggregateNature();
            assertThat(tr).doesNotHaveStatus(TrustStatus.ACTIVATING);

            TrustResult<Void> result = tr.incorporateDiscoveredEntityIds(EntityIds.empty());
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(InvalidStatusForOperation.class);
        }
      
    }
}