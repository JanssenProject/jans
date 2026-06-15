package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.metadata.FileMetadataSource;
import io.jans.shibboleth.model.metadata.ManualMetadataSource;
import io.jans.shibboleth.model.metadata.MdqMetadataSource;
import io.jans.shibboleth.model.metadata.MetadataSource;
import io.jans.shibboleth.model.metadata.MetadataSourceType;
import io.jans.shibboleth.model.metadata.NoMetadataSource;
import io.jans.shibboleth.model.metadata.UpstreamMetadataSource;
import io.jans.shibboleth.model.metadata.UriMetadataSource;
import io.jans.shibboleth.model.metadata.manual.AssertionConsumerService;
import io.jans.shibboleth.model.metadata.manual.SamlBinding;
import io.jans.shibboleth.model.metadata.manual.ValidityPeriod;
import io.jans.shibboleth.model.config.profiles.*;
import io.jans.shibboleth.model.config.profiles.common.*;
import io.jans.shibboleth.model.util.TrustResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import io.jans.shibboleth.model.config.profiles.capabilities.CommonConfigurationCapable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import org.junit.jupiter.api.Tag;
//import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static io.jans.shibboleth.model.TrustRelationshipAssert.assertThat;
import static io.jans.shibboleth.model.config.profiles.ProfileConfigurationAssert.assertThat;



public class TrustRelationshipTest {
    

    private static final Stream<Arguments> creationParametersWithValidValues() {

        var individualTrDisplayName = io.jans.shibboleth.model.core.DisplayName.of("IndividualTR").getValue();
        var aggregateTrDisplayName  = io.jans.shibboleth.model.core.DisplayName.of("AggregateTR").getValue();

        var individualTrDescription = Description.of("Individual TR");
        var aggregateTrDescription  = Description.of("Aggregate TR");
        return Stream.of(
            Arguments.of(individualTrDisplayName,individualTrDescription,TrustNature.INDIVIDUAL),
            Arguments.of(aggregateTrDisplayName,aggregateTrDescription,TrustNature.AGGREGATE)
        );
    }

    private static final Stream<Arguments> creationParametersWithNullValuesAndMissingFieldNames() {

        var displayName = io.jans.shibboleth.model.core.DisplayName.of("SomeTR").getValue();
        var description = Description.of("Some TR");
        var trustnature = TrustNature.INDIVIDUAL;

        return Stream.of(
            Arguments.of(displayName,description,null,"nature"),
            Arguments.of(displayName,null,trustnature,"description"),
           Arguments.of(null,description,trustnature,"displayName")
        );
    }

    private static final Stream<TrustRelationship> draftTrustRelationshipsOfAllNatures() {

        return Stream.of(
            TrustRelationshipFixtures.sampleDraftIndividualTrustRelationship(),
            TrustRelationshipFixtures.sampleDraftAggregateTrustRelationship()
        );
    }

    private static final Stream<Arguments> draftTrustRelationshipsWithSupportedMetadataSources() {

        TrustRelationship individual = TrustRelationshipFixtures.sampleDraftIndividualTrustRelationship();
        TrustRelationship aggregate  = TrustRelationshipFixtures.sampleDraftAggregateTrustRelationship();

        return Stream.of( 

            //Individual nature 
            Arguments.of(individual,NoMetadataSource.getInstance()),
            Arguments.of(individual,TrustRelationshipFixtures.sampleFileMetadataSource()),
            Arguments.of(individual,TrustRelationshipFixtures.sampleUriMetadataSource()),
            Arguments.of(individual,TrustRelationshipFixtures.sampleUpstreamMetadatSource()),
            Arguments.of(individual,TrustRelationshipFixtures.sampleManualMetadataSource()),

            //Aggregate nature 
            Arguments.of(aggregate,NoMetadataSource.getInstance()),
            Arguments.of(aggregate,TrustRelationshipFixtures.sampleFileMetadataSource()),
            Arguments.of(aggregate,TrustRelationshipFixtures.sampleUriMetadataSource()),
            Arguments.of(aggregate,TrustRelationshipFixtures.sampleMdqMetadataSource())
        );

    }

