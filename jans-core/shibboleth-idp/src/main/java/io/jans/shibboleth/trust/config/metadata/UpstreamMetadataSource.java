package io.jans.shibboleth.trust.config.metadata;

import io.jans.shibboleth.trust.config.EntityId;
import io.jans.shibboleth.trust.config.Id;
import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.config.error.IdNotAssigned;
import io.jans.shibboleth.trust.shared.Result;

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

    public static Result<MetadataSource> of(Id parentId, EntityId entityId) {

        if ( parentId == null ) {

            return Result.failure(CannotBeNullOrBlank.forField("parentId"));
        }

        if ( entityId == null ) {

            return Result.failure(CannotBeNullOrBlank.forField("entityId"));
        }

        if ( parentId.isNotAssigned() ) {

            return Result.failure(IdNotAssigned.forUpstreamMetadataSource());
        }

        return Result.success(new UpstreamMetadataSource(parentId,entityId));
    }

    public Id getParentId() {

        return parentId;
    }

    public EntityId getEntityId() {

        return entityId;
    }
}
