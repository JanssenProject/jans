package io.jans.shibboleth.trust.persistence.config;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

/**
 * Reduced-attribute projection entry over the same {@code jansTrustRelationship} object class as
 * {@link TrustRelationshipEntry}, declaring only the summary columns (TP10). Listing loads these — the
 * {@code @JsonObject} blobs (metadata source, profiles, released attributes, diagnostics) are never
 * fetched or deserialized. Read-only; the repository maps it straight to the view summary DTO.
 */
@DataEntry(sortBy = "displayName", sortByName = "displayName")
@ObjectClass("jansTrustRelationship")
public class TrustRelationshipSummaryEntry extends BaseEntry {

    private static final long serialVersionUID = 1L;
    
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
}
