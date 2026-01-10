package io.jans.configapi.service.auth;

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
import io.jans.configapi.core.model.adminui.AUIConfiguration;
import io.jans.configapi.core.model.adminui.AdminUISession;
import io.jans.configapi.core.service.ConfigHttpService;
import io.jans.model.net.HttpServiceResponse;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.EncryptionService;
import io.jans.util.security.StringEncrypter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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

    private String getDnForSession(String sessionId) {
        return String.format("inum=%s,%s", sessionId, SESSION_DN);
    }

    public AdminUISession getSession(String sessionId) {
        if (logger.isInfoEnabled()) {
            logger.info(SID_MSG, escapeLog(sessionId));
        }

        AdminUISession configApiSession = null;
        try {
            configApiSession = persistenceEntryManager
                    .find(AdminUISession.class, getDnForSession(sessionId));
        } catch (Exception ex) {
            logger.error(SID_ERROR + "{}", sessionId, ex);
        }
        return configApiSession;
    }

    public void removeAllExpiredSessions() {
        final Filter filter = Filter.createPresenceFilter(SID);
        List<AdminUISession> adminUISessions =  persistenceEntryManager.findEntries(SESSION_DN, AdminUISession.class, filter);
        Date currentDate = new Date();
        adminUISessions.stream().filter(ele ->
                ((ele.getExpirationDate().getTime() - currentDate.getTime()) < 0))
                .forEach(e -> persistenceEntryManager.remove(e));
    }

    public boolean isCachedTokenValid(String token, AUIConfiguration auiConfiguration) throws JsonProcessingException {
        logger.debug("Inside isCachedTokenValid : Token introspection from auth-server.");

        Map<String, String> body = new HashMap<>();

        if (!Strings.isNullOrEmpty(token)) {
            body.put("token", token);
        }

        HttpServiceResponse httpServiceResponse = httpService
                .executePost(auiConfiguration.getAuiBackendApiServerIntrospectionEndpoint(),
                        token, toUrlEncodedString(body),
                        ContentType.APPLICATION_FORM_URLENCODED,
                        "Bearer " );
        String jsonString = null;
        if (httpServiceResponse.getHttpResponse() != null
                && httpServiceResponse.getHttpResponse().getStatusLine() != null) {

            logger.debug(
                    "httpServiceResponse.getHttpResponse():{}, httpServiceResponse.getHttpResponse().getStatusLine():{}, httpServiceResponse.getHttpResponse().getEntity():{}",
                    httpServiceResponse.getHttpResponse(), httpServiceResponse.getHttpResponse().getStatusLine(),
                    httpServiceResponse.getHttpResponse().getEntity());
            if(httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();

                HttpEntity httpEntity = httpServiceResponse.getHttpResponse().getEntity();
                if (httpEntity != null) {
                    jsonString = httpService.getContent(httpEntity);

                    HashMap<String, Object> payloadMap = mapper.readValue(jsonString, HashMap.class);
                    if(payloadMap.containsKey("active")) {
                        return (boolean) payloadMap.get("active");
                    }
                    return false;
                }
                logger.error("Error in introspection of token to access Config API endpoints. Response is null.");
            }
        }
        return false;
    }

    public TokenResponse getApiProtectionToken(String ujwtString, AUIConfiguration auiConfiguration) throws StringEncrypter.EncryptionException, JsonProcessingException {
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

    public Map<String, Object> getToken(TokenRequest tokenRequest, String tokenEndpoint, String userInfoJwt) throws JsonProcessingException {

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
                    .executePost(tokenEndpoint, tokenRequest.getEncodedCredentials(), toUrlEncodedString(body), ContentType.APPLICATION_FORM_URLENCODED,
                            "Basic " );
            String jsonString = null;
            if (httpServiceResponse.getHttpResponse() != null
                    && httpServiceResponse.getHttpResponse().getStatusLine() != null) {

                logger.debug(
                        " FINAL  httpServiceResponse.getHttpResponse():{}, httpServiceResponse.getHttpResponse().getStatusLine():{}, httpServiceResponse.getHttpResponse().getEntity():{}",
                        httpServiceResponse.getHttpResponse(), httpServiceResponse.getHttpResponse().getStatusLine(),
                        httpServiceResponse.getHttpResponse().getEntity());
                if(httpServiceResponse.getHttpResponse().getStatusLine().getStatusCode() == 200) {
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
        } catch (Exception e) {
            logger.error(TOKEN_GENERATION_ERROR, e);
            throw e;
        }
        return Collections.emptyMap();
    }

    private static String toUrlEncodedString(Map<String, String> params) {
        return params.entrySet()
                .stream()
                .filter(e -> e.getValue() !=null)
                .map(e ->
                        URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                                URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)
                )
                .collect(Collectors.joining("&"));
    }

    public AdminConf fetchAdminUIConfiguration() {
        return persistenceEntryManager.find(AdminConf.class, ADMIN_UI_CONFIG_DN);
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
