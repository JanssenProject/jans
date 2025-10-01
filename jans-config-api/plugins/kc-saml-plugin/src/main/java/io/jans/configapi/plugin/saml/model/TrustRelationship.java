/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.saml.model;


import com.fasterxml.jackson.annotation.JsonInclude;

import io.jans.model.GluuStatus;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.JsonObject;
import io.jans.orm.annotation.ObjectClass;
import io.jans.orm.model.base.Entry;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansTrustRelationship")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrustRelationship extends Entry implements Serializable {

    private static final long serialVersionUID = 7912166229997681502L;

    @AttributeName(ignoreDuringUpdate = true)
    @Schema(description = "Unique identifier")
    private String inum;

    @AttributeName
    @Schema(description = "Creator of Trust Relationship.")
    private String owner;

    @AttributeName(name = "name")
    @NotNull
    @Size(min = 0, max = 60, message = "Length of the name should not exceed 60")
    @Schema(description = "The alphanumeric ID string that is used to identify the Trust Relationship.")
    private String name;

    @NotNull
    @Size(min = 0, max = 60, message = "Length of the Display Name should not exceed 60")
    @AttributeName
    @Schema(description = "Trust Relationship display name.")
    private String displayName;

    @NotNull
    @Size(min = 0, max = 4000, message = "Length of the Description should not exceed 4000")
    @AttributeName
    @Schema(description = "Description of the Trust Relationship.")
    private String description;
    
    @Schema(description = "URL to use when the auth server needs to redirect.")
    @AttributeName
    private String baseUrl;

    @AttributeName(name = "jansEnabled")
    @Schema(description = "Indicates if Trust Relationship is enabled.")
    private boolean enabled;

    @AttributeName(name = "displayInConsole")
    @Schema(description = "Indicates if Trust Relationship should always be listed in the UI.")
    private boolean alwaysDisplayInConsole;

    @AttributeName(name = "jansPreferredMethod")
    @Schema(description = "Preferred Authenticator Type.")
    private String clientAuthenticatorType;

    @AttributeName(name = "jansClntSecret")
    @Schema(description = "Client secret.")
    private String secret;

    @AttributeName(name = "jansRegistrationAccessTkn")
    @Schema(description = "Registration access token.")
    private String registrationAccessToken;

    @Schema(description = "Boolean value if consent is required.")
    private Boolean consentRequired;

    @NotNull
    @AttributeName(name = "jansSAMLspMetaDataSourceTyp")
    @Schema(description = "Trust Relationship SP metadata type - file, URI.")
    private MetadataSourceType spMetaDataSourceType;

    @JsonObject
    @AttributeName(name = "samlMetadata")
    @Schema(description = "SAML entity metadata.")
    private SAMLMetadata samlMetadata;

    @AttributeName(name = "jansRedirectURI")
    @Schema(description = "List of valid Redirect URI.")
    private String[] redirectUris;


    @AttributeName(name = "jansSAMLspMetaDataFN")
    @Hidden
    @Schema(description = "Trust Relationship metadata file name.")
    private String spMetaDataFN;

    @AttributeName(name = "jansSAMLspMetaDataURL")
    @Schema(description = "SAML entity metadata file URL.")
    private String spMetaDataURL;

    @AttributeName(name = "jansMetaLocation")
    @Schema(description = "Trust Relationship metadata file location.")
    private String metaLocation;

    @AttributeName(name = "jansReleasedAttr")
    @Schema(description = "Trust Relationship attributes that will be released to SAML server.")
    private List<String> releasedAttributes;

    @Pattern(regexp = "^$|(^(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])", message = "Please enter a valid url, including protocol (http/https)")
    @AttributeName(name = "jansPostLogoutRedirectURI")
    @Schema(description = "Logout request URL.")
    private String spLogoutURL;

    @AttributeName(name = "jansStatus")
    @Schema(description = "Trust Relationship setup status.")
    private GluuStatus status;

    @AttributeName(name = "jansValidationStatus")
    @Schema(description = "Trust Relationship validation status.")
    private ValidationStatus validationStatus;

    @AttributeName(name = "jansValidationLog")
    @Schema(description = "Validation log.")
    private List<String> validationLog;

    @Schema(description = "List of profile configuration.")
    private Map<String, ProfileConfiguration> profileConfigurations = new HashMap<String, ProfileConfiguration>();

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAlwaysDisplayInConsole() {
        return alwaysDisplayInConsole;
    }

    public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {
        this.alwaysDisplayInConsole = alwaysDisplayInConsole;
    }

    public String getClientAuthenticatorType() {
        return clientAuthenticatorType;
    }

    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        this.clientAuthenticatorType = clientAuthenticatorType;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getRegistrationAccessToken() {
        return registrationAccessToken;
    }

    public void setRegistrationAccessToken(String registrationAccessToken) {
        this.registrationAccessToken = registrationAccessToken;
    }

    public Boolean getConsentRequired() {
        return consentRequired;
    }

    public void setConsentRequired(Boolean consentRequired) {
        this.consentRequired = consentRequired;
    }

    public MetadataSourceType getSpMetaDataSourceType() {
        return spMetaDataSourceType;
    }

    public void setSpMetaDataSourceType(MetadataSourceType spMetaDataSourceType) {
        this.spMetaDataSourceType = spMetaDataSourceType;
    }

    public SAMLMetadata getSamlMetadata() {
        return samlMetadata;
    }

    public void setSamlMetadata(SAMLMetadata samlMetadata) {
        this.samlMetadata = samlMetadata;
    }

    public String[] getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String[] redirectUris) {
        this.redirectUris = redirectUris;
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

    public String getMetaLocation() {
        return metaLocation;
    }

    public void setMetaLocation(String metaLocation) {
        this.metaLocation = metaLocation;
    }

    public List<String> getReleasedAttributes() {
        return releasedAttributes;
    }

    public void setReleasedAttributes(List<String> releasedAttributes) {
        this.releasedAttributes = releasedAttributes;
    }

    public String getSpLogoutURL() {
        return spLogoutURL;
    }

    public void setSpLogoutURL(String spLogoutURL) {
        this.spLogoutURL = spLogoutURL;
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

    public Map<String, ProfileConfiguration> getProfileConfigurations() {
        return profileConfigurations;
    }

    public void setProfileConfigurations(Map<String, ProfileConfiguration> profileConfigurations) {
        this.profileConfigurations = profileConfigurations;
    }

    private static class SortByDatasourceTypeComparator implements Comparator<TrustRelationship> {

        public int compare(TrustRelationship first, TrustRelationship second) {

            return first.getSpMetaDataSourceType().getRank() - second.getSpMetaDataSourceType().getRank();
        }
    }

    public static void sortByDataSourceType(List<TrustRelationship> trustRelationships) {
        Collections.sort(trustRelationships, new SortByDatasourceTypeComparator());
    }

    @Override
    public String toString() {
        return "TrustRelationship [inum=" + inum + ", owner=" + owner + ", name=" + name + ", displayName="
                + displayName + ", description=" + description 
                + ", baseUrl=" + baseUrl + ", enabled=" + enabled
                + ", alwaysDisplayInConsole=" + alwaysDisplayInConsole + ", clientAuthenticatorType="
                + clientAuthenticatorType + ", secret=" + secret + ", registrationAccessToken="
                + registrationAccessToken + ", consentRequired=" + consentRequired + ", spMetaDataSourceType="
                + spMetaDataSourceType + ", samlMetadata=" + samlMetadata + ", redirectUris="
                + Arrays.toString(redirectUris) + ", spMetaDataFN=" + spMetaDataFN + ", spMetaDataURL=" + spMetaDataURL
                + ", metaLocation=" + metaLocation + ", releasedAttributes=" + releasedAttributes 
                + ", spLogoutURL=" + spLogoutURL + ", status=" + status + ", validationStatus=" + validationStatus
                + ", validationLog=" + validationLog + ", profileConfigurations=" + profileConfigurations + "]";
    }

}
