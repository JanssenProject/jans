package io.jans.shibboleth.model.util;

import io.jans.shibboleth.model.core.Id;
import io.jans.shibboleth.model.core.DisplayName;
import io.jans.shibboleth.model.core.Description;
import io.jans.shibboleth.model.core.TrustNature;
import io.jans.shibboleth.model.core.Version;
import io.jans.shibboleth.model.core.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.model.core.TrustStatus;
import io.jans.shibboleth.model.metadata.MetadataSource;
import io.jans.shibboleth.model.EntityIds;
import io.jans.shibboleth.model.config.profiles.ShibbolethSsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2ArtifactResolutionProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2AttributeQueryProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2EcpProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2SsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2LogoutProfileConfiguration;

import io.jans.shibboleth.model.TrustRelationship;
import io.jans.shibboleth.model.ReleasedAttributes;

public class BuildContext {
    
    private final TrustRelationship original;

    private final Id id;
    private final DisplayName displayName;
    private final Description description;
    private final TrustNature nature;
    private final Version version;
    private final TrustStatus status;
    private final MetadataSource metadataSource;
    private final EntityIds discoveredEntityIds;
    private final ShibbolethSsoProfileConfiguration shibbolethSsoProfileConfiguration;
    private final Saml2ArtifactResolutionProfileConfiguration saml2ArtifactResolutionProfileConfiguration;
    private final Saml2AttributeQueryProfileConfiguration saml2AttributeQueryProfileConfiguration;
    private final Saml2EcpProfileConfiguration saml2EcpProfileConfiguration;
    private final Saml2SsoProfileConfiguration saml2SsoProfileConfiguration;
    private final Saml2LogoutProfileConfiguration saml2LogoutProfileConfiguration;

    private final OperationType operationType;
    
    private final ReleasedAttributes releasedAttributes;

    private final ActivationDiagnostics activationDiagnostics;

    public BuildContext(TrustRelationship original,
        Id id, DisplayName displayName, Description description,
        TrustNature nature, Version version, TrustStatus status, MetadataSource metadataSource,
        EntityIds discoveredEntityIds, ShibbolethSsoProfileConfiguration shibbolethSsoProfileConfiguration,
        Saml2ArtifactResolutionProfileConfiguration saml2ArtifactResolutionProfileConfiguration, 
        Saml2AttributeQueryProfileConfiguration saml2AttributeQueryProfileConfiguration,
        Saml2EcpProfileConfiguration saml2EcpProfileConfiguration, Saml2SsoProfileConfiguration saml2SsoProfileConfiguration,
        Saml2LogoutProfileConfiguration saml2LogoutProfileConfiguration, 
        ReleasedAttributes releasedAttributes, ActivationDiagnostics activationDiagnostics, OperationType operationType) {
        
        this.original = original;
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.nature = nature;
        this.version = version;
        this.status = status;
        this.metadataSource = metadataSource;
        this.discoveredEntityIds = discoveredEntityIds;
        this.shibbolethSsoProfileConfiguration = shibbolethSsoProfileConfiguration;
        this.saml2ArtifactResolutionProfileConfiguration = saml2ArtifactResolutionProfileConfiguration;
        this.saml2AttributeQueryProfileConfiguration = saml2AttributeQueryProfileConfiguration;
        this.saml2EcpProfileConfiguration = saml2EcpProfileConfiguration;
        this.saml2SsoProfileConfiguration = saml2SsoProfileConfiguration;
        this.saml2LogoutProfileConfiguration = saml2LogoutProfileConfiguration;
        this.releasedAttributes = releasedAttributes;
        this.activationDiagnostics = activationDiagnostics;
        this.operationType = operationType != null ? operationType : OperationType.NONE;
    }

    public TrustRelationship getOriginal() {

        return original;
    }

    public Id getId() {

        return id;
    }

    public DisplayName getDisplayName() {

        return displayName;
    }

    public Description getDescription() {

        return description;
    }

    public TrustNature getNature() {

        return nature;
    }

    public Version getVersion() {

        return version;
    }

    public TrustStatus getStatus() {

        return status;
    }

    public MetadataSource getMetadataSource() {

        return metadataSource;
    }

    public boolean updateMetadataSourceCalled() {

        return operationType == OperationType.UPDATE_METADATA_SOURCE;
    }

    public EntityIds getDiscoveredEntityIds() {

        return discoveredEntityIds;
    }

    public ShibbolethSsoProfileConfiguration getShibbolethSsoProfileConfiguration() {

        return shibbolethSsoProfileConfiguration;
    }

    public Saml2ArtifactResolutionProfileConfiguration getSaml2ArtifactResolutionProfileConfiguration() {

        return saml2ArtifactResolutionProfileConfiguration;
    }

