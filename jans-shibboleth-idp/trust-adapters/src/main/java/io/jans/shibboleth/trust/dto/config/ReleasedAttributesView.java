package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Read view of the attributes a trust relationship releases. Mirrors the update request shape
 * ({@code { attributes: [...] }}); an empty list means it releases none.
 */
public class ReleasedAttributesView {

    @JsonProperty("attributes")
    private final List<ReleasedAttributeDto> attributes;

    public ReleasedAttributesView(List<ReleasedAttributeDto> attributes) {

        this.attributes = attributes;
    }

    public List<ReleasedAttributeDto> getAttributes() {

        return attributes;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return Objects.equals(attributes, ((ReleasedAttributesView) o).attributes);
    }

    @Override
    public int hashCode() {

        return Objects.hash(attributes);
    }

    @Override
    public String toString() {

        return "ReleasedAttributesView{attributes=" + attributes + '}';
    }
}