    private static final Stream<Arguments> draftTrustRelationshipsWithProfileConfigurationsAndProfileTypes() {

        TrustRelationship individual = TrustRelationshipFixtures.sampleDraftIndividualTrustRelationship();
        TrustRelationship aggregate  = TrustRelationshipFixtures.sampleDraftAggregateTrustRelationship();

        return Stream.of(

            //Individual nature 
            Arguments.of(individual,TrustRelationshipFixtures.activeShibbolethSsoProfileConfiguration(),ProfileType.SHIBBOLETH_SSO),
            Arguments.of(individual,TrustRelationshipFixtures.activeSaml2ArtifactResolutionProfileConfiguration(),ProfileType.SAML2_ARTIFACT_RESOLUTION),
            Arguments.of(individual,TrustRelationshipFixtures.activeSaml2AttributeQueryProfileConfiguration(),ProfileType.SAML2_ATTRIBUTE_QUERY),
            Arguments.of(individual,TrustRelationshipFixtures.activeSaml2EcpProfileConfiguration(),ProfileType.SAML2_ECP), 
            Arguments.of(individual,TrustRelationshipFixtures.activeSaml2SsoProfileConfiguration(),ProfileType.SAML2_SSO),
            Arguments.of(individual,TrustRelationshipFixtures.activeSaml2LogoutProfileConfiguration(),ProfileType.SAML2_LOGOUT),

            //Aggregate nature
            Arguments.of(aggregate,TrustRelationshipFixtures.activeShibbolethSsoProfileConfiguration(),ProfileType.SHIBBOLETH_SSO),
            Arguments.of(aggregate,TrustRelationshipFixtures.activeSaml2ArtifactResolutionProfileConfiguration(),ProfileType.SAML2_ARTIFACT_RESOLUTION),
            Arguments.of(aggregate,TrustRelationshipFixtures.activeSaml2AttributeQueryProfileConfiguration(),ProfileType.SAML2_ATTRIBUTE_QUERY),
            Arguments.of(aggregate,TrustRelationshipFixtures.activeSaml2EcpProfileConfiguration(),ProfileType.SAML2_ECP), 
            Arguments.of(aggregate,TrustRelationshipFixtures.activeSaml2SsoProfileConfiguration(),ProfileType.SAML2_SSO),
            Arguments.of(aggregate,TrustRelationshipFixtures.activeSaml2LogoutProfileConfiguration(),ProfileType.SAML2_LOGOUT) 
        );
    }

    private static final Stream<Arguments> draftTrustRelationshipsAndProfileTypes() {

        TrustRelationship individual = TrustRelationshipFixtures.sampleDraftIndividualTrustRelationship();
        TrustRelationship aggregate  = TrustRelationshipFixtures.sampleDraftAggregateTrustRelationship();

        return Stream.of(

            //Individual nature
            Arguments.of(individual,ProfileType.SHIBBOLETH_SSO,"shibbolethSsoProfileConfiguration"),
            Arguments.of(individual,ProfileType.SAML2_ARTIFACT_RESOLUTION,"saml2ArtifactResolutionProfileConfiguration"),
            Arguments.of(individual,ProfileType.SAML2_ATTRIBUTE_QUERY,"saml2AttributeQueryProfileConfiguration"),
            Arguments.of(individual,ProfileType.SAML2_ECP,"saml2EcpProfileConfiguration"),
            Arguments.of(individual,ProfileType.SAML2_SSO,"saml2SsoProfileConfiguration"),
            Arguments.of(individual,ProfileType.SAML2_LOGOUT,"saml2LogoutProfileConfiguration"),

            //Aggregate nature
            Arguments.of(aggregate,ProfileType.SHIBBOLETH_SSO,"shibbolethSsoProfileConfiguration"),
            Arguments.of(aggregate,ProfileType.SAML2_ARTIFACT_RESOLUTION,"saml2ArtifactResolutionProfileConfiguration"),
            Arguments.of(aggregate,ProfileType.SAML2_ATTRIBUTE_QUERY,"saml2AttributeQueryProfileConfiguration"),
            Arguments.of(aggregate,ProfileType.SAML2_ECP,"saml2EcpProfileConfiguration"),
            Arguments.of(aggregate,ProfileType.SAML2_SSO,"saml2SsoProfileConfiguration"),
            Arguments.of(aggregate,ProfileType.SAML2_LOGOUT,"saml2LogoutProfileConfiguration")
        );
    }

