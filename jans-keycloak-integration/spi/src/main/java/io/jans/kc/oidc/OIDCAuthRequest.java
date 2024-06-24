package io.jans.kc.oidc;

import java.util.List;
import java.util.ArrayList;

public class OIDCAuthRequest {
    
    private String clientId;
    private String state;
    private String nonce;
    private List<String> scopes;
    private List<String> responseTypes;
    private String redirectUri;

    public OIDCAuthRequest() {

        this.clientId = null;
        this.state = null;
        this.nonce = null;
        this.scopes = new ArrayList<>();
        this.responseTypes = new ArrayList<>();
        this.redirectUri = null;
    }

    public String getClientId() {

        return this.clientId;
    }

    public void setClientId(String clientId) {

        this.clientId = clientId;
    }

    public void setState(String state) {
        
        this.state = state;
    }

    public final String getState() {

        return this.state;
    }

    public void setNonce(String nonce) {

        this.nonce = nonce;
    }

    public final String getNonce() {

        return this.nonce;
    }

    public void addScope(String scope) {

        this.scopes.add(scope);
    }

    public final List<String> getScopes() {

        return this.scopes;
    }

    public void addResponseType(String responseType) {

        this.responseTypes.add(responseType);
    }

    public final List<String> getResponseTypes() {

        return this.responseTypes;
    }


    public void setRedirectUri(String redirectUri) {

        this.redirectUri = redirectUri;
    }
    
    public String getRedirectUri() {

        return this.redirectUri;
    }

}
