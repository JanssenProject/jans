package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.session.AuthorizationChallengeSession;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.orm.PersistenceEntryManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * @author Yuriy Z
 */
@Named
@ApplicationScoped
public class AuthorizationChallengeSessionService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private StaticConfiguration staticConfiguration;

    public AuthorizationChallengeSession newAuthorizationChallengeSession() {
        final String id = UUID.randomUUID().toString();
        return newAuthorizationChallengeSession(id);
    }

    public AuthorizationChallengeSession newAuthorizationChallengeSession(String id) {
        int lifetimeInSeconds = appConfiguration.getAuthorizationChallengeSessionLifetimeInSeconds();

        final Calendar calendar = new GregorianCalendar();
        final Date creationDate = calendar.getTime();
        calendar.add(Calendar.SECOND, lifetimeInSeconds);
        final Date expirationDate = calendar.getTime();

        AuthorizationChallengeSession session = new AuthorizationChallengeSession();
        session.setId(id);
        session.setDn(buildDn(id));
        session.setDeletable(true);
        session.setTtl(lifetimeInSeconds);
        session.setCreationDate(creationDate);
        session.setExpirationDate(expirationDate);
        return session;
    }

    public String buildDn(String id) {
        return String.format("jansId=%s,%s", id, staticConfiguration.getBaseDn().getSessions());
    }

    public AuthorizationChallengeSession getAuthorizationChallengeSessionByDn(String dn) {
        try {
            return persistenceEntryManager.find(AuthorizationChallengeSession.class, dn);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
            return null;
        }
    }

    public AuthorizationChallengeSession getAuthorizationChallengeSession(String id) {
        if (StringUtils.isNotBlank(id)) {
            AuthorizationChallengeSession result = getAuthorizationChallengeSessionByDn(buildDn(id));
            log.debug("Found {} entries for authorizationChallengeSession id = {}", result != null ? 1 : 0, id);

            return result;
        }
        return null;
    }

    public void persist(AuthorizationChallengeSession entity) {
        persistenceEntryManager.persist(entity);
    }

    public void merge(AuthorizationChallengeSession entity) {
        persistenceEntryManager.merge(entity);
    }
}