    public Saml2AttributeQueryProfileConfiguration getSaml2AttributeQueryProfileConfiguration() {

        return saml2AttributeQueryProfileConfiguration;
    }

    public Saml2EcpProfileConfiguration getSaml2EcpProfileConfiguration() {

        return saml2EcpProfileConfiguration;
    }

    public Saml2SsoProfileConfiguration getSaml2SsoProfileConfiguration() {

        return saml2SsoProfileConfiguration;
    }

    public Saml2LogoutProfileConfiguration getSaml2LogoutProfileConfiguration() {

        return saml2LogoutProfileConfiguration;
    }

    public boolean updateProfileConfigurationCalled() {

        return operationType == OperationType.UPDATE_SHIBBOLETH_SSO_PROFILE_CONFIGURATION
            || operationType == OperationType.UPDATE_SAML2_ARTIFACT_RESOLUTION_PROFILE_CONFIGURATION
            || operationType == OperationType.UPDATE_SAML2_ATTRIBUTE_QUERY_PROFILE_CONFIGURATION
            || operationType == OperationType.UPDATE_SAML2_ECP_PROFILE_CONFIGURATION
            || operationType == OperationType.UPDATE_SAML2_SSO_PROFILE_CONFIGURATION
            || operationType == OperationType.UPDATE_SAML2_LOGOUT_PROFILE_CONFIGURATION;
    }

    public ReleasedAttributes getReleasedAttributes() {

        return releasedAttributes;
    }

    public ActivationDiagnostics getActivationDiagnostics() {

        return activationDiagnostics;
    }

    public boolean hasRealMetadataSource() {

        return TrustPredicates.hasRealMetadataSource(this);
    }

    public boolean hasNoRealMetadataSource() {

        return !hasRealMetadataSource();
    }

    public boolean hasAnyActiveProfileConfiguration() {

        return TrustPredicates.hasAnyActiveProfile(this);
    }

    public boolean hasNoActiveProfileConfiguration() {

        return !hasAnyActiveProfileConfiguration();
    }

    public boolean activateCalled() {

        return operationType == OperationType.ACTIVATE;
    }

    public boolean cancelActivationCalled() {

        return operationType == OperationType.CANCEL_ACTIVATION;
    }

    public boolean deactivateCalled() {

        return operationType == OperationType.DEACTIVATE;
    }

    public boolean finalizeActivationCalled() {

        return operationType == OperationType.FINALIZE_ACTIVATION;
    }

    public boolean incorporateDiscoveredEntityIdsCalled() {

        return operationType == OperationType.INCORPORATE_DISCOVERED_ENTITY_IDS;
    }

    public boolean hasSuccessfulActivationDiagnostics() {

        return TrustPredicates.hasSuccessfulActivationDiagnostics(this);
    }

    public boolean hasFailedActivationDiagnostics() {

        return TrustPredicates.hasFailedActivationDiagnostics(this);
    }

    public boolean isAggregateNature() {

        return TrustPredicates.hasTrustNature(this,TrustNature.AGGREGATE);
    }

    public boolean isIndividualNature() {

        return TrustPredicates.hasTrustNature(this,TrustNature.INDIVIDUAL);
    }

    public boolean isDraftStatus() {

        return status == TrustStatus.DRAFT;
    }

    public boolean isReadyStatus() {

        return status == TrustStatus.READY;
    }

    public boolean isActivatingStatus() {

        return status == TrustStatus.ACTIVATING;
    }

    public boolean isActiveStatus() {

        return status == TrustStatus.ACTIVE;
    }

    public boolean isInactiveStatus() {

        return status == TrustStatus.INACTIVE;
    }

    public boolean hasMetadataSourceChanged() {

        if (original == null) return false;

        return !original.getMetadataSource().equals(metadataSource);
    }

    public boolean hasNoProfileConfigurationChanged() {

        if (original == null) return true;

        return original.getShibbolethSsoProfileConfiguration().equals(shibbolethSsoProfileConfiguration)
            && original.getSaml2ArtifactResolutionProfileConfiguration().equals(saml2ArtifactResolutionProfileConfiguration)
            && original.getSaml2AttributeQueryProfileConfiguration().equals(saml2AttributeQueryProfileConfiguration)
            && original.getSaml2EcpProfileConfiguration().equals(saml2EcpProfileConfiguration)
            && original.getSaml2SsoProfileConfiguration().equals(saml2SsoProfileConfiguration)
            && original.getSaml2LogoutProfileConfiguration().equals(saml2LogoutProfileConfiguration);
    }

    public boolean hasAnyProfileConfigurationChanged() {

        return !hasNoProfileConfigurationChanged();
    }
}
