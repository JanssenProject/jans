package io.jans.as.model.config.adminui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OIDCClientSettings {

    private String opHost;
    private String clientId;
    private String clientSecret;
    private String tokenEndpoint;
    private String redirectUri;
    private String postLogoutUri;
    private String frontchannelLogoutUri;
    private List<String> scopes;
    private List<String> acrValues;

    public OIDCClientSettings() {
        //Do not remove
    }

    public OIDCClientSettings(String opHost, String clientId, String clientSecret) {

        this.opHost = opHost;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public OIDCClientSettings(String opHost, String clientId, String clientSecret, String tokenEndpoint) {

        this.opHost = opHost;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenEndpoint = tokenEndpoint;
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

    @Override
    public String toString() {
        return "OIDCClientSettings{" +
                "opHost='" + opHost + '\'' +
                ", clientId='" + clientId + '\'' +
                ", clientSecret='" + clientSecret + '\'' +
                ", tokenEndpoint='" + tokenEndpoint + '\'' +
                ", redirectUri='" + redirectUri + '\'' +
                ", postLogoutUri='" + postLogoutUri + '\'' +
                ", frontchannelLogoutUri='" + frontchannelLogoutUri + '\'' +
                ", scopes=" + scopes +
                ", acrValues=" + acrValues +
                '}';
    }
}
