package io.jans.as.model.config.adminui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OIDCClientSettings {

    private String introspectionEndpoint;
    private String tokenEndpoint;
    private String redirectUri;
    private String postLogoutUri;
    private String frontchannelLogoutUri;
    private List<String> scopes;
    private List<String> acrValues;
    private String opHost;
    private String clientId;
    private String clientSecret;
    private List<KeyValuePair> additionalParameters;

    public OIDCClientSettings() {
        //Do not remove
    }

    public OIDCClientSettings(String opHost, String clientId, String clientSecret) {

        this.opHost = opHost;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public OIDCClientSettings(OIDCClientSettings oidcClientSettings) {
        this.introspectionEndpoint = oidcClientSettings.getIntrospectionEndpoint();
        this.tokenEndpoint = oidcClientSettings.getTokenEndpoint();
        this.redirectUri = oidcClientSettings.getRedirectUri();
        this.postLogoutUri = oidcClientSettings.getPostLogoutUri();
        this.frontchannelLogoutUri = oidcClientSettings.getFrontchannelLogoutUri();
        this.scopes = oidcClientSettings.getScopes();
        this.acrValues = oidcClientSettings.getAcrValues();
        this.opHost = oidcClientSettings.getOpHost();
        this.clientId = oidcClientSettings.getClientId();
        this.clientSecret = oidcClientSettings.getClientSecret();
        this.additionalParameters = oidcClientSettings.getAdditionalParameters();
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getOpHost() {
        return opHost;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getClientId() {
        return clientId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getClientSecret() {
        return clientSecret;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getPostLogoutUri() {
        return postLogoutUri;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public List<String> getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acrValues = acrValues;
    }

    public String getFrontchannelLogoutUri() {
        return frontchannelLogoutUri;
    }

    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public List<KeyValuePair> getAdditionalParameters() {
        return additionalParameters;
    }

    public void setAdditionalParameters(List<KeyValuePair> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    @Override
    public String toString() {
        return "OIDCClientSettings{" +
                "introspectionEndpoint='" + introspectionEndpoint + '\'' +
                ", tokenEndpoint='" + tokenEndpoint + '\'' +
                ", redirectUri='" + redirectUri + '\'' +
                ", postLogoutUri='" + postLogoutUri + '\'' +
                ", frontchannelLogoutUri='" + frontchannelLogoutUri + '\'' +
                ", scopes=" + scopes +
                ", acrValues=" + acrValues +
                ", opHost='" + opHost + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", additionalParameters='" + additionalParameters + '\'' +
                '}';
    }
}
