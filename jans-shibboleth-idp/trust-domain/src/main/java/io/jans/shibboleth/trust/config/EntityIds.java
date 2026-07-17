package io.jans.shibboleth.trust.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import io.jans.shibboleth.trust.config.error.*;
import io.jans.shibboleth.trust.shared.DomainError;
import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

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
        private DomainError error;

        private Builder() { }

        private Builder(EntityIds existing) {

            if (existing != null) {

                this.ids.addAll(existing.ids);
            }
        }

        public Builder add(EntityId id) {

            if (error != null) {

                return this;
            }

            if (id == null) {

                error = RequiredValueMissing.forField("id");
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
                    error = RequiredValueMissing.forField("id");
                    return this;
                }

                this.ids.add(id);
            }

            return this;
        }

        public Result<EntityIds> build() {

            if (error != null) {

                return Result.failure(DomainObjectCreationFailed.forClassWithCause(EntityIds.class, error));
            }

            return Result.success(new EntityIds(ids));
        }

    }
}