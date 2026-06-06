package io.jans.shibboleth.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Arrays;

import io.jans.common.Result;
import io.jans.shibboleth.model.core.*;
import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.util.TrustResult;

public class EntityIds {

    private final Set<EntityId> ids;

    private EntityIds(Set<EntityId> ids) {

        this.ids = Set.copyOf(ids);
    }

    public static EntityIds empty() {

        return new EntityIds(Set.of());
    }

    public static TrustResult<EntityIds> of(Collection<String> rawIds) {

        if( rawIds == null || rawIds.isEmpty() ) {

            return TrustResult.success(empty());
        }

        Set<EntityId> entityIds = new HashSet<>();
        for(String rawId : rawIds) {

            Result<EntityId> entityId = EntityId.of(rawId);
            if(entityId.isFailure()) {
                return TrustResult.failure((TrustError)entityId.getError());
            }
            entityIds.add(entityId.getValue());
        }
        return TrustResult.success(new EntityIds(entityIds));
    }

    public static TrustResult<EntityIds> of(String ... rawEntityIds) {

        return of(Arrays.asList(rawEntityIds));
    }

    public boolean hasAny() {

        return !ids.isEmpty();
    }

    public boolean hasNone() {

        return ids.isEmpty();
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        EntityIds that = (EntityIds) o;

        return Objects.equals(this.ids,that.ids);
    }

    @Override
    public int hashCode() {

        return Objects.hash(ids);
    }

    
}