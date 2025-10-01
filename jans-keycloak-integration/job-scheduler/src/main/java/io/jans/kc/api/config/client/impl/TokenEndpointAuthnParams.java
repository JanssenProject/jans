package io.jans.kc.api.config.client.impl;

import java.util.ArrayList;
import java.util.List;

public class TokenEndpointAuthnParams {
    
    private enum AuthnMethod {
        AUTHN_METHOD_BASIC,
        AUTHN_METHOD_POST,
        AUTHN_METHOD_PRIVATE_KEY_JWT,
        AUTHN_METHOD_UNSUPPORTED
    }

    private String clientId;
    private String clientSecret;
    private AuthnMethod authnMethod;
    private List<String> scopes;

    private TokenEndpointAuthnParams(String clientId, String clientSecret, AuthnMethod authnMethod) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authnMethod = authnMethod;
        this.scopes = new ArrayList<String>();
    }

    private TokenEndpointAuthnParams(String clientId,String clientSecret, AuthnMethod authnMethod, List<String> scopes) {

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authnMethod = authnMethod;
        this.scopes = scopes;
    }

    public String clientId() {

        return this.clientId;
    }

    public String clientSecret() {

        return this.clientSecret;
    }

    public List<String> scopes() {

        return this.scopes;
    }

    public boolean isBasicAuthn() {

        return this.authnMethod == AuthnMethod.AUTHN_METHOD_BASIC;
    }

    public boolean isPostAuthn() {

        return this.authnMethod == AuthnMethod.AUTHN_METHOD_POST;
    }

    public boolean isPrivateKeyJwtAuthn() {

        return this.authnMethod == AuthnMethod.AUTHN_METHOD_PRIVATE_KEY_JWT;
    }

    public static final TokenEndpointAuthnParams basicAuthn(String clientId, String clientSecret,List<String> scopes) {

        return basicOrPostAuthn(clientId, clientSecret, AuthnMethod.AUTHN_METHOD_BASIC, scopes);
    }

    public static final TokenEndpointAuthnParams postAuthn(String clientId, String clientSecret, List<String> scopes) { 

        return basicOrPostAuthn(clientId, clientSecret, AuthnMethod.AUTHN_METHOD_POST, scopes);
    }

    private static final TokenEndpointAuthnParams basicOrPostAuthn(String clientId, String clientSecret,AuthnMethod authnMethod, List<String> scopes) {
        
        if(clientId == null || clientId.isEmpty()) {
            throw new TokenEndpointAuthnParamError("Missing clientId when creating basic authn credentials");
        }

        if(clientSecret == null || clientSecret.isEmpty()) {
            throw new TokenEndpointAuthnParamError("Missing client secret when creating basic authn credentials");
        }

        return new TokenEndpointAuthnParams(clientId, clientSecret,authnMethod,scopes);
    }
}
