package io.jans.shibboleth.model;

import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.metadata.*;
import io.jans.shibboleth.model.util.TrustResult;


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

    public TrustStatus getStatus() {

        return status;
    }

    public int getVersion() {

        return version;
    }

    public boolean isNew() {

        return ! id.isAssigned();
    }

    public boolean hasNoMetadataSource() {

        return metadataSource.getType() == MetadataSourceType.NONE;
    }

    public boolean hasAnyDiscoveredEntityIds() {

        return discoveredEntityIds.hasAnyDiscoveredEntityIds();
    }

    public boolean hasAnyRegisteredIdpInstances() {

        return idpInstances.hasAnyRegisteredInstance();
    }

    public boolean hasAnyWorkItem() {

        return workItems.hasAny();
    }

    public boolean hasNoEnabledProfiles() {

        
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