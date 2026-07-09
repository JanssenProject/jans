package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.core.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.model.core.diagnostics.ActivationStatus;
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
import net.bytebuddy.asm.Advice.Argument;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import org.junit.jupiter.api.Tag;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static io.jans.shibboleth.model.TrustRelationshipAssert.assertThat;
import static io.jans.shibboleth.model.config.profiles.ProfileConfigurationAssert.assertThat;
import static io.jans.shibboleth.model.TrustRelationshipFixtures.*;



public class TrustRelationshipTests {
    

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

        return Stream.of( sampleDraftIndividualTrustRelationship(),sampleDraftAggregateTrustRelationship() );
    }

    private static final Stream<Arguments> draftTrustRelationshipsWithSupportedMetadataSources() {

        TrustRelationship individual = sampleDraftIndividualTrustRelationship();
        TrustRelationship aggregate  = sampleDraftAggregateTrustRelationship();

        return Stream.of( 

            //Individual nature 
            Arguments.of(individual,NoMetadataSource.getInstance()),
            Arguments.of(individual,sampleFileMetadataSource()),
            Arguments.of(individual,sampleUriMetadataSource()),
            Arguments.of(individual,sampleUpstreamMetadatSource()),
            Arguments.of(individual,sampleManualMetadataSource()),

            //Aggregate nature 
            Arguments.of(aggregate,NoMetadataSource.getInstance()),
            Arguments.of(aggregate,sampleFileMetadataSource()),
            Arguments.of(aggregate,sampleUriMetadataSource()),
            Arguments.of(aggregate,sampleMdqMetadataSource())
        );

    }

    private static final Stream<Arguments> draftTrustRelationshipsWithProfileConfigurationsAndAccessors() {

        TrustRelationship individual = sampleDraftIndividualTrustRelationship();
        TrustRelationship aggregate  = sampleDraftAggregateTrustRelationship();

        return Stream.of(

            //Individual nature 
            Arguments.of(individual,activeShibbolethSsoProfileConfiguration(),ProfileConfigurationAccessor.SHIBBOLETH_SSO),
            Arguments.of(individual,activeSaml2ArtifactResolutionProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ARTIFACT_RESOLUTION),
            Arguments.of(individual,activeSaml2AttributeQueryProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ATTRIBUTE_QUERY),
            Arguments.of(individual,activeSaml2EcpProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ECP), 
            Arguments.of(individual,activeSaml2SsoProfileConfiguration(),ProfileConfigurationAccessor.SAML2_SSO),
            Arguments.of(individual,activeSaml2LogoutProfileConfiguration(),ProfileConfigurationAccessor.SAML2_LOGOUT),

            //Aggregate nature
            Arguments.of(aggregate,activeShibbolethSsoProfileConfiguration(),ProfileConfigurationAccessor.SHIBBOLETH_SSO),
            Arguments.of(aggregate,activeSaml2ArtifactResolutionProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ARTIFACT_RESOLUTION),
            Arguments.of(aggregate,activeSaml2AttributeQueryProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ATTRIBUTE_QUERY),
            Arguments.of(aggregate,activeSaml2EcpProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ECP), 
            Arguments.of(aggregate,activeSaml2SsoProfileConfiguration(),ProfileConfigurationAccessor.SAML2_SSO),
            Arguments.of(aggregate,activeSaml2LogoutProfileConfiguration(),ProfileConfigurationAccessor.SAML2_LOGOUT) 
        );
    }

    private static final Stream<Arguments> draftTrustRelationshipsAndAccessors() {

        TrustRelationship individual = sampleDraftIndividualTrustRelationship();
        TrustRelationship aggregate  = sampleDraftAggregateTrustRelationship();

        return Stream.of(

            //Individual nature
            Arguments.of(individual,ProfileConfigurationAccessor.SHIBBOLETH_SSO,"shibbolethSsoProfileConfiguration"),
            Arguments.of(individual,ProfileConfigurationAccessor.SAML2_ARTIFACT_RESOLUTION,"saml2ArtifactResolutionProfileConfiguration"),
            Arguments.of(individual,ProfileConfigurationAccessor.SAML2_ATTRIBUTE_QUERY,"saml2AttributeQueryProfileConfiguration"),
            Arguments.of(individual,ProfileConfigurationAccessor.SAML2_ECP,"saml2EcpProfileConfiguration"),
            Arguments.of(individual,ProfileConfigurationAccessor.SAML2_SSO,"saml2SsoProfileConfiguration"),
            Arguments.of(individual,ProfileConfigurationAccessor.SAML2_LOGOUT,"saml2LogoutProfileConfiguration"),

            //Aggregate nature
            Arguments.of(aggregate,ProfileConfigurationAccessor.SHIBBOLETH_SSO,"shibbolethSsoProfileConfiguration"),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_ARTIFACT_RESOLUTION,"saml2ArtifactResolutionProfileConfiguration"),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_ATTRIBUTE_QUERY,"saml2AttributeQueryProfileConfiguration"),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_ECP,"saml2EcpProfileConfiguration"),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_SSO,"saml2SsoProfileConfiguration"),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_LOGOUT,"saml2LogoutProfileConfiguration")
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

        TrustRelationship individual = sampleDraftIndividualTrustRelationshipWithActiveProfile();
        TrustRelationship aggregate  = sampleDraftAggregateTrustRelationshipWithActiveProfile();
        return Stream.of(

            //Individual Nature
            Arguments.of(individual,sampleFileMetadataSource()),
            Arguments.of(individual,sampleUriMetadataSource()),
            Arguments.of(individual,sampleUpstreamMetadatSource()),
            Arguments.of(individual,sampleManualMetadataSource()),

            //Aggregate Nature
            Arguments.of(aggregate,sampleFileMetadataSource()),
            Arguments.of(aggregate,sampleUriMetadataSource()),
            Arguments.of(aggregate,sampleMdqMetadataSource())
        );
    }

    private static final Stream<Arguments> draftTrustRelationshipsWithRealMetadataSourceAndActiveProfileConfigurationToUpdate() {

        TrustRelationship individual = sampleDraftIndividualTrustRelationshipWithRealMetadataSource();
        TrustRelationship aggregate  = sampleDraftAggregateTrustRelationshipWithRealMetadataSource();

        return Stream.of(

            //Individual TrustRelationship
            Arguments.of(individual,activeShibbolethSsoProfileConfiguration(),ProfileConfigurationAccessor.SHIBBOLETH_SSO),
            Arguments.of(individual,activeSaml2ArtifactResolutionProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ARTIFACT_RESOLUTION),

            //Aggregate TrustRelationship
            Arguments.of(aggregate,activeSaml2SsoProfileConfiguration(),ProfileConfigurationAccessor.SAML2_SSO),
            Arguments.of(aggregate,activeSaml2EcpProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ECP)
        );
    }

    private static final Stream<Arguments> readyTrustRelationshipsOfAllNatures() {

        return Stream.of(
            Arguments.of(sampleReadyIndividualTrustRelationship()),
            Arguments.of(sampleReadyAggregateTrustRelationship())
        );
    }

    private static final Stream<Arguments> readyTrustRelationshipsWithSingleActiveProfileConfiguration() {

        ShibbolethSsoProfileConfiguration shibbolethsso = activeShibbolethSsoProfileConfiguration();
        Saml2SsoProfileConfiguration saml2sso = activeSaml2SsoProfileConfiguration();

        TrustRelationship individual = sampleReadyIndividualTrustRelationship(ProfileConfigurationAccessor.SHIBBOLETH_SSO,shibbolethsso);
        TrustRelationship aggregate  = sampleReadyAggregateTrustRelationship(ProfileConfigurationAccessor.SAML2_SSO,saml2sso);

        return Stream.of(
            Arguments.of(individual,ProfileConfigurationAccessor.SHIBBOLETH_SSO),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_SSO)
        );
    }

    private static final Stream<Arguments> readyTrustRelationshipsWithActivationDiagnostics() {

        TrustRelationship individual = sampleActivatingIndividualTrustRelationship()
            .finalizeActivation(sampleActivationDiagnosticsForSuccessfulActivation())
            .getValue()
            .updateMetadataSource(NoMetadataSource.getInstance())
            .getValue()
            .updateMetadataSource(sampleUriMetadataSource())
            .getValue();
        
        TrustRelationship aggregate = sampleActivatingAggregateTrustRelationship()
            .finalizeActivation(sampleActivationDiagnosticsForFailedActivation())
            .getValue();
        
        return Stream.of(Arguments.of(individual),Arguments.of(aggregate));
    }


    private static final Stream<Arguments> activatingTrustRelationshipsOfAllNatures() {
 
        return Stream.of(
            Arguments.of(sampleActivatingIndividualTrustRelationship()),
            Arguments.of(sampleActivatingAggregateTrustRelationship())
        );
    }

    private static final Stream<Arguments> activatingTrustRelationshipsOfAllNaturesWithSupportedMetadataSources() {

        TrustRelationship individual = sampleActivatingIndividualTrustRelationship();
        TrustRelationship aggregate  = sampleActivatingAggregateTrustRelationship();

        return Stream.of(
            Arguments.of(individual,sampleFileMetadataSource()),
            Arguments.of(individual,sampleUriMetadataSource()),
            Arguments.of(individual,sampleUpstreamMetadatSource()),
            Arguments.of(individual,sampleManualMetadataSource()),
            Arguments.of(individual,NoMetadataSource.getInstance()),

            Arguments.of(aggregate,sampleFileMetadataSource()),
            Arguments.of(aggregate,sampleUriMetadataSource()),
            Arguments.of(aggregate,sampleMdqMetadataSource()),
            Arguments.of(aggregate,NoMetadataSource.getInstance())
        );
    }


    private static final Stream<Arguments> activatingTrustRelationshipsWithProfileConfigurations() {

        TrustRelationship individual = sampleActivatingIndividualTrustRelationship();
        TrustRelationship aggregate  = sampleActivatingAggregateTrustRelationship();
        return Stream.of(

            //Individual
            Arguments.of(individual,activeShibbolethSsoProfileConfiguration(),ProfileConfigurationAccessor.SHIBBOLETH_SSO),
            Arguments.of(individual,activeSaml2ArtifactResolutionProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ARTIFACT_RESOLUTION),
            Arguments.of(individual,activeSaml2AttributeQueryProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ATTRIBUTE_QUERY),
            Arguments.of(individual,activeSaml2EcpProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ECP),
            Arguments.of(individual,activeSaml2SsoProfileConfiguration(),ProfileConfigurationAccessor.SAML2_SSO),
            Arguments.of(individual,activeSaml2LogoutProfileConfiguration(),ProfileConfigurationAccessor.SAML2_LOGOUT),

            //Aggregate
            Arguments.of(aggregate,activeShibbolethSsoProfileConfiguration(),ProfileConfigurationAccessor.SHIBBOLETH_SSO),
            Arguments.of(aggregate,activeSaml2ArtifactResolutionProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ARTIFACT_RESOLUTION),
            Arguments.of(aggregate,activeSaml2AttributeQueryProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ATTRIBUTE_QUERY),
            Arguments.of(aggregate,activeSaml2EcpProfileConfiguration(),ProfileConfigurationAccessor.SAML2_ECP),
            Arguments.of(aggregate,activeSaml2SsoProfileConfiguration(),ProfileConfigurationAccessor.SAML2_SSO),
            Arguments.of(aggregate,activeSaml2LogoutProfileConfiguration(),ProfileConfigurationAccessor.SAML2_LOGOUT)
        );
    }

    private static final Stream<Arguments> activatingTrustRelationshipsWithSuccessActivationDiagnostics() {

        return Stream.of(
            Arguments.of(sampleActivatingIndividualTrustRelationship(),sampleActivationDiagnosticsForSuccessfulActivation()),
            Arguments.of(sampleActivatingAggregateTrustRelationship(),sampleActivationDiagnosticsForSuccessfulActivation())
        );
    }

    private static final Stream<Arguments> activatingTrustRelationshipsWithFailedActivationDiagnostics() {

        return Stream.of(
            Arguments.of(sampleActivatingIndividualTrustRelationship(),sampleActivationDiagnosticsForFailedActivation()),
            Arguments.of(sampleActivatingAggregateTrustRelationship(),sampleActivationDiagnosticsForFailedActivation())
        );
    }

    private static final Stream<Arguments> activeTrustRelationshipsOfAllNatures() {

        return Stream.of(
            Arguments.of(sampleActiveIndividualTrustRelationship()),
            Arguments.of(sampleActiveAggregateTrustRelationship())  
        );
    }

    private static final Stream<Arguments> activeTrustRelationshipsOfAllNaturesWithAllProfileAccessors() {

        TrustRelationship individual = sampleActiveIndividualTrustRelationship();
        TrustRelationship aggregate  = sampleActiveAggregateTrustRelationship();

        return Stream.of(

            //individual 
            Arguments.of(individual,ProfileConfigurationAccessor.SHIBBOLETH_SSO),
            Arguments.of(individual,ProfileConfigurationAccessor.SAML2_ARTIFACT_RESOLUTION),
            Arguments.of(individual,ProfileConfigurationAccessor.SAML2_ATTRIBUTE_QUERY),
            Arguments.of(individual,ProfileConfigurationAccessor.SAML2_ECP),
            Arguments.of(individual,ProfileConfigurationAccessor.SAML2_SSO),
            Arguments.of(individual,ProfileConfigurationAccessor.SAML2_LOGOUT),

            //aggregate
            Arguments.of(aggregate,ProfileConfigurationAccessor.SHIBBOLETH_SSO),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_ARTIFACT_RESOLUTION),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_ATTRIBUTE_QUERY),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_ECP),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_SSO),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_LOGOUT)
        );
    }

    private static final Stream<Arguments> inactiveTrustRelationshipsOfAllNatures() {

        return Stream.of(
            Arguments.of(sampleInactiveIndividualTrustRelationship()),
            Arguments.of(sampleInactiveAggregateTrustRelationship())
        );
    }

    private static final Stream<Arguments> inactiveTrustRelationshipsOfAllNaturesWithNoRealMetadataSource() {

        return Stream.of(
            Arguments.of(sampleInactiveIndividualTrustRelationshipWithNoRealMetadataSource()),
            Arguments.of(sampleInactiveAggregateTrustRelationshipWithNoRealMetadataSource())
        );
    }

    private static final Stream<Arguments> inactiveTrustRelationshipsOfAllNaturesWithNoActiveProfileConfiguration() {

        return Stream.of( 
            Arguments.of(sampleInactiveIndividualTrustRelationshipWithNoActiveProfileConfiguration()),
            Arguments.of(sampleInactiveAggregateTrustRelationshipWithNoActiveProfileConfiguration())
        );
    }

    private static final Stream<Arguments> aggregateTrustRelationshipsNotInActivatingState() {

        return Stream.of(
            Arguments.of(sampleDraftAggregateTrustRelationship()),
            Arguments.of(sampleReadyAggregateTrustRelationship()),
            Arguments.of(sampleActiveAggregateTrustRelationship()),
            Arguments.of(sampleInactiveAggregateTrustRelationship())
        );
    }

    private static final Stream<Arguments> individualTrustRelationshipsInMultipleStates() {

        return Stream.of(
            Arguments.of(sampleDraftIndividualTrustRelationship()),
            Arguments.of(sampleReadyIndividualTrustRelationship()),
            Arguments.of(sampleActivatingIndividualTrustRelationship()),
            Arguments.of(sampleActiveIndividualTrustRelationship()),
            Arguments.of(sampleInactiveIndividualTrustRelationship())
        );
    }

    private static final Stream<Arguments> trustRelationshipsOfAllNaturesNotInActivatingState() {

        return Stream.of(
            //Individual
            Arguments.of(sampleDraftIndividualTrustRelationship()),
            Arguments.of(sampleReadyIndividualTrustRelationship()),
            Arguments.of(sampleActiveIndividualTrustRelationship()),
            Arguments.of(sampleInactiveIndividualTrustRelationship()),

            //Aggregate
            Arguments.of(sampleDraftAggregateTrustRelationship()),
            Arguments.of(sampleReadyAggregateTrustRelationship()),
            Arguments.of(sampleActiveAggregateTrustRelationship()),
            Arguments.of(sampleInactiveAggregateTrustRelationship())
        );
    }
    
    private static final Stream<Arguments> trustRelationshipsOfAllNaturesWithIncompatibleMetadataSources() {

        return Stream.of(

            //Individual
            Arguments.of(sampleDraftIndividualTrustRelationship(),sampleMdqMetadataSource()),
            Arguments.of(sampleReadyIndividualTrustRelationship(),sampleMdqMetadataSource()),
            Arguments.of(sampleActiveIndividualTrustRelationship(),sampleMdqMetadataSource()),
            Arguments.of(sampleInactiveIndividualTrustRelationship(),sampleMdqMetadataSource()),
            //Aggregate
            Arguments.of(sampleDraftAggregateTrustRelationship(),sampleManualMetadataSource()),
            Arguments.of(sampleReadyAggregateTrustRelationship(),sampleUpstreamMetadatSource()),
            Arguments.of(sampleActiveAggregateTrustRelationship(),sampleManualMetadataSource()),
            Arguments.of(
                TrustRelationshipFixtures.sampleInactiveAggregateTrustRelationship(),
                TrustRelationshipFixtures.sampleUpstreamMetadatSource()
            )
        );
    }

    private static final Stream<Arguments> draftTrustRelationshipsWithRequiredFieldsInvalidators() {

        return Stream.of(

            Arguments.of(sampleDraftIndividualTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withId(null)),
            Arguments.of(sampleDraftAggregateTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withDisplayName(null)),
            Arguments.of(sampleDraftIndividualTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withDescription(null)),
            Arguments.of(sampleDraftAggregateTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withNature(null)),
            Arguments.of(sampleDraftIndividualTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withVersion(null)),
            Arguments.of(sampleDraftAggregateTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withStatus(null)),
            Arguments.of(sampleDraftIndividualTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withMetadataSource(null)),
            Arguments.of(sampleDraftAggregateTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withShibbolethSsoProfileConfiguration(null)),
            Arguments.of(sampleDraftIndividualTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withSaml2ArtifactResolutionProfileConfiguration(null)),
            Arguments.of(sampleDraftAggregateTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withSaml2AttributeQueryProfileConfiguration(null)),
            Arguments.of(sampleDraftIndividualTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withSaml2EcpProfileConfiguration(null)),
            Arguments.of(sampleDraftAggregateTrustRelationship(),(Consumer<TrustRelationship.Builder>) b -> b.withSaml2SsoProfileConfiguration(null)),
            Arguments.of(sampleDraftIndividualTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withSaml2LogoutProfileConfiguration(null)),
            Arguments.of(sampleDraftAggregateTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withReleasedAttributes(null)),
            Arguments.of(sampleDraftIndividualTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withActivationDiagnostics(null)),
            Arguments.of(sampleDraftAggregateTrustRelationship(), (Consumer<TrustRelationship.Builder>) b -> b.withDiscoveredEntityIds(null))
        );
    }

    private static Stream<Arguments> draftTrustRelationshipsWithProfileConfigUpdaters() {

        return Stream.of(

            Arguments.of(sampleDraftIndividualTrustRelationship(),ProfileConfigurationAccessor.SHIBBOLETH_SSO,"shibbolethSsoProfileConfiguration"),
            Arguments.of(sampleDraftAggregateTrustRelationship(),ProfileConfigurationAccessor.SAML2_ARTIFACT_RESOLUTION,"saml2ArtifactResolutionProfileConfiguration"),
            Arguments.of (sampleDraftIndividualTrustRelationship(),ProfileConfigurationAccessor.SAML2_ATTRIBUTE_QUERY,"saml2AttributeQueryProfileConfiguration"),
            Arguments.of(sampleDraftAggregateTrustRelationship(),ProfileConfigurationAccessor.SAML2_ECP,"saml2EcpProfileConfiguration"),
            Arguments.of(sampleDraftIndividualTrustRelationship(),ProfileConfigurationAccessor.SAML2_SSO,"saml2SsoProfileConfiguration"),
            Arguments.of(sampleDraftAggregateTrustRelationship(),ProfileConfigurationAccessor.SAML2_LOGOUT,"saml2LogoutProfileConfiguration")
        );
    }

    private static Stream<Arguments> activeTrustRelationshipsOfAllNaturesWithDifferentMetadataSources() {

        MetadataSource filesource = FileMetadataSource.of("/opt/gluu/original_sp.xml").getValue();
        MetadataSource urisource = UriMetadataSource.of(URI.create("https://sample.gluu.org/sp_metadata.xml")).getValue();
        TrustRelationship individual = sampleActiveIndividualTrustRelationship(filesource);
        TrustRelationship aggregate = sampleActiveAggregateTrustRelationship(urisource);
        
        return Stream.of(
            Arguments.of(individual,urisource),
            Arguments.of(aggregate,filesource)
        );
    }

    private static Stream<Arguments> activeTrustRelationshipsOfAllNaturesWithSameMetadataSources() {

        MetadataSource filesource = FileMetadataSource.of("/opt/gluu/original_sp.xml").getValue();
        MetadataSource urisource = UriMetadataSource.of(URI.create("https://sample.gluu.org/sp_metadata.xml")).getValue();
        
        TrustRelationship individual = sampleActiveIndividualTrustRelationship(filesource);
        TrustRelationship aggregate  = sampleActiveAggregateTrustRelationship(urisource);
        
        return Stream.of(
            Arguments.of(individual,filesource),
            Arguments.of(aggregate,urisource)
        );
    }

    private static Stream<Arguments> activeTrustRelationshipsOfAllNaturesWithDifferentProfileConfiguration() {

        Saml2SsoProfileConfiguration newsaml2sso = Saml2SsoProfileConfiguration
            .from(TrustRelationshipFixtures.activeSaml2SsoProfileConfiguration())
            .assertionLifetime(Duration.ofDays(1000))
            .build()
            .getValue();
        
        Saml2LogoutProfileConfiguration newsaml2logout = Saml2LogoutProfileConfiguration
            .from(TrustRelationshipFixtures.activeSaml2LogoutProfileConfiguration())
            .messageSigningPolicy(MessageSigningPolicy.SIGN_NONE)
            .build()
            .getValue();
        
        TrustRelationship individual = sampleActiveIndividualTrustRelationship(ProfileConfigurationAccessor.SAML2_SSO, newsaml2sso);
        TrustRelationship aggregate  = sampleActiveAggregateTrustRelationship(ProfileConfigurationAccessor.SAML2_LOGOUT,newsaml2logout);
        
        return Stream.of (
            Arguments.of(individual,newsaml2logout,ProfileConfigurationAccessor.SAML2_LOGOUT),
            Arguments.of(aggregate,newsaml2sso,ProfileConfigurationAccessor.SAML2_SSO)
        );
    }

    private static Stream<Arguments> activeTrustRelationshipsOfAllNaturesWithSameProfileConfiguration() {

        Saml2SsoProfileConfiguration saml2sso = Saml2SsoProfileConfiguration
            .from(TrustRelationshipFixtures.activeSaml2SsoProfileConfiguration())
            .assertionLifetime(Duration.ofDays(1000))
            .build()
            .getValue();
        
        Saml2LogoutProfileConfiguration saml2logout = Saml2LogoutProfileConfiguration
            .from(TrustRelationshipFixtures.activeSaml2LogoutProfileConfiguration())
            .messageSigningPolicy(MessageSigningPolicy.SIGN_NONE)
            .build()
            .getValue();
        
        TrustRelationship individual = sampleActiveIndividualTrustRelationship(ProfileConfigurationAccessor.SAML2_LOGOUT,saml2logout);
        TrustRelationship aggregate  = sampleActiveAggregateTrustRelationship(ProfileConfigurationAccessor.SAML2_SSO,saml2sso);

        return Stream.of (
            Arguments.of(individual,saml2logout,ProfileConfigurationAccessor.SAML2_LOGOUT),
            Arguments.of(aggregate,saml2sso,ProfileConfigurationAccessor.SAML2_SSO)
        );
    }

    private static Stream<Arguments> activeTrustRelationshipsOfAllNaturesWithActiveProfileAccessor() {

        Saml2SsoProfileConfiguration saml2sso = Saml2SsoProfileConfiguration
            .from(TrustRelationshipFixtures.activeSaml2SsoProfileConfiguration())
            .assertionLifetime(Duration.ofDays(1000))
            .build()
            .getValue();
        
        Saml2LogoutProfileConfiguration saml2logout = Saml2LogoutProfileConfiguration
            .from(TrustRelationshipFixtures.activeSaml2LogoutProfileConfiguration())
            .messageSigningPolicy(MessageSigningPolicy.SIGN_NONE)
            .build()
            .getValue();
        
        TrustRelationship individual = sampleActiveIndividualTrustRelationship(ProfileConfigurationAccessor.SAML2_LOGOUT,saml2logout);
        TrustRelationship aggregate  = sampleActiveAggregateTrustRelationship(ProfileConfigurationAccessor.SAML2_SSO,saml2sso);

        return Stream.of(
            Arguments.of(individual,ProfileConfigurationAccessor.SAML2_LOGOUT),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_SSO)
        );
    }

    private static Stream<Arguments> trustRelationshipsOfVariousStatuses() {

        return Stream.of(
            Arguments.of(sampleDraftIndividualTrustRelationship()),
            Arguments.of(sampleDraftAggregateTrustRelationship()),
            
            Arguments.of(sampleReadyIndividualTrustRelationship()),
            Arguments.of(sampleReadyAggregateTrustRelationship()),

            Arguments.of(sampleActivatingIndividualTrustRelationship()),
            Arguments.of(sampleActivatingAggregateTrustRelationship()),

            Arguments.of(sampleActiveIndividualTrustRelationship()),
            Arguments.of(sampleActiveAggregateTrustRelationship()),

            Arguments.of(sampleInactiveIndividualTrustRelationship()),
            Arguments.of(sampleInactiveAggregateTrustRelationship())
        );
    }

    private static Stream<Arguments> trustRelationshipsWithIdempotentUpdateOperations() {

        MetadataSource filesource = sampleFileMetadataSource();
        MetadataSource urisource = sampleUriMetadataSource();
        MetadataSource manualsource = sampleManualMetadataSource();
        MetadataSource mdqsource = sampleMdqMetadataSource();
        MetadataSource upstreamsource = sampleUpstreamMetadatSource();

        return Stream.of(
            Arguments.of(sampleDraftIndividualTrustRelationship(),displayNameIdempotentUpdate()),
            Arguments.of(sampleDraftIndividualTrustRelationship(),descriptionIdempotentUpdate()),
            Arguments.of(sampleDraftAggregateTrustRelationship(),displayNameIdempotentUpdate()),
            Arguments.of(sampleDraftAggregateTrustRelationship(),descriptionIdempotentUpdate()),

            Arguments.of(sampleDraftIndividualTrustRelationship(filesource),metadataSourceIdempotentUpdate()),
            Arguments.of(sampleDraftAggregateTrustRelationship(urisource),metadataSourceIdempotentUpdate()),
            Arguments.of(sampleReadyIndividualTrustRelationship(upstreamsource),metadataSourceIdempotentUpdate()),
            Arguments.of(sampleReadyIndividualTrustRelationship(manualsource),metadataSourceIdempotentUpdate()),
            Arguments.of(sampleReadyAggregateTrustRelationship(mdqsource),metadataSourceIdempotentUpdate()),

            Arguments.of(sampleDraftIndividualTrustRelationship(),releasedAttributesIdempotentUpdate()),
            Arguments.of(sampleDraftAggregateTrustRelationship(),releasedAttributesIdempotentUpdate()),
            Arguments.of(sampleReadyIndividualTrustRelationship(),releasedAttributesIdempotentUpdate()),
            Arguments.of(sampleReadyAggregateTrustRelationship(),releasedAttributesIdempotentUpdate())
        );
    }

    private static Stream<Arguments> trustRelationshipsOfAllStatusesExceptActive() {

        return Stream.of(
            Arguments.of(sampleDraftIndividualTrustRelationship()),
            Arguments.of(sampleDraftAggregateTrustRelationship()),
            
            Arguments.of(sampleReadyIndividualTrustRelationship()),
            Arguments.of(sampleReadyAggregateTrustRelationship()),

            Arguments.of(sampleActivatingIndividualTrustRelationship()),
            Arguments.of(sampleActivatingAggregateTrustRelationship()),

            Arguments.of(sampleInactiveIndividualTrustRelationship()),
            Arguments.of(sampleInactiveAggregateTrustRelationship())
        );
    }

    /**
     * Creation Tests
     */
    @Nested
    @DisplayName("Foundation & Creation -- Creation Tests")
    public class CreationTests {

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#creationParametersWithValidValues")
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

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#creationParametersWithNullValuesAndMissingFieldNames")
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
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsOfAllNatures")
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
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsOfAllNatures")
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
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsOfAllNatures")
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
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsOfAllNatures")
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
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsWithSupportedMetadataSources")
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
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsWithProfileConfigurationsAndAccessors")
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

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsAndReleasedAttributes")
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
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsOfAllNatures")
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
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsOfAllNatures")
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
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsAndAccessors")
        @DisplayName(
            "GIVEN a TrustRelationship " +
            "WHEN updateXXXProfileConfiguration() is called with a null parameter " +
            "THEN the call should fail with the appropriate error "
        )
        public void shouldFailWhenUpdateProfileConfigurationWithNull(TrustRelationship tr, ProfileConfigurationAccessor accessor,String requiredFieldName) {
        
            TrustResult<TrustRelationship> result = accessor.update(tr,null);
        
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
                    
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
            CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
            assertThat(cause.getFieldName()).isEqualTo(requiredFieldName);
        }
        
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsOfAllNatures")
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
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsWithAnActiveProfileAndRealMetadataSource")
        @DisplayName(
            "GIVEN a DRAFT TrustRelationship  with at least one active profile " +
            "WHEN updateMetadataSource() is called with a no-NONE metadata source " +
            "THEN the TrustRelationship should transition to READY state AND increment version "
        )
        public void shouldTransitionToReady_whenRealMetadataSourceAddedWithActiveProfile(TrustRelationship tr,  MetadataSource source) {

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
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsWithRealMetadataSourceAndActiveProfileConfigurationToUpdate")
        @DisplayName(
            "GIVEN a DRAFT TrustRelationship with a REAL(non-NONE) metadata source " +
            "WHEN updateXXXProfileConfiguration is called with an ACTIVE profile configuration " +
            "THEN the TrustRelationship should transition to READY status AND increment version "
        )
        public void shouldTransitionToReady_whenProfileConfigurationEnabledWithRealMetadataSource(TrustRelationship tr, Object profileconfig, ProfileConfigurationAccessor accessor) {

            assertThat(tr).isInDraftStatus();
            assertThat(tr).hasRealMetadataSource();
            assertThat(accessor.getStatus(profileconfig)).isEqualTo(ProfileStatus.ACTIVE);

            TrustResult<TrustRelationship> result = accessor.update(tr,profileconfig);
            
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();
            assertThat(updated).isInReadyStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#readyTrustRelationshipsWithSingleActiveProfileConfiguration")
        @DisplayName(
            "GIVEN a READY TrustRelationship with at least one active profile configuration " + 
            "WHEN updateXXXProfileConfiguration() is called such that all profiles become disabled " +
            "THEN should transit to DRAFT and increment version "
        )
        public void shouldTransitionToDraft_whenAllProfilesDisabledFromReady(TrustRelationship tr,ProfileConfigurationAccessor accessor) {

            assertThat(tr).isInReadyStatus();
            assertThat(tr).hasRealMetadataSource();
            assertThat(tr).hasAtLeastOneActiveProfileConfiguration();
            assertThat(tr).hasActiveProfileConfigurationCount(1);

            TrustResult<TrustRelationship> result = accessor.updateStatus(tr,ProfileStatus.INACTIVE);
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInDraftStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#readyTrustRelationshipsOfAllNatures")
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


    @Nested
    @DisplayName("State Transitions -- Activation and Deactivation")
    public class ActivationAndDeactivationTests {

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#readyTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN a READY TrustRelationship " +
            "WHEN activate() is called " +
            "THEN should transition to ACTIVATING state , increment version and clear previous activation diagnostics "
        )
        public void shouldTransitionToActivating_whenActivateCalledFromReady(TrustRelationship tr) {

            assertThat(tr).isInReadyStatus();

            TrustResult<TrustRelationship> result = tr.activate();
            
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();
            
            assertThat(updated).isInActivatingStatus();
            assertThat(updated).hasNoActivationDiagnostics();
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activatingTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN an ACTIVATING TrustRelationship " +
            "WHEN cancelActivation() is called " + 
            "THEN should transition to READY state and increment version "
        )
        public void shouldTransitionToReady_whenCancelActivationCalledFromActivating(TrustRelationship tr) {

            assertThat(tr).isInActivatingStatus();
            TrustResult<TrustRelationship> result = tr.cancelActivation();

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInReadyStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activatingTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN an ACTIVATING TrustRelationship " +
            "WHEN finalizeActivation() is called with null ActivationDiagnostics " +
            "THEN should fail with the appropriate error"
        )
        public void shouldFailWhenFinalizeActivationIsCalledWithNullActivationContext(TrustRelationship tr) {

            assertThat(tr).isInActivatingStatus();

            TrustResult<TrustRelationship> result = tr.finalizeActivation(null);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);

            CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
            assertThat(cause).isNotNull();
            assertThat(cause.getFieldName()).isEqualTo("activationDiagnostics");
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activatingTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN an ACTIVATING TrustRelationship " +
            "WHEN finalizeActivation() is called with a successful ActivationDiagnostics " +
            "THEN should transition to ACTIVE state and increment version " 
        )
        public void shouldTransitionToActive_whenFinalizeActivationSucceeds(TrustRelationship tr) {

            ActivationDiagnostics diagnostics = TrustRelationshipFixtures.sampleActivationDiagnosticsForSuccessfulActivation();

            assertThat(tr).isInActivatingStatus();
            assertThat(diagnostics.getStatus()).isEqualTo(ActivationStatus.SUCCEEDED);

            TrustResult<TrustRelationship> result = tr.finalizeActivation(diagnostics);

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInActiveStatus();
            assertThat(updated.getActivationDiagnostics()).isEqualTo(diagnostics);
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activatingTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN an ACTIVATING TrustRelationship " +
            "WHEN finalizeActivation() is called with a failed ActivationDiagnostics " + 
            "THEN should transition to READY and increment version "
        )
        public void shouldTransitionToReady_whenFinalizeActivationFails(TrustRelationship tr) {

            ActivationDiagnostics diagnostics = TrustRelationshipFixtures.sampleActivationDiagnosticsForFailedActivation();

            assertThat(tr).isInActivatingStatus();
            assertThat(diagnostics.getStatus()).isEqualTo(ActivationStatus.FAILED);

            TrustResult<TrustRelationship> result = tr.finalizeActivation(diagnostics);

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInReadyStatus();
            assertThat(updated.getActivationDiagnostics()).isEqualTo(diagnostics);
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activeTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN an ACTIVE TrustRelationship " +
            "WHEN deactivate() is called " +
            "THEN should transition to INACTIVE state and increment version "
        )
        public void shouldTransitionToInactive_whenDeactivateCalledFromActive(TrustRelationship tr) {

            assertThat(tr).isInActiveStatus();
            
            TrustResult<TrustRelationship> result = tr.deactivate();

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInInactiveStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#inactiveTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN an INACTIVE TrustRelationship with a real metadata source and at least one active profile " +
            "WHEN activate() is called " + 
            "THEN should transition to ACTIVATING state and increment version "
        )
        public void shouldTransitionToActivatingFromInactive_whenRequirementsMet(TrustRelationship tr) {

            assertThat(tr).isInInactiveStatus();
            assertThat(tr).hasRealMetadataSource();
            assertThat(tr).hasAtLeastOneActiveProfileConfiguration();

            TrustResult<TrustRelationship> result = tr.activate();

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInActivatingStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource({
            "io.jans.shibboleth.model.TrustRelationshipTests#inactiveTrustRelationshipsOfAllNaturesWithNoRealMetadataSource",
            "io.jans.shibboleth.model.TrustRelationshipTests#inactiveTrustRelationshipsOfAllNaturesWithNoActiveProfileConfiguration"
        })
        @DisplayName(
            "GIVEN an INACTIVE TrustRelationship with no real metadata source or no active profile " +
            "WHEN activate() is called " +
            "THEN should transition to DRAFT state and increment version "
        )
        public void shouldTransitionToDraft_whenActivateCalledFromInactiveButRequirementsNotMet(TrustRelationship tr) {

            assertThat(tr).isInInactiveStatus();
            assertThat(tr.hasNoRealMetadataSource() || tr.hasNoActiveProfileConfiguration()).isTrue();

            TrustResult<TrustRelationship> result = tr.activate();

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInDraftStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }
    
    }

    @Nested
    @DisplayName("State Transitions -- Updates From ACTIVE State")
    public class UpdatesFromActiveStateTests {

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activeTrustRelationshipsOfAllNaturesWithDifferentMetadataSources")
        @DisplayName(
            "GIVEN an ACTIVE TrustRelationship " +
            "WHEN updateMetadataSource() is called with a different real metadata source " +
            "THEN should transition to ACTIVATING state and increment version "
        )
        public void shouldTransitionToActivating_whenMetadataSourceUpdatedFromActive(TrustRelationship tr, MetadataSource source) {
        
            assertThat(tr).isInActiveStatus();
            assertThat(source).isNotEqualTo(NoMetadataSource.getInstance());
            assertThat(source).isNotEqualTo(tr.getMetadataSource());
        
            TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();
            assertThat(updated).isInActivatingStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activeTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN an ACTIVE TrustRelationship " +
            "WHEN updateMetadataSource() is called with the *same* current metadata source " +
            "THEN should remain in ACTIVE state and version should not change (idempotent) "
        )
        public void shouldRemainInActive_whenMetadataSourceUpdateIsNoOp(TrustRelationship tr) {

            assertThat(tr).isInActiveStatus();
            MetadataSource source = tr.getMetadataSource();

            TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship same = result.getValue();
            assertThat(same).isInActiveStatus();
            assertThat(same).isVersion(tr.getVersion());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activeTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN an ACTIVE TrustRelationship " +
            "WHEN updateMetadataSource() is called with a metadatasource of type `NONE` " +
            "THEN should transition to DRAFT state and increment version "
        )
        public void shouldTransitionToDraft_whenMetadataSourceSetToNoneFromActive(TrustRelationship tr) {

            assertThat(tr).isInActiveStatus();
            MetadataSource source = NoMetadataSource.getInstance();

            TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInDraftStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activeTrustRelationshipsOfAllNaturesWithDifferentProfileConfiguration")
        @DisplayName(
            "GIVEN an ACTIVE TrustRelationship " +
            "WHEN updateXXXProfileConfiguration() is called with a *different* configuration than the current one " +
            "     that keeps at one active profile " +
            "THEN should transition to ACTIVATING state and increment version "
        )
        public void shouldTransitionToActivating_whenProfileConfigurationActuallyChangedFromActive(
            TrustRelationship tr,Object profileconfig, ProfileConfigurationAccessor accessor) {

            assertThat(tr).isInActiveStatus();
            assertThat(accessor.extract(tr)).isNotEqualTo(profileconfig);

            TrustResult<TrustRelationship> result = accessor.update(tr, profileconfig);
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();
            assertThat(updated).isInActivatingStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }
        
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activeTrustRelationshipsOfAllNaturesWithAllProfileAccessors")
        @DisplayName(
            "GIVEN an ACTIVE TrustRelationship " +
            "WHEN updateXXXProfileConfiguration() is called with the *same* configuration as the current one " +
            "THEN should remaine in ACTIVE state and version should not change (idempotent) "
        )
        public void shouldRemainInActive_whenProfileConfigurationIsNoOp(TrustRelationship tr, ProfileConfigurationAccessor accessor) {

            assertThat(tr).isInActiveStatus();
            
            TrustResult<TrustRelationship> result = accessor.update(tr,accessor.extract(tr));
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship same = result.getValue();

            assertThat(same).isInActiveStatus();
            assertThat(same).isVersion(tr.getVersion());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activeTrustRelationshipsOfAllNaturesWithActiveProfileAccessor")
        @DisplayName(
            "GIVEN an ACTIVE TrustRelationship " +
            "WHEN updateXXXProfileConfiguration() is called such that all profiles become disabled " +
            "THEN should transition to DRAFT state and increment version "
        )
        public void shouldTransitionToDraft_whenAllProfilesDisabledFromActive(TrustRelationship tr,ProfileConfigurationAccessor accessor) {

            assertThat(tr).isInActiveStatus();
            assertThat(tr).hasActiveProfileConfigurationCount(1);

            TrustResult<TrustRelationship> result = accessor.updateStatus(tr,ProfileStatus.INACTIVE);
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInDraftStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }
    }

    @Nested
    @DisplayName("State Transitions -- Discovered Entity IDs Tests")
    public class DiscoveredEntityIdsTests {

        @Test
        @DisplayName(
            "GIVEN an ACTIVATING AGGREGATE TrustRelationship " +
            "WHEN incorporateDiscoveredEntityIds() is called with new valid entity ids " +
            "THEN should update the discovered entityids and increment version (no state change) "
        )
        public void shouldIncorporateDiscoveredEntityIds_whenInActivatingStateForAggregate() {

            TrustRelationship tr = TrustRelationshipFixtures.sampleActivatingAggregateTrustRelationship();
            EntityIds ids = TrustRelationshipFixtures.sampleEntityIds();

            assertThat(tr).isInActivatingStatus();
            assertThat(tr.getDiscoveredEntityIds()).isNotEqualTo(ids);

            TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(ids);
            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated.getDiscoveredEntityIds()).isEqualTo(ids);
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @Test
        @DisplayName(
            "GIVEN an ACTIVATING AGGREGATE TrustRelationship with existing discovered entity ids " + 
            "WHEN incorporateDiscoveredEntityIds() is called with the exact same entity ids " +
            "THEN should be idempotent (no version change , no error)" 
        )
        public void shouldBeIdempotent_whenIncorporateDiscoveredEntityIdsWithSameValue() {

            TrustRelationship tr = TrustRelationshipFixtures.sampleActivatingAggregateTrustRelationshipWithDiscoveredEntityIds();

            assertThat(tr).isInActivatingStatus();
            assertThat(tr).hasAnyDiscoveredEntityIds();

            EntityIds ids = EntityIds.from(tr.getDiscoveredEntityIds()).build().getValue();
            assertThat(ids).isEqualTo(tr.getDiscoveredEntityIds());

            TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(ids);

            assertThat(result.isSuccess()).isTrue();

            TrustRelationship same = result.getValue();
            assertThat(same.getDiscoveredEntityIds()).isEqualTo(ids);
            assertThat(same).isVersion(tr.getVersion());

        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#aggregateTrustRelationshipsNotInActivatingState")
        @DisplayName(
            "GIVEN an AGGREGATE TrustRelationship that is not in ACTIVATING state " +
            "WHEN incorporateDiscoveredEntityIds() is called with valid entityIDs " +
            "THEN should fail with appropriate error"
        )
        public void shouldRejectIncorporateDiscoveredEntityIds_whenAggregateNotInActivatingState(TrustRelationship tr) {

            assertThat(tr).isOfAggregateNature();
            assertThat(tr).doesNotHaveStatus(TrustStatus.ACTIVATING);

            EntityIds ids = TrustRelationshipFixtures.sampleEntityIds();
            TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(ids);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(DomainObjectConsistencyFailed.class);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#individualTrustRelationshipsInMultipleStates")
        @DisplayName(
            "GIVEN an INDIVIDUAL TrustRelationship irrespective of state " +
            "WHEN incorporateDiscoveredEntityIds() is called with valid entityIDs " +
            "THEN should fail with appropriate error "
        )
        public void shouldRejectIncorporateDiscoveredEntityIds_whenTrustIsIndividual(TrustRelationship tr) {

            assertThat(tr).isOfIndividualNature();

            EntityIds ids = TrustRelationshipFixtures.sampleEntityIds();
            TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(ids);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(DomainObjectConsistencyFailed.class);
        }

        @Test
        @DisplayName(
            "GIVEN an ACTIVATING AGGREGATE TrustRelationship " +
            "WHEN incorporateDiscoveredEntityIds() is called with null entityIds " +
            "THEN should fail with the appropriate error"
        )
        public void shouldFailIncorporateDiscoveredEntityIdsWhenEntityIdsIsNull() {

            TrustRelationship tr = TrustRelationshipFixtures.sampleActivatingAggregateTrustRelationship();

            assertThat(tr).isInActivatingStatus();
            assertThat(tr).isOfAggregateNature();

            TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(null);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
        }
    }

    @Nested
    @DisplayName("Restrictions , Nature Rules & Error Cases -- State Restriction Rules")
    public class StateRestrictionRulesTest {

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activatingTrustRelationshipsOfAllNaturesWithSupportedMetadataSources")
        @DisplayName(
            "GIVEN an ACTIVATING TrustRelationship " + 
            "WHEN updateMetadataSource() is called " +
            "THEN should fail with the appropriate error "
        )
        public void shouldFailWhenUpdateMetadataSourceCalledInActivatingState(TrustRelationship tr,MetadataSource source) {

            assertThat(tr).isInActivatingStatus();

            TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(DomainObjectConsistencyFailed.class);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activatingTrustRelationshipsWithProfileConfigurations")
        @DisplayName(
            "GIVEN an ACTIVATING TrustRelationship " +
            "WHEN updateXXXProfileConfiguration() is called " +
            "THEN should fail with the appropriate error " 
        )
        public void shouldFailWhenUpdateProfileConfigurationCalledInActivatingStatus(TrustRelationship tr,Object config, ProfileConfigurationAccessor accessor) {

            assertThat(tr).isInActivatingStatus();

            TrustResult<TrustRelationship> result = accessor.update(tr,config);
            
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(DomainObjectConsistencyFailed.class);
        }


        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#trustRelationshipsOfAllNaturesNotInActivatingState")
        @DisplayName(
            "GIVEN a TrustRelationship that is NOT in ACTIVATING state " +
            "WHEN finalizeActivation() is called " +
            "THEN should fail with the appropriate error "
        )
        public void shouldFailFinalizeActivation_whenNotInActivatingState(TrustRelationship tr) {

            assertThat(tr).doesNotHaveStatus(TrustStatus.ACTIVATING);

            ActivationDiagnostics diagnostics = TrustRelationshipFixtures.sampleActivationDiagnosticsForFailedActivation();
            TrustResult<TrustRelationship> result = tr.finalizeActivation(diagnostics);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(DomainObjectConsistencyFailed.class);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#trustRelationshipsOfAllNaturesNotInActivatingState")
        @DisplayName(
            "GIVEN a TrustRelationship that is NOT in ACTIVATING state " +
            "WHEN cancelActivation() is called " + 
            "THEN should fail with OperationRestrictedByStatus error "
        )
        public void shouldFailCancelActivation_whenNotInActivatingState(TrustRelationship tr) {

            assertThat(tr).doesNotHaveStatus(TrustStatus.ACTIVATING);
            TrustResult<TrustRelationship> result = tr.cancelActivation();

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();

            assertThat(error.getCause()).isInstanceOf(DomainObjectConsistencyFailed.class);
        }
    }

    @Nested
    @DisplayName("Restrictions, Nature Rules & Error Cases -- Nature Restrictions")
    public class NatureRestrictionsTests {

        @Test
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipFixtures#sampleActivatingIndividualTrustRelationship")
        @DisplayName(
            "GIVEN an ACTIVATING INDIVIDUAL TrustRelationship " +
            "WHEN incorporateDiscoveredEntityIds() is called "  +
            "THEN should fail with the appropriate error "
        )
        public void shouldRejectIncorporateDiscoveredEntityIds_whenTrustIsIndividual() {

            TrustRelationship tr = TrustRelationshipFixtures.sampleActivatingIndividualTrustRelationship();

            assertThat(tr).isOfIndividualNature();
            assertThat(tr).isInActivatingStatus();

            EntityIds ids = TrustRelationshipFixtures.sampleEntityIds();
            TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(ids);
            
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();

            assertThat(error.getCause()).isInstanceOf(DomainObjectConsistencyFailed.class);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#trustRelationshipsOfAllNaturesWithIncompatibleMetadataSources")
        @DisplayName(
            "GIVEN a TrustRelationship NOT in ACTIVATING state " +
            "WHEN updateMetadataSource() is called with a metadatasource incompatible with its nature " +
            "THEN should fail with the appropriate error "
        )
        public void shouldFailWhenUsingIncompatibleMetadataSourceForTrustNature(TrustRelationship tr, MetadataSource source) {

            assertThat(tr).doesNotSupportMetadataSource(source);

            TrustResult<TrustRelationship> result = tr.updateMetadataSource(source);
            
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(DomainObjectConsistencyFailed.class);
        }
    }

    @Nested
    @DisplayName("Restrictions, Nature Rules and Error Cases -- Consistency and Invariant Violations ")
    public class ConsistencyAndInvariantViolationsTests {

       @ParameterizedTest
       @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsWithRequiredFieldsInvalidators")
       @DisplayName(
            "GIVEN a Builder with one required field set to null or invalid " +
            "WHEN build() is called " +
            "THEN should fail with the appropriate error "
       )
       public void shouldFailWhenRequiredFieldsAreNullOrInvalidDuringBuild(TrustRelationship tr,Consumer<TrustRelationship.Builder> invalidator) {

            TrustRelationship.Builder builder = TrustRelationship.from(tr);
            invalidator.accept(builder);
            TrustResult<TrustRelationship> result = builder.build();

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
       }

       @ParameterizedTest
       @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsOfAllNatures")
       @DisplayName(
            "GIVEN a Builder with metadataSource set to null " +
            "WHEN build() is called  " +
            "THEN should fail with the appropriate error "
       )
       public void shouldFailWhenMetadataSourceIsNull(TrustRelationship tr) {

            TrustResult<TrustRelationship> result = TrustRelationship
                .from(tr)
                .withMetadataSource(null)
                .build();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
       }

       @ParameterizedTest
       @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsWithProfileConfigUpdaters")
       @DisplayName(
            "GIVEN a Builder with at least one profileconfiguration set to null " +
            "WHEN build() is called  " + 
            "THEN should fail with CannotBeNullOrBlank as root cause " 
       )
       public void shouldFailWhenAnyProfileConfigurationIsNull(TrustRelationship tr, ProfileConfigurationAccessor accessor,String requiredField) {

            TrustRelationship.Builder builder  = accessor.configureWithBuilder(TrustRelationship.from(tr),null);

            TrustResult<TrustRelationship> result = builder.build();
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            DomainObjectUpdateFailed error  = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
            CannotBeNullOrBlank cause = (CannotBeNullOrBlank) error.getCause();
            assertThat(cause.getFieldName()).isEqualTo(requiredField);

       }

       @ParameterizedTest
       @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#draftTrustRelationshipsOfAllNatures")
       @DisplayName(
            "GIVEN a Builder with discoveredEntityIds set to null " +
            "WHEN build() is called " +
            "THEN should fail with the appropriate error "
       )
       public void shouldFailWhenDiscoveredEntityIdsIsNull(TrustRelationship tr) {

            TrustResult<TrustRelationship> result = TrustRelationship
                .from(tr)
                .withDiscoveredEntityIds(null)
                .build();
            
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);

            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
       }
       
    }

    @Nested
    @DisplayName("Advanced Scenarios and Edge Cases -- Complex State Transitions and Interactions")
    public class ComplexStateTransitionsAndInteractionsTests {
        
        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activeTrustRelationshipsOfAllNaturesWithDifferentMetadataSources")
        @DisplayName(
            "GIVEN an ACTIVE TrustRelationship " +
            "WHEN updateMetadataSource() is called with a  *different* metadatasource " +
            "THEN should transition to ACTIVATING state and increment version " 
        )
        public void shouldTransitionToActivating_whenMetadataSourceUpdatedFromActive(TrustRelationship tr, MetadataSource newsource) {

            assertThat(tr).isInActiveStatus();
            assertThat(newsource).isNotEqualTo(tr.getMetadataSource());

            TrustResult<TrustRelationship> result = tr.updateMetadataSource(newsource);

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInActivatingStatus();
            assertThat(updated).isVersion(tr.getVersion().next());
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activeTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN an ACTIVE TrustRelationship " +
            "WHEN updateMetadataSource() is called with the *same* current metadata source " +
            "THEN should remain in ACTIVE state and version should not change (idempotent) " 
        )
        public void shouldRemainInActive_whenMetadataSourceUpdateIsNoOp(TrustRelationship tr) {

            assertThat(tr).isInActiveStatus();
            MetadataSource samesource = tr.getMetadataSource();

            TrustResult<TrustRelationship> result = tr.updateMetadataSource(samesource);

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship same = result.getValue();

            assertThat(same).isInActiveStatus();
            assertThat(same).isVersion(tr.getVersion());
        }
    }

    @Nested
    @DisplayName("Advanced Scenarios and Edge Cases -- Activation Diagnostics Behavior")
    public class ActivationDiagnosticsBehaviorTests {

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#readyTrustRelationshipsWithActivationDiagnostics")
        @DisplayName(
            "GIVEN a READY TrustRelationship with previous activation diagnostics " +
            "WHEN activate() is called " +
            "THEN should clear previous diagnostics and transition to ACTIVATING "
        )
        public void shouldClearPreviousDiagnostics_whenActivateIsCalledFromReady(TrustRelationship tr) {

            assertThat(tr).isInReadyStatus();
            assertThat(tr).hasActivationDiagnostics();

            TrustResult<TrustRelationship> result = tr.activate();

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).hasNoActivationDiagnostics();
            assertThat(updated).isInActivatingStatus();
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activatingTrustRelationshipsWithSuccessActivationDiagnostics")
        @DisplayName(
            "GIVEN an ACTIVATING TrustRelationship " +
            "WHEN finalizeActivation() is called with a successful ActivationContext containing diagnostics " +
            "THEN the resulting TrustRelationship should be in ACTIVE state and contain the activation diagnostics " 
        )
        public void shouldIncludeActivationDiagnosticsAfterSuccessfulFinalizeActivation(TrustRelationship tr,ActivationDiagnostics success_diagnostics) {

            assertThat(tr).isInActivatingStatus();
            assertThat(success_diagnostics.getStatus()).isEqualTo(ActivationStatus.SUCCEEDED);

            TrustResult<TrustRelationship> result = tr.finalizeActivation(success_diagnostics);

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInActiveStatus();
            assertThat(updated.getActivationDiagnostics()).isEqualTo(success_diagnostics);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activatingTrustRelationshipsWithFailedActivationDiagnostics")
        @DisplayName(
            "GIVEN an ACTIVATING TrustRelationship " +
            "WHEN finalizeActivation() is called with a failed ActivationContext " +
            "THEN the resulting TrustRelationship should be in READY state and contain the activation diagnostics "
        )
        public void shouldIncludeActivationDiagnosticsAfterFailedFinalizeActivation(TrustRelationship tr, ActivationDiagnostics failed_diagnostics) {

            assertThat(tr).isInActivatingStatus();
            assertThat(failed_diagnostics.getStatus()).isEqualTo(ActivationStatus.FAILED);

            TrustResult<TrustRelationship> result = tr.finalizeActivation(failed_diagnostics);

            assertThat(result.isSuccess()).isTrue();
            TrustRelationship updated = result.getValue();

            assertThat(updated).isInReadyStatus();
            assertThat(updated.getActivationDiagnostics()).isEqualTo(failed_diagnostics);
        }
    }

    @Nested
    @DisplayName("Advanced Scenarios and Edge Cases -- Reconstruction & Persistence Scenarios")
    public class ReconstructionAndPersistenceScenariosTests {

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#trustRelationshipsOfVariousStatuses")
        @DisplayName(
            "GIVEN a fully populated TrustRelationship in any state " +
            "WHEN it is rebuilt using the builder from persisted data (reconstruction scenario) " +
            "THEN all data, state, version and invariants should be preserved " 
        )
        public void shouldPreserveAllDataWhenRebuildingFromStorage(TrustRelationship tr) {

            TrustResult<TrustRelationship> result  = TrustRelationship.builder()
                .withId(tr.getId())
                .withVersion(tr.getVersion())
                .withDisplayName(tr.getDisplayName())
                .withDescription(tr.getDescription())
                .withNature(tr.getNature())
                .withStatus(tr.getStatus())
                .withMetadataSource(tr.getMetadataSource())
                .withDiscoveredEntityIds(tr.getDiscoveredEntityIds())
                .withShibbolethSsoProfileConfiguration(tr.getShibbolethSsoProfileConfiguration())
                .withSaml2ArtifactResolutionProfileConfiguration(tr.getSaml2ArtifactResolutionProfileConfiguration())
                .withSaml2AttributeQueryProfileConfiguration(tr.getSaml2AttributeQueryProfileConfiguration())
                .withSaml2EcpProfileConfiguration(tr.getSaml2EcpProfileConfiguration())
                .withSaml2SsoProfileConfiguration(tr.getSaml2SsoProfileConfiguration())
                .withSaml2LogoutProfileConfiguration(tr.getSaml2LogoutProfileConfiguration())
                .withReleasedAttributes(tr.getReleasedAttributes())
                .withActivationDiagnostics(tr.getActivationDiagnostics())
                .build();
            
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(tr);
        }
    }

    @Nested
    @DisplayName("Advanced Scenarios and Edge Cases -- Cross-Cutting & Regression-Prone Scenarios")
    public class CrossCuttingAndRegressionProneScenariosTests {

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#trustRelationshipsWithIdempotentUpdateOperations")
        @DisplayName(
            "GIVEN a TrustRelationship in any valid state where the operation is allowed " +
            "WHEN any update method/operation is called with the same current value " +
            "THEN version should not be incremented and state should remain the same "
        )
        public void shouldMaintainVersionWhenNoActualChangeInAnyState(TrustRelationship tr, 
            Function<TrustRelationship,TrustResult<TrustRelationship>> operation) {

            TrustResult<TrustRelationship> result = operation.apply(tr);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue().getVersion()).isEqualTo(tr.getVersion());

        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activatingTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN an ACTIVATING TrustRelationship " +
            "WHEN incorporateDiscoveredEntityIds() is called with null EntityIds " +
            "THEN should fail with appropriate error "
        )
        public void shouldFailIncorporateDiscoveredEntityIdsWhenEntityIdsIsNull(TrustRelationship tr) {

            assertThat(tr).isInActivatingStatus();

            TrustResult<TrustRelationship> result = tr.incorporateDiscoveredEntityIds(null);

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(CannotBeNullOrBlank.class);
        }
    }

    @Nested
    @DisplayName("Advanced Scenarios and Edge Cases -- Additional Error Cases")
    public class AdditionalErrorCasesTests {

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activatingTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN an ACTIVATING TrustRelationship " +
            "WHEN activate() is called " + 
            "THEN should fail with OperationForbiddenFromStatus error"
        )
        public void shouldFailWhenActivateCalledFromActivatingState(TrustRelationship tr) {

            assertThat(tr).isInActivatingStatus();

            TrustResult<TrustRelationship> result = tr.activate();

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(DomainObjectConsistencyFailed.class);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#trustRelationshipsOfAllStatusesExceptActive")
        @DisplayName(
            "GIVEN a TrustRelationship that is not in ACTIVE state " +
            "WHEN deactivate() is called " +
            "THEN should fail with OperationForbiddenFromStatus error"
        )
        public void shouldFailWhenDeactivateCalledFromNonActiveState(TrustRelationship tr) {

            assertThat(tr).isNotInStatus(TrustStatus.ACTIVE);

            TrustResult<TrustRelationship> result = tr.deactivate();

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(DomainObjectConsistencyFailed.class);
        }

        @ParameterizedTest
        @MethodSource("io.jans.shibboleth.model.TrustRelationshipTests#activatingTrustRelationshipsOfAllNatures")
        @DisplayName(
            "GIVEN an ACTIVATING TrustRelationship " +
            "WHEN deactivate() is called " +
            "THEN should fail with OperationForbiddenFromStatus error"
        )
        public void shouldFailWhenDeactivateCalledFromActivatingStatus(TrustRelationship tr) {

            assertThat(tr).isInActivatingStatus();

            TrustResult<TrustRelationship> result = tr.deactivate();

            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isInstanceOf(DomainObjectUpdateFailed.class);
            DomainObjectUpdateFailed error = (DomainObjectUpdateFailed) result.getError();
            assertThat(error.getCause()).isInstanceOf(DomainObjectConsistencyFailed.class);
        }
    }
} 