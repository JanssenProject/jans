package org.gluu.oxauth.auth;

import com.google.common.collect.Sets;
import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.common.User;
import org.gluu.oxauth.service.SessionIdService;
import org.slf4j.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 */
@RequestScoped
@Named
public class SelectAccountAction {

    @Inject
    private Logger log;

    @Inject
    private SessionIdService sessionIdService;

    private Set<SessionId> currentSessions = Sets.newHashSet();

    public void prepare() {
        currentSessions = Sets.newHashSet();
        for (SessionId sessionId : sessionIdService.getCurrentSessions()) {
            final User user = sessionIdService.getUser(sessionId);
            if (user == null) {
                log.error("Failed to get user for session. Skipping it from current_sessions, id: " + sessionId.getId());
                continue;
            }
            currentSessions.add(sessionId);
        }
    }

    public Set<SessionId> getCurrentSessions() {
        return currentSessions;
    }

    public void select(SessionId selectedSession) {
        log.debug("Selected account: " + selectedSession.getId());
    }
}
