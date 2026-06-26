package io.jans.shibboleth.model;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.jans.shibboleth.model.config.profiles.Saml2ArtifactResolutionProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2AttributeQueryProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2EcpProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2LogoutProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2SsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.SamlProfileConfigurationDefaults;
import io.jans.shibboleth.model.config.profiles.ShibbolethSsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.common.ProfileStatus;
import io.jans.shibboleth.model.core.Description;
import io.jans.shibboleth.model.core.DisplayName;
import io.jans.shibboleth.model.core.EntityId;
import io.jans.shibboleth.model.core.Id;
import io.jans.shibboleth.model.core.ReleasedAttribute;
import io.jans.shibboleth.model.core.TrustNature;
import io.jans.shibboleth.model.core.TrustStatus;
import io.jans.shibboleth.model.core.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.model.core.diagnostics.ActivationLogEntry;
import io.jans.shibboleth.model.core.diagnostics.ActivationStatus;
import io.jans.shibboleth.model.core.diagnostics.LogLevel;
import io.jans.shibboleth.model.core.diagnostics.Origin;
import io.jans.shibboleth.model.metadata.FileMetadataSource;
import io.jans.shibboleth.model.metadata.ManualMetadataSource;
import io.jans.shibboleth.model.metadata.MdqMetadataSource;
import io.jans.shibboleth.model.metadata.MetadataSource;
import io.jans.shibboleth.model.metadata.NoMetadataSource;
import io.jans.shibboleth.model.metadata.UpstreamMetadataSource;
import io.jans.shibboleth.model.metadata.UriMetadataSource;
import io.jans.shibboleth.model.metadata.manual.AssertionConsumerService;
import io.jans.shibboleth.model.metadata.manual.SamlBinding;
import io.jans.shibboleth.model.metadata.manual.ValidityPeriod;

public class TrustRelationshipFixtures {
    

    public static final TrustRelationship sampleDraftIndividualTrustRelationship() {

        DisplayName displayName = DisplayName.of("SampleIndividualTR").getValue();
        Description description = Description.of("Sample Individual TR");

        return TrustRelationship.create(displayName, description,TrustNature.INDIVIDUAL).getValue();
    }

    public static final TrustRelationship sampleDraftAggregateTrustRelationship() {

        DisplayName displayName = DisplayName.of("SampleAggregateTR").getValue();
        Description description = Description.of("Sample Aggregate TR");

        return TrustRelationship.create(displayName,description,TrustNature.AGGREGATE).getValue();
    }

    public static final TrustRelationship sampleDraftIndividualTrustRelationshipWithActiveProfile() {

        return sampleDraftIndividualTrustRelationship()
            .updateShibbolethSsoProfileConfiguration(activeShibbolethSsoProfileConfiguration())
            .getValue();
    }

    
    public static final TrustRelationship sampleDraftAggregateTrustRelationshipWithActiveProfile() {

        return sampleDraftAggregateTrustRelationship()
            .updateSaml2SsoProfileConfiguration(activeSaml2SsoProfileConfiguration())
            .getValue();
    }

    public static final TrustRelationship sampleDraftIndividualTrustRelationshipWithRealMetadataSource() {

        return sampleDraftIndividualTrustRelationship()
            .updateMetadataSource(sampleFileMetadataSource())
            .getValue();
    }

    public static final TrustRelationship sampleDraftAggregateTrustRelationshipWithRealMetadataSource() {

        return sampleDraftAggregateTrustRelationship()
            .updateMetadataSource(sampleUriMetadataSource())
            .getValue();
    }

    public static final TrustRelationship sampleReadyIndividualTrustRelationship() {

        return sampleDraftIndividualTrustRelationshipWithActiveProfile()
            .updateMetadataSource(sampleFileMetadataSource())
            .getValue();
    }


    public static final TrustRelationship sampleReadyAggregateTrustRelationship() {
 
        return sampleDraftAggregateTrustRelationshipWithActiveProfile()
            .updateMetadataSource(sampleMdqMetadataSource())
            .getValue();
    }

    public static final TrustRelationship sampleActivatingIndividualTrustRelationship() {

        return sampleReadyIndividualTrustRelationship().activate().getValue();
    }

    public static final TrustRelationship sampleActivatingAggregateTrustRelationship() {

        return sampleReadyAggregateTrustRelationship().activate().getValue();
    }

    public static final TrustRelationship sampleActivatingAggregateTrustRelationshipWithDiscoveredEntityIds() {

       return sampleActivatingAggregateTrustRelationship()
            .incorporateDiscoveredEntityIds(sampleEntityIds())
            .getValue();
    }

