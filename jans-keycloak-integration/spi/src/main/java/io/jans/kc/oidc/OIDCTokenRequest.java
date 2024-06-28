package io.jans.kc.oidc;

import java.net.URI;

public class OIDCTokenRequest {

    private String code;
    //in the future , replace this with a client credentials 
    //interface to support various authntication credential schemes
    private String clientId;
    private String clientSecret;
    private URI redirecturi;

    public OIDCTokenRequest(String code, String clientId,String clientSecret,URI redirecturi) {
        this.code = code;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirecturi = redirecturi;
    }

    public String getCode() {

        return this.code;
    }

    public String getClientId() {

        return this.clientId;
    }

    public String getClientSecret() {

        return this.clientSecret;
    }

    public URI getRedirectUri() {

        return this.redirecturi;
    }
}
