package io.jans.configapi.plugin.adminui.service.auth;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import io.jans.as.client.TokenRequest;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.configapi.plugin.adminui.model.auth.TokenResponse;
import io.jans.configapi.plugin.adminui.model.auth.UserInfoRequest;
import io.jans.configapi.plugin.adminui.model.auth.UserInfoResponse;
import io.jans.configapi.plugin.adminui.model.config.AUIConfiguration;
import io.jans.configapi.plugin.adminui.model.exception.ApplicationException;
import io.jans.configapi.plugin.adminui.service.config.AUIConfigurationService;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class OAuth2Service {
    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    private Client client = ClientBuilder.newClient();

    /**
     * Calls token endpoint from the Identity Provider and returns a valid Access Token.
     */
    public TokenResponse getAccessToken(String code) throws Exception {
        try {
            log.info("Getting access token with code: {}", code);
            if(Strings.isNullOrEmpty(code)) {
                log.error("Bad Request: Authourization `code` blank or empty.");
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), "Bad Request: Authourization `code` blank or empty.");
            }
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setCode(code);

            tokenRequest.setAuthUsername(auiConfiguration.getAuthServerClientId());
            tokenRequest.setAuthPassword(auiConfiguration.getAuthServerClientSecret());
            tokenRequest.setGrantType(GrantType.AUTHORIZATION_CODE);
            tokenRequest.setRedirectUri(auiConfiguration.getAuthServerRedirectUrl());
            tokenRequest.setScope(auiConfiguration.getAuthServerScope());
            io.jans.as.client.TokenResponse tokenResponse = getToken(tokenRequest, auiConfiguration.getAuthServerTokenEndpoint());

            TokenResponse tokenResp = new TokenResponse();
            tokenResp.setAccessToken(tokenResponse.getAccessToken());
            tokenResp.setIdToken(tokenResponse.getIdToken());
            tokenResp.setRefreshToken(tokenResponse.getRefreshToken());

            return tokenResp;
        } catch (ApplicationException e) {
            log.error("Problems processing access token call", e);
            throw e;
        } catch (Exception e) {
            log.error("Problems processing access token call", e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Problems processing access token call. Check logs for details.");
        }
    }

    /**
     * Calls token endpoint from the Identity Provider and returns a valid Access Token.
     */
    public TokenResponse getApiProtectionToken(String userInfoJwt) throws Exception {
        try {
            log.info("Getting api-protection token with userInfoJwt: {}", userInfoJwt);
            if(Strings.isNullOrEmpty(userInfoJwt)) {
                log.error("Bad Request: User-Info jwt is blank or empty.");
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), "Bad Request: User-Info jwt is blank or empty.");
            }
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            io.jans.as.client.TokenRequest tokenRequest = new io.jans.as.client.TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setAuthUsername(auiConfiguration.getTokenServerClientId());
            tokenRequest.setAuthPassword(auiConfiguration.getTokenServerClientSecret());
            tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setRedirectUri(auiConfiguration.getTokenServerRedirectUrl());

            io.jans.as.client.TokenResponse tokenResponse = getToken(tokenRequest, auiConfiguration.getTokenServerTokenEndpoint(), userInfoJwt);

            final Jwt tokenJwt = Jwt.parse(tokenResponse.getAccessToken());
            Map<String, Object> claims = getClaims(tokenJwt);
            TokenResponse tokenResp = new TokenResponse();
            tokenResp.setAccessToken(tokenResponse.getAccessToken());
            tokenResp.setIdToken(tokenResponse.getIdToken());
            tokenResp.setRefreshToken(tokenResponse.getRefreshToken());

            if (claims.get("scope") != null && claims.get("scope") instanceof List) {
                tokenResp.setScopes((List) claims.get("scope"));
            }

            if (claims.get("iat") != null) {
                tokenResp.setIat(Long.valueOf(claims.get("iat").toString()));
            }

            if (claims.get("exp") != null) {
                tokenResp.setExp(Long.valueOf(claims.get("exp").toString()));
            }

            if (claims.get("iss") != null) {
                tokenResp.setIssuer(claims.get("iss").toString());
            }

            return tokenResp;

        } catch (ApplicationException e) {
            log.error("Problems processing api-protection token call", e);
            throw e;
        } catch (Exception e) {
            log.error("Problems processing api-protection token call", e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Problems processing api-protection token call. Check logs for details.");
        }
    }

    public UserInfoResponse getUserInfo(UserInfoRequest userInfoRequest) throws Exception {
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine();
        try {
            log.info("Getting User-Info from auth-server: {}", userInfoRequest.getAccessToken());
            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();

            final URI userInfoUri = new URI(auiConfiguration.getAuthServerUserInfoEndpoint());

            String accessToken = org.apache.logging.log4j.util.Strings.isNotBlank(userInfoRequest.getAccessToken()) ? userInfoRequest.getAccessToken() : null;

            if (Strings.isNullOrEmpty(userInfoRequest.getCode()) && Strings.isNullOrEmpty(accessToken)) {
                log.error("Bad Request: Either `code` or `access_token` is required.");
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), "Bad Request: Either `code` or `access_token` is required.");
            }

            if (org.apache.logging.log4j.util.Strings.isNotBlank(userInfoRequest.getCode()) && org.apache.logging.log4j.util.Strings.isBlank(accessToken)) {
                TokenResponse tokenResponse = getAccessToken(userInfoRequest.getCode());
                accessToken = tokenResponse.getAccessToken();
            }
            log.info("Access Token : {}", accessToken);
            MultivaluedMap<String, String> body = new MultivaluedHashMap<>();
            body.putSingle("access_token", accessToken);

            ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
            ResteasyWebTarget target = client.target(UriBuilder.fromPath(auiConfiguration.getAuthServerUserInfoEndpoint()));

            Response response = target.request()
                    .header("Authorization", "Bearer " + accessToken)
                    .post(Entity.form(body));

            log.info("User-Info response status code: {}", response.getStatus());

            if (response.getStatus() == 200) {
                String entity = response.readEntity(String.class);
                log.info("User-Info response entity: {}", entity);
                final Jwt jwtUserInfo = Jwt.parse(entity);

                log.info("User-Info response jwtUserInfo: {}", jwtUserInfo.toString());

                UserInfoResponse userInfoResponse = new UserInfoResponse();
                userInfoResponse.setClaims(getClaims(jwtUserInfo));
                userInfoResponse.setJwtUserInfo(entity);

                log.info("User-Info response userInfoResponse: {}", userInfoResponse.toString());

                return userInfoResponse;
            }

        } catch (ApplicationException e) {
            log.error("Problems processing user-info call", e);
            throw e;
        } catch (Exception e) {
            log.error("Problems processing user-info call", e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Problems processing user-info call. Check logs for details.");
        } finally {
            if (engine != null) {
                engine.close();
            }
        }
        return null;
    }

    /**
     * Calls token endpoint from the Identity Provider and returns a valid Token.
     */

    public io.jans.as.client.TokenResponse getToken(TokenRequest tokenRequest, String tokenEndpoint) throws Exception {
        return getToken(tokenRequest, tokenEndpoint, null);
    }

    public io.jans.as.client.TokenResponse getToken(TokenRequest tokenRequest, String tokenEndpoint, String userInfoJwt) throws Exception {
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine();
        try {
            engine.setFollowRedirects(false);

            MultivaluedMap<String, String> body = new MultivaluedHashMap<>();
            if (!Strings.isNullOrEmpty(tokenRequest.getCode())) {
                body.putSingle("code", tokenRequest.getCode());
            }

            if (!Strings.isNullOrEmpty(tokenRequest.getScope())) {
                body.putSingle("scope", tokenRequest.getScope());
            }

            if (!Strings.isNullOrEmpty(userInfoJwt)) {
                body.putSingle("ujwt", userInfoJwt);
            }

            body.putSingle("grant_type", tokenRequest.getGrantType().getValue());
            body.putSingle("redirect_uri", tokenRequest.getRedirectUri());
            body.putSingle("client_id", tokenRequest.getAuthUsername());

            ResteasyClient client = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).httpEngine(engine).build();
            ResteasyWebTarget target = client.target(UriBuilder.fromPath(tokenEndpoint));

            Response response = target.request()
                    .header("Authorization", "Basic " + tokenRequest.getEncodedCredentials())
                    .post(Entity.form(body));

            log.info("Get Access Token body: {}", body.toString());
            log.info("Get Access Token status code: {}", response.getStatus());
            if (response.getStatus() == 200) {
                String entity = response.readEntity(String.class);

                io.jans.as.client.TokenResponse tokenResponse = new io.jans.as.client.TokenResponse();
                tokenResponse.setEntity(entity);
                tokenResponse.injectDataFromJson(entity);

                return tokenResponse;
            }

        } catch(Exception e) {
            log.error("Problems processing IdP token call", e);
            throw e;

        } finally {
            if (engine != null) {
                engine.close();
            }
        }
        return null;
    }

    private Map<String, Object> getClaims(Jwt jwtObj) {
        if (jwtObj == null) {
            return null;
        }
        JwtClaims jwtClaims = jwtObj.getClaims();
        Map<String, Object> claims = Maps.newHashMap();
        Set<String> keys = jwtClaims.keys();
        keys.forEach((key) -> {

            if (jwtClaims.getClaim(key) instanceof String)
                claims.put(key, jwtClaims.getClaim(key).toString());
            if (jwtClaims.getClaim(key) instanceof Integer)
                claims.put(key, Integer.valueOf(jwtClaims.getClaim(key).toString()));
            if (jwtClaims.getClaim(key) instanceof Long)
                claims.put(key, Long.valueOf(jwtClaims.getClaim(key).toString()));
            if (jwtClaims.getClaim(key) instanceof Boolean)
                claims.put(key, Boolean.valueOf(jwtClaims.getClaim(key).toString()));

            else if (jwtClaims.getClaim(key) instanceof JSONArray) {
                List<String> sourceArr = jwtClaims.getClaimAsStringList(key);
                claims.put(key, sourceArr);
            } else if (jwtClaims.getClaim(key) instanceof JSONObject)
                claims.put(key, ((JSONObject) jwtClaims.getClaim(key)));
        });
        return claims;
    }
}
