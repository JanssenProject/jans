/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import io.jans.as.common.model.common.User;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.Pair;
import io.jans.as.model.util.Util;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.model.exception.AcrChangedException;
import io.jans.as.server.model.exception.InvalidSessionStateException;
import io.jans.as.server.security.Identity;
import io.jans.as.server.service.exception.FailedComputeSessionStateException;
import io.jans.as.server.service.external.ExternalApplicationSessionService;
import io.jans.as.server.service.external.ExternalAuthenticationService;
import io.jans.as.server.service.external.session.SessionEvent;
import io.jans.as.server.service.external.session.SessionEventType;
import io.jans.as.server.service.stat.StatService;
import io.jans.as.server.util.ServerUtil;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;
import io.jans.service.CacheService;
import io.jans.service.LocalCacheService;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version December 8, 2018
 */
@RequestScoped
@Named
public class SessionIdService {

    private static final int MAX_MERGE_ATTEMPTS = 3;
    private static final int DEFAULT_LOCAL_CACHE_EXPIRATION = 2;

    @Inject
    private Logger log;

    @Inject
    private ExternalAuthenticationService externalAuthenticationService;

    @Inject
    private ExternalApplicationSessionService externalApplicationSessionService;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private FacesContext facesContext;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private RequestParameterService requestParameterService;

    @Inject
    private UserService userService;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private CookieService cookieService;

    @Inject
    private Identity identity;

    @Inject
    private LocalCacheService localCacheService;

    @Inject
    private CacheService cacheService;

    @Inject
    private StatService statService;

    private String buildDn(String sessionId) {
        return String.format("jansId=%s,%s", sessionId, staticConfiguration.getBaseDn().getSessions());
    }

    public Set<SessionId> getCurrentSessions() {
        final Set<String> ids = cookieService.getCurrentSessions();
        final Set<SessionId> sessions = Sets.newHashSet();
        for (String sessionId : ids) {
            if (StringUtils.isBlank(sessionId)) {
                log.error("Invalid sessionId in current_sessions: {}", sessionId);
                continue;
            }

            final SessionId sessionIdObj = getSessionId(sessionId);
            if (sessionIdObj == null) {
                log.trace("Unable to find session object by id: {} (expired?)", sessionId);
                continue;
            }

            if (sessionIdObj.getState() != SessionIdState.AUTHENTICATED) {
                log.error("Session is not authenticated, id: {}", sessionId);
                continue;
            }
            sessions.add(sessionIdObj);
        }
        return sessions;
    }

    public String getAcr(SessionId session) {
        if (session == null) {
            return null;
        }

        String acr = session.getSessionAttributes().get(JwtClaimName.AUTHENTICATION_CONTEXT_CLASS_REFERENCE);
        if (StringUtils.isBlank(acr)) {
            acr = session.getSessionAttributes().get("acr_values");
        }
        return acr;
    }

    // #34 - update session attributes with each request
    // 1) redirect_uri change -> update session
    // 2) acr change -> throw acr change exception
    // 3) client_id change -> do nothing
    // https://github.com/GluuFederation/oxAuth/issues/34
    public SessionId assertAuthenticatedSessionCorrespondsToNewRequest(SessionId session, String acrValuesStr) throws AcrChangedException {
        if (session != null && !session.getSessionAttributes().isEmpty() && session.getState() == SessionIdState.AUTHENTICATED) {

            final Map<String, String> sessionAttributes = session.getSessionAttributes();

            String sessionAcr = getAcr(session);

            if (StringUtils.isBlank(sessionAcr)) {
                log.trace("Failed to fetch acr from session, attributes: {}", sessionAttributes);
                return session;
            }

            List<String> acrValuesList = acrValuesList(acrValuesStr);
            boolean isAcrChanged = !acrValuesList.isEmpty() && !acrValuesList.contains(sessionAcr);
            if (isAcrChanged) {
                Map<String, Integer> acrToLevel = externalAuthenticationService.acrToLevelMapping();
                Integer sessionAcrLevel = acrToLevel.get(externalAuthenticationService.scriptName(sessionAcr));

                for (String acrValue : acrValuesList) {
                    Integer currentAcrLevel = acrToLevel.get(externalAuthenticationService.scriptName(acrValue));

                    log.info("Acr is changed. Session acr: {} (level: {}), current acr: {} (level: {})",
                            sessionAcr, sessionAcrLevel, acrValue, currentAcrLevel);

                    // Requested acr method not enabled
                    if (currentAcrLevel == null) {
                        throw new AcrChangedException(false);
                    }

                    if (sessionAcrLevel < currentAcrLevel) {
                        throw new AcrChangedException();
                    }
                }
                // https://github.com/GluuFederation/oxAuth/issues/291
                return session; // we don't want to reinit login because we have stronger acr (avoid overriding)
            }

            reinitLogin(session, false);
        }

        return session;
    }

