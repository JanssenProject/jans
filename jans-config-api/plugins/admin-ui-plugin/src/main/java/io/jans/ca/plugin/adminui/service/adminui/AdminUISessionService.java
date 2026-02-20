package io.jans.ca.plugin.adminui.service.adminui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import io.jans.as.client.TokenRequest;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.configapi.core.model.adminui.AUIConfiguration;
import io.jans.configapi.core.model.adminui.AdminUISession;
import io.jans.configapi.core.model.exception.ConfigApiApplicationException;
import io.jans.configapi.core.service.ConfigHttpService;
import io.jans.model.net.HttpServiceResponse;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;
import io.jans.service.EncryptionService;
import io.jans.util.security.StringEncrypter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.jans.ca.plugin.adminui.utils.CommonUtils.addMinutes;

import static io.jans.as.model.util.Util.escapeLog;

@ApplicationScoped
public class AdminUISessionService {

    private static final String SID_MSG = "Get Config API Session by sid:{}";
    private static final String SID_ERROR = "Failed to load  Config API session entry with sid ";
    private static final String ADMIN_UI_CONFIG_DN = "ou=admin-ui,ou=configuration,o=jans";
    private static final String SID = "sid";
    private static final String SESSION_DN = "ou=adminUISession,ou=admin-ui,o=jans";
    private static final String TOKEN_GENERATION_ERROR = "Error in generating token to access Config API endpoints";
    @Inject
    Logger logger;

    @Inject
    PersistenceEntryManager persistenceEntryManager;


    @Inject
    EncryptionService encryptionService;

    @Inject
    ConfigHttpService httpService;

    @Inject
    AUIConfigurationService auiConfigurationService;

    /**
     * Builds the LDAP distinguished name (DN) for a session identifier.
     *
     * @param sessionId the session identifier to include in the DN
     * @return the session DN in the form "inum={sessionId},<base session DN>"
     */
    private String getDnForSession(String sessionId) {
        return String.format("inum=%s,%s", sessionId, SESSION_DN);
    }

    /**
     * Load the Admin UI session corresponding to the given session identifier.
     *
     * @param sessionId the session identifier used to build the session DN
     * @return the matching AdminUISession, or `null` if no session is found or an error occurs
     */
    public AdminUISession getSession(String sessionId) {
        if (logger.isInfoEnabled()) {
            logger.info(SID_MSG, escapeLog(sessionId));
        }

        AdminUISession configApiSession = null;
        try {
            configApiSession = persistenceEntryManager
                    .find(AdminUISession.class, getDnForSession(sessionId));
        } catch (EntryPersistenceException e) {
            //do not throw error if the record is not present in database
            return configApiSession;
        } catch (Exception ex) {
            logger.error(SID_ERROR + "{}", sessionId, ex);
            throw ex;
        }
        return configApiSession;
    }

    /**
     * Updates the expiration time of an Admin UI session based on user activity.
     * <p>
     * After a successful login, an {@link AdminUISession} is persisted in the database
     * with an expiration date derived from the configured session timeout. On each
     * subsequent request, this method may extend the session expiration to enforce
     * an idle-based logout policy.
     * </p>
     *
     * <p>
     * The session expiration is refreshed only when:
     * <ul>
     *   <li>The session and its expiration date are not {@code null}</li>
     *   <li>More than 30 seconds has passed since the session was last updated</li>
     * </ul>
     * </p>
     *
     * <p>
     * This approach prevents frequent database updates while ensuring that an
     * active user session remains valid and a force logout occurs only when the
     * application has been idle longer than the configured
     * {@code max_idle_time}.
     * </p>
     *
     * @param adminUISession the persisted Admin UI session to be evaluated and updated
     */
    public void updateSessionExpiryDate(AdminUISession adminUISession) {
        if (adminUISession == null || adminUISession.getExpirationDate() == null) {
            return;
        }
        try {
            Date lastUpdated = adminUISession.getLastUpdated();
            long nowMillis = System.currentTimeMillis();

            // Update expiry date only if last update was more than 30 sec ago : the intent of the 30-second throttle is to reduce database writes
            if (lastUpdated != null) {
                long secondsSinceLastUpdate =
                        TimeUnit.MILLISECONDS.toSeconds(nowMillis - lastUpdated.getTime());

                if (secondsSinceLastUpdate < 30) {
                    return;
                }
            }

            AUIConfiguration config = auiConfigurationService.getAUIConfiguration();
            if (config == null || config.getSessionTimeoutInMins() == null) {
                logger.warn("AUI configuration is null, cannot update session expiry");
                return;
            }
            int sessionTimeoutMins = config.getSessionTimeoutInMins();

            Date now = new Date(nowMillis);
            // do not update if the sesiion is already expired. AdminUICookieFilter will remove this session.
            if (adminUISession.getExpirationDate().before(now)) {
                return;
            }
            adminUISession.setExpirationDate(addMinutes(now, sessionTimeoutMins));
            adminUISession.setLastUpdated(now);
            persistenceEntryManager.merge(adminUISession);
        } catch (Exception e) {
            logger.warn("Failed to update session expiry for session {}",
                    adminUISession.getSessionId(), e);
        }
    }

