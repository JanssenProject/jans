package io.jans.shibboleth.model.core;

import java.util.UUID;
import java.util.Objects;

import io.jans.shibboleth.model.error.*;
import io.jans.shibboleth.model.util.TrustResult;

public class Id {

    private UUID value;
    private boolean assigned;

    private Id(UUID value, boolean assigned) {

        this.value = value;
        this.assigned = assigned;
    }

    public static Id unassigned() {

        return new Id(null,false);
    }

    public static Id of(UUID value) {

        if (value == null) {

            return unassigned();
        }

        return new Id(value,true);
    }

    public static Id generate() {

        return new Id(UUID.randomUUID(),true);
    }

    public boolean isAssigned() {

        return assigned;
    }

    public TrustResult<UUID> getValue() {

        if (!assigned) {

            return TrustResult.failure(IdError.notAssigned());
        }

        return TrustResult.success(value);
    }

    @Override
    public boolean equals(Object o) {

        if( this == o ) return true;

        if ( o == null || getClass() != o.getClass() ) return false;

        Id that = (Id) o;
        return Objects.equals(value,that.value);
    }

    @Override
    public int hashCode() {

        return Objects.hash(value);
    }

    @Override
    public String toString() {

        return value.toString();
    }
}