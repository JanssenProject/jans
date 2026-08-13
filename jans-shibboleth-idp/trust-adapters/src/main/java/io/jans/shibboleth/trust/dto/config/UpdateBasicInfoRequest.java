package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Request body for updating a trust relationship's basic info — its display name and description —
 * in one step ({@code PUT /v1/trust/config/trust-relationships/{id}/basic-info}).
 *
 * <p>A dumb data holder. The whole block is replaced: {@code display_name} is required and must be
 * non-blank; {@code description} is optional and an omitted or null value clears it to empty. Unknown
 * properties are rejected rather than silently ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateBasicInfoRequest {

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("description")
    private String description;

    public UpdateBasicInfoRequest() {
    }

    public UpdateBasicInfoRequest(String displayName, String description) {

        this.displayName = displayName;
        this.description = description;
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

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UpdateBasicInfoRequest that = (UpdateBasicInfoRequest) o;
        return Objects.equals(displayName, that.displayName)
            && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {

        return Objects.hash(displayName, description);
    }

    @Override
    public String toString() {

        return "UpdateBasicInfoRequest{displayName='" + displayName + "', description='" + description + "'}";
    }
}
