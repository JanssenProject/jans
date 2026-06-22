package io.jans.shibboleth.model;

import java.net.URI;
import java.time.Instant;
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

        return TrustRelationship.from(sampleDraftIndividualTrustRelationship())
            .withShibbolethSsoProfileConfiguration(activeShibbolethSsoProfileConfiguration())
            .build()
            .getValue();
    }

    
    public static final TrustRelationship sampleDraftAggregateTrustRelationshipWithActiveProfile() {

        return TrustRelationship.from(sampleDraftAggregateTrustRelationship())
            .withSaml2SsoProfileConfiguration(activeSaml2SsoProfileConfiguration())
            .build()
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

        return TrustRelationship.from(sampleDraftIndividualTrustRelationshipWithActiveProfile())
            .withMetadataSource(sampleFileMetadataSource())
            .build()
            .getValue();
    }


    public static final TrustRelationship sampleReadyAggregateTrustRelationship() {

        return TrustRelationship.from(sampleDraftAggregateTrustRelationshipWithActiveProfile())
            .withMetadataSource(sampleMdqMetadataSource())
            .build()
            .getValue();
    }

    public static final TrustRelationship sampleActivatingIndividualTrustRelationship() {

        return TrustRelationship.from(sampleReadyIndividualTrustRelationship())
            .activateCalled()
            .build()
            .getValue();
    }

    public static final TrustRelationship sampleActivatingAggregateTrustRelationship() {

        return TrustRelationship.from(sampleReadyAggregateTrustRelationship())
            .activateCalled()
            .build()
            .getValue();
    }

    public static final TrustRelationship sampleActiveIndividualTrustRelationship() {

        return TrustRelationship.from(sampleActivatingIndividualTrustRelationship())
            .finalizeActivationCalled()
            .withActivationDiagnostics(sampleActivationDiagnosticsForSuccessfulActivation())
            .build()
            .getValue();
    }

    public static final TrustRelationship sampleActiveAggregateTrustRelationship() {

        return TrustRelationship.from(sampleActivatingAggregateTrustRelationship())
            .finalizeActivationCalled()
            .withActivationDiagnostics(sampleActivationDiagnosticsForSuccessfulActivation())
            .build()
            .getValue();
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

    public static final Saml2ArtifactResolutionProfileConfiguration activeSaml2ArtifactResolutionProfileConfiguration() {

        return Saml2ArtifactResolutionProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2ArtifactResolution())
            .status(ProfileStatus.ACTIVE)
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

    public static final Saml2EcpProfileConfiguration activeSaml2EcpProfileConfiguration() {

        return Saml2EcpProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2Ecp())
            .status(ProfileStatus.ACTIVE)
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

    public static final Saml2LogoutProfileConfiguration activeSaml2LogoutProfileConfiguration() {

        return Saml2LogoutProfileConfiguration
            .from(SamlProfileConfigurationDefaults.saml2Logout())
            .status(ProfileStatus.ACTIVE)
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
}
