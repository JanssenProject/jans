package io.jans.kc.oidc;

import java.net.URI;

public interface OIDCService {

    public URI getAuthorizationEndpoint(String issuerUrl) throws OIDCMetaError;
    public URI getTokenEndpoint(String issuerUrl) throws OIDCMetaError;
    public URI getUserInfoEndpoint(String issuerUrl) throws OIDCMetaError;
    public URI createAuthorizationUrl(String issuerUrl, OIDCAuthRequest request) throws OIDCMetaError;
    public OIDCTokenResponse requestTokens(String issuerUrl, OIDCTokenRequest tokenreq) throws OIDCTokenRequestError;
    public OIDCUserInfoResponse requestUserInfo(String issuerUrl, OIDCAccessToken accesstoken) throws OIDCUserInfoRequestError;
}
