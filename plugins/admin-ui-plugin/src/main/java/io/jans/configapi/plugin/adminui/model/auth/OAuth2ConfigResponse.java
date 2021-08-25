package io.jans.configapi.plugin.adminui.model.auth;

public class OAuth2ConfigResponse {

    private String authzBaseUrl;
    private String clientId;
    private String responseType;
    private String scope;
    private String redirectUrl;
    private String acrValues;
    private String frontChannelLogoutUrl;
    private String postLogoutRedirectUri;
    private String endSessionEndpoint;

    public String getAuthzBaseUrl() {
        return authzBaseUrl;
    }

    public void setAuthzBaseUrl(String authzBaseUrl) {
        this.authzBaseUrl = authzBaseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public String getFrontChannelLogoutUrl() {
        return frontChannelLogoutUrl;
    }

    public void setFrontChannelLogoutUrl(String frontChannelLogoutUrl) {
        this.frontChannelLogoutUrl = frontChannelLogoutUrl;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public void setEndSessionEndpoint(String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }

}
