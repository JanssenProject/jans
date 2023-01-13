/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.legacy.service;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.model.common.Prompt;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.as.model.jwt.JwtClaimName;
import io.jans.as.model.util.Pair;
import io.jans.fido2.security.Identity;
import io.jans.fido2.legacy.service.external.ExternalApplicationSessionService;
import io.jans.fido2.legacy.service.external.session.SessionEvent;
import io.jans.fido2.legacy.service.external.session.SessionEventType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.service.CacheService;
import io.jans.service.LocalCacheService;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.jans.as.model.configuration.AppConfiguration.DEFAULT_SESSION_ID_LIFETIME;
import static org.apache.commons.lang.BooleanUtils.isTrue;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version December 8, 2018
 */
@RequestScoped
public class SessionIdService {

    private static final int MAX_MERGE_ATTEMPTS = 3;
    private static final int DEFAULT_LOCAL_CACHE_EXPIRATION = 2;

    @Inject
    private Logger log;

    @Inject
    private ExternalApplicationSessionService externalApplicationSessionService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

//    @Inject
//    private CookieService cookieService;

    @Inject
    private Identity identity;

    @Inject
    private LocalCacheService localCacheService;

    @Inject
    private CacheService cacheService;

    private String buildDn(String sessionId) {
        return String.format("jansId=%s,%s", sessionId, staticConfiguration.getBaseDn().getSessions());
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

    public SessionId getSessionId() {
//        String sessionId = cookieService.getSessionIdFromCookie();
//
//        if (StringHelper.isEmpty(sessionId) && identity.getSessionId() != null) {
//            sessionId = identity.getSessionId().getId();
//        }
        String sessionId = identity.getSessionId().getId();
        if (StringHelper.isNotEmpty(sessionId)) {
            return getSessionId(sessionId);
        } else {
            log.trace("Session cookie not exists");
        }

        return null;
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
        return DEFAULT_SESSION_ID_LIFETIME;
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

    public void externalEvent(SessionEvent event) {
        externalApplicationSessionService.externalEvent(event);
    }
}