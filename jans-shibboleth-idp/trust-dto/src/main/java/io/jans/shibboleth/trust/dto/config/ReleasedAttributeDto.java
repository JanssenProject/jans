package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.UUID;

/**
 * An attribute released by a trust relationship: its identifier and display name.
 */
public class ReleasedAttributeDto {

    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("display_name")
    private final String displayName;

    public ReleasedAttributeDto(UUID id, String displayName) {

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
