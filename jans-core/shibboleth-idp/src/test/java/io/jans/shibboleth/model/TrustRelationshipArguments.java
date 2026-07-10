package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.metadata.FileMetadataSource;
import io.jans.shibboleth.model.metadata.MetadataSource;
import io.jans.shibboleth.model.metadata.NoMetadataSource;
import io.jans.shibboleth.model.metadata.UriMetadataSource;
import io.jans.shibboleth.model.config.profiles.*;
import io.jans.shibboleth.model.config.profiles.common.*;

import org.junit.jupiter.params.provider.Arguments;

import java.net.URI;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.jans.shibboleth.model.TrustRelationshipFixtures.*;

/**
 * Argument providers for the parameterized tests in {@link TrustRelationshipTests}.
 * Referenced from {@code @MethodSource("io.jans.shibboleth.model.TrustRelationshipArguments#...")}.
 */
public final class TrustRelationshipArguments {

    private TrustRelationshipArguments() {}

    public static Stream<Arguments> creationParametersWithValidValues() {

        var individualTrDisplayName = io.jans.shibboleth.model.core.DisplayName.of("IndividualTR").getValue();
        var aggregateTrDisplayName  = io.jans.shibboleth.model.core.DisplayName.of("AggregateTR").getValue();

        var individualTrDescription = Description.of("Individual TR");
        var aggregateTrDescription  = Description.of("Aggregate TR");
        return Stream.of(
            Arguments.of(individualTrDisplayName,individualTrDescription,TrustNature.INDIVIDUAL),
            Arguments.of(aggregateTrDisplayName,aggregateTrDescription,TrustNature.AGGREGATE)
        );
    }

    public static Stream<Arguments> creationParametersWithNullValuesAndMissingFieldNames() {

        var displayName = io.jans.shibboleth.model.core.DisplayName.of("SomeTR").getValue();
        var description = Description.of("Some TR");
        var trustnature = TrustNature.INDIVIDUAL;

        return Stream.of(
            Arguments.of(displayName,description,null,"nature"),
            Arguments.of(displayName,null,trustnature,"description"),
           Arguments.of(null,description,trustnature,"displayName")
        );
    }

    public static Stream<TrustRelationship> draftTrustRelationshipsOfAllNatures() {

        return Stream.of( sampleDraftIndividualTrustRelationship(),sampleDraftAggregateTrustRelationship() );
    }

