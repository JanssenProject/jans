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

import io.swagger.v3.oas.annotations.media.Schema;

@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansTrustedIdp")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentityProvider extends Entry implements Serializable {

    @AttributeName(ignoreDuringUpdate = true)
    @Schema(description = "Unique identifier.")
    private String inum;

    @NotNull
    @AttributeName
    @Schema(description = "Creator of IDP.")
    private String creatorId;

    @NotNull
    @AttributeName(name = "name")
    @Schema(description = "Name uniquely identifies an identity provider.")
    private String name;

    @NotNull
    @Size(min = 0, max = 60, message = "Length of the Display Name should not exceed 60")
    @AttributeName
    @Schema(description = "Identity provider display name.")
    private String displayName;

    @NotNull
    @Size(min = 0, max = 500, message = "Length of the Description should not exceed 500")
    @AttributeName
    @Schema(description = "Description of Identity provider.")
    private String description;
        
    @NotNull
    @AttributeName(name = "realm")
    @Schema(description = "Realm in which Identity provider is created.")
    private String realm;

    @AttributeName(name = "jansEnabled")
    @Schema(description = "Indicates if Identity provider is enabled.")
    private boolean enabled;
    
    @AttributeName(name = "signingCertificate")
    @Schema(description = "Digital certificate used to verify the authenticity of the request.")
    private String signingCertificate;
    
    @AttributeName(name = "validateSignature")
    private String validateSignature;
       
    @AttributeName(name = "singleLogoutServiceUrl")
    @Schema(description = "Url used to send logout requests.")
    private String singleLogoutServiceUrl;  
    
    @AttributeName(name = "nameIDPolicyFormat")
    @Schema(description = " URI reference corresponding to a name identifier format.")
    private String nameIDPolicyFormat;
    
    @AttributeName(name = "principalAttribute")
    @Schema(description = " Name or Friendly Name of the attribute used to identify external users.")
    private String principalAttribute;
    
    @AttributeName(name = "principalType")
    @Schema(description = "Way to identify and track external users from the assertion.")
    private String principalType;
    
    @AttributeName(name = "entityId")
    @Schema(description = "Entity ID that will be used to uniquely identify this SAML Service Provider.")
    private String idpEntityId;
    
    @AttributeName(name = "singleSignOnServiceUrl")
    @Schema(description = "Url used to send SAML authentication requests.")
    private String singleSignOnServiceUrl;
    
    @AttributeName(name = "encryptionPublicKey")
    @Schema(description = "Public key to use to encrypt the message.")
    private String encryptionPublicKey;
    
    @AttributeName
    @Schema(description = "IDP provider, should be SAML.")
    private String providerId;

   @AttributeName
   @Schema(description = "If enabled, email provided by this provider is not verified even if verification is enabled for the realm.")
    private boolean trustEmail;

    @AttributeName
    @Schema(description = "Enable/disable if tokens must be stored after authenticating users.")
    private boolean storeToken;

    @AttributeName
    @Schema(description = "Enable/disable if new users can read any stored tokens.")
    private boolean addReadTokenRoleOnCreate;

    @AttributeName
    private boolean authenticateByDefault;

    @AttributeName
    @Schema(description = "If true, users cannot log in through this provider. They can only link to this provider.")
    private boolean linkOnly;

    @AttributeName
    @Schema(description = "Alias of authentication flow, which is triggered after first login with this identity provider. Term 'First Login' means that no Keycloak account is currently linked to the authenticated identity provider account.")
    private String firstBrokerLoginFlowAlias;

    @AttributeName
    @Schema(description = "Alias of authentication flow, which is triggered after each login with this identity provider.")
    private String postBrokerLoginFlowAlias;

    @AttributeName(name = "jansSAMLspMetaDataFN")
    @Hidden
    private String spMetaDataFN;

    @AttributeName(name = "jansSAMLspMetaDataURL")
    @Schema(description = "SAML SP metadata file URL.")
    private String spMetaDataURL;
    
    @AttributeName(name = "jansSAMLspMetaLocation")
    @Schema(description = "SP metadata file location.")
    private String spMetaDataLocation;

    @AttributeName(name = "jansSAMLidpMetaDataFN")
    @Hidden
    private String idpMetaDataFN;

    @AttributeName(name = "jansSAMLidpMetaDataURL")
    @Schema(description = "SAML IDP metadata file URL.")
    private String idpMetaDataURL;
    
    @AttributeName(name = "jansSAMLidpMetaLocation")
    @Schema(description = "SAML IDP metadata file location.")
    private String idpMetaDataLocation;

    @AttributeName(name = "jansStatus")
    @Schema(description = "IDP setup status.")
    private GluuStatus status;

    @AttributeName(name = "jansValidationStatus")
    @Schema(description = "IDP validation status.")
    private ValidationStatus validationStatus;

    @AttributeName(name = "jansValidationLog")
    @Schema(description = "IDP validation log.")
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
