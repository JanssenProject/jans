package io.jans.shibboleth.trust.config;

import java.util.Objects;
import java.util.UUID;

import io.jans.shibboleth.trust.config.error.IdNotAssigned;
import io.jans.kernel.Result;

/**
 * An aggregate's identity, which may not have been assigned yet — persistence assigns it.
 *
 * <p>Deliberately not a record. A record would publish a component accessor returning the raw
 * {@code UUID}, and for an unassigned id that value is {@code null} — exactly what this type exists
 * to keep callers from seeing. The only way to reach the value is {@link #getValue()}, which returns
 * a {@link Result} so an unassigned id is a failure a caller must handle rather than a null to trip
 * over.
 */
public final class Id {

    /**
     * {@code null} when unassigned. Never returned directly; whether an id is assigned is derived
     * from it rather than tracked separately, so the two cannot disagree.
     */
    private final UUID value;

    private Id(UUID value) {

        this.value = value;
    }

    public static Id unassigned() {

        return new Id(null);
    }

    public static Id of(UUID value) {

        return new Id(value);
    }

    public static Id generate() {

        return new Id(UUID.randomUUID());
    }

    public boolean isAssigned() {

        return value != null;
    }

    public boolean isNotAssigned() {

        return value == null;
    }

    public Result<UUID> getValue() {

        if (value == null) {

            return Result.failure(IdNotAssigned.accessingValueOfUnassignedId());
        }

        return Result.success(value);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Id that = (Id) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {

        return Objects.hash(value);
    }

    @Override
    public String toString() {

        return value != null ? value.toString() : "[unassigned id]";
    }
}
