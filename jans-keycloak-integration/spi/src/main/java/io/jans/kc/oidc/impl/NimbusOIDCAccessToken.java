package io.jans.kc.oidc.impl;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import io.jans.kc.oidc.OIDCAccessToken;

public class NimbusOIDCAccessToken implements OIDCAccessToken {
    
    private AccessToken accessToken;

    public NimbusOIDCAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }
    
    public BearerAccessToken asBearerToken() {

         return new BearerAccessToken(accessToken.getValue());
    }
}
