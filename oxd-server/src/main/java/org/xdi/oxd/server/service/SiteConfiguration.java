package org.xdi.oxd.server.service;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 28/09/2015
 */

public class SiteConfiguration implements Serializable {

    @JsonProperty(value = "oxd_id")
    private String oxdId;
    @JsonProperty(value = "response_types")
    private List<String> responseTypes;
    @JsonProperty(value = "client_id")
    private String clientId;
    @JsonProperty(value = "client_secret")
    private String clientSecret;
    @JsonProperty(value = "redirect_uris")
    private List<String> redirectUris;
    @JsonProperty(value = "scope")
    private List<String> scope;
    @JsonProperty(value = "ui_locales")
    private List<String> uiLocales;
    @JsonProperty(value = "claims_locales")
    private List<String> claimsLocales;
    @JsonProperty(value = "acr_values")
    private List<String> acrValues;
    @JsonProperty(value = "grant_type")
    private String grantType;

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

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getOxdId() {
        return oxdId;
    }

    public void setOxdId(String oxdId) {
        this.oxdId = oxdId;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("SiteConfiguration");
        sb.append("{acrValues=").append(acrValues);
        sb.append(", oxdId='").append(oxdId).append('\'');
        sb.append(", responseTypes=").append(responseTypes);
        sb.append(", clientId='").append(clientId).append('\'');
        sb.append(", clientSecret='").append(clientSecret).append('\'');
        sb.append(", redirectUris=").append(redirectUris);
        sb.append(", scope=").append(scope);
        sb.append(", uiLocales=").append(uiLocales);
        sb.append(", claimsLocales=").append(claimsLocales);
        sb.append(", grantType='").append(grantType).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
