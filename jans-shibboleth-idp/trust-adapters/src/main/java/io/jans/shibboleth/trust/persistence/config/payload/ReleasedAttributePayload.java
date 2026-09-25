package io.jans.shibboleth.trust.persistence.config.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A stored released attribute: its {@code id} (UUID string, absent when the id is unassigned) and its
 * display name. The trust relationship's released attributes are persisted as a JSON list of these
 * (the {@code jansReleasedAttr} {@code @JsonObject} column). A dumb, public-field carrier (TP3).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReleasedAttributePayload {

    public String id;
    public String displayName;
}
