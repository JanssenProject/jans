package org.xdi.oxd.server.service;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.xdi.oxd.server.model.UmaResource;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 28/09/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class SiteConfiguration implements Serializable {

    @JsonProperty(value = "oxd_id")
    private String oxdId;

    @JsonProperty(value = "op_host")
    private String opHost;

    @JsonProperty(value = "id_token")
    private String idToken;
    @JsonProperty(value = "access_token")
    private String accessToken;

    @JsonProperty(value = "authorization_redirect_uri")
    private String authorizationRedirectUri;
    @JsonProperty(value = "logout_redirect_uri")
    private String postLogoutRedirectUri;

    @JsonProperty(value = "application_type")
    private String applicationType;
    @JsonProperty(value = "redirect_uris")
    private List<String> redirectUris;
    @JsonProperty(value = "response_types")
    private List<String> responseTypes;

    @JsonProperty(value = "client_id")
    private String clientId;
    @JsonProperty(value = "client_secret")
    private String clientSecret;
    @JsonProperty(value = "client_registration_access_token")
    private String clientRegistrationAccessToken;
    @JsonProperty(value = "client_registration_client_uri")
    private String clientRegistrationClientUri;
    @JsonProperty(value = "client_id_issued_at")
    private Date clientIdIssuedAt;
    @JsonProperty(value = "client_secret_expires_at")
    private Date clientSecretExpiresAt;
    @JsonProperty(value = "client_name")
    private String clientName;
    @JsonProperty(value = "sector_identifier_uri")
    private String sectorIdentifierUri;

    @JsonProperty(value = "scope")
    private List<String> scope;
    @JsonProperty(value = "ui_locales")
    private List<String> uiLocales;
    @JsonProperty(value = "claims_locales")
    private List<String> claimsLocales;
    @JsonProperty(value = "acr_values")
    private List<String> acrValues;
    @JsonProperty(value = "grant_types")
    private List<String> grantType;
    @JsonProperty(value = "contacts")
    private List<String> contacts;

    @JsonProperty(value = "user_id")
    private String userId;
    @JsonProperty(value = "user_secret")
    private String userSecret;

    @JsonProperty(value = "aat")
    private String aat;
    @JsonProperty(value = "aat_expires_in")
    private int aatExpiresIn;
    @JsonProperty(value = "aat_created_at")
    private Date aatCreatedAt;
    @JsonProperty(value = "aat_refresh_token")
    private String aatRefreshToken;

    @JsonProperty(value = "pat")
    private String pat;
    @JsonProperty(value = "pat_expires_in")
    private int patExpiresIn;
    @JsonProperty(value = "pat_created_at")
    private Date patCreatedAt;
    @JsonProperty(value = "pat_refresh_token")
    private String patRefreshToken;
    @JsonProperty(value = "uma_protected_resources")
    private List<UmaResource> umaProtectedResources = Lists.newArrayList();

    @JsonProperty(value = "rpt")
    private String rpt;
    @JsonProperty(value = "rpt_expires_at")
    private Date rptExpiresAt;
    @JsonProperty(value = "rpt_created_at")
    private Date rptCreatedAt;

    public SiteConfiguration() {
    }

    public SiteConfiguration(SiteConfiguration conf) {
        this.oxdId = conf.oxdId;

        this.opHost = conf.opHost;

        this.idToken = conf.idToken;
        this.accessToken = conf.accessToken;

        this.authorizationRedirectUri = conf.authorizationRedirectUri;
        this.postLogoutRedirectUri = conf.postLogoutRedirectUri;

        this.applicationType = conf.applicationType;
        this.redirectUris = conf.redirectUris;
        this.responseTypes = conf.responseTypes;

        this.clientId = conf.clientId;
        this.clientSecret = conf.clientSecret;
        this.clientRegistrationAccessToken = conf.clientRegistrationAccessToken;
        this.clientRegistrationClientUri = conf.clientRegistrationClientUri;
        this.clientIdIssuedAt = conf.clientIdIssuedAt;
        this.clientSecretExpiresAt = conf.clientSecretExpiresAt;
        this.clientName = conf.clientName;
        this.sectorIdentifierUri = conf.sectorIdentifierUri;

        this.scope = conf.scope;
        this.uiLocales = conf.uiLocales;
        this.claimsLocales = conf.claimsLocales;
        this.acrValues = conf.acrValues;
        this.grantType = conf.grantType;
        this.contacts = conf.contacts;

        this.userId = conf.userId;
        this.userSecret = conf.userSecret;

        this.aat = conf.aat;
        this.aatExpiresIn = conf.aatExpiresIn;
        this.aatCreatedAt = conf.aatCreatedAt;
        this.aatRefreshToken = conf.aatRefreshToken;

        this.pat = conf.pat;
        this.patExpiresIn = conf.patExpiresIn;
        this.patCreatedAt = conf.patCreatedAt;
        this.patRefreshToken = conf.patRefreshToken;

        this.rpt = conf.rpt;
        this.rptExpiresAt = conf.rptExpiresAt;
        this.rptCreatedAt = conf.rptCreatedAt;

        this.umaProtectedResources = conf.umaProtectedResources;
    }

    public String getAatRefreshToken() {
        return aatRefreshToken;
    }

    public void setAatRefreshToken(String aatRefreshToken) {
        this.aatRefreshToken = aatRefreshToken;
    }

    public String getPatRefreshToken() {
        return patRefreshToken;
    }

    public void setPatRefreshToken(String patRefreshToken) {
        this.patRefreshToken = patRefreshToken;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserSecret() {
        return userSecret;
    }

    public void setUserSecret(String userSecret) {
        this.userSecret = userSecret;
    }

    public String getAat() {
        return aat;
    }

    public void setAat(String aat) {
        this.aat = aat;
    }

    public int getAatExpiresIn() {
        return aatExpiresIn;
    }

    public void setAatExpiresIn(int aatExpiresIn) {
        this.aatExpiresIn = aatExpiresIn;
    }

    public Date getAatCreatedAt() {
        return aatCreatedAt;
    }

    public void setAatCreatedAt(Date aatCreatedAt) {
        this.aatCreatedAt = aatCreatedAt;
    }

    public String getPat() {
        return pat;
    }

    public void setPat(String pat) {
        this.pat = pat;
    }

    public int getPatExpiresIn() {
        return patExpiresIn;
    }

    public void setPatExpiresIn(int patExpiresIn) {
        this.patExpiresIn = patExpiresIn;
    }

    public Date getPatCreatedAt() {
        return patCreatedAt;
    }

    public void setPatCreatedAt(Date patCreatedAt) {
        this.patCreatedAt = patCreatedAt;
    }

    public String getSectorIdentifierUri() {
        return sectorIdentifierUri;
    }

    public void setSectorIdentifierUri(String sectorIdentifierUri) {
        this.sectorIdentifierUri = sectorIdentifierUri;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Date getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    public void setClientIdIssuedAt(Date clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    public String getClientRegistrationAccessToken() {
        return clientRegistrationAccessToken;
    }

    public void setClientRegistrationAccessToken(String clientRegistrationAccessToken) {
        this.clientRegistrationAccessToken = clientRegistrationAccessToken;
    }

    public String getClientRegistrationClientUri() {
        return clientRegistrationClientUri;
    }

    public void setClientRegistrationClientUri(String clientRegistrationClientUri) {
        this.clientRegistrationClientUri = clientRegistrationClientUri;
    }

    public Date getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    public void setClientSecretExpiresAt(Date clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getAuthorizationRedirectUri() {
        return authorizationRedirectUri;
    }

    public void setAuthorizationRedirectUri(String authorizationRedirectUri) {
        this.authorizationRedirectUri = authorizationRedirectUri;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public List<String> getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acrValues = acrValues;
    }

    public List<String> getClaimsLocales() {
        return claimsLocales;
    }

    public void setClaimsLocales(List<String> claimsLocales) {
        this.claimsLocales = claimsLocales;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public List<String> getGrantType() {
        return grantType;
    }

    public void setGrantType(List<String> grantType) {
        this.grantType = grantType;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
    }

    public String getOpHost() {
        return opHost;
    }

    public String opHostWithoutProtocol() {
        if (StringUtils.contains(opHost, "//")) {
            return StringUtils.substringAfter(opHost, "//");
        }
        return opHost;
    }

    public void setOpHost(String opHost) {
        this.opHost = opHost;
    }

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public List<String> getUiLocales() {
        return uiLocales;
    }

    public void setUiLocales(List<String> uiLocales) {
        this.uiLocales = uiLocales;
    }

    public List<UmaResource> getUmaProtectedResources() {
        if (umaProtectedResources == null) {
            umaProtectedResources = Lists.newArrayList();
        }
        return umaProtectedResources;
    }

    public void setUmaProtectedResources(List<UmaResource> umaProtectedResources) {
        this.umaProtectedResources = umaProtectedResources;
    }

    public String getRpt() {
        return rpt;
    }

    public void setRpt(String rpt) {
        this.rpt = rpt;
    }

    public Date getRptExpiresAt() {
        return rptExpiresAt;
    }

    public void setRptExpiresAt(Date rptExpiresAt) {
        this.rptExpiresAt = rptExpiresAt;
    }

    public Date getRptCreatedAt() {
        return rptCreatedAt;
    }

    public void setRptCreatedAt(Date rptCreatedAt) {
        this.rptCreatedAt = rptCreatedAt;
    }

    public UmaResource umaResource(String path, String httpMethod) {
        for (UmaResource resource : umaProtectedResources) {
            if (path.equalsIgnoreCase(resource.getPath()) && resource.getHttpMethods() != null) {
                for (String http : resource.getHttpMethods()) {
                    if (http.equalsIgnoreCase(httpMethod)) {
                        return resource;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SiteConfiguration");
        sb.append("{acrValues=").append(acrValues);
        sb.append(", oxdId='").append(oxdId).append('\'');
        sb.append(", opHost='").append(opHost).append('\'');
        sb.append(", authorizationRedirectUri='").append(authorizationRedirectUri).append('\'');
        sb.append(", applicationType='").append(applicationType).append('\'');
        sb.append(", sectorIdentifierUri='").append(sectorIdentifierUri).append('\'');
        sb.append(", redirectUris=").append(redirectUris);
        sb.append(", responseTypes=").append(responseTypes);
        sb.append(", clientId='").append(clientId).append('\'');
        sb.append(", clientSecret='").append(clientSecret).append('\'');
        sb.append(", scope=").append(scope);
        sb.append(", uiLocales=").append(uiLocales);
        sb.append(", claimsLocales=").append(claimsLocales);
        sb.append(", grantType=").append(grantType);
        sb.append(", contacts=").append(contacts);
        sb.append(", aat=").append(aat);
        sb.append(", aatCreatedAt=").append(aatCreatedAt);
        sb.append(", aatExpiresIn=").append(aatExpiresIn);
        sb.append(", pat=").append(pat);
        sb.append(", patCreatedAt=").append(patCreatedAt);
        sb.append(", patExpiresIn=").append(patExpiresIn);
        sb.append(", umaProtectedResources=").append(umaProtectedResources);
        sb.append(", rpt=").append(rpt);
        sb.append(", rptCreatedAt=").append(rptCreatedAt);
        sb.append(", rptExpiresIn=").append(rptExpiresAt);
        sb.append('}');
        return sb.toString();
    }
}