    private static boolean shouldReinitSession(Map<String, String> sessionAttributes, Map<String, String> currentSessionAttributes) {
        final Map<String, String> copySessionAttributes = new HashMap<>(sessionAttributes);
        final Map<String, String> copyCurrentSessionAttributes = new HashMap<>(currentSessionAttributes);

        // it's up to RP whether to change state per request
        copySessionAttributes.remove(AuthorizeRequestParam.STATE);
        copyCurrentSessionAttributes.remove(AuthorizeRequestParam.STATE);

        return !copyCurrentSessionAttributes.equals(copySessionAttributes);
    }

    /**
     * @param session
     * @param force
     * @return returns whether session was updated
     */
    public boolean reinitLogin(SessionId session, boolean force) {
        final Map<String, String> sessionAttributes = session.getSessionAttributes();
        final Map<String, String> currentSessionAttributes = getCurrentSessionAttributes(sessionAttributes);
        if (log.isTraceEnabled()) {
            log.trace("sessionAttributes: {}", sessionAttributes);
            log.trace("currentSessionAttributes: {}", currentSessionAttributes);
            log.trace("shouldReinitSession: {}, force: {}", shouldReinitSession(sessionAttributes, currentSessionAttributes), force);
        }

        if (force || shouldReinitSession(sessionAttributes, currentSessionAttributes)) {
            sessionAttributes.putAll(currentSessionAttributes);

            // Reinit login
            sessionAttributes.put("c", "1");

            for (Iterator<Entry<String, String>> it = currentSessionAttributes.entrySet().iterator(); it.hasNext(); ) {
                Entry<String, String> currentSessionAttributesEntry = it.next();
                String name = currentSessionAttributesEntry.getKey();
                if (name.startsWith("auth_step_passed_")) {
                    it.remove();
                }
            }

            session.setSessionAttributes(currentSessionAttributes);

            if (force) {
                // Reset state to unauthenticated
                session.setState(SessionIdState.UNAUTHENTICATED);
                externalEvent(new SessionEvent(SessionEventType.UNAUTHENTICATED, session));
            }

            boolean updateResult = updateSessionId(session, true, true, true);
            if (!updateResult) {
                log.debug("Failed to update session entry: '{}'", session.getId());
            }
            if (log.isTraceEnabled()) {
                log.trace("sessionAttributes after update: {}, ", session.getSessionAttributes());
            }
            return updateResult;
        }
        return false;
    }

    public SessionId resetToStep(SessionId session, int resetToStep) {
        final Map<String, String> sessionAttributes = session.getSessionAttributes();

        int currentStep = 1;
        if (sessionAttributes.containsKey(io.jans.as.model.config.Constants.AUTH_STEP)) {
            currentStep = StringHelper.toInteger(sessionAttributes.get(io.jans.as.model.config.Constants.AUTH_STEP), currentStep);
        }

        if (resetToStep <= currentStep) {
	        for (int i = resetToStep; i <= currentStep; i++) {
	            String key = String.format("auth_step_passed_%d", i);
	            sessionAttributes.remove(key);
	        }
        } else {
        	// Scenario when we sckip steps. In this case we need to mark all previous steps as passed
	        for (int i = currentStep + 1; i < resetToStep; i++) {
	            sessionAttributes.put(String.format("auth_step_passed_%d", i), Boolean.TRUE.toString());
	        }
        }

        sessionAttributes.put(io.jans.as.model.config.Constants.AUTH_STEP, String.valueOf(resetToStep));

        boolean updateResult = updateSessionId(session, true, true, true);
        if (!updateResult) {
            log.debug("Failed to update session entry: '{}'", session.getId());
            return null;
        }

        return session;
    }

