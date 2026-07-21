package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.config.profile.common.ProfileStatus;
import io.jans.shibboleth.trust.config.profile.common.ProfileType;

import java.util.Objects;

/**
 * The kind of profile and whether it is active, without its settings. The full profile
 * configuration is available from the corresponding profile sub-resource.
 */
public class ProfileSummary {

    @JsonProperty("type")
    private final ProfileType type;

    @JsonProperty("status")
    private final ProfileStatus status;

    public ProfileSummary(ProfileType type, ProfileStatus status) {

        this.type = type;
        this.status = status;
    }

    public ProfileType getType() {

        return type;
    }

    public ProfileStatus getStatus() {

        return status;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProfileSummary that = (ProfileSummary) o;
        return type == that.type && status == that.status;
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, status);
    }

    @Override
    public String toString() {

        return "ProfileSummary{type=" + type + ", status=" + status + '}';
    }
}
