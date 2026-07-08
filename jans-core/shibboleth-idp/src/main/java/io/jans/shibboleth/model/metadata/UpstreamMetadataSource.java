package io.jans.shibboleth.model.metadata;

import io.jans.shibboleth.model.core.EntityId;
import io.jans.shibboleth.model.core.Id;
import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.error.IdNotAssigned;
import io.jans.shibboleth.model.util.TrustResult;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;

        if (o == null || getClass() != o.getClass()) return false;

        UpstreamMetadataSource other = (UpstreamMetadataSource) o;

        return Objects.equals(parentId,other.parentId)
            && Objects.equals(entityId,other.entityId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(parentId,entityId);
    }

    public static TrustResult<MetadataSource> of(Id parentId, EntityId entityId) {

        if ( parentId == null ) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("parentId"));
        }

        if ( entityId == null ) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("entityId"));
        }

        if ( parentId.isNotAssigned() ) {

            return TrustResult.failure(IdNotAssigned.forUpstreamMetadataSource());
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