    private static final Stream<Arguments> draftTrustRelationshipsAndReleasedAttributes() {

        TrustRelationship individual = TrustRelationshipFixtures.sampleDraftIndividualTrustRelationship();
        TrustRelationship aggregate  = TrustRelationshipFixtures.sampleDraftAggregateTrustRelationship();

        return Stream.of(
            //Individual Nature
            Arguments.of(individual,ReleasedAttributes.empty()),
            Arguments.of(individual,TrustRelationshipFixtures.sampleReleasedAttributes()),

            //Aggregate Nature
            Arguments.of(aggregate,ReleasedAttributes.empty()),
            Arguments.of(individual,TrustRelationshipFixtures.sampleReleasedAttributes())
        );
    }

    private static final Stream<Arguments> draftTrustRelationshipsWithAnActiveProfileAndRealMetadataSource() {

        TrustRelationship individual = TrustRelationshipFixtures.sampleDraftIndividualTrustRelationshipWithActiveProfile();
        TrustRelationship aggregate  = TrustRelationshipFixtures.sampleDraftAggregateTrustRelationshipWithActiveProfile();
        return Stream.of(

            //Individual Nature
            Arguments.of(individual,TrustRelationshipFixtures.sampleFileMetadataSource()),
            Arguments.of(individual,TrustRelationshipFixtures.sampleUriMetadataSource()),
            Arguments.of(individual,TrustRelationshipFixtures.sampleUpstreamMetadatSource()),
            Arguments.of(individual,TrustRelationshipFixtures.sampleManualMetadataSource()),

            //Aggregate Nature
            Arguments.of(aggregate,TrustRelationshipFixtures.sampleFileMetadataSource()),
            Arguments.of(aggregate,TrustRelationshipFixtures.sampleUriMetadataSource()),
            Arguments.of(aggregate,TrustRelationshipFixtures.sampleMdqMetadataSource())
        );
    }

    private static final Stream<Arguments> draftTrustRelationshipsWithARealMetadataSource() {

        TrustRelationship individual = TrustRelationshipFixtures.sampleDraftIndividualTrustRelationshipWithRealMetadataSource();
        TrustRelationship aggregate  = TrustRelationshipFixtures.sampleDraftAggregateTrustRelationshipWithRealMetadataSource();

        return Stream.of(

            //Individual TrustRelationship
            Arguments.of(
                TrustRelationshipFixtures.sampleDraftIndividualTrustRelationshipWithRealMetadataSource(),
                TrustRelationshipFixtures.activeShibbolethSsoProfileConfiguration(),
                ProfileType.SHIBBOLETH_SSO
            ),
            Arguments.of(
                TrustRelationshipFixtures.sampleDraftIndividualTrustRelationshipWithRealMetadataSource(),
                TrustRelationshipFixtures.activeSaml2ArtifactResolutionProfileConfiguration(),
                ProfileType.SAML2_ARTIFACT_RESOLUTION
            ),

            //Aggregate TrustRelationship
            Arguments.of(
                TrustRelationshipFixtures.sampleDraftAggregateTrustRelationshipWithRealMetadataSource(),
                TrustRelationshipFixtures.activeSaml2SsoProfileConfiguration(),
                ProfileType.SAML2_SSO
            ),
            Arguments.of(
                TrustRelationshipFixtures.sampleDraftAggregateTrustRelationshipWithRealMetadataSource(),
                TrustRelationshipFixtures.activeSaml2EcpProfileConfiguration(),
                ProfileType.SAML2_ECP
            )
        );
    }

