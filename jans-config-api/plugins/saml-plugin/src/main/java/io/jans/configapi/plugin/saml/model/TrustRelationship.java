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
public class TrustRelationship extends Entry implements Serializable {

    private static final long serialVersionUID = 7912166229997681502L;

    @AttributeName(ignoreDuringUpdate = true)
    private String inum;

    @AttributeName(name = "jansClntId")
    private String clientId;

    @NotNull
    @Size(min = 0, max = 60, message = "Length of the Display Name should not exceed 60")
    @AttributeName
    private String displayName;

    @NotNull
    @Size(min = 0, max = 4000, message = "Length of the Description should not exceed 4000")
    @AttributeName
    private String description;

    // Access settings
    /**
     * Root URL appended to relative URLs
     */
    @AttributeName
    private String rootUrl;

    /**
     * URL to the admin interface of the client.
     * 
     */
    @AttributeName
    private String adminUrl;

    /**
     * Default URL, Home URL to use when the auth server needs to redirect or link
     * back to the client.
     * 
     */
    @AttributeName
    private String baseUrl;

    @AttributeName(name = "surrogateAuthRequired")
    private boolean surrogateAuthRequired;

    @AttributeName(name = "jansEnabled")
    private boolean enabled;

    /**
     * Always list this client in the Account UI, even if the user does not have an
     * active session.
     */
    @AttributeName(name = "displayInConsole")
    private boolean alwaysDisplayInConsole;

    @AttributeName(name = "jansPreferredMethod")
    private String clientAuthenticatorType;

    @AttributeName(name = "jansClntSecret")
    private String secret;

    @AttributeName(name = "jansRegistrationAccessTkn")
    private String registrationAccessToken;

    /**
     * Valid URI pattern a browser can redirect to after a successful login. Simple
     * wildcards are allowed such as 'http://example.com/*'. Relative path can be
     * specified too such as /my/relative/path/*. Relative paths are relative to the
     * client root URL, or if none is specified the auth server root URL is used.
     * For SAML, you must set valid URI patterns if you are relying on the consumer
     * service URL embedded with the login request.
     */
    @AttributeName(name = "jansRedirectURI")
    private List<String> redirectUris;

    @AttributeName(name = "jansWebOrigins")
    private List<String> webOrigins;

    private Boolean consentRequired;

    @NotNull
    @AttributeName(name = "jansSAMLspMetaDataSourceTyp")
    private MetadataSourceType spMetaDataSourceType;

    @AttributeName(name = "gluuSAMLspMetaDataFN")
    private String spMetaDataFN;

    @AttributeName(name = "jansMetaLocation")
    private String metaLocation;

    private Boolean jansIsFed;

    private ProfileConfiguration ssoProfileConfiguration;

    @AttributeName(name = "jansReleasedAttr")
    private List<String> releasedAttributes;

    @AttributeName
    private String protocol;

    @AttributeName(name = "jansStatus")
    private GluuStatus status;

    @AttributeName(name = "jansValidationStatus")
    private ValidationStatus validationStatus;

    @AttributeName(name = "jansEntityId")
    private List<String> jansEntityId;

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

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.surrogateAuthRequired = surrogateAuthRequired;
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

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(List<String> webOrigins) {
        this.webOrigins = webOrigins;
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

    public String getSpMetaDataFN() {
        return spMetaDataFN;
    }

    public void setSpMetaDataFN(String spMetaDataFN) {
        this.spMetaDataFN = spMetaDataFN;
    }

    public String getMetaLocation() {
        return metaLocation;
    }

    public void setMetaLocation(String metaLocation) {
        this.metaLocation = metaLocation;
    }

    public Boolean getJansIsFed() {
        return jansIsFed;
    }

    public void setJansIsFed(Boolean jansIsFed) {
        this.jansIsFed = jansIsFed;
    }

    public ProfileConfiguration getSsoProfileConfiguration() {
        return ssoProfileConfiguration;
    }

    public void setSsoProfileConfiguration(ProfileConfiguration ssoProfileConfiguration) {
        this.ssoProfileConfiguration = ssoProfileConfiguration;
    }

    public List<String> getReleasedAttributes() {
        return releasedAttributes;
    }

    public void setReleasedAttributes(List<String> releasedAttributes) {
        this.releasedAttributes = releasedAttributes;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
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

    public List<String> getJansEntityId() {
        return jansEntityId;
    }

    public void setJansEntityId(List<String> jansEntityId) {
        this.jansEntityId = jansEntityId;
    }

    public List<String> getValidationLog() {
        return validationLog;
    }

    public void setValidationLog(List<String> validationLog) {
        this.validationLog = validationLog;
    }

    @Override
    public String toString() {
        return "TrustRelationship [inum=" + inum + ", clientId=" + clientId + ", displayName=" + displayName
                + ", description=" + description + ", rootUrl=" + rootUrl + ", adminUrl=" + adminUrl + ", baseUrl="
                + baseUrl + ", surrogateAuthRequired=" + surrogateAuthRequired + ", enabled=" + enabled
                + ", alwaysDisplayInConsole=" + alwaysDisplayInConsole + ", clientAuthenticatorType="
                + clientAuthenticatorType + ", secret=" + secret + ", registrationAccessToken="
                + registrationAccessToken + ", redirectUris=" + redirectUris + ", webOrigins=" + webOrigins
                + ", consentRequired=" + consentRequired + ", spMetaDataSourceType=" + spMetaDataSourceType
                + ", spMetaDataFN=" + spMetaDataFN + ", metaLocation=" + metaLocation + ", jansIsFed=" + jansIsFed
                + ", ssoProfileConfiguration=" + ssoProfileConfiguration + ", releasedAttributes=" + releasedAttributes
                + ", protocol=" + protocol + ", status=" + status + ", validationStatus=" + validationStatus
                + ", jansEntityId=" + jansEntityId + ", validationLog=" + validationLog + "]";
    }

    private static class SortByDatasourceTypeComparator implements Comparator<TrustRelationship> {

        public int compare(TrustRelationship first, TrustRelationship second) {

            return first.getSpMetaDataSourceType().getRank() - second.getSpMetaDataSourceType().getRank();
        }
    }

    public static void sortByDataSourceType(List<TrustRelationship> trustRelationships) {
        Collections.sort(trustRelationships, new SortByDatasourceTypeComparator());
    }
}
