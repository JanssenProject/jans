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

    public OIDCClientSettings() {
        //Do not remove
    }

    public OIDCClientSettings(String opHost, String clientId, String clientSecret) {

        this.opHost = opHost;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public OIDCClientSettings(String opHost, String clientId, String clientSecret, String tokenEndpoint, String introspectionEndpoint) {

        this.opHost = opHost;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenEndpoint = tokenEndpoint;
        this.introspectionEndpoint = introspectionEndpoint;
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

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getPostLogoutUri() {
        return postLogoutUri;
    }

    public void setPostLogoutUri(String postLogoutUri) {
        this.postLogoutUri = postLogoutUri;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
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

    public void setFrontchannelLogoutUri(String frontchannelLogoutUri) {
        this.frontchannelLogoutUri = frontchannelLogoutUri;
    }

    public String getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(String introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
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
                '}';
    }
}
