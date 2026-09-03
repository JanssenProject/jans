package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.config.TrustNature;

import java.util.Objects;

/**
 * Request body for creating a trust relationship
 * ({@code POST /v1/trust/config/trust-relationships}).
 *
 * <p>A dumb data holder: only the fields a client may set at creation appear here. Everything else
 * (id, status, version, metadata source, profile configurations, released attributes) is
 * server-initialised and therefore absent from this request. Unknown properties are rejected
 * rather than silently ignored, so a client sending a misspelled or unsupported field is told.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class CreateTrustRelationshipRequest {

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("nature")
    private TrustNature nature;

    public CreateTrustRelationshipRequest() {
    }

    public CreateTrustRelationshipRequest(String displayName, String description, TrustNature nature) {

        this.displayName = displayName;
        this.description = description;
        this.nature = nature;
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

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CreateTrustRelationshipRequest that = (CreateTrustRelationshipRequest) o;
        return Objects.equals(displayName, that.displayName)
            && Objects.equals(description, that.description)
            && nature == that.nature;
    }

    @Override
    public int hashCode() {

        return Objects.hash(displayName, description, nature);
    }

    @Override
    public String toString() {

        return "CreateTrustRelationshipRequest{"
            + "displayName='" + displayName + '\''
            + ", description='" + description + '\''
            + ", nature=" + nature
            + '}';
    }
}
