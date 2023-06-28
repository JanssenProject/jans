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

@DataEntry(sortBy = { "displayName" })
@ObjectClass(value = "jansSAMLconfig")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JansTrustRelationship extends Entry implements Serializable {

    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansClntId")
    private String clientId;

    @AttributeName
    private String protocol;

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

    /**
     * Always list this client in the Account UI, even if the user does not have an
     * active session.
     */
    @AttributeName(name = "jansEnabled")
    private boolean alwaysDisplayInConsole;

    @AttributeName(name = "jansEnabled")
    private boolean enabled;

    @AttributeName(name = "jansPreferredMethod")
    private boolean clientAuthenticatorType;

    @AttributeName(name = "jansClntSecret")
    private String clientSecret;

    // Access settings
    /**
     * Root URL appended to relative URLs
     */
    @AttributeName
    private String rootUrl;

    /**
     * Default URL, Home URL to use when the auth server needs to redirect or link
     * back to the client.
     * 
     */
    @AttributeName
    private String baseUrl;

    /**
     * URL to the admin interface of the client.
     * 
     */
    @AttributeName
    private String adminUrl;

    /**
     * Valid URI pattern a browser can redirect to after a successful login. Simple
     * wildcards are allowed such as 'http://example.com/*'. Relative path can be
     * specified too such as /my/relative/path/*. Relative paths are relative to the
     * client root URL, or if none is specified the auth server root URL is used.
     * For SAML, you must set valid URI patterns if you are relying on the consumer
     * service URL embedded with the login request.
     */
    private List<String> redirectUris;

    private Boolean consentRequired;

    private Boolean publicClient;

    private Boolean frontchannelLogout;

    private Boolean jansIsFed;

    @AttributeName(name = "jansReleasedAttr")
    private List<String> releasedAttributes;

    @NotNull
    @AttributeName(name = "jansSAMLspMetaDataSourceTyp")
    private MetadataSourceType spMetaDataSourceType;

    @Transient
    private transient List<CustomAttribute> releasedCustomAttributes = new ArrayList<CustomAttribute>();

    private Map<String, MetadataFilter> metadataFilters = new HashMap<String, MetadataFilter>();

    private Map<String, ProfileConfiguration> profileConfigurations = new HashMap<String, ProfileConfiguration>();

    @AttributeName(name = "jansSAMLMetaDataFilter")
    private List<String> jansSAMLMetaDataFilter;

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
    
    
    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        this.inum = inum;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
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

    public boolean isAlwaysDisplayInConsole() {
        return alwaysDisplayInConsole;
    }

    public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {
        this.alwaysDisplayInConsole = alwaysDisplayInConsole;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isClientAuthenticatorType() {
        return clientAuthenticatorType;
    }

    public void setClientAuthenticatorType(boolean clientAuthenticatorType) {
        this.clientAuthenticatorType = clientAuthenticatorType;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public Boolean getConsentRequired() {
        return consentRequired;
    }

    public void setConsentRequired(Boolean consentRequired) {
        this.consentRequired = consentRequired;
    }

    public Boolean getPublicClient() {
        return publicClient;
    }

    public void setPublicClient(Boolean publicClient) {
        this.publicClient = publicClient;
    }

    public Boolean getFrontchannelLogout() {
        return frontchannelLogout;
    }

    public void setFrontchannelLogout(Boolean frontchannelLogout) {
        this.frontchannelLogout = frontchannelLogout;
    }

    public Boolean getJansIsFed() {
        return jansIsFed;
    }

    public void setJansIsFed(Boolean jansIsFed) {
        this.jansIsFed = jansIsFed;
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

    private static class SortByDatasourceTypeComparator implements Comparator<JansTrustRelationship> {

        public int compare(JansTrustRelationship first, JansTrustRelationship second) {

            return first.getSpMetaDataSourceType().getRank() - second.getSpMetaDataSourceType().getRank();
        }
    }

    public static void sortByDataSourceType(List<JansTrustRelationship> trustRelationships) {
        Collections.sort(trustRelationships, new SortByDatasourceTypeComparator());
    }
}
