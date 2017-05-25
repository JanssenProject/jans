package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/09/2015
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterSiteParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "op_host")
    private String opHost;
    @JsonProperty(value = "op_discovery_path")
    private String opDiscoveryPath;
    @JsonProperty(value = "authorization_redirect_uri")
    private String authorizationRedirectUri;
    @JsonProperty(value = "post_logout_redirect_uri")
    private String postLogoutRedirectUri;
    @JsonProperty(value = "protection_access_token")
    private String protectionAccessToken;

    @JsonProperty(value = "redirect_uris")
    private List<String> redirectUris;
    @JsonProperty(value = "response_types")
    private List<String> responseTypes;

    @JsonProperty(value = "client_id")
    private String clientId;
    @JsonProperty(value = "client_secret")
    private String clientSecret;
    @JsonProperty(value = "client_name")
    private String clientName;
    @JsonProperty(value = "client_jwks_uri")
    private String clientJwksUri;
    @JsonProperty(value = "client_token_endpoint_auth_method")
    private String clientTokenEndpointAuthMethod;
    @JsonProperty(value = "client_request_uris")
    private List<String> clientRequestUris;
    @JsonProperty(value = "client_logout_uris")
    private List<String> clientLogoutUri;
    @JsonProperty(value = "client_sector_identifier_uri")
    private String clientSectorIdentifierUri;

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
    @JsonProperty(value = "trusted_client")
    private Boolean trustedClient = false;

    public RegisterSiteParams() {
    }

    public String getProtectionAccessToken() {
        return protectionAccessToken;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protectionAccessToken = protectionAccessToken;
    }

    public Boolean getTrustedClient() {
        return trustedClient;
    }

    public void setTrustedClient(Boolean trustedClient) {
        this.trustedClient = trustedClient;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getOpHost() {
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

    public String getClientSectorIdentifierUri() {
        return clientSectorIdentifierUri;
    }

    public void setClientSectorIdentifierUri(String clientSectorIdentifierUri) {
        this.clientSectorIdentifierUri = clientSectorIdentifierUri;
    }

    public List<String> getClientLogoutUri() {
        return clientLogoutUri;
    }

    public void setClientLogoutUri(List<String> clientLogoutUri) {
        this.clientLogoutUri = clientLogoutUri;
    }

    public List<String> getClientRequestUris() {
        return clientRequestUris;
    }

    public void setClientRequestUris(List<String> clientRequestUris) {
        this.clientRequestUris = clientRequestUris;
    }

    public String getClientTokenEndpointAuthMethod() {
        return clientTokenEndpointAuthMethod;
    }

    public void setClientTokenEndpointAuthMethod(String clientTokenEndpointAuthMethod) {
        this.clientTokenEndpointAuthMethod = clientTokenEndpointAuthMethod;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public String getClientJwksUri() {
        return clientJwksUri;
    }

    public void setClientJwksUri(String clientJwksUri) {
        this.clientJwksUri = clientJwksUri;
    }

    public String getAuthorizationRedirectUri() {
        return authorizationRedirectUri;
    }

    public void setAuthorizationRedirectUri(String authorizationRedirectUri) {
        this.authorizationRedirectUri = authorizationRedirectUri;
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

    public List<String> getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acrValues = acrValues;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RegisterSiteParams");
        sb.append("{acrValues=").append(acrValues);
        sb.append(", opHost='").append(opHost).append('\'');
        sb.append(", opDiscoveryPath='").append(opDiscoveryPath).append('\'');
        sb.append(", authorizationRedirectUri='").append(authorizationRedirectUri).append('\'');
        sb.append(", redirectUris=").append(redirectUris);
        sb.append(", responseTypes=").append(responseTypes);
        sb.append(", clientId='").append(clientId).append('\'');
        sb.append(", clientSecret='").append(clientSecret).append('\'');
        sb.append(", clientName='").append(clientName).append('\'');
        sb.append(", sectorIdentifierUri='").append(clientSectorIdentifierUri).append('\'');
        sb.append(", scope=").append(scope);
        sb.append(", uiLocales=").append(uiLocales);
        sb.append(", claimsLocales=").append(claimsLocales);
        sb.append(", grantType=").append(grantType);
        sb.append(", contacts=").append(contacts);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String getOxdId() {
        throw new UnsupportedOperationException();
    }
}