    /**
     * Removes all AdminUISession entries whose expirationDate is earlier than the current time.
     * This method queries sessions under the service's session base DN and deletes any persisted
     * AdminUISession whose expiration date has already passed.
     */
    public void removeAllExpiredSessions() {
        final Filter filter = Filter.createPresenceFilter(SID);
        List<AdminUISession> adminUISessions = persistenceEntryManager.findEntries(SESSION_DN, AdminUISession.class, filter);
        Date currentDate = new Date();
        adminUISessions.stream().filter(ele ->
                        ((ele.getExpirationDate().getTime() - currentDate.getTime()) < 0))
                .forEach(e -> persistenceEntryManager.remove(e));
    }

    /**
     * Checks whether a cached token is active by calling the Admin UI introspection endpoint.
     *
     * @param token            the token to introspect; may be null or empty
     * @param auiConfiguration configuration holding the introspection endpoint URL
     * @return `true` if the introspection response contains `"active": true`, `false` otherwise
     * @throws JsonProcessingException if the introspection response body cannot be parsed as JSON
     */
    public boolean isCachedTokenValid(String token, AUIConfiguration auiConfiguration) throws JsonProcessingException {
        logger.debug("Inside isCachedTokenValid : Token introspection from auth-server.");

        Map<String, String> body = new HashMap<>();

        if (!Strings.isNullOrEmpty(token)) {
            body.put("token", token);
        }

        HttpServiceResponse httpServiceResponse = httpService
                .executePost(auiConfiguration.getAuiBackendApiServerIntrospectionEndpoint(),
                        token, CommonUtils.toUrlEncodedString(body),
                        ContentType.APPLICATION_FORM_URLENCODED,
                        "Bearer ");
        String jsonString = null;
        if (httpServiceResponse.getHttpResponse() != null
                && httpServiceResponse.getHttpResponse().getStatusLine() != null) {

            logger.debug(
                    "httpServiceResponse.getHttpResponse():{}, httpServiceResponse.getHttpResponse().getStatusLine():{}, httpServiceResponse.getHttpResponse().getEntity():{}",
                    httpServiceResponse.getHttpResponse(), httpServiceResponse.getHttpResponse().getStatusLine(),
                    httpServiceResponse.getHttpResponse().getEntity());
            if (httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();

                HttpEntity httpEntity = httpServiceResponse.getHttpResponse().getEntity();
                if (httpEntity != null) {
                    jsonString = httpService.getContent(httpEntity);

                    HashMap<String, Object> payloadMap = mapper.readValue(jsonString, HashMap.class);
                    if (payloadMap.containsKey("active")) {
                        return (boolean) payloadMap.get("active");
                    }
                    return false;
                }
                logger.error("Error in introspection of token to access Config API endpoints. Response is null.");
            }
        }
        return false;
    }

    /**
     * Obtains an API protection access token for the Admin UI backend using client credentials and a user-info JWT.
     *
     * @param ujwtString       the user-info JWT to include in the token request; must be non-null and non-empty to generate a token
     * @param auiConfiguration configuration containing the backend token endpoint, client ID, encrypted client secret, and redirect URI
     * @return a TokenResponse containing the access token, or `null` if `ujwtString` is null or empty
     * @throws StringEncrypter.EncryptionException if decrypting the client secret fails
     * @throws JsonProcessingException             if parsing token responses fails
     */
    public TokenResponse getApiProtectionToken(String ujwtString, AUIConfiguration auiConfiguration) throws StringEncrypter.EncryptionException, ConfigApiApplicationException {
        try {
            logger.debug("Getting api-protection token");
            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setAuthUsername(auiConfiguration.getAuiBackendApiServerClientId());
            tokenRequest.setAuthPassword(encryptionService.decrypt(auiConfiguration.getAuiBackendApiServerClientSecret()));
            tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setRedirectUri(auiConfiguration.getAuiBackendApiServerRedirectUrl());

            Map<String, Object> tokenResponse = null;
            if (Strings.isNullOrEmpty(ujwtString)) {
                logger.warn("User-Info JWT is null or empty. Token to access Config Api will not be generated.");
                return null;
            }
            tokenResponse = getToken(tokenRequest, auiConfiguration.getAuiBackendApiServerTokenEndpoint(), ujwtString);

            TokenResponse tokenResp = new TokenResponse();
            tokenResp.setAccessToken((String) tokenResponse.get("access_token"));
            return tokenResp;
        } catch (Exception e) {
            logger.error(TOKEN_GENERATION_ERROR, e);
            throw e;
        }
    }

