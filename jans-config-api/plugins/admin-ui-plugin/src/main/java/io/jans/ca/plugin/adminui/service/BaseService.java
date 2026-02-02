package io.jans.ca.plugin.adminui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.jans.as.client.TokenRequest;
import io.jans.as.model.jwt.Jwt;
import io.jans.ca.plugin.adminui.model.auth.DCRResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.utils.AppConstants;
import io.jans.ca.plugin.adminui.utils.ClientFactory;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.core.service.ConfigHttpService;
import io.jans.model.net.HttpServiceResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class BaseService {

    @Inject
    Logger log;

    @Inject
    public ConfigHttpService httpService;

    protected ObjectMapper mapper = new ObjectMapper();

    protected static final String[] TLS_ENABLED_PROTOCOLS = new String[]{"TLSv1.3", "TLSv1.2"};
    protected static final String[] TLS_ALLOWED_CIPHER_SUITES = new String[]{
            // TLS 1.3 cipher suites
            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_CHACHA20_POLY1305_SHA256",
            // TLS 1.2 cipher suites
            // ECDHE + RSA
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            // ECDHE + ECDSA
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256"
    };

    /**
     * Obtain an access token from the authorization server using the provided token request.
     *
     * @param tokenRequest  the token request parameters (may include grant type, code, verifier, client credentials, etc.)
     * @param tokenEndpoint the token endpoint URL to send the request to
     * @return a TokenResponse containing the token data on success, or {@code null} on failure
     */
    public io.jans.as.client.TokenResponse getToken(TokenRequest tokenRequest, String tokenEndpoint) {
        return getToken(tokenRequest, tokenEndpoint, null);
    }


    /**
     * Sends a token request to the specified token endpoint using values from the provided TokenRequest.
     *
     * Constructs a form from the token request fields (code, scope, code_verifier, grant_type, redirect_uri, client_id)
     * and includes the optional `ujwt` parameter when `userInfoJwt` is provided, then POSTs the form to `tokenEndpoint`
     * using the credentials from `tokenRequest`.
     *
     * @param tokenRequest  values used to build the token request (authorization code, PKCE verifier, grant type, redirect URI, client id, and encoded credentials)
     * @param tokenEndpoint URL of the authorization server token endpoint
     * @param userInfoJwt   optional JWT to include as the `ujwt` form parameter when present
     * @return a TokenResponse populated from the endpoint JSON on HTTP 200; `null` otherwise
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

            if (!Strings.isNullOrEmpty(tokenRequest.getCodeVerifier())) {
                body.putSingle("code_verifier", tokenRequest.getCodeVerifier());
            }

            if (tokenRequest.getGrantType() != null && !Strings.isNullOrEmpty(tokenRequest.getGrantType().getValue())) {
                body.putSingle("grant_type", tokenRequest.getGrantType().getValue());
            }
            if (!Strings.isNullOrEmpty(tokenRequest.getRedirectUri())) {
                body.putSingle("redirect_uri", tokenRequest.getRedirectUri());
            }
            if (!Strings.isNullOrEmpty(tokenRequest.getAuthUsername())) {
                body.putSingle("client_id", tokenRequest.getAuthUsername());
            }

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
            log.error("Problems processing token call", e);
            throw e;
        }
        return null;
    }

    /**
     * Performs Dynamic Client Registration (DCR) using the provided Software Statement Assertion (SSA).
     *
     * @param ssaJwt the SSA JWT issued by the Scan server
     * @return a DCRResponse containing the registered client's ID, secret, issuer (opHost), hardwareId, and scan hostname when registration succeeds; `null` if registration fails
     */
    public DCRResponse executeDCR(String ssaJwt) {
        try {
            log.info("executing DCR");
            Integer httpStatus = null;
            if (Strings.isNullOrEmpty(ssaJwt)) {
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.BLANK_JWT.getDescription());
            }
            final Jwt tokenJwt = Jwt.parse(ssaJwt);
            Map<String, Object> claims = CommonUtils.getClaims(tokenJwt);

            if (claims.get("iss") == null) {
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.ISS_CLAIM_NOT_FOUND.getDescription());
            }

            if (claims.get("org_id") == null) {
                throw new ApplicationException(Response.Status.BAD_REQUEST.getStatusCode(), ErrorResponse.ORG_ID_CLAIM_NOT_FOUND.getDescription());
            }
            String issuer = StringUtils.removeEnd(claims.get("iss").toString(), "/");
            String hardwareId = claims.get("org_id").toString();

            CloseableHttpClient httpClient = httpService.createHttpsClientWithTlsPolicy(TLS_ENABLED_PROTOCOLS,
                    TLS_ALLOWED_CIPHER_SUITES);
            Map<String, String> body = new HashMap<>();
            body.put("software_statement", ssaJwt);
            body.put("response_types", "token");
            body.put("redirect_uris", issuer);
            body.put("client_name", "admin-ui-license-client-" + UUID.randomUUID().toString());

            HttpServiceResponse httpServiceResponse = httpService
                    .executePost(httpClient,
                            issuer + "/jans-auth/restv1/register",
                            null, null,
                            mapper.writeValueAsString(body),
                            ContentType.APPLICATION_JSON,
                            null);
            try {
                if (httpServiceResponse == null) {
                    log.error("Error in DCR: HTTP request failed, no response received");
                    return null;
                }
                String jsonString = null;
                if (httpServiceResponse.getHttpResponse() != null
                        && httpServiceResponse.getHttpResponse().getStatusLine() != null) {

                    log.debug(
                            "httpServiceResponse.getHttpResponse():{}, httpServiceResponse.getHttpResponse().getStatusLine():{}, httpServiceResponse.getHttpResponse().getEntity():{}",
                            httpServiceResponse.getHttpResponse(), httpServiceResponse.getHttpResponse().getStatusLine(),
                            httpServiceResponse.getHttpResponse().getEntity());
                    HttpEntity httpEntity = httpServiceResponse.getHttpResponse().getEntity();
                    httpStatus = httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode();
                    if (httpStatus == 201 && httpEntity != null) {
                        jsonString = httpService.getContent(httpEntity);
                        JsonNode entityNode = mapper.readTree(jsonString);
                        JSONObject entity = new JSONObject(entityNode.toString());

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
                    jsonString = httpService.getContent(httpEntity);
                    log.error("Error in DCR, Http Staus: {}, Message: {}", httpStatus, jsonString);
                    return null;
                }
            } finally {
                if (httpServiceResponse != null) {
                    httpServiceResponse.closeConnection(); // Returns connection to pool
                }
            }
            log.error("Error in DCR. HTTP Response is null");
            return null;
        } catch (Exception e) {
            log.error(ErrorResponse.ERROR_IN_DCR.getDescription(), e);
            return null;
        }

    }

    /**
     * Perform token introspection against the given introspection endpoint.
     *
     * @param accessToken           the access token to be introspected
     * @param introspectionEndpoint the full URL of the introspection endpoint
     * @return an Optional containing the introspection response as a Map when the server returns HTTP 200, `Optional.empty()` otherwise
     * @throws NoSuchAlgorithmException if a required cryptographic algorithm is unavailable when building the HTTP client
     * @throws KeyManagementException   if an error occurs initializing key management for the HTTP client
     */
    public Optional<Map<String, Object>> introspectToken(String accessToken, String introspectionEndpoint) throws
            NoSuchAlgorithmException, KeyManagementException {
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