package io.jans.shibboleth.trust.persistence.config;

import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.BaseEntry;

import io.jans.shibboleth.trust.persistence.config.payload.MetadataSourcePayload;

/**
 * jans-orm storage entry for a {@code TrustRelationship}. Object class {@code jansTrustRelationship},
 * under {@code ou=trust-relationships,o=jans}; the DN (via {@link BaseEntry}) carries {@code jansId}.
 *
 * <p>This slice holds the queryable flat columns only (id, display name, description, nature, status,
 * version); the {@code @JsonObject} payloads for metadata source, profiles, released attributes and
 * activation diagnostics are added as the mapper's round-trip matrix widens.
 */
@DataEntry(sortBy = "displayName", sortByName = "displayName")
@ObjectClass("jansTrustRelationship")
public class TrustRelationshipEntry extends BaseEntry {

    @AttributeName(name = "jansId", consistency = true)
    private String id;

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

    @JsonObject
    @AttributeName(name = "jansMetadataSrc")
    private MetadataSourcePayload metadataSource;

    public String getId() {

        return id;
    }

    public void setId(String id) {

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

    public MetadataSourcePayload getMetadataSource() {

        return metadataSource;
    }

    public void setMetadataSource(MetadataSourcePayload metadataSource) {

        this.metadataSource = metadataSource;
    }
}
