/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.service.auth;

import io.jans.as.common.model.common.User;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.common.model.session.SessionId;
import io.jans.configapi.util.AuthUtil;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;


@ApplicationScoped
public class SessionService {

    @Inject
    PersistenceEntryManager persistenceEntryManager;
    
    @Inject
    private AppConfiguration appConfiguration;

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
    
    public void revokeSession(String userId) {
        logger.error("Revoke session sid:{}, authUtil:{}", userId, authUtil);
        logger.error("authUtil.getEndSessionEndpoint():{}",  authUtil.getEndSessionEndpoint());
        
        //Get User details
        String userDn = this.getDnForUser(userId);
        logger.error("Fetch user details - userDn:{}",userDn);
        User user = getUserBasedOnInum(userDn);
        if(user==null) {
            throw new NoSuchElementException("User  -'"+userId+"'  does not exists!!");
        }
        
        logger.error("User detail - user:{}",user);
        authUtil.revokeSession(authUtil.getEndSessionEndpoint(),requestAccessToken(user.getUserId()), user.getUserId());
    }
    
    private String getDnForUser(String userId) {
        if (StringHelper.isEmpty(userId)) {
            return staticConfiguration.getBaseDn().getPeople();
        }
        return String.format("jansId=%s,%s", userId, staticConfiguration.getBaseDn().getPeople());
    }
    
    private User getUserBasedOnInum(String inum) {
        logger.error("Fetch user inum:{}",inum);
        User result = null;
        try {
            result = getUserByDn(inum);
        } catch (Exception ex) {
            logger.error("Failed to load user entry", ex);
        }
        return result;
    }
    
    private User getUserByDn(String dn, String... returnAttributes) {
        logger.error("Fetch user - dn:{}",dn);
        if (StringHelper.isEmpty(dn)) {
            return null;
        }
        return persistenceEntryManager.find(dn, User.class, returnAttributes);
    }

    
    private String requestAccessToken(final String clientId) {
        final List<String> scopes = Arrays.asList("revoke_session");
        String accessToken = authUtil.requestAccessToken(clientId,scopes);
        
        logger.info("oAuth AccessToken response - accessToken:{}", accessToken);      
        return accessToken;
    }

}
