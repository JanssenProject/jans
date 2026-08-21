package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Read view for upstream (inherited) metadata (`type: UPSTREAM`).
 */
public class UpstreamMetadataSourceView extends MetadataSourceView {

    @JsonProperty("parent_id")
    private final String parentId;

    @JsonProperty("entity_id")
    private final String entityId;

    public UpstreamMetadataSourceView(String parentId, String entityId) {

        this.parentId = parentId;
        this.entityId = entityId;
    }

    public String getParentId() {

        return parentId;
    }

    public String getEntityId() {

        return entityId;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UpstreamMetadataSourceView that = (UpstreamMetadataSourceView) o;
        return Objects.equals(parentId, that.parentId) && Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(parentId, entityId);
    }

    @Override
    public String toString() {

        return "UpstreamMetadataSourceView{parentId='" + parentId + "', entityId='" + entityId + "'}";
    }
}
