/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.service.CacheService;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class SessionService {

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    StaticConfiguration staticConfiguration;

    @Inject
    CacheService cacheService;

    @Inject
    private Logger logger;

    private String getDnForSession(String sessionId) {
        if (StringHelper.isEmpty(sessionId)) {
            return staticConfiguration.getBaseDn().getSessions();
        }
        return String.format("jansId=%s,%s", sessionId, staticConfiguration.getBaseDn().getSessions());
    }

    public SessionId getSessionById(String sid) {
        logger.debug("Get Session by sid:{}", sid);
        SessionId sessionId = null;
        try {
            sessionId = persistenceEntryManager.find(SessionId.class, getDnForSession(sid));
        } catch (Exception ex) {
            logger.error("Failed to load session entry", ex);
        }
        return sessionId;
    }

    public List<SessionId> getAllSessions(int sizeLimit) {
        logger.debug("Get All Session sizeLimit:{}", sizeLimit);
        return persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class, null, sizeLimit);
    }

    public List<SessionId> getAllSessions() {
        return persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class, null);
    }

    public List<SessionId> getSessions() {
        List<SessionId> sessionList = persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class,
                Filter.createGreaterOrEqualFilter("exp", persistenceEntryManager.encodeTime(getDnForSession(null),
                        new Date(System.currentTimeMillis()))),
                0);
        logger.debug("All sessionList:{}", sessionList);

        sessionList.sort((SessionId s1, SessionId s2) -> s2.getCreationDate().compareTo(s1.getCreationDate()));
        logger.debug("Sorted Session sessionList:{}", sessionList);
        return sessionList;
    }

    public void revokeSession(String userDn) {
        logger.debug("Revoke session userDn:{}, cacheService:{}", userDn, cacheService);

        if (StringUtils.isNotBlank(userDn)) {
            Filter filter = Filter.createANDFilter(Filter.createEqualityFilter("jansUsrDN", userDn),
                    Filter.createEqualityFilter("jansState", SessionIdState.AUTHENTICATED));

            List<SessionId> sessionList = persistenceEntryManager.findEntries(getDnForSession(null), SessionId.class,
                    filter);
            logger.debug("User sessionList:{}", sessionList);

            if (sessionList == null || sessionList.isEmpty()) {
                throw new NotFoundException(
                        "No " + SessionIdState.AUTHENTICATED + " session exists for the user '" + userDn + "'!!!");
            }

            sessionList.stream().forEach(session -> {
                persistenceEntryManager.remove(session.getDn(), SessionId.class);
                cacheService.remove(session.getDn());
            });
        }

    }

}
