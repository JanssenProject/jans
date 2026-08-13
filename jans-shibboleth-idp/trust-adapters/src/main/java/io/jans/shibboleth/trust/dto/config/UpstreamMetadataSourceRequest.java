package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Metadata inherited from a parent (aggregate) trust relationship, for one entity within it
 * (`type: UPSTREAM`). Valid for INDIVIDUAL trust relationships only.
 */
public class UpstreamMetadataSourceRequest extends MetadataSourceRequest {

    @JsonProperty("parent_id")
    private String parentId;

    @JsonProperty("entity_id")
    private String entityId;

    public UpstreamMetadataSourceRequest() {
    }

    public UpstreamMetadataSourceRequest(String parentId, String entityId) {

        this.parentId = parentId;
        this.entityId = entityId;
    }

    public String getParentId() {

        return parentId;
    }

    public void setParentId(String parentId) {

        this.parentId = parentId;
    }

    public String getEntityId() {

        return entityId;
    }

    public void setEntityId(String entityId) {

        this.entityId = entityId;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UpstreamMetadataSourceRequest that = (UpstreamMetadataSourceRequest) o;
        return Objects.equals(parentId, that.parentId) && Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(parentId, entityId);
    }

    @Override
    public String toString() {

        return "UpstreamMetadataSourceRequest{parentId='" + parentId + "', entityId='" + entityId + "'}";
    }
}
