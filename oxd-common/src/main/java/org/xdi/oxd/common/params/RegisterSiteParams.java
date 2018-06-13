package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnore;
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
    @JsonProperty(value = "claims_redirect_uri")
    private List<String> claimsRedirectUri;

    @JsonProperty(value = "client_id")
    private String clientId;
    @JsonProperty(value = "client_secret")
    private String clientSecret;
    @JsonProperty(value = "client_registration_access_token")
    private String clientRegistrationAccessToken;
    @JsonProperty(value = "client_registration_client_uri")
    private String clientRegistrationClientUri;
    @JsonProperty(value = "client_name")
    private String clientName;
    @JsonProperty(value = "client_jwks_uri")
    private String clientJwksUri;
    @JsonProperty(value = "client_token_endpoint_auth_method")
    private String clientTokenEndpointAuthMethod;
    @JsonProperty(value = "client_token_endpoint_auth_signing_alg")
    private String clientTokenEndpointAuthSigningAlg;
    @JsonProperty(value = "client_request_uris")
    private List<String> clientRequestUris;
    @JsonProperty(value = "client_frontchannel_logout_uris")
    private List<String> clientFrontchannelLogoutUri;
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
    @JsonProperty(value = "oxd_rp_programming_language")
    private String oxdRpProgrammingLanguage;

    public RegisterSiteParams() {
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

    public String getOxdRpProgrammingLanguage() {
        return oxdRpProgrammingLanguage;
    }

    public void setOxdRpProgrammingLanguage(String oxdRpProgrammingLanguage) {
        this.oxdRpProgrammingLanguage = oxdRpProgrammingLanguage;
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

    public List<String> getClientFrontchannelLogoutUri() {
        return clientFrontchannelLogoutUri;
    }

    public void setClientFrontchannelLogoutUri(List<String> clientFrontchannelLogoutUri) {
        this.clientFrontchannelLogoutUri = clientFrontchannelLogoutUri;
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

    public String getClientTokenEndpointAuthSigningAlg() {
        return clientTokenEndpointAuthSigningAlg;
    }

    public void setClientTokenEndpointAuthSigningAlg(String clientTokenEndpointAuthSigningAlg) {
        this.clientTokenEndpointAuthSigningAlg = clientTokenEndpointAuthSigningAlg;
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

    public List<String> getClaimsRedirectUri() {
        return claimsRedirectUri;
    }

    public void setClaimsRedirectUri(List<String> claimsRedirectUri) {
        this.claimsRedirectUri = claimsRedirectUri;
    }


    @Override
    public String toString() {
        return "RegisterSiteParams{" +
                "opHost='" + opHost + '\'' +
                ", opDiscoveryPath='" + opDiscoveryPath + '\'' +
                ", authorizationRedirectUri='" + authorizationRedirectUri + '\'' +
                ", postLogoutRedirectUri='" + postLogoutRedirectUri + '\'' +
                ", protectionAccessToken='" + protectionAccessToken + '\'' +
                ", redirectUris=" + redirectUris +
                ", responseTypes=" + responseTypes +
                ", claimsRedirectUri=" + claimsRedirectUri +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", clientRegistrationAccessToken='" + clientRegistrationAccessToken + '\'' +
                ", clientRegistrationClientUri='" + clientRegistrationClientUri + '\'' +
                ", clientName='" + clientName + '\'' +
                ", clientJwksUri='" + clientJwksUri + '\'' +
                ", clientTokenEndpointAuthMethod='" + clientTokenEndpointAuthMethod + '\'' +
                ", clientTokenEndpointAuthSigningAlg='" + clientTokenEndpointAuthSigningAlg + '\'' +
                ", clientRequestUris=" + clientRequestUris +
                ", clientFrontchannelLogoutUri=" + clientFrontchannelLogoutUri +
                ", clientSectorIdentifierUri='" + clientSectorIdentifierUri + '\'' +
                ", scope=" + scope +
                ", uiLocales=" + uiLocales +
                ", claimsLocales=" + claimsLocales +
                ", acrValues=" + acrValues +
                ", grantType=" + grantType +
                ", contacts=" + contacts +
                ", trustedClient=" + trustedClient +
                ", oxdRpProgrammingLanguage='" + oxdRpProgrammingLanguage + '\'' +
                '}';
    }

    @JsonIgnore
    @Override
    public String getOxdId() {
        return "no";
    }
}

