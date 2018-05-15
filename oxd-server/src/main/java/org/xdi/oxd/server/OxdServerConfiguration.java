package org.xdi.oxd.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OxdServerConfiguration extends Configuration {

    @JsonProperty
    @NotEmpty
    private String oxdHost = "localhost";
    @JsonProperty
    @NotEmpty
    private String oxdPort = "8099";
    @JsonProperty
    @NotEmpty
    private String opHost = "https://ce-dev3.gluu.org";
    @JsonProperty
    @NotEmpty
    private String authorizationRedirectUrl = "https://client.example.com/cb";
    @JsonProperty
    @NotEmpty
    private String redirectUrl = "https://client.example.com/cb";
    @JsonProperty
    @NotEmpty
    private String logoutUrl = "https://client.example.com/logout";
    @JsonProperty
    @NotEmpty
    private String postLogoutRedirectUrl = "https://client.example.com/cb/logout";
    @JsonProperty
    @NotEmpty
    private String userID = "test_user";
    @JsonProperty
    @NotEmpty
    private String userSecret = "test_user_password";

    public String getOxdHost() {
        return oxdHost;
    }

    public void setOxdHost(String oxdHost) {
        this.oxdHost = oxdHost;
    }

    public String getOxdPort() {
        return oxdPort;
    }

    public void setOxdPort(String oxdPort) {
        this.oxdPort = oxdPort;
    }

    public String getOpHost() {
        return opHost;
    }

    public void setOpHost(String opHost) {
        this.opHost = opHost;
    }

    public String getAuthorizationRedirectUrl() {
        return authorizationRedirectUrl;
    }

    public void setAuthorizationRedirectUrl(String authorizationRedirectUrl) {
        this.authorizationRedirectUrl = authorizationRedirectUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    public String getPostLogoutRedirectUrl() {
        return postLogoutRedirectUrl;
    }

    public void setPostLogoutRedirectUrl(String postLogoutRedirectUrl) {
        this.postLogoutRedirectUrl = postLogoutRedirectUrl;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserSecret() {
        return userSecret;
    }

    public void setUserSecret(String userSecret) {
        this.userSecret = userSecret;
    }

    @Override
    public String toString() {
        return "OxdHttpsConfiguration{" +
                "oxdHost='" + oxdHost + '\'' +
                ", oxdPort='" + oxdPort + '\'' +
                ", opHost='" + opHost + '\'' +
                ", authorizationRedirectUrl='" + authorizationRedirectUrl + '\'' +
                ", redirectUrl='" + redirectUrl + '\'' +
                ", logoutUrl='" + logoutUrl + '\'' +
                ", postLogoutRedirectUrl='" + postLogoutRedirectUrl + '\'' +
                ", userID='" + userID + '\'' +
                ", userSecret='" + userSecret + '\'' +
                "} " + super.toString();
    }
}
