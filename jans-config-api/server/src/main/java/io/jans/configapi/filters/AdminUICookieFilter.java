package io.jans.configapi.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.configapi.core.model.adminui.AUIConfiguration;
import io.jans.configapi.core.model.adminui.AdminUISession;
import io.jans.configapi.core.model.adminui.CedarlingLogType;
import io.jans.configapi.core.model.adminui.CedarlingPolicyStrRetrievalPoint;
import io.jans.configapi.core.model.exception.ConfigApiApplicationException;
import io.jans.configapi.service.auth.AdminUISessionService;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.TtlCache;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;

@Provider
@Priority(500)
public class AdminUICookieFilter implements ContainerRequestFilter {

    @Inject
    Logger log;

    @Inject
    AdminUISessionService configApiSessionService;

    @Inject
    ConfigurationService configurationService;

    AUIConfiguration auiConfiguration;

    TtlCache<String, String> ujwtTokenCache;

    private static final String ADMIN_UI_SESSION_ID = "admin_ui_session_id";
    private static final long CACHE_TTL = 360000; // 1 hour
    private static final String AUTHENTICATION_SCHEME = "Bearer";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            log.info("Inside AdminUICookieFilter filter...");
            Map<String, Cookie> cookies = requestContext.getCookies();
            initializeCaches();
            removeExpiredSessions();
            Optional<String> ujwtOptional = fetchUJWTFromAdminUISession(cookies);
            //return if session record is not present in the database
            if (ujwtOptional.isEmpty()) {
                return;
            }

            initializeAdminUIConfiguration();
            if (auiConfiguration == null) {
                return;
            }

            String accessToken = null;

