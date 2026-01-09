package io.jans.configapi.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jans.as.client.TokenResponse;
import io.jans.as.model.config.adminui.AdminConf;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.configapi.core.model.adminui.AUIConfiguration;
import io.jans.configapi.core.model.adminui.AdminUISession;
import io.jans.configapi.core.model.adminui.CedarlingLogType;
import io.jans.configapi.core.model.adminui.CedarlingPolicyStrRetrievalPoint;
import io.jans.configapi.service.auth.AdminUISessionService;
import io.jans.configapi.service.auth.ConfigurationService;
import io.jans.configapi.util.TtlCache;
import io.jans.util.security.StringEncrypter;
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

    private static final String CONFIG_API_SESSION_ID = "config_api_session_id";
    private static final long CACHE_TTL = 90000; // 15 mins
    private static final String AUTHENTICATION_SCHEME = "Bearer";
    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
        log.info("Inside AdminUICookieFilter filter...");
        Map<String, Cookie> cookies = requestContext.getCookies();
        initializeCaches();
        removeExpiredSessions();
        Optional<String> ujwtOptional = fetchUJWTFromAdminUISession(cookies);
        //return if session rcord is not present in the database
        if(ujwtOptional.isEmpty()) {
            return;
        }

        initializeAdminUIConfiguration();
        if(auiConfiguration == null) {
            return;
        }

            String accessToken = null;
            try {
                accessToken = getAccessToken(ujwtOptional.get(), auiConfiguration);
            } catch (JsonProcessingException e) {
                abortWithException(requestContext, Response.Status.INTERNAL_SERVER_ERROR, "Error in parsing token generated for accessing Config API: " + e.getMessage());
            } catch (InvalidJwtException | StringEncrypter.EncryptionException e) {
                abortWithException(requestContext, Response.Status.INTERNAL_SERVER_ERROR, "JWT token generated for accessing Config API is not valid: " + e.getMessage());
            }
            requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, AUTHENTICATION_SCHEME + " " + accessToken);
        } catch (Exception e) {
            abortWithException(requestContext, Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void removeExpiredSessions() {
        configApiSessionService.removeAllExpiredSessions();
    }

    private String getAccessToken(String ujwtString, AUIConfiguration auiConfiguration) throws StringEncrypter.EncryptionException, JsonProcessingException, InvalidJwtException {
        String sub = getSubjectFromUJWT(ujwtString);
        if(ujwtTokenCache.get(sub) != null) {
            return ujwtTokenCache.get(sub);
        }
        TokenResponse tokenResponse = configApiSessionService.getApiProtectionToken(ujwtString, auiConfiguration);
        ujwtTokenCache.put(sub, tokenResponse.getAccessToken(), CACHE_TTL);
        return tokenResponse.getAccessToken();
    }

    private void initializeCaches() {
        if(ujwtTokenCache == null) {
            ujwtTokenCache = new TtlCache<>();
        }
    }

    private String getSubjectFromUJWT(String ujwtString) {
        final Jwt tokenJwt;
        try {
            tokenJwt = Jwt.parse(ujwtString);
            Map<String, Object> claims = configApiSessionService.getClaims(tokenJwt);

            if (claims.get("sub") == null) {
                throw new RuntimeException("The `sub` claim missing in User-Info JWT");
            }
            return ((String) claims.get("sub"));
        } catch (InvalidJwtException e) {
            throw new RuntimeException("Invalid User-Info JWT : {}", e);
        }
    }

    private Optional<String> fetchUJWTFromAdminUISession(Map<String, Cookie> cookies) {
        //if no cookies
        if (cookies == null) {
            return Optional.empty();
        }
        if(!cookies.containsKey(CONFIG_API_SESSION_ID)) {
            return Optional.empty();
        }
        Cookie adminUISessionCookie = cookies.get(CONFIG_API_SESSION_ID);
        String sessionId = adminUISessionCookie.getValue();
        AdminUISession configApiSession = configApiSessionService.getSession(sessionId);
        //if config api session does not exist
        if(configApiSession == null) {
            return Optional.empty();
        }
        String ujwtString = configApiSession.getUjwt();
        return Optional.of(ujwtString);
    }

    private void initializeAdminUIConfiguration() {
        if(auiConfiguration != null) {
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

    private void abortWithException(ContainerRequestContext requestContext,  Response.Status status, String errMsg) {
        requestContext.abortWith(Response.status(status)
                .entity(errMsg)
                .build());
    }

}
