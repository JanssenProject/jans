package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Read view for MDQ-based metadata (`type: MDQ`).
 */
public class MdqMetadataSourceView extends MetadataSourceView {

    @JsonProperty("base_url")
    private final String baseUrl;

    public MdqMetadataSourceView(String baseUrl) {

        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {

        return baseUrl;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return Objects.equals(baseUrl, ((MdqMetadataSourceView) o).baseUrl);
    }

    @Override
    public int hashCode() {

        return Objects.hash(baseUrl);
    }

    @Override
    public String toString() {

        return "MdqMetadataSourceView{baseUrl='" + baseUrl + "'}";
    }
}
