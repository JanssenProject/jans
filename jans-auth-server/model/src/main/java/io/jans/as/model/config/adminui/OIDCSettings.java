package io.jans.as.model.config.adminui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public class OIDCSettings {

    private OIDCClientSettings authServerClient;
    private OIDCClientSettings tokenServerClient;

    public OIDCClientSettings getAuthServerClient() {
        return authServerClient;
    }

    public void setAuthServerClient(OIDCClientSettings authServerClient) {
        this.authServerClient = authServerClient;
    }

    public OIDCClientSettings getTokenServerClient() {
        return tokenServerClient;
    }

    public void setTokenServerClient(OIDCClientSettings tokenServerClient) {
        this.tokenServerClient = tokenServerClient;
    }
}
