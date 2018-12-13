package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterSiteParams implements HasOxdIdParams {

    @JsonProperty(value = "op_host")
    private String op_host;
    @JsonProperty(value = "op_discovery_path")
    private String op_discovery_path;
    @JsonProperty(value = "authorization_redirect_uri")
    private String authorization_redirect_uri;
    @JsonProperty(value = "post_logout_redirect_uri")
    private String post_logout_redirect_uri;

    @JsonProperty(value = "redirect_uris")
    private List<String> redirect_uris;
    @JsonProperty(value = "response_types")
    private List<String> response_types;
    @JsonProperty(value = "claims_redirect_uri")
    private List<String> claims_redirect_uri;

    @JsonProperty(value = "client_id")
    private String client_id;
    @JsonProperty(value = "client_secret")
    private String client_secret;
    @JsonProperty(value = "client_registration_access_token")
    private String client_registration_access_token;
    @JsonProperty(value = "client_registration_client_uri")
    private String client_registration_client_uri;
    @JsonProperty(value = "client_name")
    private String client_name;
    @JsonProperty(value = "client_jwks_uri")
    private String client_jwks_uri;
    @JsonProperty(value = "client_token_endpoint_auth_method")
    private String client_token_endpoint_auth_method;
    @JsonProperty(value = "client_token_endpoint_auth_signing_alg")
    private String client_token_endpoint_auth_signing_alg;
    @JsonProperty(value = "client_request_uris")
    private List<String> client_request_uris;
    @JsonProperty(value = "client_frontchannel_logout_uris")
    private List<String> client_frontchannel_logout_uris;
    @JsonProperty(value = "client_sector_identifier_uri")
    private String client_sector_identifier_uri;

    @JsonProperty(value = "scope")
    private List<String> scope;
    @JsonProperty(value = "ui_locales")
    private List<String> ui_locales;
    @JsonProperty(value = "claims_locales")
    private List<String> claims_locales;
    @JsonProperty(value = "acr_values")
    private List<String> acr_values;
    @JsonProperty(value = "grant_types")
    private List<String> grant_types;
    @JsonProperty(value = "contacts")
    private List<String> contacts;
    @JsonProperty(value = "trusted_client")
    private Boolean trusted_client = false;
    @JsonProperty(value = "access_token_as_jwt")
    private Boolean access_token_as_jwt = false;
    @JsonProperty(value = "access_token_signing_alg")
    private String access_token_signing_alg;
    @JsonProperty(value = "rpt_as_jwt")
    private Boolean rpt_as_jwt;

    public RegisterSiteParams() {
    }

    public Boolean getRptAsJwt() {
        return rpt_as_jwt;
    }

    public void setRptAsJwt(Boolean rpt_as_jwt) {
        this.rpt_as_jwt = rpt_as_jwt;
    }

    public Boolean getAccessTokenAsJwt() {
        return access_token_as_jwt;
    }

    public void setAccessTokenAsJwt(Boolean access_token_as_jwt) {
        this.access_token_as_jwt = access_token_as_jwt;
    }

    public String getAccessTokenSigningAlg() {
        return access_token_signing_alg;
    }

    public void setAccessTokenSigningAlg(String access_token_signing_alg) {
        this.access_token_signing_alg = access_token_signing_alg;
    }

    public String getClientRegistrationAccessToken() {
        return client_registration_access_token;
    }

    public void setClientRegistrationAccessToken(String clientRegistrationAccessToken) {
        this.client_registration_access_token = clientRegistrationAccessToken;
    }

    public String getClientRegistrationClientUri() {
        return client_registration_client_uri;
    }

    public void setClientRegistrationClientUri(String clientRegistrationClientUri) {
        this.client_registration_client_uri = clientRegistrationClientUri;
    }

    public Boolean getTrustedClient() {
        return trusted_client;
    }

    public void setTrustedClient(Boolean trustedClient) {
        this.trusted_client = trustedClient;
    }

    public String getClientName() {
        return client_name;
    }

    public void setClientName(String clientName) {
        this.client_name = clientName;
    }

    public String getOpHost() {
        return op_host;
    }

    public void setOpHost(String opHost) {
        this.op_host = opHost;
    }

    public String getOpDiscoveryPath() {
        return op_discovery_path;
    }

    public void setOpDiscoveryPath(String opDiscoveryPath) {
        this.op_discovery_path = opDiscoveryPath;
    }

    public String getClientSectorIdentifierUri() {
        return client_sector_identifier_uri;
    }

    public void setClientSectorIdentifierUri(String clientSectorIdentifierUri) {
        this.client_sector_identifier_uri = clientSectorIdentifierUri;
    }

    public List<String> getClientFrontchannelLogoutUris() {
        return client_frontchannel_logout_uris;
    }

    public void setClientFrontchannelLogoutUris(List<String> clientFrontchannelLogoutUris) {
        this.client_frontchannel_logout_uris = clientFrontchannelLogoutUris;
    }

    public List<String> getClientRequestUris() {
        return client_request_uris;
    }

    public void setClientRequestUris(List<String> clientRequestUris) {
        this.client_request_uris = clientRequestUris;
    }

    public String getClientTokenEndpointAuthMethod() {
        return client_token_endpoint_auth_method;
    }

    public void setClientTokenEndpointAuthMethod(String clientTokenEndpointAuthMethod) {
        this.client_token_endpoint_auth_method = clientTokenEndpointAuthMethod;
    }

    public String getPostLogoutRedirectUri() {
        return post_logout_redirect_uri;
    }

    public void setPostLogoutRedirectUri(String post_logout_redirect_uri) {
        this.post_logout_redirect_uri = post_logout_redirect_uri;
    }

    public String getClientTokenEndpointAuthSigningAlg() {
        return client_token_endpoint_auth_signing_alg;
    }

    public void setClientTokenEndpointAuthSigningAlg(String clientTokenEndpointAuthSigningAlg) {
        this.client_token_endpoint_auth_signing_alg = clientTokenEndpointAuthSigningAlg;
    }

    public String getClientJwksUri() {
        return client_jwks_uri;
    }

    public void setClientJwksUri(String clientJwksUri) {
        this.client_jwks_uri = clientJwksUri;
    }

    public String getAuthorizationRedirectUri() {
        return authorization_redirect_uri;
    }

    public void setAuthorizationRedirectUri(String authorizationRedirectUri) {
        this.authorization_redirect_uri = authorizationRedirectUri;
    }

    public List<String> getClaimsLocales() {
        return claims_locales;
    }

    public void setClaimsLocales(List<String> claimsLocales) {
        this.claims_locales = claimsLocales;
    }

    public String getClientId() {
        return client_id;
    }

    public void setClientId(String clientId) {
        this.client_id = clientId;
    }

    public String getClientSecret() {
        return client_secret;
    }

    public void setClientSecret(String clientSecret) {
        this.client_secret = clientSecret;
    }

    public List<String> getGrantTypes() {
        return grant_types;
    }

    public void setGrantTypes(List<String> grantTypes) {
        this.grant_types = grantTypes;
    }

    public List<String> getRedirectUris() {
        return redirect_uris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirect_uris = redirectUris;
    }

    public List<String> getResponseTypes() {
        return response_types;
    }

    public void setResponseTypes(List<String> responseTypes) {
        this.response_types = responseTypes;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public List<String> getUiLocales() {
        return ui_locales;
    }

    public void setUiLocales(List<String> uiLocales) {
        this.ui_locales = uiLocales;
    }

    public List<String> getAcrValues() {
        return acr_values;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acr_values = acrValues;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public List<String> getClaimsRedirectUri() {
        return claims_redirect_uri;
    }

    public void setClaimsRedirectUri(List<String> claimsRedirectUri) {
        this.claims_redirect_uri = claimsRedirectUri;
    }


    @Override
    public String toString() {
        return "RegisterSiteParams{" +
                "op_host='" + op_host + '\'' +
                ", op_discovery_path='" + op_discovery_path + '\'' +
                ", authorization_redirect_uri='" + authorization_redirect_uri + '\'' +
                ", post_logout_redirect_uri='" + post_logout_redirect_uri + '\'' +
                ", redirect_uris=" + redirect_uris +
                ", response_types=" + response_types +
                ", claims_redirect_uri=" + claims_redirect_uri +
                ", client_id='" + client_id + '\'' +
                ", client_registration_access_token='" + client_registration_access_token + '\'' +
                ", client_registration_client_uri='" + client_registration_client_uri + '\'' +
                ", client_name='" + client_name + '\'' +
                ", client_jwks_uri='" + client_jwks_uri + '\'' +
                ", client_token_endpoint_auth_method='" + client_token_endpoint_auth_method + '\'' +
                ", client_token_endpoint_auth_signing_alg='" + client_token_endpoint_auth_signing_alg + '\'' +
                ", client_request_uris=" + client_request_uris +
                ", client_frontchannel_logout_uris=" + client_frontchannel_logout_uris +
                ", client_sector_identifier_uri='" + client_sector_identifier_uri + '\'' +
                ", scope=" + scope +
                ", ui_locales=" + ui_locales +
                ", claims_locales=" + claims_locales +
                ", acr_values=" + acr_values +
                ", grant_types=" + grant_types +
                ", contacts=" + contacts +
                ", trusted_client=" + trusted_client +
                ", access_token_as_jwt=" + access_token_as_jwt +
                ", access_token_signing_alg=" + access_token_signing_alg +
                ", rpt_as_jwt=" + rpt_as_jwt +
                '}';
    }

    @JsonIgnore
    @Override
    public String getOxdId() {
        return "no";
    }
}

