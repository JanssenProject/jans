/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.jans.orm.model.base.Entry;
import io.jans.as.model.common.*;
import io.jans.configapi.plugin.saml.model.*;
import io.jans.model.GluuStatus;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.as.persistence.model.ClientAttributes;
import io.jans.orm.annotation.*;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.model.base.DeletableEntity;
import io.jans.orm.model.base.LocalizedString;
import org.apache.commons.lang.StringUtils;

import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.*;

@DataEntry(sortBy = {"displayName"})
@ObjectClass(value = "jansSAMLconfig")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JansTrustRelationship extends Entry implements Serializable{
    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @NotNull
    @Size(min = 0, max = 60, message = "Length of the Display Name should not exceed 60")
    @AttributeName
    private String displayName;

    @NotNull
    @Size(min = 0, max = 4000, message = "Length of the Description should not exceed 4000")
    @AttributeName
    private String description;

    @AttributeName(name = "jansStatus")
    private GluuStatus status;

    @AttributeName(name = "jansValidationStatus")
    private ValidationStatus validationStatus;

    @AttributeName(name = "jansReleasedAttr")
    private List<String> releasedAttributes;

    @NotNull
    @AttributeName(name = "jansSAMLspMetaDataSourceTyp")
    private MetadataSourceType spMetaDataSourceType;

    @AttributeName(name = "o")
    private String owner;

    @Transient
    private transient List<CustomAttribute> releasedCustomAttributes = new ArrayList<CustomAttribute>();

    private Map<String, MetadataFilter> metadataFilters = new HashMap<String, MetadataFilter>();

    private Map<String, ProfileConfiguration> profileConfigurations = new HashMap<String, ProfileConfiguration>();

    @AttributeName(name = "jansSAMLMetaDataFilter")
    private List<String> jansSAMLMetaDataFilter;


    @AttributeName(name = "jansIsFed")
    private String jansIsFed;

    @AttributeName(name = "jansEntityId")
    private List<String> jansEntityId;

    @AttributeName(name = "jansProfileConf")
    private List<String> jansProfileConf;

    @Pattern(regexp = "^(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", message = "Please enter a valid SP url, including protocol (http/https)")
    @AttributeName(name = "url")
    private String url;

    @Pattern(regexp = "^$|(^(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])", message = "Please enter a valid url, including protocol (http/https)")
    @AttributeName(name = "jansPostLogoutRedirectURI")
    private String spLogoutURL;

    @AttributeName(name = "jansValidationLog")
    private List<String> validationLog;

    @AttributeName(name = "jansEntityTyp")
    private EntityType entityType;
    
    

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

    public GluuStatus getStatus() {
        return status;
    }

    public void setStatus(GluuStatus status) {
        this.status = status;
    }

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public List<String> getReleasedAttributes() {
        return releasedAttributes;
    }

    public void setReleasedAttributes(List<String> releasedAttributes) {
        this.releasedAttributes = releasedAttributes;
    }

    public MetadataSourceType getSpMetaDataSourceType() {
        return spMetaDataSourceType;
    }

    public void setSpMetaDataSourceType(MetadataSourceType spMetaDataSourceType) {
        this.spMetaDataSourceType = spMetaDataSourceType;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<CustomAttribute> getReleasedCustomAttributes() {
        return releasedCustomAttributes;
    }

    public void setReleasedCustomAttributes(List<CustomAttribute> releasedCustomAttributes) {
        this.releasedCustomAttributes = releasedCustomAttributes;
    }

    public Map<String, MetadataFilter> getMetadataFilters() {
        return metadataFilters;
    }

    public void setMetadataFilters(Map<String, MetadataFilter> metadataFilters) {
        this.metadataFilters = metadataFilters;
    }

    public Map<String, ProfileConfiguration> getProfileConfigurations() {
        return profileConfigurations;
    }

    public void setProfileConfigurations(Map<String, ProfileConfiguration> profileConfigurations) {
        this.profileConfigurations = profileConfigurations;
    }

    public List<String> getJansSAMLMetaDataFilter() {
        return jansSAMLMetaDataFilter;
    }

    public void setJansSAMLMetaDataFilter(List<String> jansSAMLMetaDataFilter) {
        this.jansSAMLMetaDataFilter = jansSAMLMetaDataFilter;
    }

    public String getJansIsFed() {
        return jansIsFed;
    }

    public void setJansIsFed(String jansIsFed) {
        this.jansIsFed = jansIsFed;
    }

    public List<String> getJansEntityId() {
        return jansEntityId;
    }

    public void setJansEntityId(List<String> jansEntityId) {
        this.jansEntityId = jansEntityId;
    }

    public List<String> getJansProfileConf() {
        return jansProfileConf;
    }

    public void setJansProfileConf(List<String> jansProfileConf) {
        this.jansProfileConf = jansProfileConf;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSpLogoutURL() {
        return spLogoutURL;
    }

    public void setSpLogoutURL(String spLogoutURL) {
        this.spLogoutURL = spLogoutURL;
    }

    public List<String> getValidationLog() {
        return validationLog;
    }

    public void setValidationLog(List<String> validationLog) {
        this.validationLog = validationLog;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    private static class SortByDatasourceTypeComparator implements Comparator<JansTrustRelationship> {

        public int compare(JansTrustRelationship first, JansTrustRelationship second) {

            return first.getSpMetaDataSourceType().getRank() - second.getSpMetaDataSourceType().getRank();
        }
    }

    public static void sortByDataSourceType(List<JansTrustRelationship> trustRelationships) {
        Collections.sort(trustRelationships,new SortByDatasourceTypeComparator());
    }
}
