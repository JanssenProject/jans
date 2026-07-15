package io.jans.shibboleth.trust.config;

import io.jans.shibboleth.trust.shared.RequiredValueMissing;
import io.jans.shibboleth.trust.shared.Result;

import java.util.Objects;

public class ReleasedAttribute {
    private final Id id;
    private final String displayName;

    private ReleasedAttribute(Id id , String displayName) {

        this.id = id;
        this.displayName = displayName;
    }

    public Id getId() {

        return id;
    }

    public String getDisplayName() {

        return displayName;
    }

    public static Result<ReleasedAttribute> of(Id id, String displayName) {

        if (id == null) {

            return Result.failure(RequiredValueMissing.forField("id"));
        }

        if (displayName == null || displayName.isBlank()) {

            return Result.failure(RequiredValueMissing.forField("displayName"));
        }

        return Result.success(new ReleasedAttribute(id, displayName));
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ReleasedAttribute that = (ReleasedAttribute) o;
        return Objects.equals(id, that.id) && Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, displayName);
    }
}