    private Map<String, String> getCurrentSessionAttributes(Map<String, String> sessionAttributes) {
        if (facesContext == null) {
            return sessionAttributes;
        }

        // Update from request
        final Map<String, String> currentSessionAttributes = new HashMap<>(sessionAttributes);

        Map<String, String> requestParameters = externalContext.getRequestParameterMap();
        Map<String, String> newRequestParameterMap = requestParameterService.getAllowedParameters(requestParameters);
        for (Entry<String, String> newRequestParameterMapEntry : newRequestParameterMap.entrySet()) {
            String name = newRequestParameterMapEntry.getKey();
            if (!StringHelper.equalsIgnoreCase(name, io.jans.as.model.config.Constants.AUTH_STEP)) {
                currentSessionAttributes.put(name, newRequestParameterMapEntry.getValue());
            }
        }
        if (!requestParameters.containsKey(AuthorizeRequestParam.CODE_CHALLENGE) || !requestParameters.containsKey(AuthorizeRequestParam.CODE_CHALLENGE_METHOD)) {
            currentSessionAttributes.remove(AuthorizeRequestParam.CODE_CHALLENGE);
            currentSessionAttributes.remove(AuthorizeRequestParam.CODE_CHALLENGE_METHOD);
        }

        return currentSessionAttributes;
    }

    public SessionId getSessionId() {
        String sessionId = cookieService.getSessionIdFromCookie();

        if (StringHelper.isEmpty(sessionId) && identity.getSessionId() != null) {
            sessionId = identity.getSessionId().getId();
        }

        SessionId result = null; 
        if (StringHelper.isNotEmpty(sessionId)) {
        	result = getSessionId(sessionId);
        	if ((result == null) && identity.getSessionId() != null) {
        		// Here we cover scenario when user were redirected from /device-code to ACR method
        		// which call this method in prepareForStep for step 1. The cookie in this case is not updated yet.
        		// hence actual information about session_id only in identity.
                sessionId = identity.getSessionId().getId();
            	result = getSessionId(sessionId);
        	}
        } else {
            log.trace("Session cookie not exists");
        }

        return result;
    }

    public Map<String, String> getSessionAttributes(SessionId sessionId) {
        if (sessionId != null) {
            return sessionId.getSessionAttributes();
        }

        return null;
    }

    public SessionId generateAuthenticatedSessionId(HttpServletRequest httpRequest, String userDn) throws InvalidSessionStateException {
        Map<String, String> sessionIdAttributes = new HashMap<>();
        sessionIdAttributes.put(io.jans.as.model.config.Constants.PROMPT, "");

        return generateAuthenticatedSessionId(httpRequest, userDn, sessionIdAttributes);
    }

    public SessionId generateAuthenticatedSessionId(HttpServletRequest httpRequest, String userDn, String prompt) throws InvalidSessionStateException {
        Map<String, String> sessionIdAttributes = new HashMap<>();
        sessionIdAttributes.put(io.jans.as.model.config.Constants.PROMPT, prompt);

        return generateAuthenticatedSessionId(httpRequest, userDn, sessionIdAttributes);
    }

    public SessionId generateAuthenticatedSessionId(HttpServletRequest httpRequest, String userDn, Map<String, String> sessionIdAttributes) throws InvalidSessionStateException {
        SessionId sessionId = generateSessionId(userDn, new Date(), SessionIdState.AUTHENTICATED, sessionIdAttributes, true);
        if (sessionId == null) {
            throw new InvalidSessionStateException("Failed to generate authenticated session.");
        }

        reportActiveUser(sessionId);

        if (externalApplicationSessionService.isEnabled()) {
            String userName = sessionId.getSessionAttributes().get(Constants.AUTHENTICATED_USER);
            boolean externalResult = externalApplicationSessionService.executeExternalStartSessionMethods(httpRequest, sessionId);
            log.info("Start session result for '{}': '{}'", userName, externalResult);

            if (!externalResult) {
                reinitLogin(sessionId, true);
                throw new InvalidSessionStateException("Session creation is prohibited by external session script!");
            }

            externalEvent(new SessionEvent(SessionEventType.AUTHENTICATED, sessionId).setHttpRequest(httpRequest));
        }

        return sessionId;
    }

