package io.jans.casa.conf;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OIDCSettings {

    @JsonProperty("authz_redirect_uri")
    private String redirectUri;

    @JsonProperty("post_logout_uri")
    private String postLogoutUri;

    @JsonProperty("frontchannel_logout_uri")
    private String frontLogoutUri;

    private List<String> scopes;

    @JsonProperty("client")
    private OIDCClientSettings client;

    @JsonProperty("op_host")
    private String opHost;

    @JsonIgnore
    private List<String> acrValues;

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getPostLogoutUri() {
        return postLogoutUri;
    }

    public String getOpHost() {
        return opHost;
    }

    public List<String> getAcrValues() {
        return acrValues;
    }

    public String getFrontLogoutUri() {
        return frontLogoutUri;
    }

    public List<String> getScopes() {
        return scopes;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public OIDCClientSettings getClient() {
        return client;
    }

    public void setOpHost(String opHost) {
        this.opHost = opHost;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public void setPostLogoutUri(String postLogoutUri) {
        this.postLogoutUri = postLogoutUri;
    }

    public void setAcrValues(List<String> acrValues) {
        this.acrValues = acrValues;
    }

    public void setClient(OIDCClientSettings client) {
        this.client = client;
    }

    public void setFrontLogoutUri(String frontLogoutUri) {
        this.frontLogoutUri = frontLogoutUri;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

}
