package io.jans.shibboleth.trust.config;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.jans.shibboleth.trust.config.profile.ProfileConfigurationAccessor;
import io.jans.shibboleth.trust.config.profile.Saml2ArtifactResolutionProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.Saml2AttributeQueryProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.Saml2EcpProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.Saml2LogoutProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.Saml2SsoProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.SamlProfileConfigurationDefaults;
import io.jans.shibboleth.trust.config.profile.ShibbolethSsoProfileConfiguration;
import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.Description;
import io.jans.shibboleth.trust.config.DisplayName;
import io.jans.shibboleth.trust.config.EntityId;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.ReleasedAttribute;
import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustStatus;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationLogEntry;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationStatus;
import io.jans.shibboleth.trust.shared.diagnostics.LogLevel;
import io.jans.shibboleth.trust.shared.Origin;
import io.jans.shibboleth.trust.config.metadata.FileMetadataSource;
import io.jans.shibboleth.trust.config.metadata.ManualMetadataSource;
import io.jans.shibboleth.trust.config.metadata.MdqMetadataSource;
import io.jans.shibboleth.trust.config.metadata.MetadataSource;
import io.jans.shibboleth.trust.config.metadata.NoMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UpstreamMetadataSource;
import io.jans.shibboleth.trust.config.metadata.UriMetadataSource;
import io.jans.shibboleth.trust.config.metadata.manual.AssertionConsumerService;
import io.jans.shibboleth.trust.config.metadata.manual.SamlBinding;
import io.jans.shibboleth.trust.config.metadata.manual.ValidityPeriod;

