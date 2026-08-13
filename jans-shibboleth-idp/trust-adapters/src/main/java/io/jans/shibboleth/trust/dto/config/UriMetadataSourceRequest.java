package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Metadata fetched from a single URL (`type: URI`).
 */
public class UriMetadataSourceRequest extends MetadataSourceRequest {

    @JsonProperty("uri")
    private String uri;

    public UriMetadataSourceRequest() {
    }

    public UriMetadataSourceRequest(String uri) {

        this.uri = uri;
    }

    public String getUri() {

        return uri;
    }

    public void setUri(String uri) {

        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return Objects.equals(uri, ((UriMetadataSourceRequest) o).uri);
    }

    @Override
    public int hashCode() {

        return Objects.hash(uri);
    }

    @Override
    public String toString() {

        return "UriMetadataSourceRequest{uri='" + uri + "'}";
    }
}
