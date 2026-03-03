package io.jans.idp.authn.impl;

import io.jans.as.client.AuthorizationRequest;
import io.jans.as.client.AuthorizationResponse;
import io.jans.as.client.TokenClient;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.client.UserInfoClient;
import io.jans.as.client.UserInfoResponse;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.ResponseType;
import io.jans.as.model.jwt.Jwt;
import io.jans.idp.authn.context.JansAuthenticationContext;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JansAuthenticationService {

    private static final Logger LOG = LoggerFactory.getLogger(JansAuthenticationService.class);

    private String authServerBaseUrl;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userInfoEndpoint;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private List<String> scopes;
    
    public JansAuthenticationService() {
        this.scopes = Arrays.asList("openid", "profile", "email");
    }

    public void setAuthServerBaseUrl(@Nonnull String authServerBaseUrl) {
        this.authServerBaseUrl = authServerBaseUrl;
        this.authorizationEndpoint = authServerBaseUrl + "/jans-auth/restv1/authorize";
        this.tokenEndpoint = authServerBaseUrl + "/jans-auth/restv1/token";
        this.userInfoEndpoint = authServerBaseUrl + "/jans-auth/restv1/userinfo";
    }

    public void setClientId(@Nonnull String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(@Nonnull String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setRedirectUri(@Nonnull String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public void setScopes(@Nonnull List<String> scopes) {
        this.scopes = scopes;
    }

    @Nonnull
    public String buildAuthorizationUrl(@Nonnull JansAuthenticationContext context) {
        LOG.debug("Building authorization URL for relying party: {}", context.getRelayingPartyId());
        
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        
        context.setState(state);
        context.setNonce(nonce);

        AuthorizationRequest request = new AuthorizationRequest(
            Arrays.asList(ResponseType.CODE),
            clientId,
            scopes,
            redirectUri,
            nonce
        );
        
        request.setState(state);
        
        if (context.getAcrValues() != null && !context.getAcrValues().isEmpty()) {
            request.setAcrValues(Arrays.asList(context.getAcrValues().split(" ")));
        }
        
        String authUrl = authorizationEndpoint + "?" + request.getQueryString();
        
        context.setExternalProviderUri(authUrl);
        LOG.debug("Generated authorization URL: {}", authUrl);
        
        return authUrl;
    }

    public boolean processAuthorizationResponse(@Nonnull JansAuthenticationContext context, 
                                                @Nonnull String code, 
                                                @Nonnull String state) {
        LOG.debug("Processing authorization response, code present: {}", code != null);
        
        if (!state.equals(context.getState())) {
            LOG.error("State mismatch: expected {}, got {}", context.getState(), state);
            context.setErrorMessage("State parameter mismatch");
            return false;
        }
        
        context.setAuthorizationCode(code);
        
        try {
            TokenResponse tokenResponse = exchangeCodeForTokens(code);
            
            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                LOG.error("Failed to obtain tokens from authorization server");
                context.setErrorMessage("Token exchange failed");
                return false;
            }
            
            context.setAccessToken(tokenResponse.getAccessToken());
            context.setIdToken(tokenResponse.getIdToken());
            
            String userPrincipal = extractUserPrincipal(tokenResponse);
            if (userPrincipal == null) {
                userPrincipal = getUserInfoSubject(tokenResponse.getAccessToken());
            }
            
            if (userPrincipal != null) {
                context.setUserPrincipal(userPrincipal);
                context.setAuthenticated(true);
                LOG.info("Successfully authenticated user: {}", userPrincipal);
                return true;
            } else {
                LOG.error("Could not determine user principal");
                context.setErrorMessage("Could not determine user identity");
                return false;
            }
            
        } catch (Exception e) {
            LOG.error("Error processing authorization response", e);
            context.setErrorMessage("Authentication error: " + e.getMessage());
            return false;
        }
    }

    @Nullable
    private TokenResponse exchangeCodeForTokens(@Nonnull String code) {
        LOG.debug("Exchanging authorization code for tokens");
        
        try {
            TokenRequest tokenRequest = new TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(code);
            tokenRequest.setRedirectUri(redirectUri);
            tokenRequest.setAuthUsername(clientId);
            tokenRequest.setAuthPassword(clientSecret);
            tokenRequest.setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
            
            TokenClient tokenClient = new TokenClient(tokenEndpoint);
            tokenClient.setRequest(tokenRequest);
            
            return tokenClient.exec();
        } catch (Exception e) {
            LOG.error("Error exchanging code for tokens", e);
            return null;
        }
    }

    @Nullable
    private String extractUserPrincipal(@Nonnull TokenResponse tokenResponse) {
        String idToken = tokenResponse.getIdToken();
        if (idToken == null) {
            return null;
        }
        
        try {
            Jwt jwt = Jwt.parse(idToken);
            String sub = jwt.getClaims().getClaimAsString("sub");
            LOG.debug("Extracted subject from ID token: {}", sub);
            return sub;
        } catch (Exception e) {
            LOG.warn("Could not parse ID token to extract subject", e);
            return null;
        }
    }

    @Nullable
    private String getUserInfoSubject(@Nonnull String accessToken) {
        LOG.debug("Fetching user info from userinfo endpoint");
        
        try {
            UserInfoClient userInfoClient = new UserInfoClient(userInfoEndpoint);
            UserInfoResponse userInfoResponse = userInfoClient.execUserInfo(accessToken);
            
            if (userInfoResponse != null && userInfoResponse.getClaims() != null) {
                List<String> subjectClaims = userInfoResponse.getClaim("sub");
                if (subjectClaims != null && !subjectClaims.isEmpty()) {
                    return subjectClaims.get(0);
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not fetch user info", e);
        }
        
        return null;
    }
}
