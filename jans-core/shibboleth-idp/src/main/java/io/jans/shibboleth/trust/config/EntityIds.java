package io.jans.shibboleth.trust.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.Arrays;

import io.jans.common.Result;
import io.jans.shibboleth.trust.config.*;
import io.jans.shibboleth.trust.config.error.*;
import io.jans.shibboleth.trust.config.util.TrustResult;

public class EntityIds {

    private final Set<EntityId> ids;

    private EntityIds(Set<EntityId> ids) {

        this.ids = Set.copyOf(ids);
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

    public static Builder from(EntityIds entityIds) {

        return new Builder(entityIds);
    }

    public static Builder builder() {

        return new Builder(null);
    }

    public static EntityIds empty() {

        return new EntityIds(Set.of());
    }
    
    public static class Builder {

        private final Set<EntityId> ids = new LinkedHashSet<>();
        private TrustError error; 

        private Builder() { }

        private Builder(EntityIds existing) {

            if (existing != null) {

                this.ids.addAll(existing.ids);
            }
        }

        private Builder add(EntityId id) {

            if (error != null) {

                return this;
            }

            if (id == null) {

                error = CannotBeNullOrBlank.forField("id");
            }

            ids.add(id);
            return this;
        }

        public Builder addAll(Collection<EntityId> ids) {

            if (error != null) {

                return this;
            }

            for(EntityId id : ids) {

                if (id == null) {
                    error = CannotBeNullOrBlank.forField("id");
                    return this;
                }

                this.ids.add(id);
            }

            return this;
        }

        public TrustResult<EntityIds> build() {

            if (error != null) {

                return TrustResult.failure(DomainObjectCreationFailed.forClassWithCause(EntityIds.class, error));
            }

            return TrustResult.success(new EntityIds(ids));
        }

    }
}