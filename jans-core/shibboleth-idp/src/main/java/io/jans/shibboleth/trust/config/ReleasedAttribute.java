package io.jans.shibboleth.trust.config;

import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.shared.Result;

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

            return Result.failure(CannotBeNullOrBlank.forField("id"));
        }

        if (displayName == null || displayName.isBlank()) {

            return Result.failure(CannotBeNullOrBlank.forField("displayName"));
        }

        return Result.success(new ReleasedAttribute(id, displayName));
    }
}
