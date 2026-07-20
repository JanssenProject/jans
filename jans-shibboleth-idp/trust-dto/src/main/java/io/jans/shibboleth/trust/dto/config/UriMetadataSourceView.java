package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Read view for URL-based metadata (`type: URI`).
 */
public class UriMetadataSourceView extends MetadataSourceView {

    @JsonProperty("uri")
    private final String uri;

    public UriMetadataSourceView(String uri) {

        this.uri = uri;
    }

    public String getUri() {

        return uri;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return Objects.equals(uri, ((UriMetadataSourceView) o).uri);
    }

    @Override
    public int hashCode() {

        return Objects.hash(uri);
    }

    @Override
    public String toString() {

        return "UriMetadataSourceView{uri='" + uri + "'}";
    }
}
