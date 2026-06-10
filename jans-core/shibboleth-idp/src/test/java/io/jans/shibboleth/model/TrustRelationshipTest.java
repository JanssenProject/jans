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
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static io.jans.shibboleth.model.TrustRelationshipAssert.assertThat;



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

    private static final TrustRelationship draftIndividualTrustRelationship() {
        
        var displayName = io.jans.shibboleth.model.core.DisplayName.of("IndividualTR").getValue();
        var description = Description.of("Individual TR");
        return TrustRelationship.create(displayName,description,TrustNature.INDIVIDUAL).getValue();
    }

    private static final TrustRelationship draftAggregateTrustRelationship() {

        var displayName = io.jans.shibboleth.model.core.DisplayName.of("AggregateTR").getValue();
        var description = Description.of("Aggregate TR");
        return TrustRelationship.create(displayName,description, TrustNature.AGGREGATE).getValue();
    }

    private static final Stream<TrustRelationship> draftTrustRelationshipsOfAllNatures() {

        return Stream.of(draftIndividualTrustRelationship(),draftAggregateTrustRelationship());
    }

    private static final Stream<Arguments> draftTrustRelationshipsWithSupportedMetadataSources() {

        AssertionConsumerService acs = AssertionConsumerService.of(URI.create("https://sp.gluu.org/login"),SamlBinding.HTTP_POST).getValue();
        MetadataSource filesource = FileMetadataSource.of("/var/gluu/sp_metadata.xml").getValue();
        MetadataSource urisource = UriMetadataSource.of(URI.create("https://sp.gluu.org/my/sp.xml")).getValue();
        MetadataSource upstreamsource = UpstreamMetadataSource.of(Id.generate(),EntityId.of("sp.gluu.org").getValue()).getValue();
        MetadataSource manualsource  = ManualMetadataSource.withNoSigningCertificate()
            .entityId(EntityId.of("sp.gluu.org").getValue())
            .validUntil(ValidityPeriod.daysFromNow(1))
            .assertionConsumerService(acs)
            .build().getValue();
        MetadataSource mdq = MdqMetadataSource.of(URI.create("https://sp.gluu.org/mdq/")).getValue();
        
        return Stream.of( 

            //Individual nature 
            Arguments.of(draftIndividualTrustRelationship(),NoMetadataSource.getInstance()),
            Arguments.of(draftIndividualTrustRelationship(),filesource),
            Arguments.of(draftIndividualTrustRelationship(),urisource),
            Arguments.of(draftIndividualTrustRelationship(),upstreamsource),
            Arguments.of(draftIndividualTrustRelationship(),manualsource),

            //Aggregate nature 
            Arguments.of(draftAggregateTrustRelationship(),NoMetadataSource.getInstance()),
            Arguments.of(draftAggregateTrustRelationship(),filesource),
            Arguments.of(draftAggregateTrustRelationship(),urisource),
            Arguments.of(draftAggregateTrustRelationship(),mdq)
        );

    }

    /**
     * Creation Tests
     */
    @Nested
    @DisplayName("Creation Tests")
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
    @DisplayName("Basic Updates and Idempotency Tests")
    public class BasicUpdatesAndIdempotencyTests {

        
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
            TrustRelationship updated = result.getValue();
            assertThat(updated.getDisplayName()).isEqualTo(newDisplayName);
            assertThat(updated.getVersion()).isEqualTo(tr.getVersion().next());
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
            TrustRelationship unchanged = result.getValue();
            assertThat(unchanged).isEqualTo(tr);
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
            TrustRelationship changed = result.getValue();

            assertThat(changed.getDescription()).isEqualTo(newDescription);
            assertThat(changed.getVersion()).isEqualTo(tr.getVersion().next());
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

            TrustRelationship unchanged = result.getValue();
            assertThat(unchanged).isEqualTo(tr);
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
            TrustRelationship changed = result.getValue();
            assertThat(changed.getMetadataSource()).isEqualTo(source);
            assertThat(changed).isInDraftStatus();
        }

    }
}