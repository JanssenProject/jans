package io.jans.shibboleth.model;

import io.jans.shibboleth.model.config.profiles.Saml2ArtifactResolutionProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2AttributeQueryProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2EcpProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2LogoutProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2SsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.SamlProfileConfigurationDefaults;
import io.jans.shibboleth.model.config.profiles.ShibbolethSsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.capabilities.CommonConfigurationCapable;
import io.jans.shibboleth.model.config.profiles.common.AssertionEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.AssertionSigningPolicy;
import io.jans.shibboleth.model.config.profiles.common.AssertionTimeCondition;
import io.jans.shibboleth.model.config.profiles.common.AttributeEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.AttributeStatementPolicy;
import io.jans.shibboleth.model.config.profiles.common.AuthenticationResultReusePolicy;
import io.jans.shibboleth.model.config.profiles.common.EncryptionFallbackPolicy;
import io.jans.shibboleth.model.config.profiles.common.EndpointValidationPolicy;
import io.jans.shibboleth.model.config.profiles.common.FriendlyNameRandomizationPolicy;
import io.jans.shibboleth.model.config.profiles.common.InterceptorFlows;
import io.jans.shibboleth.model.config.profiles.common.MessageSigningPolicy;
import io.jans.shibboleth.model.config.profiles.common.NameIdEncryptionPolicy;
import io.jans.shibboleth.model.config.profiles.common.NameIdentifiers;
import io.jans.shibboleth.model.config.profiles.common.ProfileStatus;
import io.jans.shibboleth.model.config.profiles.common.ProfileType;
import io.jans.shibboleth.model.config.profiles.common.RequestSignatureValidationPolicy;
import io.jans.shibboleth.model.config.profiles.common.RequestSigningRequirement;
import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.core.diagnostics.ActivationDiagnostics;
import io.jans.shibboleth.model.core.diagnostics.ActivationStatus;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.metadata.*;
import io.jans.shibboleth.model.rules.consistency.TrustConsistencyRules;
import io.jans.shibboleth.model.rules.state.TrustTransitionRules;
import io.jans.shibboleth.model.util.BuildContext;
import io.jans.shibboleth.model.util.OperationType;
import io.jans.shibboleth.model.util.TrustPredicates;
import io.jans.shibboleth.model.util.TrustResult;

import java.time.Duration;
import java.util.Objects;


public class TrustRelationship {

    private static final int INITIAL_VERSION = 1;

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

    private final ReleasedAttributes releasedAttributes;

    private final ActivationDiagnostics activationDiagnostics;

    private TrustRelationship(Id id, DisplayName displayName, Description description, 
        TrustNature nature, Version version, TrustStatus status, MetadataSource metadataSource, EntityIds discoveredEntityIds,
        ShibbolethSsoProfileConfiguration shibbolethSsoProfileConfiguration,
        Saml2ArtifactResolutionProfileConfiguration saml2ArtifactResolutionProfileConfiguration,
        Saml2AttributeQueryProfileConfiguration saml2AttributeQueryProfileConfiguration,
        Saml2EcpProfileConfiguration saml2EcpProfileConfiguration,
        Saml2SsoProfileConfiguration saml2SsoProfileConfiguration,
        Saml2LogoutProfileConfiguration saml2LogoutProfileConfiguration,
        ReleasedAttributes releasedAttributes,
        ActivationDiagnostics activationDiagnostics) {
        
        
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.nature = nature;

        this.version = version;
        this.status  = status;
        this.metadataSource = metadataSource;
        this.discoveredEntityIds =  discoveredEntityIds;

        this.shibbolethSsoProfileConfiguration = shibbolethSsoProfileConfiguration;
        this.saml2ArtifactResolutionProfileConfiguration = saml2ArtifactResolutionProfileConfiguration;
        this.saml2AttributeQueryProfileConfiguration = saml2AttributeQueryProfileConfiguration; 
        this.saml2EcpProfileConfiguration = saml2EcpProfileConfiguration;
        this.saml2SsoProfileConfiguration = saml2SsoProfileConfiguration;
        this.saml2LogoutProfileConfiguration = saml2LogoutProfileConfiguration;

        this.releasedAttributes = releasedAttributes;

        this.activationDiagnostics = activationDiagnostics;
    }