    public static Stream<Arguments> draftTrustRelationshipsWithSupportedMetadataSources() {

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

    public static Stream<Arguments> draftTrustRelationshipsWithProfileConfigurationsAndAccessors() {

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

    public static Stream<Arguments> draftTrustRelationshipsAndAccessors() {

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

    public static Stream<Arguments> draftTrustRelationshipsAndReleasedAttributes() {

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

    public static Stream<Arguments> draftTrustRelationshipsWithAnActiveProfileAndRealMetadataSource() {

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

    public static Stream<Arguments> draftTrustRelationshipsWithRealMetadataSourceAndActiveProfileConfigurationToUpdate() {

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

    public static Stream<Arguments> readyTrustRelationshipsOfAllNatures() {

        return Stream.of(
            Arguments.of(sampleReadyIndividualTrustRelationship()),
            Arguments.of(sampleReadyAggregateTrustRelationship())
        );
    }

    public static Stream<Arguments> readyTrustRelationshipsWithSingleActiveProfileConfiguration() {

        ShibbolethSsoProfileConfiguration shibbolethsso = activeShibbolethSsoProfileConfiguration();
        Saml2SsoProfileConfiguration saml2sso = activeSaml2SsoProfileConfiguration();

        TrustRelationship individual = sampleReadyIndividualTrustRelationship(ProfileConfigurationAccessor.SHIBBOLETH_SSO,shibbolethsso);
        TrustRelationship aggregate  = sampleReadyAggregateTrustRelationship(ProfileConfigurationAccessor.SAML2_SSO,saml2sso);

        return Stream.of(
            Arguments.of(individual,ProfileConfigurationAccessor.SHIBBOLETH_SSO),
            Arguments.of(aggregate,ProfileConfigurationAccessor.SAML2_SSO)
        );
    }

    public static Stream<Arguments> readyTrustRelationshipsWithActivationDiagnostics() {

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


    public static Stream<Arguments> activatingTrustRelationshipsOfAllNatures() {

        return Stream.of(
            Arguments.of(sampleActivatingIndividualTrustRelationship()),
            Arguments.of(sampleActivatingAggregateTrustRelationship())
        );
    }

    public static Stream<Arguments> activatingTrustRelationshipsOfAllNaturesWithSupportedMetadataSources() {

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


    public static Stream<Arguments> activatingTrustRelationshipsWithProfileConfigurations() {

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

    public static Stream<Arguments> activatingTrustRelationshipsWithSuccessActivationDiagnostics() {

        return Stream.of(
            Arguments.of(sampleActivatingIndividualTrustRelationship(),sampleActivationDiagnosticsForSuccessfulActivation()),
            Arguments.of(sampleActivatingAggregateTrustRelationship(),sampleActivationDiagnosticsForSuccessfulActivation())
        );
    }

    public static Stream<Arguments> activatingTrustRelationshipsWithFailedActivationDiagnostics() {

        return Stream.of(
            Arguments.of(sampleActivatingIndividualTrustRelationship(),sampleActivationDiagnosticsForFailedActivation()),
            Arguments.of(sampleActivatingAggregateTrustRelationship(),sampleActivationDiagnosticsForFailedActivation())
        );
    }

    public static Stream<Arguments> activeTrustRelationshipsOfAllNatures() {

        return Stream.of(
            Arguments.of(sampleActiveIndividualTrustRelationship()),
            Arguments.of(sampleActiveAggregateTrustRelationship())
        );
    }

    public static Stream<Arguments> activeTrustRelationshipsOfAllNaturesWithAllProfileAccessors() {

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

    public static Stream<Arguments> inactiveTrustRelationshipsOfAllNatures() {

        return Stream.of(
            Arguments.of(sampleInactiveIndividualTrustRelationship()),
            Arguments.of(sampleInactiveAggregateTrustRelationship())
        );
    }

    public static Stream<Arguments> inactiveTrustRelationshipsOfAllNaturesWithNoRealMetadataSource() {

        return Stream.of(
            Arguments.of(sampleInactiveIndividualTrustRelationshipWithNoRealMetadataSource()),
            Arguments.of(sampleInactiveAggregateTrustRelationshipWithNoRealMetadataSource())
        );
    }

    public static Stream<Arguments> inactiveTrustRelationshipsOfAllNaturesWithNoActiveProfileConfiguration() {

        return Stream.of(
            Arguments.of(sampleInactiveIndividualTrustRelationshipWithNoActiveProfileConfiguration()),
            Arguments.of(sampleInactiveAggregateTrustRelationshipWithNoActiveProfileConfiguration())
        );
    }

    public static Stream<Arguments> aggregateTrustRelationshipsNotInActivatingState() {

        return Stream.of(
            Arguments.of(sampleDraftAggregateTrustRelationship()),
            Arguments.of(sampleReadyAggregateTrustRelationship()),
            Arguments.of(sampleActiveAggregateTrustRelationship()),
            Arguments.of(sampleInactiveAggregateTrustRelationship())
        );
    }

    public static Stream<Arguments> individualTrustRelationshipsInMultipleStates() {

        return Stream.of(
            Arguments.of(sampleDraftIndividualTrustRelationship()),
            Arguments.of(sampleReadyIndividualTrustRelationship()),
            Arguments.of(sampleActivatingIndividualTrustRelationship()),
            Arguments.of(sampleActiveIndividualTrustRelationship()),
            Arguments.of(sampleInactiveIndividualTrustRelationship())
        );
    }

    public static Stream<Arguments> trustRelationshipsOfAllNaturesNotInActivatingState() {

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

    public static Stream<Arguments> trustRelationshipsOfAllNaturesWithIncompatibleMetadataSources() {

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

    public static Stream<Arguments> draftTrustRelationshipsWithRequiredFieldsInvalidators() {

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

    public static Stream<Arguments> draftTrustRelationshipsWithProfileConfigUpdaters() {

        return Stream.of(

            Arguments.of(sampleDraftIndividualTrustRelationship(),ProfileConfigurationAccessor.SHIBBOLETH_SSO,"shibbolethSsoProfileConfiguration"),
            Arguments.of(sampleDraftAggregateTrustRelationship(),ProfileConfigurationAccessor.SAML2_ARTIFACT_RESOLUTION,"saml2ArtifactResolutionProfileConfiguration"),
            Arguments.of (sampleDraftIndividualTrustRelationship(),ProfileConfigurationAccessor.SAML2_ATTRIBUTE_QUERY,"saml2AttributeQueryProfileConfiguration"),
            Arguments.of(sampleDraftAggregateTrustRelationship(),ProfileConfigurationAccessor.SAML2_ECP,"saml2EcpProfileConfiguration"),
            Arguments.of(sampleDraftIndividualTrustRelationship(),ProfileConfigurationAccessor.SAML2_SSO,"saml2SsoProfileConfiguration"),
            Arguments.of(sampleDraftAggregateTrustRelationship(),ProfileConfigurationAccessor.SAML2_LOGOUT,"saml2LogoutProfileConfiguration")
        );
    }

    public static Stream<Arguments> activeTrustRelationshipsOfAllNaturesWithDifferentMetadataSources() {

        MetadataSource filesource = FileMetadataSource.of("/opt/gluu/original_sp.xml").getValue();
        MetadataSource urisource = UriMetadataSource.of(URI.create("https://sample.gluu.org/sp_metadata.xml")).getValue();
        TrustRelationship individual = sampleActiveIndividualTrustRelationship(filesource);
        TrustRelationship aggregate = sampleActiveAggregateTrustRelationship(urisource);

        return Stream.of(
            Arguments.of(individual,urisource),
            Arguments.of(aggregate,filesource)
        );
    }

    public static Stream<Arguments> activeTrustRelationshipsOfAllNaturesWithSameMetadataSources() {

        MetadataSource filesource = FileMetadataSource.of("/opt/gluu/original_sp.xml").getValue();
        MetadataSource urisource = UriMetadataSource.of(URI.create("https://sample.gluu.org/sp_metadata.xml")).getValue();

        TrustRelationship individual = sampleActiveIndividualTrustRelationship(filesource);
        TrustRelationship aggregate  = sampleActiveAggregateTrustRelationship(urisource);

        return Stream.of(
            Arguments.of(individual,filesource),
            Arguments.of(aggregate,urisource)
        );
    }

    public static Stream<Arguments> activeTrustRelationshipsOfAllNaturesWithDifferentProfileConfiguration() {

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

    public static Stream<Arguments> activeTrustRelationshipsOfAllNaturesWithSameProfileConfiguration() {

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

    public static Stream<Arguments> activeTrustRelationshipsOfAllNaturesWithActiveProfileAccessor() {

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

    public static Stream<Arguments> trustRelationshipsOfVariousStatuses() {

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

    public static Stream<Arguments> trustRelationshipsWithIdempotentUpdateOperations() {

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

    public static Stream<Arguments> trustRelationshipsOfAllStatusesExceptActive() {

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
}
