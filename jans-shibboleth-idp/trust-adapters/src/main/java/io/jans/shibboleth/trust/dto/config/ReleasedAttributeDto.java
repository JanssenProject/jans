package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

/**
 * An attribute released by a trust relationship: its identifier and display name. Used both in
 * responses and as an item of the released-attributes update request.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ReleasedAttributeDto {

    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("display_name")
    private final String displayName;

    @JsonCreator
    public ReleasedAttributeDto(@JsonProperty("id") UUID id, @JsonProperty("display_name") String displayName) {

        this.id = id;
        this.displayName = displayName;
    }

    public UUID getId() {

        return id;
    }

    public String getDisplayName() {

        return displayName;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReleasedAttributeDto that = (ReleasedAttributeDto) o;
        return Objects.equals(id, that.id) && Objects.equals(displayName, that.displayName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, displayName);
    }

    @Override
    public String toString() {

        return "ReleasedAttributeDto{id=" + id + ", displayName='" + displayName + "'}";
    }
}
