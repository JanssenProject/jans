package io.jans.shibboleth.trust.config;

import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

import java.net.URI;
import java.util.Objects;

/**
 * A SAML entity identifier. The only rule is presence, so {@link #of(URI)} is the way in and the
 * constructor guard is a backstop against bypassing it.
 */
public record EntityId(URI value) {

    public EntityId {

        Objects.requireNonNull(value, "value");
    }

    public static Result<EntityId> of(URI value) {

        if (value == null) {

            return Result.failure(RequiredValueMissing.of(EntityId.class));
        }

        return Result.success(new EntityId(value));
    }

    @Override
    public String toString() {

        return "EntityId[" + value + "]";
    }
}