    public Id getId() {

        return id;
    }

    public DisplayName getDisplayName() {

        return displayName;
    }

    public TrustResult<TrustRelationship> updateDisplayName(DisplayName newDisplayName) {

        return from(this)
            .withDisplayName(newDisplayName)
            .build();
    } 

    public Description getDescription() {

        return description;
    }

    public TrustResult<TrustRelationship> updateDescription(Description newDescription) {

        return from(this)
            .withDescription(newDescription)
            .build();
    }

    public TrustNature getNature() {

        return nature;
    }

    public boolean isAggregateNature() {

        return TrustPredicates.hasTrustNature(this,TrustNature.AGGREGATE);
    }

    public boolean isIndividualNature() {

        return TrustPredicates.hasTrustNature(this,TrustNature.INDIVIDUAL);
    }


    public TrustStatus getStatus() {

        return status;
    }

    public Version getVersion() {

        return version;
    }

    public MetadataSource getMetadataSource() {

        return metadataSource;
    }


    public TrustResult<TrustRelationship> updateMetadataSource(MetadataSource source) {

        return from(this)
            .updateMetadataSourceCalled(source)
            .build();
    }

    public EntityIds getDiscoveredEntityIds() {

        return discoveredEntityIds;
    }

    public TrustResult<TrustRelationship> incorporateDiscoveredEntityIds(EntityIds discoveredEntityIds) {

        return from(this)
            .incorporateDiscoveredEntityIdsCalled(discoveredEntityIds)
            .build();
    }

    public boolean hasNoRealMetadataSource() {

        return !TrustPredicates.hasRealMetadataSource(this);
    }

    public boolean hasAnyDiscoveredEntityIds() {

        return TrustPredicates.hasAnyDiscoveredEntityIds(this);
    }

    public boolean hasNoDiscoveredEntityIds() {

        return !hasAnyDiscoveredEntityIds();
    }

    public ShibbolethSsoProfileConfiguration getShibbolethSsoProfileConfiguration() {

        return shibbolethSsoProfileConfiguration;
    }

    public Saml2AttributeQueryProfileConfiguration getSaml2AttributeQueryProfileConfiguration() {

        return saml2AttributeQueryProfileConfiguration;
    }