    /**
     * Exchange token request parameters with the authorization server and return parsed token response parameters.
     *
     * @param tokenRequest  token request details (grant type, client credentials, redirect URI; may include authorization code and PKCE verifier)
     * @param tokenEndpoint the token endpoint URL to call
     * @param userInfoJwt   optional user-info JWT to include as the `ujwt` parameter
     * @return a map of token response parameters (for example `access_token`, `expires_in`) with any `token_type` entry removed
     * @throws ConfigApiApplicationException if the HTTP exchange fails or the response cannot be parsed as JSON
     */
    public Map<String, Object> getToken(TokenRequest tokenRequest, String tokenEndpoint, String userInfoJwt) throws ConfigApiApplicationException {

        try {
            Map<String, String> body = new HashMap<>();
            if (!Strings.isNullOrEmpty(tokenRequest.getCode())) {
                body.put("code", tokenRequest.getCode());
            }

            if (!Strings.isNullOrEmpty(tokenRequest.getScope())) {
                body.put("scope", tokenRequest.getScope());
            }

            if (!Strings.isNullOrEmpty(userInfoJwt)) {
                body.put("ujwt", userInfoJwt);
            }

            if (!Strings.isNullOrEmpty(tokenRequest.getCodeVerifier())) {
                body.put("code_verifier", tokenRequest.getCodeVerifier());
            }

            body.put("grant_type", tokenRequest.getGrantType().getValue());
            body.put("redirect_uri", tokenRequest.getRedirectUri());
            body.put("client_id", tokenRequest.getAuthUsername());

            HttpServiceResponse httpServiceResponse = httpService
                    .executePost(tokenEndpoint, tokenRequest.getEncodedCredentials(), CommonUtils.toUrlEncodedString(body), ContentType.APPLICATION_FORM_URLENCODED,
                            "Basic ");
            String jsonString = null;
            if (httpServiceResponse.getHttpResponse() != null
                    && httpServiceResponse.getHttpResponse().getStatusLine() != null) {

                logger.debug(
                        " FINAL  httpServiceResponse.getHttpResponse():{}, httpServiceResponse.getHttpResponse().getStatusLine():{}, httpServiceResponse.getHttpResponse().getEntity():{}",
                        httpServiceResponse.getHttpResponse(), httpServiceResponse.getHttpResponse().getStatusLine(),
                        httpServiceResponse.getHttpResponse().getEntity());
                if (httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();

                    HttpEntity httpEntity = httpServiceResponse.getHttpResponse().getEntity();
                    if (httpEntity != null) {
                        jsonString = httpService.getContent(httpEntity);

                        HashMap<String, Object> tokenMap = mapper.readValue(jsonString, HashMap.class);
                        tokenMap.remove("token_type");
                        return tokenMap;
                    }
                    logger.error(TOKEN_GENERATION_ERROR + ": {}", "Response entity is null.");
                }

            }
            logger.error(TOKEN_GENERATION_ERROR + ": {}", "Response is null.");
            throw new ConfigApiApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), TOKEN_GENERATION_ERROR + "Response is null.");
        } catch (Exception e) {
            logger.error(TOKEN_GENERATION_ERROR, e);
            throw new ConfigApiApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getMessage());
        }
    }

    /**
     * Loads the Admin UI configuration entry from persistence.
     *
     * @return the AdminConf instance stored at ADMIN_UI_CONFIG_DN, or {@code null} if not found
     */
    public AdminConf fetchAdminUIConfiguration() {
        return persistenceEntryManager.find(AdminConf.class, ADMIN_UI_CONFIG_DN);
    }

    /**
     * Extracts the claims from a Jwt into a map keyed by claim name.
     *
     * @param jwtObj the Jwt to extract claims from; may be null
     * @return a map of claim names to their values. Values are `String`, `Integer`, `Long`, `Boolean`, `List<String>` for JSON arrays, or `JSONObject`; returns an empty map if `jwtObj` is null or contains no claims
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
            else if (jwtClaims.getClaim(key) instanceof Integer)
                claims.put(key, Integer.valueOf(jwtClaims.getClaim(key).toString()));
            else if (jwtClaims.getClaim(key) instanceof Long)
                claims.put(key, Long.valueOf(jwtClaims.getClaim(key).toString()));
            else if (jwtClaims.getClaim(key) instanceof Boolean)
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