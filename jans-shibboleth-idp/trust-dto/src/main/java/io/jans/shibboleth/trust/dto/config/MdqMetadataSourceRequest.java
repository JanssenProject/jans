package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Metadata resolved on demand from an MDQ service (`type: MDQ`). Valid for AGGREGATE trust
 * relationships only.
 */
public class MdqMetadataSourceRequest extends MetadataSourceRequest {

    @JsonProperty("base_url")
    private String baseUrl;

    public MdqMetadataSourceRequest() {
    }

    public MdqMetadataSourceRequest(String baseUrl) {

        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {

        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {

        this.baseUrl = baseUrl;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return Objects.equals(baseUrl, ((MdqMetadataSourceRequest) o).baseUrl);
    }

    @Override
    public int hashCode() {

        return Objects.hash(baseUrl);
    }

    @Override
    public String toString() {

        return "MdqMetadataSourceRequest{baseUrl='" + baseUrl + "'}";
    }
}
