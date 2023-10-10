package io.jans.as.model.config.adminui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public class OIDCSettings {

    private OIDCClientSettings auiWebClient;
    private OIDCClientSettings auiBackendApiClient;

    public OIDCClientSettings getAuiWebClient() {
        return auiWebClient;
    }

    public void setAuiWebClient(OIDCClientSettings auiWebClient) {
        this.auiWebClient = auiWebClient;
    }

    public OIDCClientSettings getAuiBackendApiClient() {
        return auiBackendApiClient;
    }

    public void setAuiBackendApiClient(OIDCClientSettings auiBackendApiClient) {
        this.auiBackendApiClient = auiBackendApiClient;
    }
}
