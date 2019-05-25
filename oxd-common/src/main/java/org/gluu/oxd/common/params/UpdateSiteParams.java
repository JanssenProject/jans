package org.gluu.oxd.common.params;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/03/2016
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateSiteParams implements HasProtectionAccessTokenParams {

    @JsonProperty(value = "oxd_id")
    private String oxd_id;
    @JsonProperty(value = "authorization_redirect_uri")
    private String authorization_redirect_uri;
    @JsonProperty(value = "post_logout_redirect_uris")
    private List<String> post_logout_redirect_uris;

    @JsonProperty(value = "redirect_uris")
    private List<String> redirect_uris;
    @JsonProperty(value = "response_types")
    private List<String> response_types;

    @JsonProperty(value = "client_id")
    private String client_id;
    @JsonProperty(value = "client_secret")
    private String client_secret;
    @JsonProperty(value = "client_jwks_uri")
    private String client_jwks_uri;
    @JsonProperty(value = "client_sector_identifier_uri")
    private String client_sector_identifier_uri;
    @JsonProperty(value = "client_token_endpoint_auth_method")
    private String client_token_endpoint_auth_method;
    @JsonProperty(value = "client_request_uris")
    private List<String> client_request_uris;
    @JsonProperty(value = "client_logout_uris")
    private List<String> client_logout_uris;

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
    @JsonProperty(value = "protection_access_token")
    private String protection_access_token;
    @JsonProperty(value = "access_token_as_jwt")
    private Boolean access_token_as_jwt = false;
    @JsonProperty(value = "access_token_signing_alg")
    private String access_token_signing_alg;
    @JsonProperty(value = "rpt_as_jwt")
    private Boolean rpt_as_jwt;

    public UpdateSiteParams() {
    }

    public Boolean getRptAsJwt() {
        return rpt_as_jwt;
    }

    public void setRptAsJwt(Boolean rpt_as_jwt) {
        this.rpt_as_jwt = rpt_as_jwt;
    }

    public String getProtectionAccessToken() {
        return protection_access_token;
    }

    public void setProtectionAccessToken(String protectionAccessToken) {
        this.protection_access_token = protectionAccessToken;
    }

    public String getClientSectorIdentifierUri() {
        return client_sector_identifier_uri;
    }

    public void setClientSectorIdentifierUri(String clientSectorIdentifierUri) {
        this.client_sector_identifier_uri = clientSectorIdentifierUri;
    }

    public String getOxdId() {
        return oxd_id;
    }

    public void setOxdId(String oxdId) {
        this.oxd_id = oxdId;
    }

    public List<String> getClientLogoutUri() {
        return client_logout_uris;
    }

    public void setClientLogoutUri(List<String> clientLogoutUri) {
        this.client_logout_uris = clientLogoutUri;
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

    public List<String> getPostLogoutRedirectUris() {
        return post_logout_redirect_uris;
    }

    public void setPostLogoutRedirectUris(List<String> postLogoutRedirectUris) {
        this.post_logout_redirect_uris = postLogoutRedirectUris;
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

    public List<String> getGrantType() {
        return grant_types;
    }

    public void setGrantType(List<String> grantType) {
        this.grant_types = grantType;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("UpdateSiteParams");
        sb.append("{acr_values=").append(acr_values);
        sb.append(", oxd_id='").append(oxd_id).append('\'');
        sb.append(", authorization_redirect_uri='").append(authorization_redirect_uri).append('\'');
        sb.append(", post_logout_redirect_uris='").append(post_logout_redirect_uris).append('\'');
        sb.append(", redirect_uris=").append(redirect_uris);
        sb.append(", response_types=").append(response_types);
        sb.append(", client_id='").append(client_id).append('\'');
        sb.append(", client_secret='").append(client_secret).append('\'');
        sb.append(", client_sector_identifier_uri='").append(client_sector_identifier_uri).append('\'');
        sb.append(", scope=").append(scope);
        sb.append(", ui_locales=").append(ui_locales);
        sb.append(", claims_locales=").append(claims_locales);
        sb.append(", grant_types=").append(grant_types);
        sb.append(", contacts=").append(contacts);
        sb.append(", protection_access_token=").append(protection_access_token);
        sb.append(", access_token_as_jwt=").append(access_token_as_jwt);
        sb.append(", access_token_signing_alg=").append(access_token_signing_alg);
        sb.append(", rpt_as_jwt=").append(rpt_as_jwt);
        sb.append('}');
        return sb.toString();
    }

}