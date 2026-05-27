package io.jans.shibboleth.model.core;


import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.error.InvalidUriSyntax;
import io.jans.shibboleth.model.util.TrustResult;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public class EntityId {

    private final String value;

    private EntityId(String value) {

        this.value = value.trim();
    }

    public static TrustResult<EntityId> of(String value) {

        if (value == null  || value.trim().isEmpty() ) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("value"));
        }

        try {
            URI uri = new URI(value);
        }catch(URISyntaxException e) {
            return TrustResult.failure(InvalidUriSyntax.forValue(value));
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