    public Saml2ArtifactResolutionProfileConfiguration getSaml2ArtifactResolutionProfileConfiguration() {

        return saml2ArtifactResolutionProfileConfiguration;
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

    public TrustResult<TrustRelationship> updateShibbolethSsoProfileConfiguration(ShibbolethSsoProfileConfiguration profileconfig) {

        return from(this)
            .updateShibbolethSsoProfileConfigurationCalled(profileconfig)
            .build();
    }

    public TrustResult<TrustRelationship> updateSaml2AttributeQueryProfileConfiguration(Saml2AttributeQueryProfileConfiguration profileconfig) {

        return from(this)
            .updateSaml2AttributeQueryProfileConfigurationCalled(profileconfig)
            .build();
    }

    public TrustResult<TrustRelationship> updateSaml2ArtifactResolutionProfileConfiguration(Saml2ArtifactResolutionProfileConfiguration profileconfig) {

        return from(this)
            .updateSaml2ArtifactResolutionProfileConfigurationCalled(profileconfig)
            .build();
    }

    public TrustResult<TrustRelationship> updateSaml2EcpProfileConfiguration(Saml2EcpProfileConfiguration profileconfig) {

        return from(this)
            .updateSaml2EcpProfileConfigurationCalled(profileconfig)
            .build();
    }

    public TrustResult<TrustRelationship> updateSaml2SsoProfileConfiguration(Saml2SsoProfileConfiguration profileconfig) {

        return from(this)
            .updateSaml2SsoProfileConfigurationCalled(profileconfig)
            .build();

    }

    public TrustResult<TrustRelationship> updateSaml2LogoutProfileConfiguration(Saml2LogoutProfileConfiguration profileconfig) {

        return from(this)
            .updateSaml2LogoutProfileConfigurationCalled(profileconfig)
            .build();
    }
    

    public boolean hasNoActiveProfileConfiguration() {

       return !TrustPredicates.hasAnyActiveProfile(this);
    }

    public boolean hasNoReleasedAttributes() {

        return TrustPredicates.hasNoReleasedAttributes(this);
    }

    public TrustResult<TrustRelationship> updateReleasedAttributes(ReleasedAttributes attributes) {

        return from(this)
            .withReleasedAttributes(attributes)
            .build();
    }

    public ReleasedAttributes getReleasedAttributes() {

        return releasedAttributes;
    }

    public ActivationDiagnostics getActivationDiagnostics() {

        return activationDiagnostics;
    }

    public boolean hasNoActivationDiagnostics() {

        return TrustPredicates.hasNoActivationDiagnostics(this);
    }

    public boolean hasSuccessfulActivationDiagnostics() {

        return TrustPredicates.hasSuccessfulActivationDiagnostics(this);
    }

    public boolean hasFailedActivationDiagnostics() {

        return TrustPredicates.hasFailedActivationDiagnostics(this);
    }

    public TrustResult<TrustRelationship> activate() {

        return from(this)
            .activateCalled()
            .build();
    }

    public TrustResult<TrustRelationship> cancelActivation() {

        return from(this)
            .cancelActivationCalled()
            .build();
    }

    public TrustResult<TrustRelationship> deactivate() {

        return from(this)
            .deactivateCalled()
            .build();
    }

    public TrustResult<TrustRelationship> finalizeActivation(ActivationDiagnostics activationDiagnostics) {

        return from(this)
            .finalizeActivationCalled(activationDiagnostics)
            .build();
    }

 
    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        TrustRelationship other = (TrustRelationship) o;
        return Objects.equals(id, other.id)
            && Objects.equals(displayName,other.displayName)
            && Objects.equals(description,other.description)
            && Objects.equals(nature,other.nature)
            && Objects.equals(version,other.version)
            && Objects.equals(status,other.status)
            && Objects.equals(metadataSource,other.metadataSource)
            && Objects.equals(discoveredEntityIds,other.discoveredEntityIds)
            && Objects.equals(shibbolethSsoProfileConfiguration,other.shibbolethSsoProfileConfiguration)
            && Objects.equals(saml2ArtifactResolutionProfileConfiguration,other.saml2ArtifactResolutionProfileConfiguration)
            && Objects.equals(saml2AttributeQueryProfileConfiguration,other.saml2AttributeQueryProfileConfiguration)
            && Objects.equals(saml2EcpProfileConfiguration,other.saml2EcpProfileConfiguration)
            && Objects.equals(saml2SsoProfileConfiguration,other.saml2SsoProfileConfiguration)
            && Objects.equals(saml2LogoutProfileConfiguration,other.saml2LogoutProfileConfiguration)
            && Objects.equals(releasedAttributes,other.releasedAttributes)
            && Objects.equals(activationDiagnostics,other.activationDiagnostics);
    }

    @Override
    public int hashCode() {

        return Objects.hash(
            id,displayName,description,nature,
            version,status,metadataSource,discoveredEntityIds,
            shibbolethSsoProfileConfiguration,
            saml2ArtifactResolutionProfileConfiguration,
            saml2AttributeQueryProfileConfiguration,
            saml2EcpProfileConfiguration,
            saml2SsoProfileConfiguration,
            saml2LogoutProfileConfiguration,
            releasedAttributes,
            activationDiagnostics
        );
    }

    private TrustRelationship withIncrementedVersion() {

        return new TrustRelationship(
            id, 
            displayName, 
            description, 
            nature, 
            version.next(), 
            status, 
            metadataSource, 
            discoveredEntityIds, 
            shibbolethSsoProfileConfiguration, 
            saml2ArtifactResolutionProfileConfiguration, 
            saml2AttributeQueryProfileConfiguration, 
            saml2EcpProfileConfiguration, 
            saml2SsoProfileConfiguration, 
            saml2LogoutProfileConfiguration,
            releasedAttributes,
            activationDiagnostics
        );
    }

