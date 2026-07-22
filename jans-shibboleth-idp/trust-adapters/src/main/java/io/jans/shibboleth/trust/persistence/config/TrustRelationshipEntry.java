package io.jans.shibboleth.trust.persistence.config;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import io.jans.shibboleth.trust.persistence.config.payload.ActivationDiagnosticsPayload;
import io.jans.shibboleth.trust.persistence.config.payload.MetadataSourcePayload;
import io.jans.shibboleth.trust.persistence.config.payload.ProfilesPayload;
import io.jans.shibboleth.trust.persistence.config.payload.ReleasedAttributePayload;

import java.util.List;

/**
 * jans-orm storage entry for a {@code TrustRelationship}. Object class {@code jansTrustRelationship},
 * under {@code ou=trust-relationships,o=jans}. The primary key is the DN ({@code @DN}, inherited from
 * {@link BaseEntry}), formed as {@code inum=<uuid>,ou=trust-relationships,o=jans}; {@code inum} is the
 * stable id attribute (part of the DN, so never updated).
 *
 * <p>Holds the queryable flat columns, the multi-valued {@code jansEntityId}, and the {@code @JsonObject}
 * payloads for the metadata source, the six profile configurations, released attributes and activation
 * diagnostics.
 */
@DataEntry(sortBy = "displayName", sortByName = "displayName")
@ObjectClass("jansTrustRelationship")
public class TrustRelationshipEntry extends BaseEntry {

    @AttributeName(name = "inum", ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "displayName")
    private String displayName;

    @AttributeName(name = "description")
    private String description;

    @AttributeName(name = "jansTrustNature")
    private String nature;

    @AttributeName(name = "jansTrustStatus")
    private String status;

    @AttributeName(name = "jansTrustVer")
    private Integer version;

    @AttributeName(name = "jansEntityId")
    private List<String> discoveredEntityIds;

    @JsonObject
    @AttributeName(name = "jansMetadataSrc")
    private MetadataSourcePayload metadataSource;

    @JsonObject
    @AttributeName(name = "jansProfiles")
    private ProfilesPayload profiles;

    @JsonObject
    @AttributeName(name = "jansReleasedAttr")
    private List<ReleasedAttributePayload> releasedAttributes;

    @JsonObject
    @AttributeName(name = "jansActivationDiag")
    private ActivationDiagnosticsPayload activationDiagnostics;

    public String getInum() {

        return inum;
    }

    public void setInum(String inum) {

        this.inum = inum;
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

    public String getNature() {

        return nature;
    }

    public void setNature(String nature) {

        this.nature = nature;
    }

    public String getStatus() {

        return status;
    }

    public void setStatus(String status) {

        this.status = status;
    }

    public Integer getVersion() {

        return version;
    }

    public void setVersion(Integer version) {

        this.version = version;
    }

    public List<String> getDiscoveredEntityIds() {

        return discoveredEntityIds;
    }

    public void setDiscoveredEntityIds(List<String> discoveredEntityIds) {

        this.discoveredEntityIds = discoveredEntityIds;
    }

    public MetadataSourcePayload getMetadataSource() {

        return metadataSource;
    }

    public void setMetadataSource(MetadataSourcePayload metadataSource) {

        this.metadataSource = metadataSource;
    }

    public ProfilesPayload getProfiles() {

        return profiles;
    }

    public void setProfiles(ProfilesPayload profiles) {

        this.profiles = profiles;
    }

    public List<ReleasedAttributePayload> getReleasedAttributes() {

        return releasedAttributes;
    }

    public void setReleasedAttributes(List<ReleasedAttributePayload> releasedAttributes) {

        this.releasedAttributes = releasedAttributes;
    }

    public ActivationDiagnosticsPayload getActivationDiagnostics() {

        return activationDiagnostics;
    }

    public void setActivationDiagnostics(ActivationDiagnosticsPayload activationDiagnostics) {

        this.activationDiagnostics = activationDiagnostics;
    }
}
