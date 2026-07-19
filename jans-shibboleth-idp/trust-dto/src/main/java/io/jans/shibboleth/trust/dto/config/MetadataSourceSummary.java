package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.config.metadata.MetadataSourceType;

import java.util.Objects;

/**
 * The kind of metadata source configured on a trust relationship, without its details. The full
 * metadata source is available from the metadata-source sub-resource.
 */
public class MetadataSourceSummary {

    @JsonProperty("type")
    private final MetadataSourceType type;

    public MetadataSourceSummary(MetadataSourceType type) {

        this.type = type;
    }

    public MetadataSourceType getType() {

        return type;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return type == ((MetadataSourceSummary) o).type;
    }

    @Override
    public int hashCode() {

        return Objects.hash(type);
    }

    @Override
    public String toString() {

        return "MetadataSourceSummary{type=" + type + '}';
    }
}