    private static final Stream<Arguments> readyTrustRelationshipsOfAllNatures() {

        TrustRelationship individual = TrustRelationshipFixtures.sampleDraftIndividualTrustRelationshipWithRealMetadataSource();
        TrustRelationship aggregate  = TrustRelationshipFixtures.sampleDraftAggregateTrustRelationshipWithRealMetadataSource();
        
        return Stream.of(

            //Individual TrustRelationships

            Arguments.of(
                individual.updateShibbolethSsoProfileConfiguration(
                    TrustRelationshipFixtures.activeShibbolethSsoProfileConfiguration()
                ).getValue()
            ),
            Arguments.of(
                individual.updateSaml2ArtifactResolutionProfileConfiguration(
                    TrustRelationshipFixtures.activeSaml2ArtifactResolutionProfileConfiguration()
                ).getValue()
            ),
            Arguments.of(
                individual.updateSaml2AttributeQueryProfileConfiguration(
                    TrustRelationshipFixtures.activeSaml2AttributeQueryProfileConfiguration()
                ).getValue()
            ),

            Arguments.of(
                individual.updateSaml2EcpProfileConfiguration(
                    TrustRelationshipFixtures.activeSaml2EcpProfileConfiguration()
                ).getValue()
            ),

            Arguments.of(
                individual.updateSaml2SsoProfileConfiguration(
                    TrustRelationshipFixtures.activeSaml2SsoProfileConfiguration()
                ).getValue()
            ),

            Arguments.of(
                individual.updateSaml2LogoutProfileConfiguration(
                    TrustRelationshipFixtures.activeSaml2LogoutProfileConfiguration()
                ).getValue()
            ),

            //Aggregate TrustRelationships
            Arguments.of(
                aggregate.updateShibbolethSsoProfileConfiguration(
                    TrustRelationshipFixtures.activeShibbolethSsoProfileConfiguration()
                ).getValue()
            ),
            
            Arguments.of(
                aggregate.updateSaml2ArtifactResolutionProfileConfiguration(
                    TrustRelationshipFixtures.activeSaml2ArtifactResolutionProfileConfiguration()
                ).getValue()
            ),
            
            Arguments.of(
                aggregate.updateSaml2AttributeQueryProfileConfiguration(
                    TrustRelationshipFixtures.activeSaml2AttributeQueryProfileConfiguration()
                ).getValue()
            ),
            
            Arguments.of(
                aggregate.updateSaml2EcpProfileConfiguration(
                    TrustRelationshipFixtures.activeSaml2EcpProfileConfiguration()
                ).getValue()
            ),
            
            Arguments.of(
                aggregate.updateSaml2SsoProfileConfiguration(
                    TrustRelationshipFixtures.activeSaml2SsoProfileConfiguration()
                ).getValue()
            ),
            
            Arguments.of(
                aggregate.updateSaml2LogoutProfileConfiguration(
                     TrustRelationshipFixtures.activeSaml2LogoutProfileConfiguration()
                ).getValue()
            )
        );
    }

