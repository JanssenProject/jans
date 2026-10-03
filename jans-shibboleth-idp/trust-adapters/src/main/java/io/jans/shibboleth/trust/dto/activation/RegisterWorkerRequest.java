package io.jans.shibboleth.trust.dto.activation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Request body for registering (announcing) a worker ({@code POST /v1/trust/activation/workers}).
 * {@code origin} is the worker's identity, "instance@host". A dumb data holder — unknown properties
 * are rejected.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class RegisterWorkerRequest {

    @JsonProperty("origin")
    private String origin;

    public RegisterWorkerRequest() {
    }

    public RegisterWorkerRequest(String origin) {

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

        return Objects.equals(origin, ((RegisterWorkerRequest) o).origin);
    }

    @Override
    public int hashCode() {

        return Objects.hash(origin);
    }

    @Override
    public String toString() {

        return "RegisterWorkerRequest{origin='" + origin + "'}";
    }
}
