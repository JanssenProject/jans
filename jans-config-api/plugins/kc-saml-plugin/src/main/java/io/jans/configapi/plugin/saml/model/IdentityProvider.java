/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.jans.model.GluuStatus;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.List;
import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansTrustedIdp")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
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
    
    @AttributeName(name = "signingCertificate")
    private String signingCertificate;
    
    @AttributeName(name = "validateSignature")
    private String validateSignature;
       
    @AttributeName(name = "singleLogoutServiceUrl")
    private String singleLogoutServiceUrl;  
    
    @AttributeName(name = "nameIDPolicyFormat")
    private String nameIDPolicyFormat;
    
    @AttributeName(name = "principalAttribute")
    private String principalAttribute;
    
    @AttributeName(name = "principalType")
    private String principalType;
    
    @AttributeName(name = "entityId")
    private String idpEntityId;
    
    @AttributeName(name = "singleSignOnServiceUrl")
    private String singleSignOnServiceUrl;
    
    @AttributeName(name = "encryptionPublicKey")
    private String encryptionPublicKey;
    
    @AttributeName
    private String providerId;

   @AttributeName
    private boolean trustEmail;

    @AttributeName
    private boolean storeToken;

    @AttributeName
    private boolean addReadTokenRoleOnCreate;

    @AttributeName
    private boolean authenticateByDefault;

    @AttributeName
    private boolean linkOnly;

    @AttributeName
    private String firstBrokerLoginFlowAlias;

    @AttributeName
    private String postBrokerLoginFlowAlias;

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

    public String getSigningCertificate() {
        return signingCertificate;
    }

    public void setSigningCertificate(String signingCertificate) {
        this.signingCertificate = signingCertificate;
    }

    public String getValidateSignature() {
        return validateSignature;
    }

    public void setValidateSignature(String validateSignature) {
        this.validateSignature = validateSignature;
    }

    public String getSingleLogoutServiceUrl() {
        return singleLogoutServiceUrl;
    }

    public void setSingleLogoutServiceUrl(String singleLogoutServiceUrl) {
        this.singleLogoutServiceUrl = singleLogoutServiceUrl;
    }

    public String getNameIDPolicyFormat() {
        return nameIDPolicyFormat;
    }

    public void setNameIDPolicyFormat(String nameIDPolicyFormat) {
        this.nameIDPolicyFormat = nameIDPolicyFormat;
    }

    public String getPrincipalAttribute() {
        return principalAttribute;
    }

    public void setPrincipalAttribute(String principalAttribute) {
        this.principalAttribute = principalAttribute;
    }

    public String getPrincipalType() {
        return principalType;
    }

    public void setPrincipalType(String principalType) {
        this.principalType = principalType;
    }

    public String getIdpEntityId() {
        return idpEntityId;
    }

    public void setIdpEntityId(String idpEntityId) {
        this.idpEntityId = idpEntityId;
    }

    public String getSingleSignOnServiceUrl() {
        return singleSignOnServiceUrl;
    }

    public void setSingleSignOnServiceUrl(String singleSignOnServiceUrl) {
        this.singleSignOnServiceUrl = singleSignOnServiceUrl;
    }

    public String getEncryptionPublicKey() {
        return encryptionPublicKey;
    }

    public void setEncryptionPublicKey(String encryptionPublicKey) {
        this.encryptionPublicKey = encryptionPublicKey;
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

    @Override
    public String toString() {
        return "IdentityProvider [inum=" + inum + ", creatorId=" + creatorId + ", name=" + name + ", displayName="
                + displayName + ", description=" + description + ", realm=" + realm + ", enabled=" + enabled
                + ", signingCertificate=" + signingCertificate + ", validateSignature=" + validateSignature
                + ", singleLogoutServiceUrl=" + singleLogoutServiceUrl + ", nameIDPolicyFormat=" + nameIDPolicyFormat
                + ", principalAttribute=" + principalAttribute + ", principalType=" + principalType + ", idpEntityId="
                + idpEntityId + ", singleSignOnServiceUrl=" + singleSignOnServiceUrl + ", encryptionPublicKey="
                + encryptionPublicKey + ", providerId=" + providerId + ", trustEmail=" + trustEmail + ", storeToken="
                + storeToken + ", addReadTokenRoleOnCreate=" + addReadTokenRoleOnCreate + ", authenticateByDefault="
                + authenticateByDefault + ", linkOnly=" + linkOnly + ", firstBrokerLoginFlowAlias="
                + firstBrokerLoginFlowAlias + ", postBrokerLoginFlowAlias=" + postBrokerLoginFlowAlias
                + ", spMetaDataFN=" + spMetaDataFN + ", spMetaDataURL=" + spMetaDataURL + ", spMetaDataLocation="
                + spMetaDataLocation + ", idpMetaDataFN=" + idpMetaDataFN + ", idpMetaDataURL=" + idpMetaDataURL
                + ", idpMetaDataLocation=" + idpMetaDataLocation + ", status=" + status + ", validationStatus="
                + validationStatus + ", validationLog=" + validationLog + "]";
    }   
      
}
