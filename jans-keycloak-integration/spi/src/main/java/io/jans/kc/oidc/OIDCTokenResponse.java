package io.jans.kc.oidc;

public interface OIDCTokenResponse {

    public OIDCAccessToken accessToken();
    public OIDCRefreshToken refreshToken();
    public OIDCTokenError error();
    public boolean indicatesSuccess();
}