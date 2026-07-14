package io.jans.shibboleth.trust.config;


import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.config.error.InvalidUriSyntax;
import io.jans.shibboleth.trust.shared.Result;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class EntityId {

    private final URI value;

    private EntityId(URI value) {

        this.value = value;
    }

    public static Result<EntityId> of(URI value) {

        if (value == null) {

            return Result.failure(CannotBeNullOrBlank.forField("value"));
        }

        return Result.success(new EntityId(value));
    }

    public URI getValue() {

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

        return "EntityId[" + value + "]";
    }
}