package io.jans.shibboleth.trust.dto.activation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.activation.model.WorkItemType;

import java.util.Objects;

/**
 * Request body for atomically claiming the next available unit of work
 * ({@code POST /v1/trust/activation/work-items/claim-next}). {@code origin} is the claiming worker's
 * identity ("instance@host"); {@code type} selects which kind of work to claim. A dumb data holder —
 * unknown properties are rejected.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ClaimNextRequest {

    @JsonProperty("origin")
    private String origin;

    @JsonProperty("type")
    private WorkItemType type;

    public ClaimNextRequest() {
    }

    public ClaimNextRequest(String origin, WorkItemType type) {

        this.origin = origin;
        this.type = type;
    }

    public String getOrigin() {

        return origin;
    }

    public void setOrigin(String origin) {

        this.origin = origin;
    }

    public WorkItemType getType() {

        return type;
    }

    public void setType(WorkItemType type) {

        this.type = type;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClaimNextRequest that = (ClaimNextRequest) o;
        return Objects.equals(origin, that.origin) && type == that.type;
    }

    @Override
    public int hashCode() {

        return Objects.hash(origin, type);
    }

    @Override
    public String toString() {

        return "ClaimNextRequest{origin='" + origin + "', type=" + type + "}";
    }
}
