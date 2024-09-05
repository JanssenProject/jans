package io.jans.ca.plugin.adminui.model.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.jans.as.model.config.adminui.KeyValuePair;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppConfigResponse {
    @Schema(description = "Auth Server host", accessMode = Schema.AccessMode.READ_ONLY)
    private String authServerHost;
    @Schema(description = "Authorization base URL", accessMode = Schema.AccessMode.READ_ONLY)
    private String authzBaseUrl;
    @Schema(description = "OIDC Client ID", accessMode = Schema.AccessMode.READ_ONLY)
    private String clientId;
    @Schema(description = "OIDC Client Secret", accessMode = Schema.AccessMode.READ_ONLY)
    private String responseType;
    @Schema(description = "Scope", accessMode = Schema.AccessMode.READ_ONLY)
    private String scope;
    @Schema(description = "Redirect URL", accessMode = Schema.AccessMode.READ_ONLY)
    private String redirectUrl;
    @Schema(description = "ACR Value", accessMode = Schema.AccessMode.READ_WRITE)
    private String acrValues;
    @Schema(description = "Front Channel Logout URL", accessMode = Schema.AccessMode.READ_ONLY)
    private String frontChannelLogoutUrl;
    @Schema(description = "Post Logout Redirect URL", accessMode = Schema.AccessMode.READ_ONLY)
    private String postLogoutRedirectUri;
    @Schema(description = "End Session Endpoint", accessMode = Schema.AccessMode.READ_ONLY)
    private String endSessionEndpoint;
    @Schema(description = "Admin UI Session Timeout", accessMode = Schema.AccessMode.READ_WRITE)
    private Integer sessionTimeoutInMins;
    @Schema(description = "Additional Authentication Parameters", accessMode = Schema.AccessMode.READ_WRITE)
    private List<KeyValuePair> additionalParameters;

    public List<KeyValuePair> getAdditionalParameters() {
        return additionalParameters;
    }

    public void setAdditionalParameters(List<KeyValuePair> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    public Integer getSessionTimeoutInMins() {
        return sessionTimeoutInMins;
    }

    public void setSessionTimeoutInMins(Integer sessionTimeoutInMins) {
        this.sessionTimeoutInMins = sessionTimeoutInMins;
    }

    public String getAuthServerHost() {
        return authServerHost;
    }

    public String getAuthzBaseUrl() {
        return authzBaseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getScope() {
        return scope;
    }

    public String getRedirectUrl() {
        return redirectUrl;
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

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public void setEndSessionEndpoint(String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }

    public void setAuthServerHost(String authServerHost) {
        this.authServerHost = authServerHost;
    }

    public void setAuthzBaseUrl(String authzBaseUrl) {
        this.authzBaseUrl = authzBaseUrl;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public void setFrontChannelLogoutUrl(String frontChannelLogoutUrl) {
        this.frontChannelLogoutUrl = frontChannelLogoutUrl;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }
}