import io.jans.shibboleth.trust.shared.Result;

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

    public static final TrustRelationship sampleDraftIndividualTrustRelationship(MetadataSource source) {

        return sampleDraftIndividualTrustRelationship()
            .updateMetadataSource(source)
            .getValue();
    }

    public static final TrustRelationship sampleDraftAggregateTrustRelationshipWithRealMetadataSource() {

        return sampleDraftAggregateTrustRelationship()
            .updateMetadataSource(sampleUriMetadataSource())
            .getValue();
    }

    public static final TrustRelationship sampleDraftAggregateTrustRelationship(MetadataSource source) {

        return sampleDraftAggregateTrustRelationship()
            .updateMetadataSource(source)
            .getValue();
    }

    public static final TrustRelationship sampleReadyIndividualTrustRelationship() {

        return sampleDraftIndividualTrustRelationshipWithActiveProfile()
            .updateMetadataSource(sampleFileMetadataSource())
            .getValue();
    }

    public static final TrustRelationship sampleReadyIndividualTrustRelationship(ProfileConfigurationAccessor accessor,Object profileconfig) {

        return accessor.update(sampleDraftIndividualTrustRelationshipWithRealMetadataSource(),profileconfig).getValue();
    }

    public static final TrustRelationship sampleReadyAggregateTrustRelationship(ProfileConfigurationAccessor accessor,Object profileconfig) {

        return accessor.update(sampleDraftAggregateTrustRelationshipWithRealMetadataSource(),profileconfig).getValue();
    }

    public static final TrustRelationship sampleReadyIndividualTrustRelationship(MetadataSource source) {

        return sampleDraftIndividualTrustRelationship(source)
            .updateSaml2SsoProfileConfiguration(activeSaml2SsoProfileConfiguration())
            .getValue();
    }

    public static final TrustRelationship sampleReadyAggregateTrustRelationship() {
 
        return sampleDraftAggregateTrustRelationshipWithActiveProfile()
            .updateMetadataSource(sampleMdqMetadataSource())
            .getValue();
    }

    public static final TrustRelationship sampleReadyAggregateTrustRelationship(MetadataSource source) {

        return sampleDraftAggregateTrustRelationship(source)
            .updateSaml2LogoutProfileConfiguration(activeSaml2LogoutProfileConfiguration())
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

    public static final TrustRelationship sampleActiveIndividualTrustRelationship(MetadataSource source) {

        return sampleDraftIndividualTrustRelationshipWithActiveProfile()
            .updateMetadataSource(source)
            .getValue()
            .activate()
            .getValue()
            .finalizeActivation(sampleActivationDiagnosticsForSuccessfulActivation())
            .getValue();
    }

    public static final TrustRelationship sampleActiveIndividualTrustRelationship(ProfileConfigurationAccessor accessor, Object profileconfig) {
        
        
        return accessor.update(sampleDraftIndividualTrustRelationshipWithRealMetadataSource(),profileconfig)
                .getValue()
                .activate()
                .getValue()
                .finalizeActivation(sampleActivationDiagnosticsForSuccessfulActivation())
                .getValue();
    }

    public static final TrustRelationship sampleActiveAggregateTrustRelationship(MetadataSource source) {

        return sampleDraftAggregateTrustRelationshipWithActiveProfile()
            .updateMetadataSource(source)
            .getValue()
            .activate()
            .getValue()
            .finalizeActivation(sampleActivationDiagnosticsForSuccessfulActivation())
            .getValue();
    }

    public static final TrustRelationship sampleActiveAggregateTrustRelationship(ProfileConfigurationAccessor accessor , Object profileconfig)  {
        
        return accessor.update(sampleDraftAggregateTrustRelationshipWithRealMetadataSource(), profileconfig)
            .getValue()
            .activate()
            .getValue()
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

    public static final Function<TrustRelationship,Result<TrustRelationship>> displayNameIdempotentUpdate() {

        return (tr) -> {
            DisplayName displayname = DisplayName.of(tr.getDisplayName().getValue()).getValue();
            return tr.updateDisplayName(displayname);
        };
    }

    public static final Function<TrustRelationship,Result<TrustRelationship>> descriptionIdempotentUpdate() {

        return (tr) -> {
            Description description = Description.of(tr.getDescription().getValue());
            return tr.updateDescription(description);
        };
    }

    public static final Function<TrustRelationship,Result<TrustRelationship>> metadataSourceIdempotentUpdate() {

        return (tr) -> {

            switch(tr.getMetadataSource().getType()) {

                case FILE:
                    FileMetadataSource filesource = (FileMetadataSource) tr.getMetadataSource();
                    return tr.updateMetadataSource(FileMetadataSource.of(filesource.getFilePath()).getValue());
                case URI:
                    UriMetadataSource urisource = (UriMetadataSource) tr.getMetadataSource();
                    return tr.updateMetadataSource(UriMetadataSource.of(urisource.getUri()).getValue());
                case UPSTREAM:
                    UpstreamMetadataSource upstreamsource = (UpstreamMetadataSource) tr.getMetadataSource();
                    return tr.updateMetadataSource(UpstreamMetadataSource.of(upstreamsource.getParentId(),upstreamsource.getEntityId()).getValue());
                case MANUAL:
                    ManualMetadataSource manualsource = (ManualMetadataSource) tr.getMetadataSource();
                    return tr.updateMetadataSource(ManualMetadataSource.from(manualsource).build().getValue());
                case MDQ:
                    MdqMetadataSource mdqsource = (MdqMetadataSource) tr.getMetadataSource();
                    return tr.updateMetadataSource(MdqMetadataSource.of(mdqsource.getBaseUrl()).getValue());
                case NONE:
                    return tr.updateMetadataSource(NoMetadataSource.getInstance());
                default:
                    return null;
            }
        };
    }

    public static final Function<TrustRelationship,Result<TrustRelationship>> shibbolethSsoIdempotentUpdate() {

        return (tr) -> {

            ShibbolethSsoProfileConfiguration duplicate = ShibbolethSsoProfileConfiguration
                .from(tr.getShibbolethSsoProfileConfiguration())
                .build()
                .getValue();
            return tr.updateShibbolethSsoProfileConfiguration(duplicate);
        };
    }

    public static final Function<TrustRelationship,Result<TrustRelationship>> saml2ArtifactResolutionIdempotentUpdate() {

        return (tr) -> {

            Saml2ArtifactResolutionProfileConfiguration duplicate = Saml2ArtifactResolutionProfileConfiguration
                .from(tr.getSaml2ArtifactResolutionProfileConfiguration())
                .build()
                .getValue();
        
            return tr.updateSaml2ArtifactResolutionProfileConfiguration(duplicate);
        };
    }

    public static final Function<TrustRelationship,Result<TrustRelationship>> saml2AttributeQueryIdempotentUpdate() {

        return (tr) -> {

            Saml2AttributeQueryProfileConfiguration duplicate = Saml2AttributeQueryProfileConfiguration
            .from(tr.getSaml2AttributeQueryProfileConfiguration())
            .build()
            .getValue();
        
            return tr.updateSaml2AttributeQueryProfileConfiguration(duplicate);
        };
    }

    public static final Function<TrustRelationship,Result<TrustRelationship>> saml2EcpIdempotentUpdate() {

        return (tr) -> {

            Saml2EcpProfileConfiguration duplicate = Saml2EcpProfileConfiguration
                .from(tr.getSaml2EcpProfileConfiguration())
                .build()
                .getValue();
        
            return tr.updateSaml2EcpProfileConfiguration(duplicate);
        };
    }

    public static final Function<TrustRelationship,Result<TrustRelationship>> saml2SsoIdempotentUpdate() {

        return (tr) -> {

            Saml2SsoProfileConfiguration duplicate = Saml2SsoProfileConfiguration
                .from(tr.getSaml2SsoProfileConfiguration())
                .build()
                .getValue();

            return tr.updateSaml2SsoProfileConfiguration(duplicate);
        };
    }

    public static final Function<TrustRelationship,Result<TrustRelationship>> saml2LogoutIdempotentUpdate() {

        return (tr) -> {

            Saml2LogoutProfileConfiguration duplicate = Saml2LogoutProfileConfiguration
                .from(tr.getSaml2LogoutProfileConfiguration())
                .build()
                .getValue();
        
            return tr.updateSaml2LogoutProfileConfiguration(duplicate);
        };
    }

    public static final Function<TrustRelationship,Result<TrustRelationship>> releasedAttributesIdempotentUpdate() {

        return (tr) -> {

            ReleasedAttributes duplicate = ReleasedAttributes.from(tr.getReleasedAttributes()).build().getValue();
            return tr.updateReleasedAttributes(duplicate);
        };
    }
}
