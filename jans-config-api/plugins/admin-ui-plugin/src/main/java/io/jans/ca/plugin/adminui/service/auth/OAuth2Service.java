package io.jans.ca.plugin.adminui.service.auth;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import io.jans.as.client.TokenRequest;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.jwt.Jwt;
import io.jans.ca.plugin.adminui.model.auth.ApiTokenRequest;
import io.jans.ca.plugin.adminui.model.auth.TokenResponse;
import io.jans.ca.plugin.adminui.model.config.AUIConfiguration;
import io.jans.ca.plugin.adminui.model.exception.ApplicationException;
import io.jans.ca.plugin.adminui.rest.auth.OAuth2Resource;
import io.jans.ca.plugin.adminui.service.BaseService;
import io.jans.ca.plugin.adminui.service.config.AUIConfigurationService;
import io.jans.ca.plugin.adminui.utils.CommonUtils;
import io.jans.ca.plugin.adminui.utils.ErrorResponse;
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

    private static final String SID_GET_MSG = "Error in get Admin UI Session by sid:{}";
    private static final String JANS_USR_DN = "jansUsrDN";
    private static final String SESSION_DN = "ou=configApiSession,ou=admin-ui,o=jans";
    /**
     * The function `getApiProtectionToken` retrieves an API protection token based on the provided parameters and handles
     * exceptions accordingly.
     *
     * @param apiTokenRequest The `getApiProtectionToken` method is responsible for obtaining an API protection token based
     * on the provided `ApiTokenRequest` and `appType`. The method first retrieves the necessary configuration from
     * `auiConfigurationService` based on the `appType`. It then constructs a `TokenRequest` object
     * @param appType The `appType` parameter in the `getApiProtectionToken` method is used to specify the type of the
     * application for which the API protection token is being requested. It is passed as a String parameter to the method.
     * The `appType` parameter helps in determining the specific configuration settings and endpoints
     * @throws ApplicationException
     * @return The method `getApiProtectionToken` returns a `TokenResponse` object containing the access token, id token,
     * refresh token, scopes, iat (issued at) timestamp, exp (expiration) timestamp, and issuer information.
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
                tokenRequest.setScope(scopeAsString(Arrays.asList(OAuth2Resource.SCOPE_OPENID)));
                tokenResponse = getToken(tokenRequest, auiConfiguration.getAuiBackendApiServerTokenEndpoint());
            } else {
                if (Strings.isNullOrEmpty(apiTokenRequest.getUjwt())) {
                    log.warn(ErrorResponse.USER_INFO_JWT_BLANK.getDescription());
                    tokenRequest.setScope(scopeAsString(Arrays.asList(OAuth2Resource.SCOPE_OPENID)));
                }
                tokenResponse = getToken(tokenRequest, auiConfiguration.getAuiBackendApiServerTokenEndpoint(), apiTokenRequest.getUjwt());
            }

            Optional<Map<String, Object>> introspectionResponse = introspectToken(tokenResponse.getAccessToken(), auiConfiguration.getAuiBackendApiServerIntrospectionEndpoint());


            TokenResponse tokenResp = new TokenResponse();
            tokenResp.setAccessToken(tokenResponse.getAccessToken());
            tokenResp.setIdToken(tokenResponse.getIdToken());
            tokenResp.setRefreshToken(tokenResponse.getRefreshToken());

            if (!introspectionResponse.isPresent()) {
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
            log.error(ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription());
            throw e;
        } catch (Exception e) {
            log.error(ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.GET_API_PROTECTION_TOKEN_ERROR.getDescription());
        }
    }

    private static String scopeAsString(List<String> scopes) throws UnsupportedEncodingException {
        Set<String> scope = Sets.newHashSet();
        scope.addAll(scopes);
        return CommonUtils.joinAndUrlEncode(scope);
    }

    private String getDnForSession(String sessionId) {
        return String.format("inum=%s,%s", sessionId, "ou=configApiSession,ou=admin-ui,o=jans");
    }

    public void setAdminUISession(String sessionId, String ujwt) throws Exception {
        AUIConfiguration auiConfiguration = auiConfigurationService.getAUIConfiguration();
        Date currentDate = new Date();

        Jwt tokenJwt = Jwt.parse(ujwt);
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

    public void removeAdminUIUserSessionByDn(String userDn) throws ApplicationException {
        try {
            Filter searchFilter = null;
            List<Filter> filters = new ArrayList<>();
            Filter userDnFilter = Filter.createSubstringFilter(JANS_USR_DN, null, new String[]{userDn}, null);
            filters.add(userDnFilter);
            searchFilter = Filter.createORFilter(filters);
            List<AdminUISession> adminUISessions =  entryManager.findEntries(SESSION_DN, AdminUISession.class, searchFilter);

            for(AdminUISession adminUISession : adminUISessions) {
                entryManager.remove(adminUISession);
            }
        } catch (Exception e) {
            log.error(ErrorResponse.ADMINUI_SESSION_REMOVE_ERROR.getDescription(), e);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ErrorResponse.ADMINUI_SESSION_REMOVE_ERROR.getDescription());
        }
    }

    public AdminUISession getSession(String sessionId) throws ApplicationException {
        AdminUISession adminUISession = null;
        try {
            adminUISession = entryManager
                    .find(AdminUISession.class, getDnForSession(sessionId));
        } catch (Exception ex) {
            log.error(SID_GET_MSG,  sessionId, ex);
            throw new ApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), SID_GET_MSG + sessionId);
        }
        return adminUISession;
    }

    public void removeSession(String sessionId) throws ApplicationException {
        AdminUISession configApiSession = getSession(sessionId);
        entryManager.remove(configApiSession);
    }

    private String getDnForUser(String userInum) {
        return String.format("inum=%s,%s", userInum, "ou=people,o=jans");
    }
}
