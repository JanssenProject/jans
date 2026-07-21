package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustStatus;

import java.util.Objects;
import java.util.UUID;

/**
 * Compact representation of a trust relationship: identity, descriptive fields and the
 * server-owned lifecycle fields. Returned by create and reused as the list-item shape.
 *
 * <p>A dumb data holder. Every trust relationship that reaches this DTO has an assigned {@code id}:
 * the id is unassigned only inside the domain at construction; persistence assigns it before
 * mapping.
 */
public class TrustRelationshipSummary {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("nature")
    private TrustNature nature;

    @JsonProperty("status")
    private TrustStatus status;

    @JsonProperty("version")
    private int version;

    public TrustRelationshipSummary() {
    }

    public UUID getId() {

        return id;
    }

    public void setId(UUID id) {

        this.id = id;
    }

    public String getDisplayName() {

        return displayName;
    }

    public void setDisplayName(String displayName) {

        this.displayName = displayName;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public TrustNature getNature() {

        return nature;
    }

    public void setNature(TrustNature nature) {

        this.nature = nature;
    }

    public TrustStatus getStatus() {

        return status;
    }

    public void setStatus(TrustStatus status) {

        this.status = status;
    }

    public int getVersion() {

        return version;
    }

    public void setVersion(int version) {

        this.version = version;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrustRelationshipSummary that = (TrustRelationshipSummary) o;
        return version == that.version
            && Objects.equals(id, that.id)
            && Objects.equals(displayName, that.displayName)
            && Objects.equals(description, that.description)
            && nature == that.nature
            && status == that.status;
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, displayName, description, nature, status, version);
    }

    @Override
    public String toString() {

        return "TrustRelationshipSummary{"
            + "id=" + id
            + ", displayName='" + displayName + '\''
            + ", description='" + description + '\''
            + ", nature=" + nature
            + ", status=" + status
            + ", version=" + version
            + '}';
    }
}
