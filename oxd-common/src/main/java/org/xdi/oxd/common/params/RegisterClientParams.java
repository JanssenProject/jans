/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.common.params;

import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 13/08/2013
 */

public class RegisterClientParams implements IParams {

    @JsonProperty(value = "discovery_url")
    private String discoveryUrl;
    @JsonProperty(value = "redirect_uris")
    private List<String> redirectUrl;
    @JsonProperty(value = "logout_redirect_url")
    private String logoutRedirectUrl;
    @JsonProperty(value = "client_name")
    private String clientName;
    @JsonProperty(value = "response_types")
    private String responseTypes;
    @JsonProperty(value = "app_type")
    private String applicationType;
    @JsonProperty(value = "grant_types")
    private String grantTypes;
    @JsonProperty(value = "contacts")
    private String contacts;
    @JsonProperty(value = "jwks_uri")
    private String jwksUri;
    @JsonProperty(value = "token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;
    @JsonProperty(value = "token_endpoint_auth_signing_alg")
    private String tokenEndpointAuthSigningAlg;
    @JsonProperty(value = "request_uris")
    private List<String> requestUris;

    public RegisterClientParams() {
    }

    public RegisterClientParams(String discoveryUrl, List<String> redirectUrl, String clientName) {
        this.discoveryUrl = discoveryUrl;
        this.redirectUrl = redirectUrl;
        this.clientName = clientName;
    }

    public List<String> getRequestUris() {
        return requestUris;
    }

    public void setRequestUris(List<String> requestUris) {
        this.requestUris = requestUris;
    }

    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    public String getTokenEndpointAuthSigningAlg() {
        return tokenEndpointAuthSigningAlg;
    }

    public void setTokenEndpointAuthSigningAlg(String tokenEndpointAuthSigningAlg) {
        this.tokenEndpointAuthSigningAlg = tokenEndpointAuthSigningAlg;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getContacts() {
        return contacts;
    }

    public void setContacts(String contacts) {
        this.contacts = contacts;
    }

    public String getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(String grantTypes) {
        this.grantTypes = grantTypes;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getResponseTypes() {
        return responseTypes;
    }

    public void setResponseTypes(String responseTypes) {
        this.responseTypes = responseTypes;
    }

    public String getDiscoveryUrl() {
        return discoveryUrl;
    }

    public void setDiscoveryUrl(String p_discoveryUrl) {
        discoveryUrl = p_discoveryUrl;
    }

    public List<String> getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(List<String> redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getLogoutRedirectUrl() {
	return logoutRedirectUrl;
    }

    public void setLogoutRedirectUrl(String p_logoutRedirectUrl) {
 	this.logoutRedirectUrl = p_logoutRedirectUrl;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String p_clientName) {
        clientName = p_clientName;
    }

    /**
     * Returns string representation of object
     *
     * @return string representation of object
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RegisterClientParams");
        sb.append("{discoveryUrl='").append(discoveryUrl).append('\'');
        sb.append(", redirectUrl='").append(redirectUrl).append('\'');
        sb.append(", logoutRedirectUrl='").append(logoutRedirectUrl).append('\'');
        sb.append(", clientName='").append(clientName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
