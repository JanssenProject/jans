package io.jans.shibboleth.model.core;

import io.jans.shibboleth.model.error.EntityIdError;
import io.jans.shibboleth.model.util.TrustResult;

import java.util.Objects;

public class EntityId {

    private final String value;

    private EntityId(String value) {

        this.value = value.trim();
    }

    public static TrustResult<EntityId> of(String value) {

        if (value == null  || value.trim().isEmpty() ) {

            return TrustResult.failure(EntityIdError.cannotBeNullOrBlank());
        }

        return TrustResult.success(new EntityId(value));
    }

    public String getValue() {

        return value;
    }

    @Override
    public boolean equals(Object o) {

        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;
        EntityId that = (EntityId) o;
        return Objects.equals(value,that.value);
    }

    @Override
    public int hashCode() {

        return Objects.hash(value);
    }

    @Override
    public String toString() {

        return value;
    }
}