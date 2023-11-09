/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.keycloak.idp.broker.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.jans.model.GluuStatus;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;
import io.jans.configapi.core.model.ValidationStatus;
import io.swagger.v3.oas.annotations.Hidden;


import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansTrustedIdp")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdentityProvider extends Entry implements Serializable {

    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @NotNull
    @AttributeName
    private String creatorId;

    @NotNull
    @AttributeName(name = "name")
    private String name;

    @NotNull
    @Size(min = 0, max = 60, message = "Length of the Display Name should not exceed 60")
    @AttributeName
    private String displayName;

    @NotNull
    @Size(min = 0, max = 500, message = "Length of the Description should not exceed 500")
    @AttributeName
    private String description;
        
    @NotNull
    @AttributeName(name = "realm")
    private String realm;

    @AttributeName(name = "jansEnabled")
    private boolean enabled;

    @AttributeName
    private String providerId;

   @AttributeName
    protected boolean trustEmail;

    @AttributeName
    protected boolean storeToken;

    @AttributeName
    protected boolean addReadTokenRoleOnCreate;

    @AttributeName
    protected boolean authenticateByDefault;

    @AttributeName
    protected boolean linkOnly;

    @AttributeName
    protected String firstBrokerLoginFlowAlias;

    @AttributeName
    protected String postBrokerLoginFlowAlias;

    @AttributeName(name = "jansSAMLspMetaDataFN")
    @Hidden
    private String spMetaDataFN;

    @AttributeName(name = "jansSAMLspMetaDataURL")
    private String spMetaDataURL;
    
    @AttributeName(name = "jansSAMLspMetaLocation")
    private String spMetaDataLocation;

    @AttributeName(name = "jansSAMLidpMetaDataFN")
    @Hidden
    private String idpMetaDataFN;

    @AttributeName(name = "jansSAMLidpMetaDataURL")
    private String idpMetaDataURL;
    
    @AttributeName(name = "jansSAMLidpMetaLocation")
    private String idpMetaDataLocation;

    @AttributeName(name = "jansStatus")
    private GluuStatus status;

    @AttributeName(name = "jansValidationStatus")
    private ValidationStatus validationStatus;

    @AttributeName(name = "jansValidationLog")
    private List<String> validationLog;
    
    @JsonObject
    @AttributeName(name = "jansSAMLidpConfDyn")
    Map<String, String> config = new HashMap<>();

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isTrustEmail() {
        return trustEmail;
    }

    public void setTrustEmail(boolean trustEmail) {
        this.trustEmail = trustEmail;
    }

    public boolean isStoreToken() {
        return storeToken;
    }

    public void setStoreToken(boolean storeToken) {
        this.storeToken = storeToken;
    }

    public boolean isAddReadTokenRoleOnCreate() {
        return addReadTokenRoleOnCreate;
    }

    public void setAddReadTokenRoleOnCreate(boolean addReadTokenRoleOnCreate) {
        this.addReadTokenRoleOnCreate = addReadTokenRoleOnCreate;
    }

    public boolean isAuthenticateByDefault() {
        return authenticateByDefault;
    }

    public void setAuthenticateByDefault(boolean authenticateByDefault) {
        this.authenticateByDefault = authenticateByDefault;
    }

    public boolean isLinkOnly() {
        return linkOnly;
    }

    public void setLinkOnly(boolean linkOnly) {
        this.linkOnly = linkOnly;
    }

    public String getFirstBrokerLoginFlowAlias() {
        return firstBrokerLoginFlowAlias;
    }

    public void setFirstBrokerLoginFlowAlias(String firstBrokerLoginFlowAlias) {
        this.firstBrokerLoginFlowAlias = firstBrokerLoginFlowAlias;
    }

    public String getPostBrokerLoginFlowAlias() {
        return postBrokerLoginFlowAlias;
    }

    public void setPostBrokerLoginFlowAlias(String postBrokerLoginFlowAlias) {
        this.postBrokerLoginFlowAlias = postBrokerLoginFlowAlias;
    }

    public String getSpMetaDataFN() {
        return spMetaDataFN;
    }

    public void setSpMetaDataFN(String spMetaDataFN) {
        this.spMetaDataFN = spMetaDataFN;
    }

    public String getSpMetaDataURL() {
        return spMetaDataURL;
    }

    public void setSpMetaDataURL(String spMetaDataURL) {
        this.spMetaDataURL = spMetaDataURL;
    }

    public String getSpMetaDataLocation() {
        return spMetaDataLocation;
    }

    public void setSpMetaDataLocation(String spMetaDataLocation) {
        this.spMetaDataLocation = spMetaDataLocation;
    }

    public String getIdpMetaDataFN() {
        return idpMetaDataFN;
    }

    public void setIdpMetaDataFN(String idpMetaDataFN) {
        this.idpMetaDataFN = idpMetaDataFN;
    }

    public String getIdpMetaDataURL() {
        return idpMetaDataURL;
    }

    public void setIdpMetaDataURL(String idpMetaDataURL) {
        this.idpMetaDataURL = idpMetaDataURL;
    }

    public String getIdpMetaDataLocation() {
        return idpMetaDataLocation;
    }

    public void setIdpMetaDataLocation(String idpMetaDataLocation) {
        this.idpMetaDataLocation = idpMetaDataLocation;
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

    public List<String> getValidationLog() {
        return validationLog;
    }

    public void setValidationLog(List<String> validationLog) {
        this.validationLog = validationLog;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "IdentityProvider [inum=" + inum + ", creatorId=" + creatorId + ", name=" + name + ", displayName="
                + displayName + ", description=" + description + ", realm=" + realm + ", enabled=" + enabled
                + ", providerId=" + providerId + ", trustEmail=" + trustEmail + ", storeToken=" + storeToken
                + ", addReadTokenRoleOnCreate=" + addReadTokenRoleOnCreate + ", authenticateByDefault="
                + authenticateByDefault + ", linkOnly=" + linkOnly + ", firstBrokerLoginFlowAlias="
                + firstBrokerLoginFlowAlias + ", postBrokerLoginFlowAlias=" + postBrokerLoginFlowAlias
                + ", spMetaDataFN=" + spMetaDataFN + ", spMetaDataURL=" + spMetaDataURL + ", spMetaDataLocation="
                + spMetaDataLocation + ", idpMetaDataFN=" + idpMetaDataFN + ", idpMetaDataURL=" + idpMetaDataURL
                + ", idpMetaDataLocation=" + idpMetaDataLocation + ", status=" + status + ", validationStatus="
                + validationStatus + ", validationLog=" + validationLog + ", config=" + config + "]";
    }

      
}