            accessToken = getAccessToken(ujwtOptional.get(), auiConfiguration);
            if (Strings.isNullOrEmpty(accessToken)) {
                abortWithException(requestContext, Response.Status.INTERNAL_SERVER_ERROR, "Generated Config Api access token is null or empty.");
            }
            requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, AUTHENTICATION_SCHEME + " " + accessToken);
        } catch (Exception e) {
            abortWithException(requestContext, Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void removeExpiredSessions() {
        configApiSessionService.removeAllExpiredSessions();
    }

    private String getAccessToken(String ujwtString, AUIConfiguration auiConfiguration) throws ConfigApiApplicationException {
        String sub = getSubjectFromUJWT(ujwtString);
        if (ujwtTokenCache.get(sub) != null) {
            String cachedToken = ujwtTokenCache.get(sub);
            try {
                if (configApiSessionService.isCachedTokenValid(cachedToken, auiConfiguration)) {
                    return cachedToken;
                }
            } catch (JsonProcessingException e) {
                log.warn("Error occurred while introspecting the cached token to access the Config API.");
                //generate a new token if cachedToken is not valid without throwing exception
            }
        }
        TokenResponse tokenResponse = null;
        try {
            tokenResponse = configApiSessionService.getApiProtectionToken(ujwtString, auiConfiguration);
        } catch (Exception e) {
            throw new ConfigApiApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Error in generating token generated for accessing Config API: " + e.getMessage());
        }
        ujwtTokenCache.put(sub, tokenResponse.getAccessToken(), CACHE_TTL);
        return tokenResponse.getAccessToken();
    }

    private void initializeCaches() {
        if (ujwtTokenCache == null) {
            ujwtTokenCache = new TtlCache<>();
        }
    }

    private String getSubjectFromUJWT(String ujwtString) throws ConfigApiApplicationException {
        final Jwt tokenJwt;
        try {
            tokenJwt = Jwt.parse(ujwtString);
            Map<String, Object> claims = configApiSessionService.getClaims(tokenJwt);

            if (claims.get("sub") == null) {
                throw new ConfigApiApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "The `sub` claim missing in User-Info JWT");
            }
            return ((String) claims.get("sub"));
        } catch (InvalidJwtException e) {
            throw new ConfigApiApplicationException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Invalid User-Info JWT : {}" + e.getMessage());
        }
    }

    private Optional<String> fetchUJWTFromAdminUISession(Map<String, Cookie> cookies) {
        //if no cookies
        if (cookies == null) {
            return Optional.empty();
        }
        if (!cookies.containsKey(ADMIN_UI_SESSION_ID)) {
            return Optional.empty();
        }
        log.debug("Found a Admin UI session cookie in request header.");
        Cookie adminUISessionCookie = cookies.get(ADMIN_UI_SESSION_ID);
        String sessionId = adminUISessionCookie.getValue();
        AdminUISession configApiSession = configApiSessionService.getSession(sessionId);
        //if config api session does not exist
        if (configApiSession == null) {
            return Optional.empty();
        }
        log.debug("Admin UI session exist in persistence.");
        String ujwtString = configApiSession.getUjwt();
        return Optional.of(ujwtString);
    }

    private void initializeAdminUIConfiguration() {
        if (auiConfiguration != null) {
            return;
        }
        AdminConf adminConf = configApiSessionService.fetchAdminUIConfiguration();
        addPropertiesToAUIConfiguration(adminConf);
    }

    private void addPropertiesToAUIConfiguration(AdminConf appConf) {
        auiConfiguration = new AUIConfiguration();
        AppConfiguration appConfiguration = configurationService.find();
        auiConfiguration.setAppType("admin_ui");
        auiConfiguration.setAuiWebServerHost(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getOpHost());
        auiConfiguration.setAuiWebServerClientId(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getClientId());
        auiConfiguration.setAuiWebServerClientSecret(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getClientSecret());
        auiConfiguration.setAuiWebServerScope(StringUtils.join(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getScopes(), "+"));
        auiConfiguration.setAuiWebServerRedirectUrl(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getRedirectUri());
        auiConfiguration.setAuiWebServerFrontChannelLogoutUrl(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getFrontchannelLogoutUri());
        auiConfiguration.setAuiWebServerPostLogoutRedirectUri(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getPostLogoutUri());
        auiConfiguration.setAuiWebServerAuthzBaseUrl(appConfiguration.getAuthorizationEndpoint());
        auiConfiguration.setAuiWebServerTokenEndpoint(appConfiguration.getTokenEndpoint());
        auiConfiguration.setAuiWebServerIntrospectionEndpoint(appConfiguration.getIntrospectionEndpoint());
        auiConfiguration.setAuiWebServerUserInfoEndpoint(appConfiguration.getUserInfoEndpoint());
        auiConfiguration.setAuiWebServerEndSessionEndpoint(appConfiguration.getEndSessionEndpoint());
        auiConfiguration.setAuiWebServerAcrValues(StringUtils.join(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getAcrValues(), "+"));

        auiConfiguration.setAuiBackendApiServerClientId(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getClientId());
        auiConfiguration.setAuiBackendApiServerClientSecret(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getClientSecret());
        auiConfiguration.setAuiBackendApiServerScope(StringUtils.join(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getScopes(), "+"));
        auiConfiguration.setAuiBackendApiServerTokenEndpoint(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getTokenEndpoint());
        auiConfiguration.setAuiBackendApiServerIntrospectionEndpoint(appConf.getMainSettings().getOidcConfig().getAuiBackendApiClient().getIntrospectionEndpoint());

        auiConfiguration.setSessionTimeoutInMins(appConf.getMainSettings().getUiConfig().getSessionTimeoutInMins());
        auiConfiguration.setAllowSmtpKeystoreEdit(appConf.getMainSettings().getUiConfig().getAllowSmtpKeystoreEdit());
        auiConfiguration.setAdditionalParameters(appConf.getMainSettings().getOidcConfig().getAuiWebClient().getAdditionalParameters());
        auiConfiguration.setCedarlingLogType(CedarlingLogType.fromString(appConf.getMainSettings().getUiConfig().getCedarlingLogType()));
        auiConfiguration.setAuiCedarlingPolicyStoreUrl(appConf.getMainSettings().getUiConfig().getAuiPolicyStoreUrl());
        auiConfiguration.setCedarlingPolicyStoreRetrievalPoint(CedarlingPolicyStrRetrievalPoint.fromString(appConf.getMainSettings().getUiConfig().getCedarlingPolicyStoreRetrievalPoint()));
        auiConfiguration.setAuiCedarlingDefaultPolicyStorePath(appConf.getMainSettings().getUiConfig().getAuiDefaultPolicyStorePath());
    }

    private void abortWithException(ContainerRequestContext requestContext, Response.Status status, String errMsg) {
        requestContext.abortWith(Response.status(status)
                .entity(errMsg)
                .build());
    }

}
