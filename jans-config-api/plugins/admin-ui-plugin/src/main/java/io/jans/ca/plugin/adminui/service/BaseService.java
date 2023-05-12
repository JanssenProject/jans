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
        return getToken(tokenRequest, tokenEndpoint, null);
    }

    /**
     * > This function takes a token request, a token endpoint, and a user info JWT, and returns a token response
     *
     * @param tokenRequest  This is the object that contains the parameters that are sent to the token endpoint.
     * @param tokenEndpoint The token endpoint of the authorization server.
     * @param userInfoJwt   This is the JWT that is returned from the userinfo endpoint.
     * @return A TokenResponse object
     */
    public io.jans.as.client.TokenResponse getToken(TokenRequest tokenRequest, String tokenEndpoint, String userInfoJwt) {

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
}
