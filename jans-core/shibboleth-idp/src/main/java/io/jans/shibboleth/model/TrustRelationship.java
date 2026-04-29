package io.jans.shibboleth.model;

import io.jans.shibboleth.model.config.profiles.Saml2ArtifactResolutionProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2AttributeQueryProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2EcpProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2LogoutProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.Saml2SsoProfileConfiguration;
import io.jans.shibboleth.model.config.profiles.ShibbolethSsoProfileConfiguration;

import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.metadata.*;
import io.jans.shibboleth.model.util.TrustResult;

import java.util.Objects;


public class TrustRelationship {


    private Id id;
    private DisplayName displayName;
    private Description description;
    private final TrustNature nature;

    private int version;
    private TrustStatus status;
    private MetadataSource metadataSource;

    private EntityIds discoveredEntityIds;
    private IdpInstances idpInstances;
    private WorkItems workItems;
    private Profiles profiles;

    private TrustRelationship(final Id id, DisplayName displayName, Description description, final TrustNature nature) {

        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.nature = nature;

        version = 1;
        status = TrustStatus.DRAFT;
        metadataSource = new NoMetadataSource();
        discoveredEntityIds =  EntityIds.empty();
        idpInstances = IdpInstances.empty();
        workItems = WorkItems.empty();
        profiles = Profiles.allDefaults();
    }

    private int incrementVersion() {

        version++;
        return version;
    }

    public Id getId() {

        return id;
    }

    public DisplayName getDisplayName() {

        return displayName;
    }

    public TrustResult<Void> updateDisplayName(DisplayName newDisplayName) {

        if (newDisplayName == null) {

            return TrustResult.failure(DisplayNameError.required());
        }
        displayName = newDisplayName;
        incrementVersion();
        return TrustResult.success(null);
    } 

    public Description getDescription() {

        return description;
    }

    public TrustResult<Void> updateDescription(Description newDescription) {

        description = newDescription != null ? newDescription : Description.of("");
        incrementVersion();
        return TrustResult.success(null);

    }

    public TrustNature getNature() {

        return nature;
    }

    public TrustStatus getStatus() {

        return status;
    }

    public int getVersion() {

        return version;
    }

    public MetadataSource getMetadataSource() {

        return metadataSource;
    }

    public TrustResult<Void> updateMetadataSource(MetadataSource source) {

        if (source == null) {

            return TrustResult.failure(MetadataSourceError.required());
        }
        return TrustResult.success(null);
    }

    public EntityIds getDiscoveredEntityIds() {

        return discoveredEntityIds;
    }

    public IdpInstances  getIdpInstances() {

        return idpInstances;
    }

    public boolean hasNoMetadataSource() {

        return Objects.equals(metadataSource,NoMetadataSource.getInstance());
    }

    public boolean hasAnyDiscoveredEntityIds() {

        return discoveredEntityIds.hasAny();
    }

    public boolean hasNoDiscoveredEntityIds() {

        return discoveredEntityIds.hasNone();
    }

    public boolean hasAnyRegisteredIdpInstances() {

        return idpInstances.hasAnyRegisteredInstance();
    }

    public boolean hasAnyWorkItem() {

        return workItems.hasAny();
    }

    public ShibbolethSsoProfileConfiguration getShibbolethSsoProfileConfiguration() {

        return profiles.getShibbolethSso();
    }

    public Saml2AttributeQueryProfileConfiguration getSaml2AttributeQueryProfileConfiguration() {

        return profiles.getSaml2AttributeQuery();
    }

    public Saml2ArtifactResolutionProfileConfiguration getSaml2ArtifactResolutionProfileConfiguration() {

        return profiles.getSaml2ArtifactResolution();
    }

    public Saml2EcpProfileConfiguration getSaml2EcpProfileConfiguration() {

        return profiles.getSaml2Ecp();
    }

    public Saml2SsoProfileConfiguration getSaml2SsoProfileConfiguration() {
        
        return profiles.getSaml2Sso();
    }

    public Saml2LogoutProfileConfiguration getSaml2LogoutProfileConfiguration() {

        return profiles.getSaml2Logout();
    }
    
    public static TrustResult<TrustRelationship> create (
        final String displayName,
        final String description,
        final TrustNature nature
    )

    {

        Id id = Id.unassigned();

        TrustResult<DisplayName> displayNameResult = DisplayName.of(displayName);
        if( displayNameResult.isFailure() ) {
            return TrustResult.failure(displayNameResult.getError());
        }
        final DisplayName displayNameValue = displayNameResult.getValue();

        Description descriptionValue = Description.of(description);
        
        if ( nature == null ) {

            return TrustResult.failure(TrustNatureError.required());
        }

        return TrustResult.success(new TrustRelationship(id,displayNameValue,descriptionValue,nature));
    }
}