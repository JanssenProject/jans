package org.gluu.oxd.server.service;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxd.server.model.UmaResource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Rp implements Serializable {

    @JsonProperty(value = "oxd_id")
    private String oxdId;

    @JsonProperty(value = "op_host")
    private String opHost;
    @JsonProperty(value = "op_discovery_path")
    private String opDiscoveryPath;

    @JsonProperty(value = "id_token")
    private String idToken;
    @JsonProperty(value = "access_token")
    private String accessToken;

    @JsonProperty(value = "authorization_redirect_uri")
    private String authorizationRedirectUri;
    @JsonProperty(value = "logout_redirect_uri")
    private String postLogoutRedirectUri;
    @JsonProperty(value = "logout_redirect_uris")
    private List<String> postLogoutRedirectUris;

    @JsonProperty(value = "application_type")
    private String applicationType;
    @JsonProperty(value = "redirect_uris")
    private List<String> redirectUris;
    @JsonProperty(value = "claims_redirect_uri")
    private List<String> claimsRedirectUri;
    @JsonProperty(value = "response_types")
    private List<String> responseTypes;
    @JsonProperty(value = "front_channel_logout_uri")
    private List<String> frontChannelLogoutUri;

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
    @JsonProperty(value = "client_jwks_uri")
    private String clientJwksUri;
    @JsonProperty(value = "token_endpoint_auth_signing_alg")
    private String tokenEndpointAuthSigningAlg;
    @JsonProperty(value = "token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;

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
    @JsonProperty(value = "rpt_token_type")
    private String rptTokenType;
    @JsonProperty(value = "rpt_pct")
    private String rptPct;
    @JsonProperty(value = "rpt_upgraded")
    private Boolean rptUpgraded;
    @JsonProperty(value = "rpt_expires_at")
    private Date rptExpiresAt;
    @JsonProperty(value = "rpt_created_at")
    private Date rptCreatedAt;
    @JsonProperty(value = "oxd_rp_programming_language")
    private String oxdRpProgrammingLanguage;
    @JsonProperty(value = "access_token_as_jwt")
    private Boolean accessTokenAsJwt = false;
    @JsonProperty(value = "access_token_signing_alg")
    private String accessTokenSigningAlg;
    @JsonProperty(value = "rpt_as_jwt")
    private Boolean rptAsJwt = false;

    public Rp() {
    }

    public Rp(Rp conf) {
        this.oxdId = conf.oxdId;

        this.opHost = conf.opHost;
        this.opDiscoveryPath = conf.opDiscoveryPath;

        this.idToken = conf.idToken;
        this.accessToken = conf.accessToken;

        this.authorizationRedirectUri = conf.authorizationRedirectUri;
        this.postLogoutRedirectUri = conf.postLogoutRedirectUri;
        this.postLogoutRedirectUris = conf.postLogoutRedirectUris;
        this.applicationType = conf.applicationType;
        this.redirectUris = conf.redirectUris;
        this.claimsRedirectUri = conf.claimsRedirectUri;
        this.responseTypes = conf.responseTypes;
        this.frontChannelLogoutUri = conf.frontChannelLogoutUri;

        this.clientId = conf.clientId;
        this.clientSecret = conf.clientSecret;
        this.clientRegistrationAccessToken = conf.clientRegistrationAccessToken;
        this.clientRegistrationClientUri = conf.clientRegistrationClientUri;
        this.clientIdIssuedAt = conf.clientIdIssuedAt;
        this.clientSecretExpiresAt = conf.clientSecretExpiresAt;
        this.clientName = conf.clientName;
        this.sectorIdentifierUri = conf.sectorIdentifierUri;
        this.clientJwksUri = conf.clientJwksUri;

        this.tokenEndpointAuthSigningAlg = conf.tokenEndpointAuthSigningAlg;
        this.tokenEndpointAuthMethod = conf.tokenEndpointAuthMethod;

        this.scope = conf.scope;
        this.uiLocales = conf.uiLocales;
        this.claimsLocales = conf.claimsLocales;
        this.acrValues = conf.acrValues;
        this.grantType = conf.grantType;
        this.contacts = conf.contacts;

        this.userId = conf.userId;
        this.userSecret = conf.userSecret;

        this.pat = conf.pat;
        this.patExpiresIn = conf.patExpiresIn;
        this.patCreatedAt = conf.patCreatedAt;
        this.patRefreshToken = conf.patRefreshToken;

        this.rpt = conf.rpt;
        this.rptTokenType = conf.rptTokenType;
        this.rptPct = conf.rptPct;
        this.rptUpgraded = conf.rptUpgraded;
        this.rptExpiresAt = conf.rptExpiresAt;
        this.rptCreatedAt = conf.rptCreatedAt;
        this.rptAsJwt = conf.rptAsJwt;

        this.umaProtectedResources = conf.umaProtectedResources;
        this.oxdRpProgrammingLanguage = conf.oxdRpProgrammingLanguage;
        this.accessTokenAsJwt = conf.accessTokenAsJwt;
        this.accessTokenSigningAlg = conf.accessTokenSigningAlg;
    }

    public Boolean getAccessTokenAsJwt() {
        return accessTokenAsJwt;
    }

    public void setAccessTokenAsJwt(Boolean accessTokenAsJwt) {
        this.accessTokenAsJwt = accessTokenAsJwt;
    }

    public Boolean getRptAsJwt() {
        return rptAsJwt;
    }

    public void setRptAsJwt(Boolean rptAsJwt) {
        this.rptAsJwt = rptAsJwt;
    }

    public String getAccessTokenSigningAlg() {
        return accessTokenSigningAlg;
    }

    public void setAccessTokenSigningAlg(String accessTokenSigningAlg) {
        this.accessTokenSigningAlg = accessTokenSigningAlg;
    }

    public List<String> getFrontChannelLogoutUri() {
        return frontChannelLogoutUri;
    }

    public void setFrontChannelLogoutUri(List<String> frontChannelLogoutUri) {
        this.frontChannelLogoutUri = frontChannelLogoutUri;
    }

    public String getTokenEndpointAuthSigningAlg() {
        return tokenEndpointAuthSigningAlg;
    }

    public void setTokenEndpointAuthSigningAlg(String tokenEndpointAuthSigningAlg) {
        this.tokenEndpointAuthSigningAlg = tokenEndpointAuthSigningAlg;
    }

    public MinimumRp asMinimumRp() {
        return new MinimumRp(oxdId, clientName);
    }

    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    public String getClientJwksUri() {
        return clientJwksUri;
    }

    public void setClientJwksUri(String clientJwksUri) {
        this.clientJwksUri = clientJwksUri;
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

    public List<String> getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
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
        if (contacts == null) {
            contacts = new ArrayList<>();
        }
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public List<String> getAcrValues() {
        if (acrValues == null) {
            acrValues = new ArrayList<>();
        }
        return acrValues;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acrValues = acrValues;
    }

    public List<String> getClaimsLocales() {
        if (claimsLocales == null) {
            claimsLocales = new ArrayList<>();
        }
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
        if (grantType == null) {
            grantType = new ArrayList<>();
        }
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

    public String getOpDiscoveryPath() {
        return opDiscoveryPath;
    }

    public void setOpDiscoveryPath(String opDiscoveryPath) {
        this.opDiscoveryPath = opDiscoveryPath;
    }

    public List<String> getClaimsRedirectUri() {
        if (claimsRedirectUri == null) {
            claimsRedirectUri = new ArrayList<>();
        }
        return claimsRedirectUri;
    }

    public void setClaimsRedirectUri(List<String> claimsRedirectUri) {
        this.claimsRedirectUri = claimsRedirectUri;
    }

    public List<String> getRedirectUris() {
        if (redirectUris == null) {
            redirectUris = new ArrayList<>();
        }
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getResponseTypes() {
        if (responseTypes == null) {
            responseTypes = new ArrayList<>();
        }
        return responseTypes;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.responseTypes = responseTypes;
    }

    public List<String> getScope() {
        if (scope == null) {
            scope = new ArrayList<>();
        }
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public List<String> getUiLocales() {
        if (uiLocales == null) {
            uiLocales = new ArrayList<>();
        }
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

    public String getOxdRpProgrammingLanguage() {
        return oxdRpProgrammingLanguage;
    }

    public void setOxdRpProgrammingLanguage(String oxdRpProgrammingLanguage) {
        this.oxdRpProgrammingLanguage = oxdRpProgrammingLanguage;
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

    public String getRptTokenType() {
        return rptTokenType;
    }

    public void setRptTokenType(String rptTokenType) {
        this.rptTokenType = rptTokenType;
    }

    public String getRptPct() {
        return rptPct;
    }

    public void setRptPct(String rptPct) {
        this.rptPct = rptPct;
    }

    public Boolean getRptUpgraded() {
        return rptUpgraded;
    }

    public void setRptUpgraded(Boolean rptUpgraded) {
        this.rptUpgraded = rptUpgraded;
    }

    public Date getRptCreatedAt() {
        return rptCreatedAt;
    }

    public void setRptCreatedAt(Date rptCreatedAt) {
        this.rptCreatedAt = rptCreatedAt;
    }

    public UmaResource umaResource(String path, String httpMethod) {
        List<UmaResource> copy = Lists.newArrayList(umaProtectedResources);
        Collections.reverse(copy);

        for (UmaResource resource : copy) {
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
        return "Rp{" +
                "oxdId='" + oxdId + '\'' +
                ", opHost='" + opHost + '\'' +
                ", opDiscoveryPath='" + opDiscoveryPath + '\'' +
                ", idToken='" + idToken + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", authorizationRedirectUri='" + authorizationRedirectUri + '\'' +
                ", postLogoutRedirectUri='" + postLogoutRedirectUri + '\'' +
                ", postLogoutRedirectUris='" + postLogoutRedirectUris + '\'' +
                ", applicationType='" + applicationType + '\'' +
                ", redirectUris=" + redirectUris +
                ", frontChannelLogoutUri=" + frontChannelLogoutUri +
                ", claimsRedirectUri=" + claimsRedirectUri +
                ", responseTypes=" + responseTypes +
                ", clientId='" + clientId + '\'' +
                ", clientRegistrationAccessToken='" + clientRegistrationAccessToken + '\'' +
                ", clientRegistrationClientUri='" + clientRegistrationClientUri + '\'' +
                ", clientIdIssuedAt=" + clientIdIssuedAt +
                ", clientSecretExpiresAt=" + clientSecretExpiresAt +
                ", clientName='" + clientName + '\'' +
                ", sectorIdentifierUri='" + sectorIdentifierUri + '\'' +
                ", clientJwksUri='" + clientJwksUri + '\'' +
                ", scope=" + scope +
                ", uiLocales=" + uiLocales +
                ", claimsLocales=" + claimsLocales +
                ", acrValues=" + acrValues +
                ", grantType=" + grantType +
                ", contacts=" + contacts +
                ", userId='" + userId + '\'' +
                ", userSecret='" + userSecret + '\'' +
                ", pat='" + pat + '\'' +
                ", patExpiresIn=" + patExpiresIn +
                ", patCreatedAt=" + patCreatedAt +
                ", patRefreshToken='" + patRefreshToken + '\'' +
                ", umaProtectedResources=" + umaProtectedResources +
                ", rpt='" + rpt + '\'' +
                ", rptTokenType='" + rptTokenType + '\'' +
                ", rptPct='" + rptPct + '\'' +
                ", rptExpiresAt=" + rptExpiresAt +
                ", rptCreatedAt=" + rptCreatedAt +
                ", rptUpgraded=" + rptUpgraded +
                ", rptAsJwt=" + rptAsJwt +
                ", tokenEndpointAuthSigningAlg=" + tokenEndpointAuthSigningAlg +
                ", tokenEndpointAuthMethod=" + tokenEndpointAuthMethod +
                ", oxdRpProgrammingLanguage=" + oxdRpProgrammingLanguage +
                ", accessTokenAsJwt=" + accessTokenAsJwt +
                ", accessTokenSigningAlg=" + accessTokenSigningAlg +
                '}';
    }
}
