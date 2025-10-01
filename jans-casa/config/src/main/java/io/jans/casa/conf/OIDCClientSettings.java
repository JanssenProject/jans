package io.jans.casa.conf;

import com.fasterxml.jackson.annotation.JsonInclude;

public class OIDCClientSettings {

    private String clientId;
    private String clientSecret;
    private String clientName;

    public OIDCClientSettings() {
        //Do not remove
    }

    public OIDCClientSettings(String clientName, String clientId, String clientSecret) {

        this.clientName = clientName;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getClientId() {
        return clientId;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String getClientSecret() {
        return clientSecret;
    }

    public String getClientName() {
        return clientName;
    }

}
