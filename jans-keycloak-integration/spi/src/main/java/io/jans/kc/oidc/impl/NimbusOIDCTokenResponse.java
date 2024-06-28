package io.jans.kc.oidc.impl;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.token.Tokens;

import io.jans.kc.oidc.OIDCAccessToken;
import io.jans.kc.oidc.OIDCRefreshToken;
import io.jans.kc.oidc.OIDCTokenError;
import io.jans.kc.oidc.OIDCTokenResponse;


public class NimbusOIDCTokenResponse implements OIDCTokenResponse {
    
    private TokenResponse tokenResponse;
    private NimbusOIDCAccessToken accessToken;
    private NimbusOIDCRefreshToken refreshToken;
    private OIDCTokenError tokenError;

    public NimbusOIDCTokenResponse(TokenResponse tokenResponse) {

        this.tokenResponse = tokenResponse;
        if(this.tokenResponse.indicatesSuccess()) {
            AccessTokenResponse atresponse = this.tokenResponse.toSuccessResponse();
            Tokens tokens = atresponse.getTokens();
            this.accessToken = new NimbusOIDCAccessToken(tokens.getAccessToken());
            this.refreshToken = new NimbusOIDCRefreshToken(tokens.getRefreshToken());
        }
    }

    @Override
    public OIDCAccessToken accessToken() {

        return accessToken;
    }

    @Override
    public OIDCRefreshToken refreshToken() {

        return refreshToken;
    }

    @Override
    public OIDCTokenError error() {

        return tokenError;
    }

    @Override
    public boolean indicatesSuccess() {

        return tokenResponse.indicatesSuccess();
    }
}