    /**
     * Creation Tests
     */
    @Nested
    @DisplayName("Foundation & Creation -- Creation Tests")
    public class CreationTests {


        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#creationParametersWithValidValues")
        @DisplayName(
            "GIVEN valid creation parameters " + 
            "WHEN create() is called " +
            "THEN it should create a TrustRelationship in DRAFT STATUS with defaults ")
        public void shouldCreateTrustRelationshipInDraftStateWithDefaults(
            io.jans.shibboleth.model.core.DisplayName displayName, 
            Description description, TrustNature nature) {
            
            TrustResult<TrustRelationship> result = TrustRelationship.create(displayName,description,nature);

            assertThat(result.isSuccess()).isTrue();

            TrustRelationship trustrelationship = result.getValue();
            var newDisplayName = io.jans.shibboleth.model.core.DisplayName.of("TestTR").getValue();

            assertThat(trustrelationship)
                .isNew()
                .hasDisplayName(displayName)
                .hasDescription(description)
                .isOfNature(nature)
                .isInDraftStatus()
                .isVersion(Version.initial())
                .hasNoMetadataSource()
                .hasNoDiscoveredEntityIds()
                .hasNoReleasedAttributes();
        
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

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#creationParametersWithNullValuesAndMissingFieldNames")
        @DisplayName(
            "GIVEN a null value for any required parameter (displayName, description or nature) " +
            "WHEN create() is called " +
            "THEN the method call fails with the appropriate error "
        )
        public void shouldFailCreationWhenAnyRequiredParameterIsNull(
            io.jans.shibboleth.model.core.DisplayName displayName, 
            Description description, TrustNature nature, String missingFieldName) {

            TrustResult<TrustRelationship> result = TrustRelationship.create(displayName,description,nature);
            assertThat(result.isFailure()).isTrue();

            assertThat(result.getError()).isInstanceOf(DomainObjectCreationFailed.class);
            DomainObjectCreationFailed error = (DomainObjectCreationFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
            CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
            assertThat(cause.getFieldName()).isEqualTo(missingFieldName);
        }
       
    }

    /**
     * Basic Updates and Idempotency Tests
     */
    @Nested
    @DisplayName("Foundation & Creation -- Basic Updates In Draft")
    public class BasicUpdatesWithIdempotencyAndNullityTests {

        
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN a DRAFT TrustRelationship with an existing display name " +
            "WHEN updateDisplayName() is called with a different name " +
            "THEN the operation succeeds, updates the display name and increments the version"
        )
        public void shouldUpdateDisplayNameAndIncrementVersion_whenDifferentNameProvided(TrustRelationship tr) {

            var newDisplayName = io.jans.shibboleth.model.core.DisplayName.of(tr.getDisplayName().getValue() + "_updated").getValue();

            assertThat(tr.getDisplayName()).isNotNull();
            assertThat(tr.getDisplayName()).isNotEqualTo(newDisplayName);

            TrustResult<TrustRelationship> result = tr.updateDisplayName(newDisplayName);

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated_tr = result.getValue();
            assertThat(updated_tr.getDisplayName()).isEqualTo(newDisplayName);
            assertThat(updated_tr.getVersion()).isEqualTo(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN a DRAFT TrustRelationship with an existing display name " +
            "WHEN updateDisplayName() is called with the same current name " +
            "THEN the operation should be idempotent (it should not change the TrustRelationship)"
        )
        public void shouldNotChangeState_whenUpdateDisplayNameWithSameName(TrustRelationship tr) {

            var sameDisplayName = io.jans.shibboleth.model.core.DisplayName.of(tr.getDisplayName().getValue()).getValue();
            assertThat(tr.getDisplayName()).isEqualTo(sameDisplayName);

            TrustResult<TrustRelationship> result = tr.updateDisplayName(sameDisplayName);
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship same_tr = result.getValue();
            assertThat(same_tr).isEqualTo(tr);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN a DRAFT TrustRelationship with an existing description " + 
            "WHEN updateDescription() is called with a different description " + 
            "THEN the operation succeeds, updates the description and increments the version "
        )
        public void shouldUpdateDescriptionAndIncrementVersion_whenDifferentDescriptionProvided(TrustRelationship tr) {

            Description newDescription = Description.of(tr.getDescription().getValue()+" Updated");
            assertThat(tr.getDescription()).isNotEqualTo(newDescription);

            TrustResult<TrustRelationship> result = tr.updateDescription(newDescription);
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated_tr = result.getValue();

            assertThat(updated_tr.getDescription()).isEqualTo(newDescription);
            assertThat(updated_tr.getVersion()).isEqualTo(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN a DRAFT TrustRelationship with an existing description " +
            "WHEN updateDescription() is called with the same current description " +
            "THEN the operation is idempotent (no changes to the TrustRelationship) " 
        )
        public void shouldNotChangeState_whenUpdateDescriptionWithSameDescription(TrustRelationship tr) {

            Description sameDescription = Description.of(tr.getDescription().getValue());
            assertThat(tr.getDescription()).isEqualTo(sameDescription);

            TrustResult<TrustRelationship> result = tr.updateDescription(sameDescription);
            assertThat(result.isSuccess()).isTrue();

            TrustRelationship same_tr = result.getValue();
            assertThat(same_tr).isEqualTo(tr);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsWithSupportedMetadataSources")
        @DisplayName (
            "GIVEN a DRAFT TrustRelationship with no active profiles " +
            "WHEN updateMetadataSource() is called " +
            "THEN the operation updates the metadata source, and maintains the DRAFT status"
        )
        public void shouldUpdateMetadataSourceAndStayInDraft_whenNoActiveProfiles(TrustRelationship tr,MetadataSource source) {

            assertThat(tr).isInDraftStatus();
            assertThat(tr).hasNoActiveProfileConfiguration();

            TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated_tr = result.getValue();
            assertThat(updated_tr.getMetadataSource()).isEqualTo(source);
            assertThat(updated_tr).isInDraftStatus();
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsWithProfileConfigurationsAndProfileTypes")
        @DisplayName(
            "GIVEN a DRAFT TrustRelationship with no metadatasources " + 
            "WHEN updateXXXProfileConfiguration() is called " +
            "THEN the operation updates the profile configuration and maintains the DRAFT status "
        )
        public void shouldUpdateProfileConfigurationAndStayInDraft_whenNoMetadataSource(TrustRelationship tr, Object profileconfig, ProfileType profiletype) {


            assertThat(tr).isInDraftStatus();
            assertThat(tr).hasNoMetadataSource();

            TrustResult<TrustRelationship> result = null;
            TrustRelationship same_or_updated_tr = null;

            switch(profiletype) {
                case SHIBBOLETH_SSO:
                    result = tr.updateShibbolethSsoProfileConfiguration((ShibbolethSsoProfileConfiguration)profileconfig);
                    assertThat(result.isSuccess()).isTrue();
                    same_or_updated_tr = result.getValue();
                    assertThat(same_or_updated_tr.getShibbolethSsoProfileConfiguration()).isEqualTo(profileconfig);
                    break;
                case SAML2_ATTRIBUTE_QUERY:
                    result = tr.updateSaml2AttributeQueryProfileConfiguration((Saml2AttributeQueryProfileConfiguration)profileconfig);
                    assertThat(result.isSuccess()).isTrue();
                    same_or_updated_tr = result.getValue();
                    assertThat(same_or_updated_tr.getSaml2AttributeQueryProfileConfiguration()).isEqualTo(profileconfig);
                    break;
                case SAML2_ARTIFACT_RESOLUTION:
                    result = tr.updateSaml2ArtifactResolutionProfileConfiguration((Saml2ArtifactResolutionProfileConfiguration)profileconfig);
                    assertThat(result.isSuccess()).isTrue();
                    same_or_updated_tr = result.getValue();
                    assertThat(same_or_updated_tr.getSaml2ArtifactResolutionProfileConfiguration()).isEqualTo(profileconfig);
                    break;
                case SAML2_ECP:
                    result = tr.updateSaml2EcpProfileConfiguration((Saml2EcpProfileConfiguration)profileconfig);
                    assertThat(result.isSuccess()).isTrue();
                    same_or_updated_tr = result.getValue();
                    assertThat(same_or_updated_tr.getSaml2EcpProfileConfiguration()).isEqualTo(profileconfig);
                    break;
                case SAML2_SSO:
                    result = tr.updateSaml2SsoProfileConfiguration((Saml2SsoProfileConfiguration)profileconfig);
                    assertThat(result.isSuccess()).isTrue();
                    same_or_updated_tr = result.getValue();
                    assertThat(same_or_updated_tr.getSaml2SsoProfileConfiguration()).isEqualTo(profileconfig);
                    break;
                case SAML2_LOGOUT:
                    result = tr.updateSaml2LogoutProfileConfiguration((Saml2LogoutProfileConfiguration)profileconfig);
                    assertThat(result.isSuccess()).isTrue();
                    same_or_updated_tr = result.getValue();
                    assertThat(same_or_updated_tr.getSaml2LogoutProfileConfiguration()).isEqualTo(profileconfig);
                    break;
                default:
                    fail("Profile Type '%s' unsupported in tests",profiletype);
                    break;
            }

        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsAndReleasedAttributes")
        @DisplayName(
            "GIVEN a DRAFT TrustRelationship " +
            "WHEN updateReleasedAttributes is called with a valid parameter " +
            "THEN the operation updates the released attributes and maintains the DRAFT status"
        )
        public void shouldUpdateReleasedAttributesAndStayInDraft(TrustRelationship tr, ReleasedAttributes attributes) {

            assertThat(tr).isInDraftStatus();
            assertThat(attributes).isNotNull();

            TrustResult<TrustRelationship> result = tr.updateReleasedAttributes(attributes);
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship same_or_updated_tr = result.getValue();

            assertThat(same_or_updated_tr).isInDraftStatus();
            assertThat(same_or_updated_tr.getReleasedAttributes()).isEqualTo(attributes);
        }

    }

    @Nested
    @DisplayName("Foundation & Creation -- Update Nullability Tests ")
    public class UpdateNullabilityTests {
        
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN a TrustRelationship " +
            "WHEN updateDisplayName() is called with a null parameter " + 
            "THEN the call should fail with the appropriate error " 
        )
        public void shouldFailWhenUpdateDisplayNameWithNull(TrustRelationship tr) {
            
            TrustResult<TrustRelationship> result = tr.updateDisplayName(null);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isNotNull();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
            CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
            assertThat(cause.getFieldName()).isEqualTo("displayName");
        }
        
        
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN a TrustRelationship " +
            "WHEN updateMetadataSource() is called with a null parameter " +
            "THEN the call should fail with the appropriate error "
        )
        public void shouldFailWhenUpdateMetadataSourceWithNull(TrustRelationship tr) {
        
            TrustResult<TrustRelationship> result = tr.<ShibbolethSsoProfileConfiguration>updateMetadataSource(null);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isNotNull();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
        }
        
        
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsAndProfileTypes")
        @DisplayName(
            "GIVEN a TrustRelationship " +
            "WHEN updateXXXProfileConfiguration() is called with a null parameter " +
            "THEN the call should fail with the appropriate error "
        )
        public void shouldFailWhenUpdateProfileConfigurationWithNull(TrustRelationship tr, ProfileType profiletype,String requiredFieldName) {
        
            TrustResult<TrustRelationship> result = null;
                    
            switch(profiletype) {
                case SHIBBOLETH_SSO:
                    result = tr.updateShibbolethSsoProfileConfiguration(null);
                    break;
                case SAML2_ATTRIBUTE_QUERY:
                    result = tr.updateSaml2AttributeQueryProfileConfiguration(null);
                    break;
                case SAML2_ARTIFACT_RESOLUTION:
                    result = tr.updateSaml2ArtifactResolutionProfileConfiguration(null);
                    break;
                case SAML2_ECP:
                    result = tr.updateSaml2EcpProfileConfiguration(null);
                    break;
                case SAML2_SSO:
                    result = tr.updateSaml2SsoProfileConfiguration(null);
                    break;
                case SAML2_LOGOUT:
                    result = tr.updateSaml2LogoutProfileConfiguration(null);
                    break;
                default:
                    fail("Profile type '%s' unsupported in tests", profiletype);
                    break;
            }
        
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
                    
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
            CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
            assertThat(cause.getFieldName()).isEqualTo(requiredFieldName);
        }
        
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN a TrustRelationship " +
            "WHEN updateReleasedAttributes() is called with a null parameter " +
            "THEN the operation fails with the appropriate error"
        )
        public void shouldFailWhenUpdateReleasedAttributesWithNull(TrustRelationship tr) {
        
            TrustResult<TrustRelationship> result = tr.updateReleasedAttributes(null);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
            CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
            assertThat(cause.getFieldName()).isEqualTo("releasedAttributes");
        }
        
    }

    @Nested
    @DisplayName("State Transitions -- DRAFT <-> READY Transitions")
    public class DraftToReadyAndViceVersaTransitionsTests {

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsWithAnActiveProfileAndRealMetadataSource")
        @DisplayName(
            "GIVEN a DRAFT TrustRelationship  with at least one active profile " +
            "WHEN updateMetadataSource() is called with a no-NONE metadata source " +
            "THEN the TrustRelationship should transition to READY state AND increment version "
        )
        public void shouldTransitionToReady_whenMetadataSourceAddedWithActiveProfile(TrustRelationship tr,  MetadataSource source) {

            assertThat(tr).isInDraftStatus();
            assertThat(tr).hasAtLeastOneActiveProfileConfiguration();
            assertThat(source.getType()).isNotEqualTo(MetadataSourceType.NONE);

            TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();
            assertThat(updated).isInReadyStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#draftTrustRelationshipsWithARealMetadataSource")
        @DisplayName(
            "GIVEN a DRAFT TrustRelationship with a REAL(non-NONE) metadata source " +
            "WHEN updateXXXProfileConfiguration is called with an ACTIVE profile configuration " +
            "THEN the TrustRelationship should transition to READY status AND increment version "
        )
        public void shouldTransitionToReady_whenProfileConfigurationEnabledWithRealMetadataSource(TrustRelationship tr, Object profileconfig, ProfileType profiletype) {

            assertThat(tr).isInDraftStatus();
            assertThat(tr).hasRealMetadataSource();
            assertThat(profileconfig,profiletype).isActive();

            TrustResult<TrustRelationship> result = null;
            switch(profiletype) {
                case SHIBBOLETH_SSO:
                    result = tr.updateShibbolethSsoProfileConfiguration((ShibbolethSsoProfileConfiguration)profileconfig);
                    break;
                case SAML2_ATTRIBUTE_QUERY:
                    result = tr.updateSaml2AttributeQueryProfileConfiguration((Saml2AttributeQueryProfileConfiguration)profileconfig);
                    break;
                case SAML2_ARTIFACT_RESOLUTION:
                    result = tr.updateSaml2ArtifactResolutionProfileConfiguration((Saml2ArtifactResolutionProfileConfiguration)profileconfig);
                    break;
                case SAML2_ECP:
                    result = tr.updateSaml2EcpProfileConfiguration((Saml2EcpProfileConfiguration)profileconfig);
                    break;
                case SAML2_SSO:
                    result = tr.updateSaml2SsoProfileConfiguration((Saml2SsoProfileConfiguration)profileconfig);
                    break;
                case SAML2_LOGOUT:
                    result = tr.updateSaml2LogoutProfileConfiguration((Saml2LogoutProfileConfiguration)profileconfig);
                    break;
                default:
                    fail("Profile type '%s' unsupported in tests",profiletype);
                    break;
            }

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();
            assertThat(updated).isInReadyStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#readyTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN a READY TrustRelationship with at least one active profile configuration " + 
            "WHEN updateXXXProfileConfiguration() is called such that all profiles become disabled " +
            "THEN should transit to DRAFT and increment version "
        )
        public void shouldTransitionToDraft_whenAllProfilesDisabledFromReady(TrustRelationship tr) {

            assertThat(tr).isInReadyStatus();
            assertThat(tr).hasRealMetadataSource();
            assertThat(tr).hasAtLeastOneActiveProfileConfiguration();

            TrustRelationship updated = tr;
            Version currentversion =  tr.getVersion();
            if( updated.getShibbolethSsoProfileConfiguration().getStatus() == ProfileStatus.ACTIVE ) {

                updated = updated.updateShibbolethSsoProfileConfiguration(
                    ShibbolethSsoProfileConfiguration.from(updated.getShibbolethSsoProfileConfiguration())
                        .status(ProfileStatus.INACTIVE).build().getValue()
                ).getValue();
                currentversion = currentversion.next();
            }

            if ( updated.getSaml2ArtifactResolutionProfileConfiguration().getStatus() == ProfileStatus.ACTIVE ) {

                updated = updated.updateSaml2ArtifactResolutionProfileConfiguration(
                    Saml2ArtifactResolutionProfileConfiguration.from(updated.getSaml2ArtifactResolutionProfileConfiguration())
                        .status(ProfileStatus.INACTIVE).build().getValue()
                ).getValue();
                currentversion = currentversion.next();
            }

            if ( updated.getSaml2AttributeQueryProfileConfiguration().getStatus() == ProfileStatus.ACTIVE ) {

                updated = updated.updateSaml2AttributeQueryProfileConfiguration(
                    Saml2AttributeQueryProfileConfiguration.from(updated.getSaml2AttributeQueryProfileConfiguration())
                        .status(ProfileStatus.INACTIVE).build().getValue()
                ).getValue();
                currentversion = currentversion.next();
            }

            if ( updated.getSaml2EcpProfileConfiguration().getStatus() == ProfileStatus.ACTIVE ) {

                updated = updated.updateSaml2EcpProfileConfiguration(
                    Saml2EcpProfileConfiguration.from(updated.getSaml2EcpProfileConfiguration())
                        .status(ProfileStatus.INACTIVE).build().getValue()
                ).getValue();
                currentversion = currentversion.next();
            }

            if ( updated.getSaml2SsoProfileConfiguration().getStatus() == ProfileStatus.ACTIVE ) {

                updated = updated.updateSaml2SsoProfileConfiguration(
                    Saml2SsoProfileConfiguration.from(updated.getSaml2SsoProfileConfiguration())
                        .status(ProfileStatus.INACTIVE).build().getValue()
                ).getValue();
                currentversion = currentversion.next();
            }

            if ( updated.getSaml2LogoutProfileConfiguration().getStatus() == ProfileStatus.ACTIVE ) {

                updated = updated.updateSaml2LogoutProfileConfiguration(
                    Saml2LogoutProfileConfiguration.from(updated.getSaml2LogoutProfileConfiguration())
                        .status(ProfileStatus.INACTIVE).build().getValue()
                ).getValue();
                currentversion = currentversion.next();
            }

            assertThat(updated).isInDraftStatus();
            assertThat(updated).isVersion(currentversion);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTest#readyTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN a READY TrustRelationship with a real metadata source " +
            "WHEN updateMetadataSource() is called with NoMetadataSource " +
            "THEN should transition to DRAFT and increment version " 
        )
        public void shouldTransitionToDraft_whenMetadataSourceSetToNoneFromReady(TrustRelationship tr) {

            assertThat(tr).isInReadyStatus();
            assertThat(tr).hasRealMetadataSource();
            assertThat(tr).hasAtLeastOneActiveProfileConfiguration();

            TrustResult<TrustRelationship> result = tr.updateMetadataSource(NoMetadataSource.getInstance());

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInDraftStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }
        
    }

}