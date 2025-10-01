package io.jans.kc.oidc.impl;

import com.nimbusds.oauth2.sdk.token.RefreshToken;

import io.jans.kc.oidc.OIDCRefreshToken;

public class NimbusOIDCRefreshToken implements OIDCRefreshToken{
    
    private RefreshToken refreshToken;

    public NimbusOIDCRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }

    private RefreshToken refreshTokenRef() {

        return this.refreshToken;
    }
}
