/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model.config;

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
@ObjectClass(value = "jansSAMLTrustConfig")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SAMLTrustRelationship extends Entry implements Serializable{
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

    @AttributeName(name = "gluuStatus")
    private GluuStatus status;

    @AttributeName(name = "ValidationStatus")
    private ValidationStatus validationStatus;

    @AttributeName(name = "gluuReleasedAttribute")
    private List<String> releasedAttributes;

    @NotNull
    @AttributeName(name = "gluuSAMLspMetaDataSourceType")
    private MetadataSourceType spMetaDataSourceType;

    @AttributeName(name = "gluuSAMLspMetaDataFN")
    private String spMetaDataFN;

    @AttributeName(name = "gluuSAMLspMetaDataURL")
    private String spMetaDataURL;

    @AttributeName(name = "o")
    private String owner;

    @AttributeName(name = "gluuSAMLmaxRefreshDelay")
    private String maxRefreshDelay;

    @Transient
    private transient List<CustomAttribute> releasedCustomAttributes = new ArrayList<CustomAttribute>();

    private Map<String, MetadataFilter> metadataFilters = new HashMap<String, MetadataFilter>();

    private Map<String, ProfileConfiguration> profileConfigurations = new HashMap<String, ProfileConfiguration>();

    @AttributeName(name = "gluuSAMLMetaDataFilter")
    private List<String> gluuSAMLMetaDataFilter;

    @AttributeName(name = "gluuTrustContact")
    private List<String> gluuTrustContact;

    @AttributeName(name = "gluuTrustDeconstruction")
    private List<String> gluuTrustDeconstruction;

    @AttributeName(name = "gluuContainerFederation")
    protected String gluuContainerFederation;

    @AttributeName(name = "gluuIsFederation")
    private String gluuIsFederation;

    @AttributeName(name = "gluuEntityId")
    private List<String> gluuEntityId;

    @AttributeName(name = "gluuProfileConfiguration")
    private List<String> gluuProfileConfiguration;

    @AttributeName(name = "gluuSpecificRelyingPartyConfig")
    private String gluuSpecificRelyingPartyConfig;

    @Pattern(regexp = "^(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", message = "Please enter a valid SP url, including protocol (http/https)")
    @AttributeName(name = "url")
    private String url;

    @Pattern(regexp = "^$|(^(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])", message = "Please enter a valid url, including protocol (http/https)")
    @AttributeName(name = "oxAuthPostLogoutRedirectURI")
    private String spLogoutURL;

    @AttributeName(name = "gluuValidationLog")
    private List<String> validationLog;

    @AttributeName(name = "researchAndScholarshipEnabled")
    private String researchBundleEnabled;

    @AttributeName(name = "EntityType")
    private EntityType entityType;
    
    private String metadataStr;
    
    private String certificate;


    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getMetadataStr() {
        return metadataStr;
    }

    public void setMetadataStr(String metadataStr) {
        this.metadataStr = metadataStr;
    }

    public void setFederation(boolean isFederation) {
        this.gluuIsFederation = Boolean.toString(isFederation);
    }

    public boolean isFederation() {
        return Boolean.parseBoolean(gluuIsFederation);
    }

    public void setContainerFederation(SAMLTrustRelationship containerFederation) {
        this.gluuContainerFederation = containerFederation.getDn();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SAMLTrustRelationship)) {
            return false;
        }

        if (getInum() == null) {
            return getInum() == ((SAMLTrustRelationship) o).getInum();
        }

        return getInum().equals(((SAMLTrustRelationship) o).getInum());
    }

    public List<String> getGluuEntityId() {
        return gluuEntityId;
    }

    /*public void setGluuEntityId(Set<String> gluuEntityId) {
        this.gluuEntityId = new ArrayList<String>(gluuEntityId);
    }*/
    public void setUniqueGluuEntityId(Set<String> gluuEntityId) {
        this.gluuEntityId = new ArrayList<String>(gluuEntityId);
    }

    
    @Deprecated
    public void setGluuEntityId(List<String> gluuEntityId) {
        this.gluuEntityId = gluuEntityId;
    }

    
    public String getEntityId() {
        if (this.gluuEntityId != null && !this.gluuEntityId.isEmpty()) {
            return this.gluuEntityId.get(0);
        }
        return "";
    }

    public void setEntityId(String entityId) {
        Set<String> entityIds = new TreeSet<String>();
        if (entityId != null) {
            entityIds.add(entityId);
        }
        setUniqueGluuEntityId(entityIds);
    }

    public void setSpecificRelyingPartyConfig(boolean specificRelyingPartyConfig) {
        this.gluuSpecificRelyingPartyConfig = Boolean.toString(specificRelyingPartyConfig);
    }

    public boolean getSpecificRelyingPartyConfig() {
        return Boolean.parseBoolean(gluuSpecificRelyingPartyConfig);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGluuContainerFederation() {
        return this.gluuContainerFederation;
    }

    public void setGluuContainerFederation(String gluuContainerFederation) {
        this.gluuContainerFederation = gluuContainerFederation;
    }

    public String getGluuIsFederation() {
        return gluuIsFederation;
    }

    public void setGluuIsFederation(String gluuIsFederation) {
        this.gluuIsFederation = gluuIsFederation;
    }

    public List<String> getGluuProfileConfiguration() {
        return gluuProfileConfiguration;
    }

    public void setGluuProfileConfiguration(List<String> gluuProfileConfiguration) {
        this.gluuProfileConfiguration = gluuProfileConfiguration;
    }

    public List<String> getGluuSAMLMetaDataFilter() {
        return gluuSAMLMetaDataFilter;
    }

    public void setGluuSAMLMetaDataFilter(List<String> gluuSAMLMetaDataFilter) {
        this.gluuSAMLMetaDataFilter = gluuSAMLMetaDataFilter;
    }

    public String getGluuSpecificRelyingPartyConfig() {
        return gluuSpecificRelyingPartyConfig;
    }

    public void setGluuSpecificRelyingPartyConfig(String gluuSpecificRelyingPartyConfig) {
        this.gluuSpecificRelyingPartyConfig = gluuSpecificRelyingPartyConfig;
    }

    public List<String> getGluuTrustContact() {
        return gluuTrustContact;
    }

    public void setGluuTrustContact(List<String> gluuTrustContact) {
        this.gluuTrustContact = gluuTrustContact;
    }

    public List<String> getGluuTrustDeconstruction() {
        return gluuTrustDeconstruction;
    }

    public void setGluuTrustDeconstruction(List<String> gluuTrustDeconstruction) {
        this.gluuTrustDeconstruction = gluuTrustDeconstruction;
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getMaxRefreshDelay() {
        return maxRefreshDelay;
    }

    public void setMaxRefreshDelay(String maxRefreshDelay) {
        this.maxRefreshDelay = maxRefreshDelay;
    }

    public Map<String, MetadataFilter> getMetadataFilters() {
        return metadataFilters;
    }

    public void setMetadataFilters(Map<String, MetadataFilter> metadataFilters) {
        this.metadataFilters = metadataFilters;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Map<String, ProfileConfiguration> getProfileConfigurations() {
        return profileConfigurations;
    }

    public void setProfileConfigurations(Map<String, ProfileConfiguration> profileConfigurations) {
        this.profileConfigurations = profileConfigurations;
    }

    public List<String> getReleasedAttributes() {
        return releasedAttributes;
    }

    public void setReleasedAttributes(List<String> releasedAttributes) {
        this.releasedAttributes = releasedAttributes;
    }

    public List<CustomAttribute> getReleasedCustomAttributes() {
        return releasedCustomAttributes;
    }

    public void setReleasedCustomAttributes(List<CustomAttribute> releasedCustomAttributes) {
        this.releasedCustomAttributes = releasedCustomAttributes;
    }

    public String getSpLogoutURL() {
        return spLogoutURL;
    }

    public void setSpLogoutURL(String spLogoutURL) {
        this.spLogoutURL = spLogoutURL;
    }

    public String getSpMetaDataFN() {
        return spMetaDataFN;
    }

    public void setSpMetaDataFN(String spMetaDataFN) {
        this.spMetaDataFN = spMetaDataFN;
    }

    public MetadataSourceType getSpMetaDataSourceType() {
        return spMetaDataSourceType;
    }

    public void setSpMetaDataSourceType(MetadataSourceType spMetaDataSourceType) {
        this.spMetaDataSourceType = spMetaDataSourceType;
    }

    public String getSpMetaDataURL() {
        return spMetaDataURL;
    }

    public void setSpMetaDataURL(String spMetaDataURL) {
        this.spMetaDataURL = spMetaDataURL;
    }

    public GluuStatus getStatus() {
        return status;
    }

    public void setStatus(GluuStatus status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getValidationLog() {
        return validationLog;
    }

    public void setValidationLog(List<String> validationLog) {
        this.validationLog = validationLog;
    }

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getResearchBundleEnabled() {
        return researchBundleEnabled;
    }

    public void setResearchBundleEnabled(String researchBundleEnabled) {
        this.researchBundleEnabled = researchBundleEnabled;
    }

    public boolean isResearchBundle() {
        return Boolean.parseBoolean(researchBundleEnabled);
    }

    public boolean getResearchBundle() {
        return Boolean.parseBoolean(researchBundleEnabled);
    }

    public void setResearchBundle(boolean researchBundle) {
        this.researchBundleEnabled = Boolean.toString(researchBundle);
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public boolean entityTypeIsFederation() {

        return (this.entityType == EntityType.FederationAggregate);
    }

    public boolean entityTypeIsSingleSp() {

        return (this.entityType == EntityType.SingleSP);
    }

    public boolean isFileMetadataSourceType() {

        return (this.spMetaDataSourceType == MetadataSourceType.FILE);
    }

    public boolean isUriMetadataSourceType() {

        return (this.spMetaDataSourceType == MetadataSourceType.URI);
    }

    public boolean isMdqMetadataSourceType() {

        return (this.spMetaDataSourceType == MetadataSourceType.MDQ);
    }

    public boolean isMdqFederation() {

        return (this.entityType == EntityType.FederationAggregate) && (this.spMetaDataSourceType == MetadataSourceType.MDQ);
    }

    private static class SortByDatasourceTypeComparator implements Comparator<SAMLTrustRelationship> {

        public int compare(SAMLTrustRelationship first, SAMLTrustRelationship second) {

            return first.getSpMetaDataSourceType().getRank() - second.getSpMetaDataSourceType().getRank();
        }
    }

    public static void sortByDataSourceType(List<SAMLTrustRelationship> trustRelationships) {
        Collections.sort(trustRelationships,new SortByDatasourceTypeComparator());
    }
}
