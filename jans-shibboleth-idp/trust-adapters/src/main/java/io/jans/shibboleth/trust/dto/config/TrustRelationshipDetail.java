package io.jans.shibboleth.trust.dto.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.jans.shibboleth.trust.config.TrustNature;
import io.jans.shibboleth.trust.config.TrustStatus;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Full view of a trust relationship: its own fields plus compact views of its structured parts.
 * The metadata source and each profile are shown by kind and status only; their complete settings
 * are fetched from their own sub-resources. Released attributes, activation diagnostics and
 * discovered entity IDs are included in full.
 */
public class TrustRelationshipDetail {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("nature")
    private TrustNature nature;

    @JsonProperty("status")
    private TrustStatus status;

    @JsonProperty("version")
    private int version;

    @JsonProperty("metadata_source")
    private MetadataSourceSummary metadataSource;

    @JsonProperty("profiles")
    private List<ProfileSummary> profiles;

    @JsonProperty("released_attributes")
    private List<ReleasedAttributeDto> releasedAttributes;

    @JsonProperty("activation_diagnostics")
    private ActivationDiagnosticsDto activationDiagnostics;

    @JsonProperty("discovered_entity_ids")
    private List<String> discoveredEntityIds;

    public TrustRelationshipDetail() {
    }

    public UUID getId() {

        return id;
    }

    public void setId(UUID id) {

        this.id = id;
    }

    public String getDisplayName() {

        return displayName;
    }

    public void setDisplayName(String displayName) {

        this.displayName = displayName;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public TrustNature getNature() {

        return nature;
    }

    public void setNature(TrustNature nature) {

        this.nature = nature;
    }

    public TrustStatus getStatus() {

        return status;
    }

    public void setStatus(TrustStatus status) {

        this.status = status;
    }

    public int getVersion() {

        return version;
    }

    public void setVersion(int version) {

        this.version = version;
    }

    public MetadataSourceSummary getMetadataSource() {

        return metadataSource;
    }

    public void setMetadataSource(MetadataSourceSummary metadataSource) {

        this.metadataSource = metadataSource;
    }

    public List<ProfileSummary> getProfiles() {

        return profiles;
    }

    public void setProfiles(List<ProfileSummary> profiles) {

        this.profiles = profiles;
    }

    public List<ReleasedAttributeDto> getReleasedAttributes() {

        return releasedAttributes;
    }

    public void setReleasedAttributes(List<ReleasedAttributeDto> releasedAttributes) {

        this.releasedAttributes = releasedAttributes;
    }

    public ActivationDiagnosticsDto getActivationDiagnostics() {

        return activationDiagnostics;
    }

    public void setActivationDiagnostics(ActivationDiagnosticsDto activationDiagnostics) {

        this.activationDiagnostics = activationDiagnostics;
    }

    public List<String> getDiscoveredEntityIds() {

        return discoveredEntityIds;
    }

    public void setDiscoveredEntityIds(List<String> discoveredEntityIds) {

        this.discoveredEntityIds = discoveredEntityIds;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrustRelationshipDetail that = (TrustRelationshipDetail) o;
        return version == that.version
            && Objects.equals(id, that.id)
            && Objects.equals(displayName, that.displayName)
            && Objects.equals(description, that.description)
            && nature == that.nature
            && status == that.status
            && Objects.equals(metadataSource, that.metadataSource)
            && Objects.equals(profiles, that.profiles)
            && Objects.equals(releasedAttributes, that.releasedAttributes)
            && Objects.equals(activationDiagnostics, that.activationDiagnostics)
            && Objects.equals(discoveredEntityIds, that.discoveredEntityIds);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, displayName, description, nature, status, version,
            metadataSource, profiles, releasedAttributes, activationDiagnostics, discoveredEntityIds);
    }
}
