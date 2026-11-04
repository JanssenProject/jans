package io.jans.shibboleth.trust.activation.model;

import java.util.Objects;
import java.util.UUID;

import io.jans.kernel.RequiredValueMissing;
import io.jans.kernel.Result;

public final class TrustRelationshipRef {

    private final UUID value;

    private TrustRelationshipRef(UUID value) {

        this.value = value;
    }

    public static Result<TrustRelationshipRef> of(UUID value) {

        if (value == null) {

            return Result.failure(RequiredValueMissing.of(TrustRelationshipRef.class));
        }

        return Result.success(new TrustRelationshipRef(value));
    }

    public UUID value() {

        return value;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrustRelationshipRef that = (TrustRelationshipRef) o;

        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {

        return Objects.hash(value);
    }

    @Override
    public String toString() {

        return value == null ? "[no trust-relationship reference]" : value.toString();
    }
}