    public static TrustResult<TrustRelationship> create (DisplayName displayName,Description description,TrustNature nature ) {
    
        return builder()
            .withId(Id.unassigned())
            .withDisplayName(displayName)
            .withDescription(description)
            .withNature(nature)
            .withVersion(Version.initial())
            .withStatus(TrustStatus.DRAFT)
            .withMetadataSource(new NoMetadataSource())
            .withDiscoveredEntityIds(EntityIds.empty())
            .withShibbolethSsoProfileConfiguration(SamlProfileConfigurationDefaults.shibbolethSso())
            .withSaml2ArtifactResolutionProfileConfiguration(SamlProfileConfigurationDefaults.saml2ArtifactResolution())
            .withSaml2AttributeQueryProfileConfiguration(SamlProfileConfigurationDefaults.saml2AttributeQuery())
            .withSaml2EcpProfileConfiguration(SamlProfileConfigurationDefaults.saml2Ecp())
            .withSaml2SsoProfileConfiguration(SamlProfileConfigurationDefaults.saml2Sso())
            .withSaml2LogoutProfileConfiguration(SamlProfileConfigurationDefaults.saml2Logout())
            .withReleasedAttributes(ReleasedAttributes.empty())
            .withActivationDiagnostics(ActivationDiagnostics.none())
            .build();
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static Builder from(TrustRelationship original) {

        return new Builder(original);
    }

    public static class Builder {

        private final TrustRelationship original; //nullable, only set when using from()

        private Id id;
        private DisplayName displayName;
        private Description description;
        private TrustNature nature;
         
        private Version version;
        private TrustStatus status;
        private MetadataSource metadataSource;
         
        private EntityIds discoveredEntityIds;
        private ShibbolethSsoProfileConfiguration shibbolethSsoProfileConfiguration;
        private Saml2ArtifactResolutionProfileConfiguration saml2ArtifactResolutionProfileConfiguration;
        private Saml2AttributeQueryProfileConfiguration saml2AttributeQueryProfileConfiguration;
        private Saml2EcpProfileConfiguration saml2EcpProfileConfiguration;
        private Saml2SsoProfileConfiguration saml2SsoProfileConfiguration;
        private Saml2LogoutProfileConfiguration saml2LogoutProfileConfiguration;

        private ReleasedAttributes releasedAttributes;

        private ActivationDiagnostics activationDiagnostics;
        
        private OperationType operationType;

        private Builder (TrustRelationship original) {

            this.original = original;
            if (original != null) {

                id = original.id; 
                displayName = original.displayName;
                description = original.description;
                nature = original.nature;
                
                version = original.version;
                status  = original.status;
                metadataSource = original.metadataSource;

                discoveredEntityIds = original.discoveredEntityIds;
                shibbolethSsoProfileConfiguration = original.shibbolethSsoProfileConfiguration;
                saml2ArtifactResolutionProfileConfiguration = original.saml2ArtifactResolutionProfileConfiguration;
                saml2AttributeQueryProfileConfiguration = original.saml2AttributeQueryProfileConfiguration;
                saml2EcpProfileConfiguration = original.saml2EcpProfileConfiguration;
                saml2SsoProfileConfiguration = original.saml2SsoProfileConfiguration;
                saml2LogoutProfileConfiguration = original.saml2LogoutProfileConfiguration;

                releasedAttributes = original.releasedAttributes;

                activationDiagnostics = original.activationDiagnostics;
            }

            this.operationType = OperationType.NONE;
        }

        public Builder withId(Id id) {

            this.id = id;
            return this;
        }

        public Builder withDisplayName(DisplayName displayName) {

            this.displayName = displayName;
            return this;
        }

        public Builder withDescription(Description description) {

            this.description = description;
            return this;
        }

        public Builder withNature(TrustNature nature) {

            this.nature = nature;
            return this;
        }

        public Builder withVersion(Version version) {

            this.version = version;
            return this;
        }

        public Builder withStatus(TrustStatus status) {

            this.status = status;
            return this;
        }

        public Builder withMetadataSource(MetadataSource metadataSource) {

            this.metadataSource = metadataSource;
            return this;
        }

        private Builder updateMetadataSourceCalled(MetadataSource metadataSource) {

            this.operationType = OperationType.UPDATE_METADATA_SOURCE;
            this.metadataSource = metadataSource;
            return this;
        }

        public Builder withDiscoveredEntityIds(EntityIds discoveredEntityIds) {

            this.discoveredEntityIds = discoveredEntityIds;
            return this;
        }

        public Builder withShibbolethSsoProfileConfiguration(ShibbolethSsoProfileConfiguration profileconfig) {

            this.shibbolethSsoProfileConfiguration = profileconfig;
            return this;
        }

        private Builder updateShibbolethSsoProfileConfigurationCalled(ShibbolethSsoProfileConfiguration profileconfig) {

            this.operationType = OperationType.UPDATE_SHIBBOLETH_SSO_PROFILE_CONFIGURATION;
            this.shibbolethSsoProfileConfiguration = profileconfig;
            return this;
        }

        public Builder withSaml2ArtifactResolutionProfileConfiguration(Saml2ArtifactResolutionProfileConfiguration profileconfig) {

            this.saml2ArtifactResolutionProfileConfiguration = profileconfig;
            return this;
        }

        private Builder updateSaml2ArtifactResolutionProfileConfigurationCalled(Saml2ArtifactResolutionProfileConfiguration profileconfig) {

            this.operationType = OperationType.UPDATE_SAML2_ARTIFACT_RESOLUTION_PROFILE_CONFIGURATION;
            this.saml2ArtifactResolutionProfileConfiguration = profileconfig;
            return this;
        }

        public Builder withSaml2AttributeQueryProfileConfiguration(Saml2AttributeQueryProfileConfiguration profileconfig) {

            this.saml2AttributeQueryProfileConfiguration = profileconfig;
            return this;
        }

        private Builder updateSaml2AttributeQueryProfileConfigurationCalled(Saml2AttributeQueryProfileConfiguration profileconfig) {

            this.operationType = OperationType.UPDATE_SAML2_ATTRIBUTE_QUERY_PROFILE_CONFIGURATION;
            this.saml2AttributeQueryProfileConfiguration = profileconfig;
            return this;
        }

        public Builder withSaml2EcpProfileConfiguration(Saml2EcpProfileConfiguration profileconfig) {

            this.saml2EcpProfileConfiguration = profileconfig;
            return this;
        }

        private Builder updateSaml2EcpProfileConfigurationCalled(Saml2EcpProfileConfiguration profileconfig) {

            this.operationType = OperationType.UPDATE_SAML2_ECP_PROFILE_CONFIGURATION;
            this.saml2EcpProfileConfiguration = profileconfig;
            return this;
        }

        public Builder withSaml2SsoProfileConfiguration(Saml2SsoProfileConfiguration profileconfig) {

            this.saml2SsoProfileConfiguration = profileconfig;
            return this;
        }

        private Builder updateSaml2SsoProfileConfigurationCalled(Saml2SsoProfileConfiguration profileConfiguration) {

            this.operationType = OperationType.UPDATE_SAML2_SSO_PROFILE_CONFIGURATION;
            this.saml2SsoProfileConfiguration = profileConfiguration;
            return this;
        }

        public Builder withSaml2LogoutProfileConfiguration(Saml2LogoutProfileConfiguration profileconfig) {

            this.saml2LogoutProfileConfiguration = profileconfig;
            return this;
        }

        public Builder updateSaml2LogoutProfileConfigurationCalled(Saml2LogoutProfileConfiguration profileconfig) {

            this.operationType = OperationType.UPDATE_SAML2_LOGOUT_PROFILE_CONFIGURATION;
            this.saml2LogoutProfileConfiguration = profileconfig;
            return this;
        }


        public Builder withReleasedAttributes(ReleasedAttributes releasedAttributes) {

            this.releasedAttributes = releasedAttributes;
            return this;
        }

        public Builder withActivationDiagnostics(ActivationDiagnostics activationDiagnostics) {

            this.activationDiagnostics = activationDiagnostics;
            return this;
        }

        private Builder activateCalled() {

            this.activationDiagnostics = ActivationDiagnostics.none();
            this.operationType = OperationType.ACTIVATE;
            return this;
        }

        private Builder cancelActivationCalled() {

            this.operationType = OperationType.CANCEL_ACTIVATION;
            return this;
        }

        private Builder deactivateCalled() {

            this.operationType = OperationType.DEACTIVATE;
            return this;
        }

        private Builder finalizeActivationCalled(ActivationDiagnostics activationDiagnostics) {

            this.operationType = OperationType.FINALIZE_ACTIVATION;
            this.activationDiagnostics = activationDiagnostics;
            return this;
        }

        private Builder incorporateDiscoveredEntityIdsCalled(EntityIds discoveredEntityIds) {

            this.operationType = OperationType.INCORPORATE_DISCOVERED_ENTITY_IDS;
            this.discoveredEntityIds = discoveredEntityIds;
            return this;
        }

        public TrustResult<TrustRelationship> build() {

            BuildContext build_context = createBuildContext();
            TrustResult<Void> rules_enforcement_result = TrustConsistencyRules.enforce(build_context);

            if (rules_enforcement_result.isFailure()) {
                
                if (creatingNew()) {
                    TrustError error = DomainObjectCreationFailed.forClassWithCause(TrustRelationship.class,rules_enforcement_result.getError());
                    return TrustResult.failure(error);
                }else {
                    TrustError error = DomainObjectUpdateFailed.forClassWithCause(TrustRelationship.class,rules_enforcement_result.getError());
                    return TrustResult.failure(error);
                }
            }

            
            if(creatingNew()) {

                return TrustResult.success(createCandidate());
            }

            TrustResult<TrustStatus> newstatus_result = TrustTransitionRules.determineNewStatus(build_context);
            if(newstatus_result.isFailure()) {

                TrustError cause = newstatus_result.getError();
                return TrustResult.failure(DomainObjectUpdateFailed.forClassWithCause(TrustRelationship.class,cause));
            }

            status = newstatus_result.getValue();

            if (isEffectivelyModified()) {

                version = version.next(); 
            }
            return TrustResult.success(createCandidate());

        }
        
        private final BuildContext createBuildContext() {

            return new BuildContext (
                    original,
                    id,
                    displayName,
                    description,
                    nature,
                    version,
                    status,
                    metadataSource,
                    discoveredEntityIds,
                    shibbolethSsoProfileConfiguration,
                    saml2ArtifactResolutionProfileConfiguration,
                    saml2AttributeQueryProfileConfiguration,
                    saml2EcpProfileConfiguration,
                    saml2SsoProfileConfiguration,
                    saml2LogoutProfileConfiguration,
                    releasedAttributes,
                    activationDiagnostics,
                    operationType
                );
        }


        private final TrustRelationship createCandidate () {

            return new TrustRelationship(
                id,
                displayName,
                description,
                nature,
                version,
                status,
                metadataSource,
                discoveredEntityIds,
                shibbolethSsoProfileConfiguration,
                saml2ArtifactResolutionProfileConfiguration,
                saml2AttributeQueryProfileConfiguration,
                saml2EcpProfileConfiguration,
                saml2SsoProfileConfiguration,
                saml2LogoutProfileConfiguration,
                releasedAttributes,
                activationDiagnostics
            );

        }

        private final boolean creatingNew() {

            return original == null;
        }

        private final boolean updatingOriginal() {

            return ! creatingNew();
        }

        private boolean isEffectivelyUnmodified() {

            return Objects.equals(original.displayName,displayName)
                && Objects.equals(original.description,description)
                && Objects.equals(original.status,status)
                && Objects.equals(original.nature,nature)
                && Objects.equals(original.metadataSource,metadataSource)
                && Objects.equals(original.discoveredEntityIds,discoveredEntityIds)
                && Objects.equals(original.shibbolethSsoProfileConfiguration,shibbolethSsoProfileConfiguration)
                && Objects.equals(original.saml2ArtifactResolutionProfileConfiguration,saml2ArtifactResolutionProfileConfiguration)
                && Objects.equals(original.saml2AttributeQueryProfileConfiguration,saml2AttributeQueryProfileConfiguration)
                && Objects.equals(original.saml2EcpProfileConfiguration,saml2EcpProfileConfiguration)
                && Objects.equals(original.saml2SsoProfileConfiguration,saml2SsoProfileConfiguration)
                && Objects.equals(original.saml2LogoutProfileConfiguration,saml2LogoutProfileConfiguration)
                && Objects.equals(original.releasedAttributes,releasedAttributes)
                && Objects.equals(original.activationDiagnostics,activationDiagnostics);
        }

        private boolean isEffectivelyModified() {

            return !isEffectivelyUnmodified();
        }
    }
}