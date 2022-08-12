/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.configapi.model.common.SessionId;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.configapi.util.AuthUtil;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;


@ApplicationScoped
public class SessionService {

    @Inject
    PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;
    
    @Inject
    AuthUtil authUtil;

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
    
    public void revokeSession(SessionId session) {
        logger.debug("Revoke session:{}, authUtil.getEndSessionEndpoint():{}", session, authUtil.getEndSessionEndpoint());
        authUtil.revokeSession(authUtil.getEndSessionEndpoint(), "", "", session.getUser().getUserId());
    }

}