    public static final TrustRelationship sampleActiveIndividualTrustRelationship() {

        return sampleActivatingIndividualTrustRelationship()
            .finalizeActivation(sampleActivationDiagnosticsForSuccessfulActivation())
            .getValue();
    }

    public static final TrustRelationship sampleActiveAggregateTrustRelationship() {


        return sampleActivatingAggregateTrustRelationship()
            .finalizeActivation(sampleActivationDiagnosticsForSuccessfulActivation())
            .getValue();
    }

    public static final TrustRelationship sampleInactiveIndividualTrustRelationship() {

        return sampleActiveIndividualTrustRelationship()
            .deactivate()
            .getValue();
    }

    public static final TrustRelationship sampleInactiveAggregateTrustRelationship() {

       return sampleActiveAggregateTrustRelationship()
            .deactivate()
            .getValue();
    }

    public static final TrustRelationship sampleInactiveIndividualTrustRelationshipWithNoRealMetadataSource() {

       return sampleInactiveIndividualTrustRelationship()
            .updateMetadataSource(NoMetadataSource.getInstance())
            .getValue();
    }
    
    public static final TrustRelationship sampleInactiveAggregateTrustRelationshipWithNoRealMetadataSource() {

        return sampleInactiveAggregateTrustRelationship()
            .updateMetadataSource(NoMetadataSource.getInstance())
            .getValue();
        
    }

    private static final TrustRelationship withNoActiveProfileConfiguration(TrustRelationship tr) {

        return tr.updateShibbolethSsoProfileConfiguration(inactiveShibbolethSsoProfileConfiguration())
            .getValue()
            .updateSaml2ArtifactResolutionProfileConfiguration(inactiveSaml2ArtifactResolutionProfileConfiguration())
            .getValue()
            .updateSaml2AttributeQueryProfileConfiguration(inactiveSaml2AttributeQueryProfileConfiguration())
            .getValue()
            .updateSaml2EcpProfileConfiguration(inactiveSaml2EcpProfileConfiguration())
            .getValue()
            .updateSaml2SsoProfileConfiguration(inactiveSaml2SsoProfileConfiguration())
            .getValue()
            .updateSaml2LogoutProfileConfiguration(inactiveSaml2LogoutProfileConfiguration())
            .getValue();
    }

    public static final TrustRelationship sampleInactiveIndividualTrustRelationshipWithNoActiveProfileConfiguration() {

        return withNoActiveProfileConfiguration(sampleInactiveIndividualTrustRelationship());
    }

    public static final TrustRelationship sampleInactiveAggregateTrustRelationshipWithNoActiveProfileConfiguration() {

        return withNoActiveProfileConfiguration(sampleInactiveAggregateTrustRelationship());
    }

    public static final MetadataSource sampleFileMetadataSource() {

        return FileMetadataSource.of("/opt/gluu/metadata/sp_metadata.xml").getValue();
    }

    public static final MetadataSource sampleUriMetadataSource() {

        return UriMetadataSource.of(URI.create("https://saml.gluu.org/downloads/sp_metadata.xml")).getValue();
    }

    public static final MetadataSource sampleUpstreamMetadatSource() {

        URI entity_id = URI.create("upstreamMetadataSource");
        return UpstreamMetadataSource.of(Id.generate(),EntityId.of(entity_id).getValue()).getValue();
    }

    public static final MetadataSource sampleManualMetadataSource() {

        final String acs_url = "https://saml.gluu.org/sp/login/complete";
        final String entity_id = "https://saml.gluu.org/sp";
        AssertionConsumerService acs = AssertionConsumerService.of(URI.create(acs_url),SamlBinding.HTTP_POST).getValue();
        return ManualMetadataSource
            .withNoSigningCertificate()
            .entityId(EntityId.of(URI.create(entity_id)).getValue())
            .validUntil(ValidityPeriod.daysFromNow(20))
            .assertionConsumerService(acs)
            .build()
            .getValue();
    }

    public static final MetadataSource sampleMdqMetadataSource() {

        final String mdq_base_url = "https://saml.gluu.org/mdq/";
        return MdqMetadataSource.of(URI.create(mdq_base_url)).getValue();
    }

    public static final ShibbolethSsoProfileConfiguration activeShibbolethSsoProfileConfiguration() {

        return ShibbolethSsoProfileConfiguration
            .from(SamlProfileConfigurationDefaults.shibbolethSso())
            .status(ProfileStatus.ACTIVE)
            .build()
            .getValue();
    }

