package io.jans.ca.plugin.adminui.service.auth;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import io.jans.as.client.TokenRequest;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.ca.plugin.adminui.model.auth.ApiTokenRequest;
import io.jans.ca.plugin.adminui.model.auth.TokenResponse;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.rest.auth.OAuth2Resource;
import io.jans.ca.plugin.adminui.service.BaseService;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
import io.jans.configapi.core.model.adminui.AUIConfiguration;
import io.jans.configapi.core.model.adminui.AdminUISession;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.EncryptionService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static io.jans.ca.plugin.adminui.utils.CommonUtils.addMinutes;

@Singleton
public class OAuth2Service extends BaseService {
    @Inject
    Logger log;

    @Inject
    AUIConfigurationService auiConfigurationService;

    @Inject
    EncryptionService encryptionService;

    @Inject
    private PersistenceEntryManager entryManager;

    private static final String SID_GET_MSG = "Error in get Admin UI Session by sid: ";
    private static final String JANS_USR_DN = "jansUsrDN";
    private static final String SESSION_DN = "ou=adminUISession,ou=admin-ui,o=jans";

    /**
     * Obtain an API protection token for the specified application and populate its token claims.
     * Uses the AUI configuration for the given appType to request a client-credentials token; when an ApiTokenRequest
     * with a user JWT (ujwt) is provided, the request includes that JWT. The returned TokenResponse contains the
     * access, ID, and refresh tokens and, when available from token introspection, scopes, issued-at (iat), expiration (exp),
     * and issuer (iss) claims.
     *
     * @param apiTokenRequest optional request parameters; when null a default OpenID scope is requested, otherwise the
     *                        contained `ujwt` (if present) is used to obtain the token
     * @param appType         identifier of the application configuration to use for token endpoint, client credentials,
     *                        and introspection endpoint lookup
     * @return a TokenResponse populated with accessToken, idToken, refreshToken and, if available, scopes, iat, exp, and issuer
     * @throws ApplicationException on error while obtaining or processing the token
     */
    public TokenResponse getApiProtectionToken(ApiTokenRequest apiTokenRequest, String appType) throws ApplicationException {
        try {
            log.debug("Getting api-protection token");

            AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration(appType);

            TokenRequest tokenRequest = new TokenRequest(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setAuthUsername(auiConfiguration.getAuiBackendApiServerClientId());
            tokenRequest.setAuthPassword(encryptionService.decrypt(auiConfiguration.getAuiBackendApiServerClientSecret()));
            tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
            tokenRequest.setRedirectUri(auiConfiguration.getAuiBackendApiServerRedirectUrl());

            io.jans.as.client.TokenResponse tokenResponse = null;
            if (apiTokenRequest == null) {
                tokenRequest.setScope(scopeAsString(List.of(OAuth2Resource.SCOPE_OPENID)));
                tokenResponse = getToken(tokenRequest, auiConfiguration.getAuiBackendApiServerTokenEndpoint());
            } else {
                if (Strings.isNullOrEmpty(apiTokenRequest.getUjwt())) {
                    log.warn(ErrorResponse.USER_INFO_JWT_BLANK.getDescription());
                    tokenRequest.setScope(scopeAsString(List.of(OAuth2Resource.SCOPE_OPENID)));
                }
                tokenResponse = getToken(tokenRequest, auiConfiguration.getAuiBackendApiServerTokenEndpoint(), apiTokenRequest.getUjwt());
            }

            Optional<Map<String, Object>> introspectionResponse = introspectToken(tokenResponse.getAccessToken(), auiConfiguration.getAuiBackendApiServerIntrospectionEndpoint());


            TokenResponse tokenResp = new TokenResponse();
            tokenResp.setAccessToken(tokenResponse.getAccessToken());
            tokenResp.setIdToken(tokenResponse.getIdToken());
            tokenResp.setRefreshToken(tokenResponse.getRefreshToken());

            if (introspectionResponse.isEmpty()) {
                return tokenResp;
            }
            final String SCOPE = "scope";
            Map<String, Object> claims = introspectionResponse.get();
            if (claims.get(SCOPE) != null) {
                if (claims.get(SCOPE) instanceof List) {
                    tokenResp.setScopes((List) claims.get(SCOPE));
                }
                if (claims.get(SCOPE) instanceof String) {
                    tokenResp.setScopes(Arrays.asList(((String) claims.get(SCOPE)).split(" ")));
                }
            }
            if (claims.get("iat") != null) {
                tokenResp.setIat(Long.parseLong(claims.get("iat").toString()));
            }

            if (claims.get("exp") != null) {
                tokenResp.setExp(Long.parseLong(claims.get("exp").toString()));
            }

            if (claims.get("iss") != null) {
                tokenResp.setIssuer(claims.get("iss").toString());
            }

            return tokenResp;
        } catch (Exception e) {
            log.error(ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription());
        }
    }

    /**
     * Convert a list of scope values into a URL-encoded space-delimited string with duplicates removed.
     *
     * @param scopes the list of scope strings; may contain duplicates
     * @return a URL-encoded space-delimited string of unique scopes
     * @throws UnsupportedEncodingException if the required character encoding is not supported during URL-encoding
     */
    private static String scopeAsString(List<String> scopes) throws UnsupportedEncodingException {
        Set<String> scope = Sets.newHashSet();
        scope.addAll(scopes);
        return CommonUtils.joinAndUrlEncode(scope);
    }

    /**
     * Constructs the distinguished name (DN) for an Admin UI session.
     *
     * @param sessionId the session identifier (inum) to include in the DN
     * @return the DN in the form "inum=&lt;sessionId&gt;,ou=adminUISession,ou=admin-ui,o=jans"
     */
    private String getDnForSession(String sessionId) {
        return String.format("inum=%s,%s", sessionId, SESSION_DN);
    }

    /**
     * Create and persist an AdminUISession for the given sessionId using claims extracted from the provided user-info JWT.
     *
     * @param sessionId the identifier for the admin UI session; used as the session's inum and to build its DN
     * @param ujwt      the user-info JWT string containing an "inum" claim that identifies the user
     * @throws ApplicationException if the "inum" claim is missing from the provided JWT
     */
    public void setAdminUISession(String sessionId, String ujwt) throws ApplicationException {
        AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
        Date currentDate = new Date();

        Jwt tokenJwt = null;
        try {
            tokenJwt = Jwt.parse(ujwt);
        } catch (InvalidJwtException e) {
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Error in parsing user-info JWT.");
        }
        Map<String, Object> claims = CommonUtils.getClaims(tokenJwt);
        if (claims.get("inum") == null) {
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "The user `inum` claim missing in User-Info JWT");
        }
        AdminUISession adminUISession = new AdminUISession();
        adminUISession.setInum(sessionId);
        adminUISession.setDn(getDnForSession(sessionId));
        adminUISession.setSessionId(sessionId);
        adminUISession.setUjwt(ujwt);
        adminUISession.setJansUsrDN(getDnForUser((String) claims.get("inum")));
        adminUISession.setCreationDate(currentDate);
        adminUISession.setExpirationDate(addMinutes(currentDate, auiConfiguration.getSessionTimeoutInMins()));

        entryManager.persist(adminUISession);
    }

