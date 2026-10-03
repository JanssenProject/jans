package io.jans.staging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Request body for claiming a staged file ({@code POST /v1/files/{token}/claim}). {@code destination} is
 * the absolute, unix-style directory the file is moved into. A dumb data holder — unknown properties are
 * rejected.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ClaimRequest {

    @JsonProperty("destination")
    private String destination;

    public ClaimRequest() {
    }

    public ClaimRequest(String destination) {

        this.destination = destination;
    }

    public String getDestination() {

        return destination;
    }

    public void setDestination(String destination) {

        this.destination = destination;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }
        if (o == null || getClass() != o.getClass()) {

            return false;
        }
        return Objects.equals(destination, ((ClaimRequest) o).destination);
    }

    @Override
    public int hashCode() {

        return Objects.hash(destination);
    }

    @Override
    public String toString() {

        return "ClaimRequest{destination='" + destination + "'}";
    }
}
