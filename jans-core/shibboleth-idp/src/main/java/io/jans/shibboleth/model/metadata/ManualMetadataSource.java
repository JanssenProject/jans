package io.jans.shibboleth.model.metadata;

import io.jans.shibboleth.model.core.EntityId;

public class ManualMetadataSource implements MetadataSource  {
    
    private final EntityId entityId;

    private ManualMetadataSource(EntityId entityId) {

        this.entityId = entityId;
    }

    @Override
    public MetadataSourceType getType() {

        return MetadataSourceType.MANUAL;
    }

    
}