    /**
     * Removes all AdminUISession entries whose jansUsrDN contains the provided user DN.
     *
     * @param userDn the user distinguished name to match within stored AdminUISession jansUsrDN values
     * @throws ApplicationException if an error occurs while searching for or removing sessions (results in HTTP 500)
     */
    public void removeAdminUIUserSessionByDn(String userDn) throws ApplicationException {
        try {
            Filter userDnFilter = Filter.createEqualityFilter(JANS_USR_DN, userDn);
            List<AdminUISession> adminUISessions = entryManager.findEntries(SESSION_DN, AdminUISession.class, userDnFilter);

            for (AdminUISession adminUISession : adminUISessions) {
                entryManager.remove(adminUISession);
            }
        } catch (Exception e) {
            log.error(ErrorResponse.ADMINUI_SESSION_REMOVE_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ADMINUI_SESSION_REMOVE_ERROR.getDescription());
        }
    }

    /**
     * Load an AdminUISession by its session identifier.
     *
     * @param sessionId the session identifier used to build the session DN
     * @return the matching AdminUISession, or `null` if no session exists for the given id
     * @throws ApplicationException if an error occurs while retrieving the session (results in HTTP 500)
     */
    public AdminUISession getSession(String sessionId) throws ApplicationException {
        AdminUISession adminUISession = null;
        try {
            adminUISession = entryManager
                    .find(AdminUISession.class, getDnForSession(sessionId));
        } catch (Exception ex) {
            log.error(SID_GET_MSG + "{}", sessionId, ex);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), SID_GET_MSG + sessionId);
        }
        return adminUISession;
    }

    /**
     * Removes the Admin UI session identified by the given sessionId.
     *
     * @param sessionId the Admin UI session identifier
     * @throws ApplicationException if the session cannot be retrieved or removed
     */
    public void removeSession(String sessionId) throws ApplicationException {
        AdminUISession configApiSession = getSession(sessionId);
        if (configApiSession == null) {
            log.warn("Session not found for removal: {}", sessionId);
            return;
        }
        entryManager.remove(configApiSession);
    }

    /**
     * Build the distinguished name (DN) for a user identified by its inum.
     *
     * @param userInum the user's inum identifier
     * @return the LDAP DN for the user, e.g. "inum={userInum},ou=people,o=jans"
     */
    private String getDnForUser(String userInum) {
        return String.format("inum=%s,%s", userInum, "ou=people,o=jans");
    }
}