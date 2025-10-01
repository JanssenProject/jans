package io.jans.ca.plugin.adminui.service;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import io.jans.as.client.TokenRequest;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.ca.plugin.adminui.model.auth.DCRResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ClientFactory;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.*;

public class BaseService {

    @Inject
    Logger log;

    public io.jans.as.client.TokenResponse getToken(TokenRequest tokenRequest, String tokenEndpoint) {
        return getToken(tokenRequest, tokenEndpoint, null, null);
    }


    /**
     * This Java function sends a token request to a specified endpoint and returns the token response if successful.
     *
     * @param tokenRequest The `getToken` method you provided is used to exchange authorization code for an access token.
     * The `TokenRequest` parameter contains information required for this token exchange process, such as code, scope,
     * grant type, redirect URI, client ID, etc.
     * @param tokenEndpoint The `tokenEndpoint` parameter in the `getToken` method is the URL where the token request will
     * be sent to in order to obtain an access token. This URL typically belongs to the authorization server that issues
     * the access tokens.
     * @param userInfoJwt The `userInfoJwt` parameter in the `getToken` method is a JSON Web Token (JWT) that contains user
     * information. This token is typically used to provide information about the authenticated user to the authorization
     * server when requesting an access token. The user information in the JWT can include details such as the user
     * @param permissionTags The `permissionTags` parameter in the `getToken` method is a list of strings that represent
     * permission tags. These permission tags are used to specify the permissions that the client application is
     * requesting. The method processes these permission tags and includes them in the request body when making a call to
     * the token endpoint.
     * @return The method is returning a `io.jans.as.client.TokenResponse` object.
     */
    public io.jans.as.client.TokenResponse getToken(TokenRequest tokenRequest, String tokenEndpoint, String userInfoJwt, List<String> permissionTags) {

        try {
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

            if (permissionTags != null && !permissionTags.isEmpty()) {
                body.put("permission_tag", Collections.singletonList(String.join(" ", permissionTags)));
            }

            if (!Strings.isNullOrEmpty(tokenRequest.getCodeVerifier())) {
                body.putSingle("code_verifier", tokenRequest.getCodeVerifier());
            }

            body.putSingle("grant_type", tokenRequest.getGrantType().getValue());
            body.putSingle("redirect_uri", tokenRequest.getRedirectUri());
            body.putSingle("client_id", tokenRequest.getAuthUsername());

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(tokenEndpoint);
            Response response = request
                    .header("Authorization", "Basic " + tokenRequest.getEncodedCredentials())
                    .post(Entity.form(body));

            log.info("Get Access Token status code: {}", response.getStatus());
            if (response.getStatus() == 200) {
                String entity = response.readEntity(String.class);

                io.jans.as.client.TokenResponse tokenResponse = new io.jans.as.client.TokenResponse();
                tokenResponse.setEntity(entity);
                tokenResponse.injectDataFromJson(entity);

                return tokenResponse;
            }
            log.error("Error in getting access token: {}", response.getEntity());

        } catch (Exception e) {
            log.error("Problems processing token call");
            throw e;

        }
        return null;
    }

    /**
     * It takes a software statement assertion (SSA) as input, and returns a client ID and client secret
     *
     * @param ssaJwt The Software Statement Assertion (SSA) JWT that you received from the Scan server.
     * @return The client id and client secret of the newly created client.
     */
    public DCRResponse executeDCR(String ssaJwt) {
        try {
            log.info("executing DCR");

            if (Strings.isNullOrEmpty(ssaJwt)) {
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.BLANK_JWT.getDescription());
            }
            final Jwt tokenJwt = Jwt.parse(ssaJwt);
            Map<String, Object> claims = getClaims(tokenJwt);

            if (claims.get("iss") == null) {
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.ISS_CLAIM_NOT_FOUND.getDescription());
            }
            String issuer = StringUtils.removeEnd(claims.get("iss").toString(), "/");
            String hardwareId = claims.get("org_id").toString();
            //claims.get("iss").toString();
            Map<String, String> body = new HashMap<>();
            body.put("software_statement", ssaJwt);
            body.put("response_types", "token");
            body.put("redirect_uris", issuer);
            body.put("client_name", "admin-ui-license-client-" + UUID.randomUUID().toString());

            Invocation.Builder request = ClientFactory.instance().getClientBuilder(issuer + "/jans-auth/restv1/register");
            Response response = request
                    .header("Content-Type", "application/json")
                    .post(Entity.entity(body, MediaType.APPLICATION_JSON));

            log.info("DCR status code: {}", response.getStatus());
            if (response.getStatus() == 201) {
                JsonObject entity = response.readEntity(JsonObject.class);

                DCRResponse dcrResponse = new DCRResponse();
                dcrResponse.setClientId(entity.getString("client_id"));
                dcrResponse.setClientSecret(entity.getString("client_secret"));
                dcrResponse.setOpHost(issuer);
                dcrResponse.setHardwareId(hardwareId);
                if (issuer.equals(AppConstants.SCAN_DEV_AUTH_SERVER)) {
                    dcrResponse.setScanHostname(AppConstants.SCAN_DEV_SERVER);
                }
                if (issuer.equals(AppConstants.SCAN_PROD_AUTH_SERVER)) {
                    dcrResponse.setScanHostname(AppConstants.SCAN_PROD_SERVER);
                }
                return dcrResponse;
            }
            log.error("Error in DCR: {}", response.readEntity(String.class));
            return null;
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_DCR.getDescription(), e);
            return null;
        }

    }

    /**
     * It takes a JWT object and returns a Map of the claims
     *
     * @param jwtObj The JWT object that you want to get the claims from.
     * @return A map of claims.
     */
    public Map<String, Object> getClaims(Jwt jwtObj) {
        Map<String, Object> claims = Maps.newHashMap();
        if (jwtObj == null) {
            return claims;
        }
        JwtClaims jwtClaims = jwtObj.getClaims();
        Set<String> keys = jwtClaims.keys();
        keys.forEach(key -> {

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
                claims.put(key, (jwtClaims.getClaim(key)));
        });
        return claims;
    }

    public Optional<Map<String, Object>> introspectToken(String accessToken, String introspectionEndpoint) {
        log.info("Token introspection from auth-server.");
        Invocation.Builder request = ClientFactory.instance().getClientBuilder(introspectionEndpoint);
        request.header("Authorization", "Bearer " + accessToken);

        MultivaluedMap<String, String> body = new MultivaluedHashMap<>();
        body.putSingle("token", accessToken);

        Response response = request.post(Entity.form(body));

        log.info("Introspection response status code: {}", response.getStatus());

        if (response.getStatus() == 200) {
            Optional<Map<String, Object>> entity = Optional.of(response.readEntity(Map.class));
            log.info("Introspection response entity: {}", entity.get().toString());
            return entity;
        }
        return Optional.empty();
    }
}
