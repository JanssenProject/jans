package io.jans.kc.spi.auth.oidc;

public interface OIDCTokenResponse {

    public OIDCAccessToken accessToken();
    public OIDCRefreshToken refreshToken();
    public OIDCTokenError error();
    public boolean indicatesSuccess();
}