    private void reportActiveUser(SessionId sessionId) {
        try {
            final User user = getUser(sessionId);
            if (user != null) {
                statService.reportActiveUser(user.getUserId());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public SessionId generateUnauthenticatedSessionId(String userDn) {
        Map<String, String> sessionIdAttributes = new HashMap<>();
        return generateSessionId(userDn, new Date(), SessionIdState.UNAUTHENTICATED, sessionIdAttributes, true);
    }

    public SessionId generateUnauthenticatedSessionId(String userDn, Date authenticationDate, SessionIdState state, Map<String, String> sessionIdAttributes, boolean persist) {
        return generateSessionId(userDn, authenticationDate, state, sessionIdAttributes, persist);
    }

    public String computeSessionState(SessionId sessionId, String clientId, String redirectUri) {
        final boolean isSameClient = clientId.equals(sessionId.getSessionAttributes().get("client_id")) &&
                redirectUri.equals(sessionId.getSessionAttributes().get("redirect_uri"));
        if (isSameClient)
            return sessionId.getSessionState();
        final String salt = UUID.randomUUID().toString();
        final String opbs = sessionId.getOPBrowserState();
        return computeSessionState(clientId, redirectUri, opbs, salt);
    }

    private String computeSessionState(String clientId, String redirectUri, String opbs, String salt) {
        try {
            final String clientOrigin = getClientOrigin(redirectUri);
            return JwtUtil.bytesToHex(JwtUtil.getMessageDigestSHA256(
                    clientId + " " + clientOrigin + " " + opbs + " " + salt)) + "." + salt;
        } catch (NoSuchProviderException | NoSuchAlgorithmException | URISyntaxException e) {
            if (log.isErrorEnabled())
                log.error("Failed generating session state! " + e.getMessage(), e);
            throw new FailedComputeSessionStateException(e.getMessage(), e);
        }
    }

    private String getClientOrigin(String redirectUri) throws URISyntaxException {
        if (StringHelper.isNotEmpty(redirectUri)) {
            final URI uri = new URI(redirectUri);
            String result = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() > 0)
                result += ":" + uri.getPort();
            return result;
        } else {
            return appConfiguration.getIssuer();
        }
    }

    private SessionId generateSessionId(String userDn, Date authenticationDate, SessionIdState state, Map<String, String> sessionIdAttributes, boolean persist) {
        final String internalSid = UUID.randomUUID().toString();
        final String outsideSid = UUID.randomUUID().toString();
        final String salt = UUID.randomUUID().toString();
        final String clientId = sessionIdAttributes.get("client_id");
        final String opbs = UUID.randomUUID().toString();
        final String redirectUri = sessionIdAttributes.get("redirect_uri");
        final String sessionState = computeSessionState(clientId, redirectUri, opbs, salt);
        final String dn = buildDn(internalSid);
        sessionIdAttributes.put(SessionId.OP_BROWSER_STATE, opbs);

        Preconditions.checkNotNull(dn);

        if (SessionIdState.AUTHENTICATED == state && StringUtils.isBlank(userDn) && !sessionIdAttributes.containsKey("uma")) {
            return null;
        }

        final SessionId sessionId = new SessionId();
        sessionId.setId(internalSid);
        sessionId.setOutsideSid(outsideSid);
        sessionId.setDn(dn);
        sessionId.setUserDn(userDn);
        sessionId.setSessionState(sessionState);

        final Pair<Date, Integer> expiration = expirationDate(sessionId.getCreationDate(), state);
        sessionId.setExpirationDate(expiration.getFirst());
        sessionId.setTtl(expiration.getSecond());

        sessionId.setAuthenticationTime(authenticationDate != null ? authenticationDate : new Date());

        if (state != null) {
            sessionId.setState(state);
        }

        sessionId.setSessionAttributes(sessionIdAttributes);
        sessionId.setLastUsedAt(new Date());

        boolean persisted = false;
        if (persist) {
            persisted = persistSessionId(sessionId);
        }

        auditLogging(sessionId);

        log.trace("Generated new session, id = '{}', state = '{}', persisted = '{}'", sessionId.getId(), sessionId.getState(), persisted);
        return sessionId;
    }

    public SessionId setSessionIdStateAuthenticated(HttpServletRequest httpRequest, HttpServletResponse httpResponse, SessionId sessionId, String userDn) {
        sessionId.setUserDn(userDn);
        sessionId.setAuthenticationTime(new Date());
        sessionId.setState(SessionIdState.AUTHENTICATED);

        final User user = getUser(sessionId);
        if (user != null) {
            statService.reportActiveUser(user.getUserId());
        }

        final boolean persisted;
        if (isTrue(appConfiguration.getChangeSessionIdOnAuthentication()) && httpResponse != null) {
            final String oldSessionId = sessionId.getId();
            final String newSessionId = UUID.randomUUID().toString();

            log.debug("Changing session id from {} to {} ...", oldSessionId, newSessionId);
            remove(sessionId);

            sessionId.setId(newSessionId);
            sessionId.setDn(buildDn(newSessionId));
            sessionId.getSessionAttributes().put(SessionId.OLD_SESSION_ID_ATTR_KEY, oldSessionId);

            persisted = persistSessionId(sessionId, true);
            cookieService.createSessionIdCookie(sessionId, httpRequest, httpResponse, false);
            log.debug("Session identifier changed from {} to {} .", oldSessionId, newSessionId);
        } else {
            persisted = updateSessionId(sessionId, true, true, true);
        }

        auditLogging(sessionId);
        log.trace("Authenticated session, id = '{}', state = '{}', persisted = '{}'", sessionId.getId(), sessionId.getState(), persisted);

        if (externalApplicationSessionService.isEnabled()) {
            String userName = sessionId.getSessionAttributes().get(Constants.AUTHENTICATED_USER);
            boolean externalResult = externalApplicationSessionService.executeExternalStartSessionMethods(httpRequest, sessionId);
            log.info("Start session result for '{}': '{}'", userName, externalResult);

            if (!externalResult) {
                reinitLogin(sessionId, true);
                throw new InvalidSessionStateException("Session creation is prohibited by external session script!");
            }
            externalEvent(new SessionEvent(SessionEventType.AUTHENTICATED, sessionId).setHttpRequest(httpRequest).setHttpResponse(httpResponse));
        }

        return sessionId;
    }

    public boolean persistSessionId(final SessionId sessionId) {
        return persistSessionId(sessionId, false);
    }

    public boolean persistSessionId(final SessionId sessionId, boolean forcePersistence) {
        List<Prompt> prompts = getPromptsFromSessionId(sessionId);

        try {
            final int unusedLifetime = appConfiguration.getSessionIdUnusedLifetime();
            if ((unusedLifetime > 0 && isPersisted(prompts)) || forcePersistence) {
                sessionId.setLastUsedAt(new Date());

                final Pair<Date, Integer> expiration = expirationDate(sessionId.getCreationDate(), sessionId.getState());
                sessionId.setPersisted(true);
                sessionId.setExpirationDate(expiration.getFirst());
                sessionId.setTtl(expiration.getSecond());
                log.trace("sessionIdAttributes: {}", sessionId.getPermissionGrantedMap());
                if (isTrue(appConfiguration.getSessionIdPersistInCache())) {
                    cacheService.put(expiration.getSecond(), sessionId.getDn(), sessionId);
                } else {
                    persistenceEntryManager.persist(sessionId);
                }
                localCacheService.put(DEFAULT_LOCAL_CACHE_EXPIRATION, sessionId.getDn(), sessionId);
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

    public boolean updateSessionId(final SessionId sessionId) {
        return updateSessionId(sessionId, true);
    }

    public boolean updateSessionId(final SessionId sessionId, boolean updateLastUsedAt) {
        return updateSessionId(sessionId, updateLastUsedAt, false, true);
    }

    public boolean updateSessionId(final SessionId sessionId, boolean updateLastUsedAt, boolean forceUpdate, boolean modified) {
        List<Prompt> prompts = getPromptsFromSessionId(sessionId);

        try {
            final int unusedLifetime = appConfiguration.getSessionIdUnusedLifetime();
            if ((unusedLifetime > 0 && isPersisted(prompts)) || forceUpdate) {
                boolean update = modified;

                if (updateLastUsedAt) {
                    Date lastUsedAt = new Date();
                    if (sessionId.getLastUsedAt() != null) {
                        long diff = lastUsedAt.getTime() - sessionId.getLastUsedAt().getTime();
                        int unusedDiffInSeconds = (int) (diff / 1000);
                        if (unusedDiffInSeconds > unusedLifetime) {
                            log.debug("Session id expired: {} by sessionIdUnusedLifetime, remove it.", sessionId.getId());
                            remove(sessionId); // expired
                            return false;
                        }

                        if (diff > 500) { // update only if diff is more than 500ms
                            update = true;
                            sessionId.setLastUsedAt(lastUsedAt);
                        }
                    } else {
                        update = true;
                        sessionId.setLastUsedAt(lastUsedAt);
                    }
                }

                if (!sessionId.isPersisted()) {
                    update = true;
                    sessionId.setPersisted(true);
                }

                if (isExpired(sessionId)) {
                    log.debug("Session id expired: {} by lifetime property, remove it.", sessionId.getId());
                    remove(sessionId); // expired
                    update = false;
                }

                if (update) {
                    mergeWithRetry(sessionId);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    public boolean isExpired(SessionId sessionId) {
        if (sessionId.getAuthenticationTime() == null) {
            return false;
        }
        final long currentLifetimeInSeconds = (System.currentTimeMillis() - sessionId.getAuthenticationTime().getTime()) / 1000;

        return currentLifetimeInSeconds > getServerSessionIdLifetimeInSeconds();
    }

    public int getServerSessionIdLifetimeInSeconds() {
        if (appConfiguration.getServerSessionIdLifetime() != null && appConfiguration.getServerSessionIdLifetime() > 0) {
            return appConfiguration.getServerSessionIdLifetime();
        }
        if (appConfiguration.getSessionIdLifetime() != null && appConfiguration.getSessionIdLifetime() > 0) {
            return appConfiguration.getSessionIdLifetime();
        }

        // we don't know for how long we can put it in cache/persistence since expiration is not set, so we set it to max integer.
        if (appConfiguration.getServerSessionIdLifetime() != null && appConfiguration.getSessionIdLifetime() != null &&
                appConfiguration.getServerSessionIdLifetime() <= 0 && appConfiguration.getSessionIdLifetime() <= 0) {
            return Integer.MAX_VALUE;
        }
        log.debug("Session id lifetime configuration is null.");
        return AppConfiguration.DEFAULT_SESSION_ID_LIFETIME;
    }

    private Pair<Date, Integer> expirationDate(Date creationDate, SessionIdState state) {
        int expirationInSeconds = state == SessionIdState.UNAUTHENTICATED ?
                appConfiguration.getSessionIdUnauthenticatedUnusedLifetime() :
                getServerSessionIdLifetimeInSeconds();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(creationDate);
        calendar.add(Calendar.SECOND, expirationInSeconds);
        return new Pair<>(calendar.getTime(), expirationInSeconds);
    }

    private void mergeWithRetry(final SessionId sessionId) {
        final Pair<Date, Integer> expiration = expirationDate(sessionId.getCreationDate(), sessionId.getState());
        sessionId.setExpirationDate(expiration.getFirst());
        sessionId.setTtl(expiration.getSecond());

        EntryPersistenceException lastException = null;
        for (int i = 1; i <= MAX_MERGE_ATTEMPTS; i++) {
            try {
                if (isTrue(appConfiguration.getSessionIdPersistInCache())) {
                    cacheService.put(expiration.getSecond(), sessionId.getDn(), sessionId);
                } else {
                    persistenceEntryManager.merge(sessionId);
                }
                localCacheService.put(DEFAULT_LOCAL_CACHE_EXPIRATION, sessionId.getDn(), sessionId);
                externalEvent(new SessionEvent(SessionEventType.UPDATED, sessionId));
                return;
            } catch (EntryPersistenceException ex) {
                lastException = ex;
                if (ex.getCause() instanceof LDAPException) {
                    LDAPException parentEx = ((LDAPException) ex.getCause());
                    log.debug("LDAP exception resultCode: '{}'", parentEx.getResultCode().intValue());
                    if ((parentEx.getResultCode().intValue() == ResultCode.NO_SUCH_ATTRIBUTE_INT_VALUE) ||
                            (parentEx.getResultCode().intValue() == ResultCode.ATTRIBUTE_OR_VALUE_EXISTS_INT_VALUE)) {
                        log.warn("Session entry update attempt '{}' was unsuccessfull", i);
                        continue;
                    }
                }

                throw ex;
            }
        }

        log.error("Session entry update attempt was unsuccessfull after '{}' attempts", MAX_MERGE_ATTEMPTS);
        throw lastException;
    }

    public void updateSessionIdIfNeeded(SessionId sessionId, boolean modified) {
        updateSessionId(sessionId, true, false, modified);
    }

    private boolean isPersisted(List<Prompt> prompts) {
        if (prompts != null && prompts.contains(Prompt.NONE)) {
            final Boolean persistOnPromptNone = appConfiguration.getSessionIdPersistOnPromptNone();
            return persistOnPromptNone != null && persistOnPromptNone;
        }
        return true;
    }

    @Nullable
    public SessionId getSessionById(@Nullable String sessionId, boolean silently) {
        return getSessionByDn(buildDn(sessionId), silently);
    }

    @Nullable
    public SessionId getSessionByDn(@Nullable String dn) {
        return getSessionByDn(dn, false);
    }

    @Nullable
    public SessionId getSessionBySid(@Nullable String sid) {
        if (StringUtils.isBlank(sid)) {
            return null;
        }

        final List<SessionId> entries = persistenceEntryManager.findEntries(staticConfiguration.getBaseDn().getSessions(), SessionId.class, Filter.createEqualityFilter("sid", sid));
        if (entries == null || entries.size() != 1) {
            return null;
        }
        return entries.get(0);
    }

    @Nullable
    public SessionId getSessionByDeviceSecret(@Nullable String deviceSecret) {
        if (StringUtils.isBlank(deviceSecret)) {
            return null;
        }

        final List<SessionId> entries = persistenceEntryManager.findEntries(staticConfiguration.getBaseDn().getSessions(), SessionId.class, Filter.createEqualityFilter("deviceSecret", deviceSecret));
        if (entries == null || entries.size() != 1) {
            return null;
        }
        return entries.get(0);
    }

    @Nullable
    public SessionId getSessionByDn(@Nullable String dn, boolean silently) {
        if (StringUtils.isBlank(dn)) {
            return null;
        }

        final Object localCopy = localCacheService.get(dn);
        if (localCopy instanceof SessionId) {
            if (isSessionValid((SessionId) localCopy)) {
                return (SessionId) localCopy;
            } else {
                localCacheService.remove(dn);
            }
        }

        try {
            final SessionId sessionId;
            if (isTrue(appConfiguration.getSessionIdPersistInCache())) {
                sessionId = (SessionId) cacheService.get(dn);
            } else {
                sessionId = persistenceEntryManager.find(SessionId.class, dn);
            }
            localCacheService.put(DEFAULT_LOCAL_CACHE_EXPIRATION, sessionId.getDn(), sessionId);
            return sessionId;
        } catch (Exception e) {
            if (!silently) {
                log.error("Failed to get session by dn: {}. {}", dn, e.getMessage());
            }
        }
        return null;
    }

    public SessionId getSessionId(HttpServletRequest request) {
        final String sessionIdFromCookie = cookieService.getSessionIdFromCookie(request);
        log.trace("SessionId from cookie: {}", sessionIdFromCookie);
        return getSessionId(sessionIdFromCookie);
    }

    public SessionId getSessionId(String sessionId) {
        return getSessionId(sessionId, false);
    }

    public SessionId getSessionId(String sessionId, boolean silently) {
        if (StringHelper.isEmpty(sessionId)) {
            return null;
        }

        try {
            final SessionId entity = getSessionById(sessionId, silently);
            log.trace("Try to get session by id: {} ...", sessionId);
            if (entity != null) {
                log.trace("Session dn: {}", entity.getDn());

                if (isSessionValid(entity)) {
                    return entity;
                }
            }
        } catch (Exception ex) {
            if (!silently) {
                log.trace(ex.getMessage(), ex);
            }
        }

        log.trace("Failed to get session by id: {}", sessionId);
        return null;
    }

    public boolean remove(SessionId sessionId) {
        try {
            if (isTrue(appConfiguration.getSessionIdPersistInCache())) {
                cacheService.remove(sessionId.getDn());
            } else {
                persistenceEntryManager.remove(sessionId.getDn(), SessionId.class);
            }
            localCacheService.remove(sessionId.getDn());
            externalEvent(new SessionEvent(SessionEventType.GONE, sessionId));
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public void remove(List<SessionId> list) {
        for (SessionId id : list) {
            try {
                remove(id);
            } catch (Exception e) {
                log.error("Failed to remove entry", e);
            }
        }
    }

    public boolean isSessionValid(SessionId sessionId) {
        if (sessionId == null) {
            return false;
        }

        final long sessionInterval = TimeUnit.SECONDS.toMillis(appConfiguration.getSessionIdUnusedLifetime());
        final long sessionUnauthenticatedInterval = TimeUnit.SECONDS.toMillis(appConfiguration.getSessionIdUnauthenticatedUnusedLifetime());

        final long timeSinceLastAccess = System.currentTimeMillis() - sessionId.getLastUsedAt().getTime();
        if (timeSinceLastAccess > sessionInterval && appConfiguration.getSessionIdUnusedLifetime() != -1) {
            return false;
        }
        return sessionId.getState() != SessionIdState.UNAUTHENTICATED || timeSinceLastAccess <= sessionUnauthenticatedInterval || appConfiguration.getSessionIdUnauthenticatedUnusedLifetime() == -1;
    }

    private List<Prompt> getPromptsFromSessionId(final SessionId sessionId) {
        String promptParam = sessionId.getSessionAttributes().get("prompt");
        return Prompt.fromString(promptParam, " ");
    }

    public boolean isSessionIdAuthenticated(SessionId sessionId) {
        if (sessionId == null) {
            return false;
        }
        return SessionIdState.AUTHENTICATED.equals(sessionId.getState());
    }

    /**
     * By definition we expects space separated acr values as it is defined in spec. But we also try maybe some client
     * sent it to us as json array. So we try both.
     *
     * @return acr value list
     */
    public List<String> acrValuesList(String acrValues) {
        List<String> acrs;
        try {
            acrs = Util.jsonArrayStringAsList(acrValues);
        } catch (JSONException ex) {
            acrs = Util.splittedStringAsList(acrValues, " ");
        }

        HashSet<String> resultAcrs = new HashSet<>();
        for (String acr : acrs) {
            resultAcrs.add(externalAuthenticationService.scriptName(acr));
        }

        return new ArrayList<>(resultAcrs);
    }

    private void auditLogging(SessionId sessionId) {
        HttpServletRequest httpServletRequest = ServerUtil.getRequestOrNull();
        if (httpServletRequest != null) {
            Action action;
            switch (sessionId.getState()) {
                case AUTHENTICATED:
                    action = Action.SESSION_AUTHENTICATED;
                    break;
                case UNAUTHENTICATED:
                    action = Action.SESSION_UNAUTHENTICATED;
                    break;
                default:
                    action = Action.SESSION_UNAUTHENTICATED;
            }
            OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpServletRequest), action);
            oAuth2AuditLog.setSuccess(true);
            applicationAuditLogger.sendMessage(oAuth2AuditLog);
        }
    }

    public User getUser(SessionId sessionId) {
        if (sessionId == null) {
            return null;
        }

        if (sessionId.getUser() != null) {
            return sessionId.getUser();
        }

        if (StringUtils.isBlank(sessionId.getUserDn())) {
            return null;
        }

        final User user = userService.getUserByDn(sessionId.getUserDn());
        if (user != null) {
            sessionId.setUser(user);
            return user;
        }

        return null;
    }

    public List<SessionId> findByUser(String userDn) {
        if (isTrue(appConfiguration.getSessionIdPersistInCache())) {
            throw new UnsupportedOperationException("Operation is not supported with sessionIdPersistInCache=true. Set it to false to avoid this exception.");
        }
        Filter filter = Filter.createEqualityFilter("jansUsrDN", userDn);
        return persistenceEntryManager.findEntries(staticConfiguration.getBaseDn().getSessions(), SessionId.class, filter);
    }

    public void externalEvent(SessionEvent event) {
        externalApplicationSessionService.externalEvent(event);
    }
}