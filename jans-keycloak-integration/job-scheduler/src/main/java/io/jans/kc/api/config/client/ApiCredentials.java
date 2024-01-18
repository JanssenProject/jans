package io.jans.kc.api.config.client;

public class ApiCredentials {
    
    private String bearerToken;

    public ApiCredentials(String bearerToken) {
        this.bearerToken = bearerToken;
    }
    
    public String bearerToken() {

        return this.bearerToken;
    }
}
