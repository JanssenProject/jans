package io.jans.shibboleth.model.metadata;

import io.jans.shibboleth.model.core.EntityId;
import io.jans.shibboleth.model.core.Id;
import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.error.IdNotAssigned;
import io.jans.shibboleth.model.util.TrustResult;

public class UpstreamMetadataSource implements MetadataSource {

    private final Id parentId;
    private final EntityId entityId;

    private UpstreamMetadataSource(Id parentId, EntityId entityId) {

        this.parentId = parentId;
        this.entityId = entityId;
    }

    @Override
    public MetadataSourceType getType() {

        return MetadataSourceType.UPSTREAM;
    }

    public static TrustResult<UpstreamMetadataSource> of(Id parentId, EntityId entityId) {

        if ( parentId == null ) {

            return TrustResult.failure(new CannotBeNullOrBlank("parentId"));
        }

        if ( entityId == null ) {

            return TrustResult.failure(new CannotBeNullOrBlank("entityId"));
        }

        if ( parentId.isNotAssigned() ) {

            return TrustResult.failure(new IdNotAssigned("Upstream metadata source requires a valid parentId"));
        }

        return TrustResult.success(new UpstreamMetadataSource(parentId,entityId));
    }

    public Id getParentId() {

        return parentId;
    }

    public EntityId getEntityId() {

        return entityId;
    }
}
