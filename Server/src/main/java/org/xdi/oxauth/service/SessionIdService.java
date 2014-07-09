package org.xdi.oxauth.service;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.StaticUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.SessionId;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.util.Util;
import org.xdi.util.StringHelper;

import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 20/12/2012
 */

@Scope(ScopeType.STATELESS)
@Name("sessionIdService")
@AutoCreate
public class SessionIdService {

    private static final String SESSION_ID_COOKIE_NAME = "session_id";

    @Logger
    private Log log;

    @In
    private LdapEntryManager ldapEntryManager;

    public static SessionIdService instance() {
        if (!Contexts.isEventContextActive() && !Contexts.isApplicationContextActive()) {
            Lifecycle.beginCall();
        }
        return (SessionIdService) Component.getInstance(SessionIdService.class);
    }

    public String getSessionIdFromCookie(HttpServletRequest request) {
        try {
            final Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(SESSION_ID_COOKIE_NAME) /*&& cookie.getSecure()*/) {
                        log.trace("Found session_id cookie: {0}", cookie.getValue());
                        return cookie.getValue();
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public void createSessionIdCookie(String sessionId) {
        try {
            final Object response = FacesContext.getCurrentInstance().getExternalContext().getResponse();
            if (response instanceof HttpServletResponse) {
                final Cookie sessionIdCookie = new Cookie(SESSION_ID_COOKIE_NAME, sessionId);
                sessionIdCookie.setPath("/");

                // ATTENTION : we have to set also HttpOnly flag but it's supported from Servlet 3.0
                // we need to upgrade to Servlet 3.0 and target to Tomcat 7 : http://tomcat.apache.org/whichversion.html
//                sessionIdCookie.setSecure(true);
//                sessionIdCookie.setHttpOnly(true);
                final HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.addCookie(sessionIdCookie);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void removeSessionIdCookie() {
        try {
            final FacesContext currentInstance = FacesContext.getCurrentInstance();
            if (currentInstance != null && currentInstance.getExternalContext() != null) {
                final Object response = currentInstance.getExternalContext().getResponse();
                if (response instanceof HttpServletResponse) {
                    removeSessionIdCookie((HttpServletResponse) response);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void removeSessionIdCookie(HttpServletResponse httpResponse) {
        final Cookie cookie = new Cookie(SESSION_ID_COOKIE_NAME, null); // Not necessary, but saves bandwidth.
        cookie.setPath("/");
        cookie.setMaxAge(0); // Don't set to -1 or it will become a session cookie!
        httpResponse.addCookie(cookie);
    }

    public String generateId(String p_userDn, List<Prompt> prompts) {
        final SessionId id = generateSessionId(p_userDn, prompts);
        if (id != null) {
            return id.getId();
        }
        return "";
    }

    public SessionId generateSessionIdInteractive(String p_userDn) {
        return generateSessionId(p_userDn, new ArrayList<Prompt>());
    }

    public SessionId generateSessionId(String p_userDn, Date authenticationDate, List<Prompt> prompts) {
        try {
            final String uuid = UUID.randomUUID().toString();
            final String dn = dn(uuid);

            if (Util.allNotBlank(p_userDn, dn)) {
                final SessionId sessionId = new SessionId();
                sessionId.setId(uuid);
                sessionId.setDn(dn);
                sessionId.setLastUsedAt(new Date());
                sessionId.setUserDn(p_userDn);
                if (authenticationDate != null) {
                    sessionId.setAuthenticationTime(authenticationDate);
                }

                final int unusedLifetime = ConfigurationFactory.getConfiguration().getSessionIdUnusedLifetime();
                FacesContext facesContext = FacesContext.getCurrentInstance();
                if (facesContext != null) {
                    Cookie cookie = new Cookie("opbs", uuid);
                    cookie.setMaxAge(unusedLifetime);
                    ((HttpServletResponse) facesContext.getExternalContext().getResponse()).addCookie(cookie);
                }

                if (unusedLifetime > 0 && isPersisted(prompts)) {
                    ldapEntryManager.persist(sessionId);
                }
                log.trace("Generated new session, id = {0}", sessionId.getId());
                return sessionId;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private static boolean isPersisted(List<Prompt> prompts) {
        if (prompts != null && prompts.contains(Prompt.NONE)) {
            final Boolean persistOnPromptNone = ConfigurationFactory.getConfiguration().getSessionIdPersistOnPromptNone();
            return persistOnPromptNone != null && persistOnPromptNone;
        }
        return true;
    }

    public SessionId generateSessionId(String p_userDn, List<Prompt> prompts) {
        return generateSessionId(p_userDn, null, prompts);
    }

    private static String dn(String p_id) {
        final String baseDn = getBaseDn();
        final StringBuilder sb = new StringBuilder();
        if (Util.allNotBlank(p_id, getBaseDn())) {
            sb.append("uniqueIdentifier=").append(p_id).append(",").append(baseDn);
        }
        return sb.toString();
    }

    public SessionId getSessionByDN(String p_dn) {
        try {
            return ldapEntryManager.find(SessionId.class, p_dn);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public SessionId getSessionId(String sessionId) {
        if (StringHelper.isEmpty(sessionId)) {
            return null;
        }

        try {
            log.trace("Try to get session by id: {0} ...", sessionId);
            final List<SessionId> entries = ldapEntryManager.findEntries(getBaseDn(), SessionId.class, Filter.create(String.format("uniqueIdentifier=%s", sessionId)));
            if (entries != null && !entries.isEmpty()) {
                final SessionId entity = entries.get(0);
                log.trace("Session dn: {0}", entity.getDn());

                if (isSessionValid(entity)) {
                    return entity;
                }
            }
        } catch (LDAPException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        log.trace("Failed to get session by id: {0}", sessionId);
        return null;
    }

    private static String getBaseDn() {
        return ConfigurationFactory.getBaseDn().getSessionId();
    }

    public boolean remove(SessionId p_sessionId) {
        try {
            ldapEntryManager.remove(p_sessionId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            return false;
        }
        return true;
    }

    public void updateSessionWithLastUsedDate(SessionId p_sessionId) {
        updateSessionWithLastUsedDate(p_sessionId, new ArrayList<Prompt>());
    }

    public void updateSessionWithLastUsedDate(SessionId p_sessionId, List<Prompt> prompts) {
        try {
            if (!isPersisted(prompts)) {
                return;
            }

            final Date newDate = new Date();
            p_sessionId.setLastUsedAt(newDate);
            ldapEntryManager.merge(p_sessionId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void cleanUpSessions() {
        final int interval = ConfigurationFactory.getConfiguration().getSessionIdUnusedLifetime();
        final List<SessionId> list = getIdsOlderThan(interval);
        if (list != null && !list.isEmpty()) {
            for (SessionId id : list) {
                remove(id);
            }
        }
    }

    public List<SessionId> getIdsOlderThan(int p_intervalInSeconds) {
        try {
            final long dateInPast = new Date().getTime() - TimeUnit.SECONDS.toMillis(p_intervalInSeconds);
            final Filter filter = Filter.create(String.format("(lastModifiedTime<=%s)", StaticUtils.encodeGeneralizedTime(new Date(dateInPast))));
            return ldapEntryManager.findEntries(getBaseDn(), SessionId.class, filter);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public boolean isSessionValid(SessionId sessionId) {
        if (sessionId == null) {
            return false;
        }

        final int interval = ConfigurationFactory.getConfiguration().getSessionIdUnusedLifetime();
        final long sessionInterval = TimeUnit.SECONDS.toMillis(interval);

        final long timeSinceLastAccess = System.currentTimeMillis() - sessionId.getLastUsedAt().getTime();
        if (timeSinceLastAccess > sessionInterval) {
            return false;
        }

        return true;
    }

}
