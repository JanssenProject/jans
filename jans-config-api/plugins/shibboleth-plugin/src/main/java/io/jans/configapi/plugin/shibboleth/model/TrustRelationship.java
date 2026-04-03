package io.jans.configapi.plugin.shibboleth.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;


@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansTrustRelationship")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrustRelationship extends Entry implements Serializable {

    private static final long serialVersionUID = 1L;

    @AttributeName(ignoreDuringUpdate = true)
    @Schema(description = "Unique identifier")
    private String inum;
    
    @NotNull
    @Size(min = 0, max = 60, message = "Length of the display name should not exceed 60")
    @AttributeName
    @Schema(description = "Trust Relationship display name.")
    private String displayName;
    
    @Size(min = 0, max = 4000, message = "Length of the Description should not exceed 4000")
    @AttributeName
    @Schema(description = "Description of the Trust Relationship.")
    private String description;
    
    @NotNull
    @AttributeName
    @Schema(description = "Nature of Trust Relationship.")    
    private EntityType entityType;
    
    @NotNull
    @AttributeName
    @Schema(description = "Metadata file source for Trust Relationship.")  
    private MetadataSource metadataSource;
    
    @AttributeName(name = "jansReleasedAttr")
    @Schema(description = "Trust Relationship attributes that will be released to SAML server.")
    private List<String> releasedAttributes;
    
    @AttributeName
    @Schema(description = "Trust Relationship version")
    private String version;
    
    @AttributeName
    @Schema(description = "Trust Relationship last synced version")
    private String lastSyncedVersion;

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

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public MetadataSource getMetadataSource() {
        return metadataSource;
    }

    public void setMetadataSource(MetadataSource metadataSource) {
        this.metadataSource = metadataSource;
    }

    public List<String> getReleasedAttributes() {
        return releasedAttributes;
    }

    public void setReleasedAttributes(List<String> releasedAttributes) {
        this.releasedAttributes = releasedAttributes;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLastSyncedVersion() {
        return lastSyncedVersion;
    }

    public void setLastSyncedVersion(String lastSyncedVersion) {
        this.lastSyncedVersion = lastSyncedVersion;
    }

    @Override
    public String toString() {
        return "TrustRelationship [inum=" + inum + ", displayName=" + displayName + ", description=" + description
                + ", entityType=" + entityType + ", metadataSource=" + metadataSource + ", releasedAttributes="
                + releasedAttributes + ", version=" + version + ", lastSyncedVersion=" + lastSyncedVersion + "]";
    }
    
}
