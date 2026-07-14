package io.jans.shibboleth.trust.config;

import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.config.util.TrustResult;

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

    public static TrustResult<ReleasedAttribute> of(Id id, String displayName) {

        if (id == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("id"));
        }

        if (displayName == null || displayName.isBlank()) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("displayName"));
        }

        return TrustResult.success(new ReleasedAttribute(id, displayName));
    }
}