    public static final ShibbolethSsoProfileConfiguration inactiveShibbolethSsoProfileConfiguration() {

        return ShibbolethSsoProfileConfiguration
            .from(SamlProfileConfigurationDefaults.shibbolethSso())
            .status(ProfileStatus.INACTIVE)
            .build()
            .getValue();
    }

    public static final Saml2ArtifactResolutionProfileConfiguration activeSaml2ArtifactResolutionProfileConfiguration() {

        return Saml2ArtifactResolutionProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2ArtifactResolution())
            .status(ProfileStatus.ACTIVE)
            .build()
            .getValue();
    }

    public static final Saml2ArtifactResolutionProfileConfiguration inactiveSaml2ArtifactResolutionProfileConfiguration() {

        return Saml2ArtifactResolutionProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2ArtifactResolution())
            .status(ProfileStatus.INACTIVE)
            .build()
            .getValue();
    }

    public static final Saml2AttributeQueryProfileConfiguration activeSaml2AttributeQueryProfileConfiguration() {

        return Saml2AttributeQueryProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2AttributeQuery())
            .status(ProfileStatus.ACTIVE)
            .build()
            .getValue();
    }

    public static final Saml2AttributeQueryProfileConfiguration inactiveSaml2AttributeQueryProfileConfiguration() {


        return Saml2AttributeQueryProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2AttributeQuery())
            .status(ProfileStatus.INACTIVE)
            .build()
            .getValue();
    }

    public static final Saml2EcpProfileConfiguration activeSaml2EcpProfileConfiguration() {

        return Saml2EcpProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2Ecp())
            .status(ProfileStatus.ACTIVE)
            .build()
            .getValue();
    }

    public static final Saml2EcpProfileConfiguration inactiveSaml2EcpProfileConfiguration() {

        return Saml2EcpProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2Ecp())
            .status(ProfileStatus.INACTIVE)
            .build()
            .getValue();
    }

    public static final Saml2SsoProfileConfiguration activeSaml2SsoProfileConfiguration() {

        return Saml2SsoProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2Sso())
            .status(ProfileStatus.ACTIVE)
            .build()
            .getValue();
    }

    public static final Saml2SsoProfileConfiguration inactiveSaml2SsoProfileConfiguration() {

        return Saml2SsoProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2Sso())
            .status(ProfileStatus.INACTIVE)
            .build()
            .getValue();
    }

    public static final Saml2LogoutProfileConfiguration activeSaml2LogoutProfileConfiguration() {

        return Saml2LogoutProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2Logout())
            .status(ProfileStatus.ACTIVE)
            .build()
            .getValue();
    }

    public static final Saml2LogoutProfileConfiguration inactiveSaml2LogoutProfileConfiguration() {

        return Saml2LogoutProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2Logout())
            .status(ProfileStatus.INACTIVE)
            .build()
            .getValue();
    }


    public static final ReleasedAttributes sampleReleasedAttributes() {

        return ReleasedAttributes
            .builder()
            .add(ReleasedAttribute.of(Id.generate(),"foo").getValue())
            .add(ReleasedAttribute.of(Id.generate(),"bar").getValue())
            .build()
            .getValue();
    }

    public static final ActivationDiagnostics sampleActivationDiagnosticsForSuccessfulActivation() {

        ActivationLogEntry entry = ActivationLogEntry.of(Instant.now(),LogLevel.INFO,"Process completed").getValue();

        return ActivationDiagnostics.builder()
            .startedAt(Instant.now())
            .completedAt(Instant.now().plusSeconds(60))
            .origin(Origin.of("someinstance@some-host"))
            .logEntries(List.of(entry))
            .status(ActivationStatus.SUCCEEDED)
            .build()
            .getValue();
    }

    public static final ActivationDiagnostics sampleActivationDiagnosticsForFailedActivation() {

        ActivationLogEntry entry = ActivationLogEntry.of(Instant.now(),LogLevel.ERROR,"Process failed").getValue();

        return ActivationDiagnostics.builder()
            .startedAt(Instant.now())
            .completedAt(Instant.now().plusSeconds(60))
            .origin(Origin.of("troubledidpinstance@some-host"))
            .logEntries(List.of(entry))
            .status(ActivationStatus.FAILED)
            .build()
            .getValue();
    }


    public static final EntityIds sampleEntityIds() {

        Collection<EntityId> ids = new ArrayList<>();
        ids.add(EntityId.of(URI.create("https://www.google.com")).getValue());
        ids.add(EntityId.of(URI.create("https://gluu.org")).getValue());
        return EntityIds.builder()
            .addAll(ids)
            .build()
            .getValue();
    }
}
