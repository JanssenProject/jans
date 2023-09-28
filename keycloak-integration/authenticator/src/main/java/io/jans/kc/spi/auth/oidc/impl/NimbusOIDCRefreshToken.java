package io.jans.kc.spi.auth.oidc.impl;

import com.nimbusds.oauth2.sdk.token.RefreshToken;

import io.jans.kc.spi.auth.oidc.OIDCRefreshToken;

public class NimbusOIDCRefreshToken implements OIDCRefreshToken{
    
    private RefreshToken refreshToken;

    public NimbusOIDCRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }

}
