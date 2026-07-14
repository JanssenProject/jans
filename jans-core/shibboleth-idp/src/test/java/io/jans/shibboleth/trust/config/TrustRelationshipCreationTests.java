package io.jans.shibboleth.trust.config;

import io.jans.shibboleth.trust.config.*;
import io.jans.shibboleth.trust.config.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.config.diagnostics.ActivationStatus;
import io.jans.shibboleth.trust.config.error.*;
import io.jans.shibboleth.trust.config.metadata.MetadataSource;
import io.jans.shibboleth.trust.config.metadata.MetadataSourceType;
import io.jans.shibboleth.trust.config.metadata.NoMetadataSource;
import io.jans.shibboleth.trust.config.profile.*;
import io.jans.shibboleth.trust.config.profile.common.*;
import io.jans.shibboleth.trust.shared.Result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static io.jans.shibboleth.trust.config.TrustRelationshipAssert.assertThat;
import static io.jans.shibboleth.trust.config.profile.ProfileConfigurationAssert.assertThat;
import static io.jans.shibboleth.trust.config.TrustRelationshipFixtures.*;

@DisplayName("Creation & Global Invariants")
public class TrustRelationshipCreationTests {

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#creationParametersWithValidValues")
    @DisplayName(
        "GIVEN valid creation parameters " + 
        "WHEN create() is called " +
        "THEN it should create a TrustRelationship in DRAFT STATUS with defaults ")
    public void shouldCreateTrustRelationshipInDraftStateWithDefaults(
        io.jans.shibboleth.trust.config.DisplayName displayName, 
        Description description, TrustNature nature) {
        
        Result<TrustRelationship> result = TrustRelationship.create(displayName,description,nature);

        assertThat(result.isSuccess()).isTrue();

        TrustRelationship trustrelationship = result.getValue();
        var newDisplayName = io.jans.shibboleth.trust.config.DisplayName.of("TestTR").getValue();

        assertThat(trustrelationship)
            .isNew()
            .hasDisplayName(displayName)
            .hasDescription(description)
            .isOfNature(nature)
            .isInDraftStatus()
            .isVersion(Version.initial())
            .hasNoRealMetadataSource()
            .hasNoDiscoveredEntityIds()
            .hasNoReleasedAttributes()
            .hasNoActiveProfileConfiguration()
            .hasNoActivationDiagnostics();
    
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

    @Test
    @DisplayName(
        "GIVEN a valid displayName and a blank description " +
        "WHEN create() is called " +
        "THEN it succeeds because a blank description is allowed")
    public void shouldCreate_whenDescriptionIsBlank() {

        io.jans.shibboleth.trust.config.DisplayName displayName =
            io.jans.shibboleth.trust.config.DisplayName.of("BlankDescriptionTR").getValue();
        Description blankDescription = Description.of("   ");

        Result<TrustRelationship> result =
            TrustRelationship.create(displayName, blankDescription, TrustNature.INDIVIDUAL);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInDraftStatus().hasDescription(blankDescription);
        assertThat(result.getValue().getDescription().getValue()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a TrustRelationship " +
        "WHEN updateDescription() is called with a null parameter " +
        "THEN the call should fail with the appropriate error")
    public void shouldFailWhenUpdateDescriptionWithNull(TrustRelationship tr) {

        Result<TrustRelationship> result = tr.updateDescription(null);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
        CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
        assertThat(cause.getFieldName()).isEqualTo("description");
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#creationParametersWithNullValuesAndMissingFieldNames")
    @DisplayName(
        "GIVEN a null value for any required parameter (displayName, description or nature) " +
        "WHEN create() is called " +
        "THEN the method call fails with the appropriate error "
    )
    public void shouldFailCreationWhenAnyRequiredParameterIsNull(
        io.jans.shibboleth.trust.config.DisplayName displayName, 
        Description description, TrustNature nature, String missingFieldName) {

        Result<TrustRelationship> result = TrustRelationship.create(displayName,description,nature);
        assertThat(result.isFailure()).isTrue();

        assertThat(result.getError()).isInstanceOf(DomainObjectCreationFailed.class);
        DomainObjectCreationFailed error = (DomainObjectCreationFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
        CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
        assertThat(cause.getFieldName()).isEqualTo(missingFieldName);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a TrustRelationship " +
        "WHEN updateDisplayName() is called with a null parameter " + 
        "THEN the call should fail with the appropriate error " 
    )
    public void shouldFailWhenUpdateDisplayNameWithNull(TrustRelationship tr) {
        
        Result<TrustRelationship> result = tr.updateDisplayName(null);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isNotNull();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
        CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
        assertThat(cause.getFieldName()).isEqualTo("displayName");
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a TrustRelationship " +
        "WHEN updateMetadataSource() is called with a null parameter " +
        "THEN the call should fail with the appropriate error "
    )
    public void shouldFailWhenUpdateMetadataSourceWithNull(TrustRelationship tr) {
    
        Result<TrustRelationship> result = tr.<ShibbolethSsoProfileConfiguration>updateMetadataSource(null);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
    
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isNotNull();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsAndAccessors")
    @DisplayName(
        "GIVEN a TrustRelationship " +
        "WHEN updateXXXProfileConfiguration() is called with a null parameter " +
        "THEN the call should fail with the appropriate error "
    )
    public void shouldFailWhenUpdateProfileConfigurationWithNull(TrustRelationship tr, ProfileConfigurationAccessor accessor,String requiredFieldName) {
    
        Result<TrustRelationship> result = accessor.update(tr,null);
    
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
                
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
        CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
        assertThat(cause.getFieldName()).isEqualTo(requiredFieldName);
    }

    @ParameterizedTest
    @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
    @DisplayName(
        "GIVEN a TrustRelationship " +
        "WHEN updateReleasedAttributes() is called with a null parameter " +
        "THEN the operation fails with the appropriate error"
    )
    public void shouldFailWhenUpdateReleasedAttributesWithNull(TrustRelationship tr) {
    
        Result<TrustRelationship> result = tr.updateReleasedAttributes(null);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
        CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
        assertThat(cause.getFieldName()).isEqualTo("releasedAttributes");
    }

   @ParameterizedTest
   @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsWithRequiredFieldsInvalidators")
   @DisplayName(
        "GIVEN a Builder with one required field set to null or invalid " +
        "WHEN build() is called " +
        "THEN should fail with the appropriate error "
   )
   public void shouldFailWhenRequiredFieldsAreNullOrInvalidDuringBuild(TrustRelationship tr,Consumer<TrustRelationship.Builder> invalidator) {

        TrustRelationship.Builder builder = TrustRelationship.from(tr);
        invalidator.accept(builder);
        Result<TrustRelationship> result = builder.build();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
   }

   @ParameterizedTest
   @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
   @DisplayName(
        "GIVEN a Builder with metadataSource set to null " +
        "WHEN build() is called  " +
        "THEN should fail with the appropriate error "
   )
   public void shouldFailWhenMetadataSourceIsNull(TrustRelationship tr) {

        Result<TrustRelationship> result = TrustRelationship
            .from(tr)
            .withMetadataSource(null)
            .build();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
   }

   @ParameterizedTest
   @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsWithProfileConfigUpdaters")
   @DisplayName(
        "GIVEN a Builder with at least one profileconfiguration set to null " +
        "WHEN build() is called  " + 
        "THEN should fail with CannotBeNullOrBlank as root cause " 
   )
   public void shouldFailWhenAnyProfileConfigurationIsNull(TrustRelationship tr, ProfileConfigurationAccessor accessor,String requiredField) {

        TrustRelationship.Builder builder  = accessor.configureWithBuilder(TrustRelationship.from(tr),null);

        Result<TrustRelationship> result = builder.build();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error  = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
        CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
        assertThat(cause.getFieldName()).isEqualTo(requiredField);

   }

   @ParameterizedTest
   @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
   @DisplayName(
        "GIVEN a Builder with discoveredEntityIds set to null " +
        "WHEN build() is called " +
        "THEN should fail with the appropriate error "
   )
   public void shouldFailWhenDiscoveredEntityIdsIsNull(TrustRelationship tr) {

        Result<TrustRelationship> result = TrustRelationship
            .from(tr)
            .withDiscoveredEntityIds(null)
            .build();
        
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
   }

   @ParameterizedTest
   @MethodSource("io.jans.shibboleth.trust.config.TrustRelationshipArguments#draftTrustRelationshipsOfAllNatures")
   @DisplayName(
        "GIVEN a Builder with version set below Version.initial() " +
        "WHEN build() is called " +
        "THEN should fail with InvalidVersion as root cause "
   )
   public void shouldFailWhenVersionIsBelowInitialDuringBuild(TrustRelationship tr) {

        Result<TrustRelationship> result = TrustRelationship
            .from(tr)
            .withVersion(Version.of(0))
            .build();

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
        DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
        assertThat(error.getCause()).isInstanceOf(InvalidVersion.class);
   }

}
