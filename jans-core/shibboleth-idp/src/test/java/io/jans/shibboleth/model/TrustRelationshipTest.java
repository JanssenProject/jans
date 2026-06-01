package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.metadata.FileMetadataSource;
import io.jans.shibboleth.model.metadata.MetadataSource;
import io.jans.shibboleth.model.metadata.MetadataSourceType;
import io.jans.shibboleth.model.metadata.NoMetadataSource;
import io.jans.shibboleth.model.metadata.UpstreamMetadataSource;

import io.jans.shibboleth.model.config.profiles.*;
import io.jans.shibboleth.model.config.profiles.common.*;
import io.jans.shibboleth.model.util.TrustResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import io.jans.shibboleth.model.config.profiles.capabilities.CommonConfigurationCapable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import java.time.Duration;

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

    public static final Stream<? extends CommonConfigurationCapable> allProfileConfigurationDefaults() {

        return Stream.of(
            SamlProfileConfigurationDefaults.shibbolethSso(),
            SamlProfileConfigurationDefaults.saml2AttributeQuery(),
            SamlProfileConfigurationDefaults.saml2ArtifactResolution(),
            SamlProfileConfigurationDefaults.saml2Ecp(),
            SamlProfileConfigurationDefaults.saml2Sso(),
            SamlProfileConfigurationDefaults.saml2Logout()
        );
    }

    @Nested
    @DisplayName("TrustRelationship Creation Tests")
    public class CreationTests {

        @Tag("refactoring")
        @ParameterizedTest
        @EnumSource(TrustNature.class)
        @DisplayName(
            "GIVEN all valid creation parameters " + 
            "WHEN creating trustrelationship " +
            "THEN it should create a trust relationship in DRAFT STATUS " +
            "  with initial version " +
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
                .isVersion(Version.initial())
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

        @Tag("refactoring")
        @Test
        @DisplayName(
            "GIVEN blank or null displayName " +
            "WHEN creating trustrelationship " + 
            "THEN creation fails with a CannotBeNullOrBlank error")
        public void shouldRejectNullOrEmptyDisplayName() {

            final TrustNature nature = TrustNature.INDIVIDUAL;
            TrustResult<TrustRelationship> result = TrustRelationship.create(null,"Jans TR",nature);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TrustRelationshipCreationFailed.class);

            result = TrustRelationship.create(" ","JansRP",nature);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TrustRelationshipCreationFailed.class);
        }

        @Tag("refactoring")
        @Test
        @DisplayName(
            "GIVEN null trust nature " + 
            "WHEN creating trustrelationship " +
            "THEN creation fails with a CannotBeNullOrBlank error ")
        public void shouldRejectNoTrustNature() {

            TrustResult<TrustRelationship> result = TrustRelationship.create("JansTR","Jans TR",null);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TrustRelationshipCreationFailed.class);
        }

        @Tag("refactoring")
        @Test
        @DisplayName(
            "GIVEN a valid display name, a valid trust nature and a null description " +
            "WHEN creating a new trustrelationship " + 
            "THEN it is created with a blank description")
        public void shouldCreateTrustRelationshipWithBlankDescription() {

            TrustResult<TrustRelationship> result = TrustRelationship.create("JansTR",null,TrustNature.INDIVIDUAL);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().getDescription()).isEqualTo(Description.of(""));
        }

       
    }

    @Nested
    @DisplayName("Basic Info Update Tests")
    public class BasicInfoUpdateTests {


        @Tag("refactoring")
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "GIVEN  " +
            " - a valid trust relationship in any state " +
            " - a display name which is different from the current trust relationship's display name " +
            "WHEN updating the display name of the existing TrustRelationship with the new display name " +
            "THEN " +
            "  - the operation should succeed " +
            "  - the TrustRelationship's version should be incremented " +
            "  - the TrustRelationship's display name is the provided display name "
        )
        public void shouldUpdateDisplayNameAndIncrementVersionWhenNewDifferentNameIsProvided(TrustRelationship tr) {

            //Given 
            var newDisplayName = io.jans.shibboleth.model.core.DisplayName.of("Jans new TR").getValue();
            assertThat(tr.getDisplayName()).isNotEqualTo(newDisplayName);

            //When 
            TrustResult<TrustRelationship> result = tr.updateDisplayName(newDisplayName);

            //Then
            assertThat(result.isSuccess()).isTrue();
            final TrustRelationship newtr = result.getValue();
            assertThat(newtr.getVersion()).isEqualTo(tr.getVersion().next());
            assertThat(newtr.getDisplayName()).isEqualTo(newDisplayName);
        }

        @Tag("refactoring")
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "GIVEN " +
            " - a valid trust relationship in any state " +
            " - a description which is different from the current trust relationship's description " +
            "WHEN updating the description of the trust relationship with the new description " +
            "THEN " +
            " - the operation should succeed " +
            " - the trustrelationship's version should be incremented " +
            " - the trustrelationship's description is the same as the newly provided description "
        )
        public void shouldUpdateDescriptionAndIncrementVersionWhenNewDifferentDescriptionIsProvided(TrustRelationship tr) {

            //Given 
            Description new_description = Description.of("Jans New Description");
            assertThat(tr.getDescription()).isNotEqualTo(new_description);

            //When
            TrustResult<TrustRelationship> result = tr.updateDescription(new_description);

            //Then
            assertThat(result.isSuccess()).isTrue();
            final TrustRelationship newtr = result.getValue();
            assertThat(newtr.getVersion()).isEqualTo(tr.getVersion().next());
            assertThat(newtr.getDescription()).isEqualTo(new_description);
        }

        @Tag("refactoring")
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "GIVEN a valid trust relationship with a  non-empty description " + 
            "WHEN updateDescription(null) is called  " +
            "THEN description is cleared and version is incremented"
        )
        public void shouldAllowNullToClearDescription(TrustRelationship tr) {

            //Given 
            assertThat(tr.getDescription().getValue()).isNotBlank();

            //When
            TrustResult<TrustRelationship> result = tr.updateDescription(null);

            //Then
            assertThat(result.isSuccess()).isTrue();
            final TrustRelationship newtr = result.getValue();
            assertThat(newtr).hasDescription(Description.of(""));
            assertThat(newtr).isVersion(tr.getVersion().next());
        }

        @Tag("refactoring")
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "GIVEN  " +
            "   - a valid trustrelationship in any status " +
            "   - a display name identical to the existing trustrelationship's display name " +
            "WHEN updating the display name of the trustrelationship with the said display name " +
            "THEN " +
            "   - the update should succeed " +
            "   - the display name should be unchanged " +
            "   - the version should be unchanged "
        )
        public void shouldBeIdempotentWhenUpdatingDisplayName(TrustRelationship tr) {

            //Given
            var displayName = io.jans.shibboleth.model.core.DisplayName.of(tr.getDisplayName().getValue()).getValue();
            assertThat(displayName).isEqualTo(tr.getDisplayName());

            //When 
            TrustResult<TrustRelationship> result = tr.updateDisplayName(displayName);

            //Then
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship newtr = result.getValue();
            assertThat(newtr.getDisplayName()).isEqualTo(tr.getDisplayName());
            assertThat(newtr.getVersion()).isEqualTo(tr.getVersion());
        }

        @Tag("refactoring")
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "GIVEN  " +
            "   - a valid trustrelationship in any status " +
            "   - a description identical to the existing trustrelationship's description " +
            "WHEN updating the description of the trustrelationship with the said description " +
            "THEN   " +
            "   - the update should succeed " +
            "   - the description should be unchanged   " +
            "   - the version should be unchanged " 
        )
        public void shouldBeIdempotentWhenUpdatingDescription(TrustRelationship tr) {

            //Given 
            Description description = Description.of(tr.getDescription().getValue());
            assertThat(description).isEqualTo(tr.getDescription());

            //When 
            TrustResult<TrustRelationship> result = tr.updateDescription(description);

            //Then
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship newtr = result.getValue();
            assertThat(newtr.getDescription()).isEqualTo(tr.getDescription());
            assertThat(newtr.getVersion()).isEqualTo(tr.getVersion());
        }

        @Tag("refactoring")
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "Given a valid trust relationship " + 
            "WHEN updating the display name of an existing TrustRelationship with a null value " +
            "THEN the update should fail with a TrustRelationshipUpdateFailed error"
        )
        public void shouldFailWithTrustRelationshipUpdateFailedWhenDisplayNameIsNull(TrustRelationship tr) {

            //When
            TrustResult<TrustRelationship> result = tr.updateDisplayName(null);

            //Then
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TrustRelationshipUpdateFailed.class);
        }
    }

    @Nested
    @DisplayName("Metadata Source Update Tests")
    public class MetadataSourceUpdateTests {

        @Tag("refactoring")
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "GIVEN an existing TrustRelationship  " +
            "WHEN updateMetadataSource(null) is called " +
            "THEN the operation fails with trustrelationshipupdatefailed message")
        public void shouldRejectNullMetadataSource(TrustRelationship tr) {

            TrustResult<TrustRelationship> result = tr.updateMetadataSource(null);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TrustRelationshipUpdateFailed.class);
        }

        @Tag("refactoring")
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#sourcesNotAllowedForAggregateTrustRelationship")
        @DisplayName(
            "GIVEN an existing TrustRelationship of AGGREGATE nature " +
            "WHEN updateMetadataSource is called with an unsupported source type " +
            "THEN return with operation restricted to nature error")
        public void shouldRejectUnsupportedMetadataTypeForAggregate(MetadataSource source) {

            TrustRelationship tr = draftAggregateTrustRelationship();
            TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TrustRelationshipUpdateFailed.class);
        }

        @Tag("refactoring")
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#sourcesNotAllowedForIndividualTrustRelationship")
        @DisplayName(
            "GIVEN an existing TrustRelationship of INDIVIDUAL Nature " +
            "WHEN updateMetadataSource is called with an unsupported source type " +
            "THEN return with operation restricted to nature error")
        public void shouldRejectUnsupportedMetadataTypeForInvididual(MetadataSource source) {

            TrustRelationship tr = draftIndividualTrustRelationship();
            TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
            
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TrustRelationshipUpdateFailed.class);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#activatingTrustRelationshipsByNature")
        @DisplayName(
            "GIVEN an existing TrustRelationship in ACTIVATING state " +
            "WHEN updateMetadataSource is called with a  supported and valid metadata source type " +
            "THEN return with operation restricted by status "
        )
        public void shouldRejectAnyMetadataChangeWhileActivating(TrustRelationship tr) {

            assertThat(tr).hasStatus(TrustStatus.ACTIVATING);
        }
    }

    @Nested
    @DisplayName("Profile Configuration Update Tests")
    public class ProfileConfigurationUpdateTests {
        
        @Tag("refactoring")
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsByNature")
        @DisplayName(
            "GIVEN an existing TrustRelationship " +
            "WHEN updateProfileConfiguration(null) is called " +
            "THEN the operation fails with trustrelationship update failed"
        )
        public void shouldFailIfProfileConfigurationIsNull(TrustRelationship tr) {

            TrustResult<TrustRelationship> result = tr.updateProfileConfiguration(null);
            assertThat(result.isFailure());
            assertThat(result.getError()).isInstanceOf(TrustRelationshipUpdateFailed.class);
        }

        @Disabled
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#allProfileConfigurations")
        @DisplayName(
            "GIVEN an existing TrustRelationship which is an INDIVIDUAL tr " +
            "WHEN updateProfileConfiguration() is called with the exact same configuration instance " +
            "THEN it should return success with no side effects (no version change)"
        )
        public <T extends CommonConfigurationCapable> void shouldBeIdempotentWhenSameConfigurationIsProvided() {

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
            TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(entityIds);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TrustRelationshipUpdateFailed.class);
        }

        @Test
        @DisplayName(
            "GIVEN an existing TrustRelationship that is of nature AGGREGATE " +
            "WHEN incorporateDiscoveredEntityIds is called with a null entityIds " +
            "THEN the operation should fail, with an error of cannotbenullorblank " )
        public void shouldFailIfEntityIdsIsNull() {

            TrustRelationship tr = draftAggregateTrustRelationship();
           
            EntityIds entityIds = null;
            TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(entityIds);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TrustRelationshipUpdateFailed.class);
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

            TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(EntityIds.empty());
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(TrustRelationshipUpdateFailed.class);
        }
      
    }
}