package io.jans.shibboleth.trust.dto.activation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Request body for renewing a work item's lease
 * ({@code POST /v1/trust/activation/work-items/{id}/heartbeat}). {@code origin} is the calling worker's
 * identity ("instance@host") — the lease-holder fence the server checks before renewing. A dumb data
 * holder — unknown properties are rejected.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class RenewLeaseRequest {

    @JsonProperty("origin")
    private String origin;

    public RenewLeaseRequest() {
    }

    public RenewLeaseRequest(String origin) {

        this.origin = origin;
    }

    public String getOrigin() {

        return origin;
    }

    public void setOrigin(String origin) {

        this.origin = origin;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return Objects.equals(origin, ((RenewLeaseRequest) o).origin);
    }

    @Override
    public int hashCode() {

        return Objects.hash(origin);
    }

    @Override
    public String toString() {

        return "RenewLeaseRequest{origin='" + origin + "'}";
    }
}
