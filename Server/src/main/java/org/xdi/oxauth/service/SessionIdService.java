/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.util.StaticUtils;

import org.apache.commons.lang.StringUtils;
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
import org.xdi.oxauth.model.common.SessionIdState;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.util.Util;
import org.xdi.util.StringHelper;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @version 0.9, 20/12/2012
 */

@Scope(ScopeType.STATELESS)
@Name("sessionIdService")
@AutoCreate
public class SessionIdService {

    private static final String SESSION_ID_COOKIE_NAME = "session_id";
    private static final String STORED_ORIGIN_PARAMETERS = "stored_origin_parameters";

    @Logger
    private Log log;

    @In
    private LdapEntryManager ldapEntryManager;

    @In
    private AuthenticationService authenticationService;

    public static SessionIdService instance() {
        if (!Contexts.isEventContextActive() && !Contexts.isApplicationContextActive()) {
            Lifecycle.beginCall();
        }
        return (SessionIdService) Component.getInstance(SessionIdService.class);
    }


    // #34 - update session attributes with each request
    // 1) redirect_uri change -> update session
    // 2) acr change -> send error
    // 3) client_id change -> do nothing
    // https://github.com/GluuFederation/oxAuth/issues/34
    public SessionId updateSessionIfNeeded(SessionId session, String redirectUri, String acrValuesStr) throws AcrChangedException {
        if (session != null && !session.getSessionAttributes().isEmpty()) {

            final Map<String, String> sessionAttributes = session.getSessionAttributes();

            boolean isAcrChanged = acrValuesStr !=null && !acrValuesStr.equals(sessionAttributes.get("acr_values"));
            if (isAcrChanged) {
                throw new AcrChangedException();
            }

            final Map<String, String> currentSessionAttributes = getCurrentSessionAttributes(sessionAttributes);
            if (!currentSessionAttributes.equals(sessionAttributes)) {
            	sessionAttributes.putAll(currentSessionAttributes);

            	// Reinit login
            	sessionAttributes.put("auth_step", "1");

            	for (Iterator<Entry<String, String>> it = currentSessionAttributes.entrySet().iterator(); it.hasNext();) {
            		Entry<String, String> currentSessionAttributesEntry = it.next();
    	        	String name = currentSessionAttributesEntry.getKey();
    	        	if (name.startsWith("auth_step_passed_")) {
    	        		it.remove();
    	        	}
				}

            	session.setSessionAttributes(currentSessionAttributes);

                boolean updateResult = updateSessionId(session, true, true);
                if (!updateResult) {
                    log.debug("Failed to update session entry: '{0}'", session.getId());
                }
            }
        }
        return session;
    }

    private Map<String, String> getCurrentSessionAttributes(Map<String, String> sessionAttributes) {
    	// Update from request
    	FacesContext facesContext = FacesContext.getCurrentInstance();
    	if (facesContext != null) {
        	// Clone before replacing new attributes
        	final Map<String, String> currentSessionAttributes = new HashMap<String, String>(sessionAttributes);

        	final ExternalContext externalContext = facesContext.getExternalContext();
	        Map<String, String> parameterMap = externalContext.getRequestParameterMap();
	        Map<String, String> newRequestParameterMap = authenticationService.getAllowedParameters(parameterMap);
	        for (Entry<String, String> newRequestParameterMapEntry : newRequestParameterMap.entrySet()) {
	        	String name = newRequestParameterMapEntry.getKey();
	        	if (!StringHelper.equalsIgnoreCase(name, "auth_step")) {
	        		currentSessionAttributes.put(name, newRequestParameterMapEntry.getValue());
	        	}
	        }

	        return currentSessionAttributes;
    	} else {
    		return sessionAttributes;
    	}
	}


	public String getSessionIdFromCookie(HttpServletRequest request) {
        try {
            final Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(SESSION_ID_COOKIE_NAME) /*&& cookie.getSecure()*/) {
                        log.trace("Found session_id cookie: '{0}'", cookie.getValue());
                        return cookie.getValue();
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public String getSessionIdFromCookie() {
		try {
		    final HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		    return getSessionIdFromCookie(request);
		} catch (Exception e) {
		    log.error(e.getMessage(), e);
		}

		return null;
	}

    public String getSessionIdFromOpbsCookie(HttpServletRequest request) {
        try {
            final Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("opbs") /*&& cookie.getSecure()*/) {
                        log.trace("Found session_id cookie: '{0}'", cookie.getValue());
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

    public SessionId getSessionId() {
    	String sessionId = getSessionIdFromCookie();

        if (StringHelper.isNotEmpty(sessionId)) {
            return getSessionId(sessionId);
        }
        
        return null;
    }

    public Map<String, String> getSessionAttributes(SessionId sessionId) {
    	if (sessionId != null) {
    		return sessionId.getSessionAttributes();
    	}
    	
		return null;
    }

    public SessionId generateAuthenticatedSessionId(String userDn) {
    	return generateAuthenticatedSessionId(userDn, "");
    }

    public SessionId generateAuthenticatedSessionId(String userDn, String prompt) {
    	Map<String, String> sessionIdAttributes = new HashMap<String, String>();
    	sessionIdAttributes.put("prompt", prompt);

    	return generateSessionId(userDn, new Date(), SessionIdState.AUTHENTICATED, sessionIdAttributes, true);
    }

    public SessionId generateAuthenticatedSessionId(String userDn, Map<String, String> sessionIdAttributes) {
    	return generateSessionId(userDn, new Date(), SessionIdState.AUTHENTICATED, sessionIdAttributes, true);
    }

	public SessionId generateSessionId(String p_userDn, Date authenticationDate, SessionIdState state, Map<String, String> sessionIdAttributes, boolean persist) {
            final String uuid = UUID.randomUUID().toString();
            final String dn = dn(uuid);

            if (StringUtils.isBlank(dn)) {
        		return null;
        	}

            if (SessionIdState.AUTHENTICATED == state) {
                if (StringUtils.isBlank(p_userDn)) {
            		return null;
            	}
            }

            final SessionId sessionId = new SessionId();
            sessionId.setId(uuid);
            sessionId.setDn(dn);

            if (StringUtils.isNotBlank(p_userDn)) {
            	sessionId.setUserDn(p_userDn);
            }

            if (authenticationDate != null) {
                sessionId.setAuthenticationTime(authenticationDate);
            }
            
        	if (state != null) {
            	sessionId.setState(state);
            }

        	configureOpbsCookie(sessionId);
        	
        	sessionId.setSessionAttributes(sessionIdAttributes);

            boolean persisted = false;
            if (persist) {
            	persisted = persistSessionId(sessionId);
            }

            log.trace("Generated new session, id = '{0}', state = '{1}', persisted = '{2}'", sessionId.getId(), sessionId.getState(), persisted);
            return sessionId;
    }

    public SessionId setSessionIdAuthenticated(SessionId sessionId, String p_userDn) {
       	sessionId.setUserDn(p_userDn);
        sessionId.setAuthenticationTime(new Date());
       	sessionId.setState(SessionIdState.AUTHENTICATED);

    	configureOpbsCookie(sessionId);

        boolean persisted = updateSessionId(sessionId, true, true);

        log.trace("Authenticated session, id = '{0}', state = '{1}', persisted = '{2}'", sessionId.getId(), sessionId.getState(), persisted);
        return sessionId;
}

	private void configureOpbsCookie(SessionId sessionId) {
		final int unusedLifetime = ConfigurationFactory.instance().getConfiguration().getSessionIdUnusedLifetime();
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            Cookie cookie = new Cookie("opbs", sessionId.getId());
            cookie.setMaxAge(unusedLifetime);
            ((HttpServletResponse) facesContext.getExternalContext().getResponse()).addCookie(cookie);
        }
	}

    public boolean persistSessionId(final SessionId sessionId) {
        return persistSessionId(sessionId, false);
    }

	public boolean persistSessionId(final SessionId sessionId, boolean forcePersistence) {
		List<Prompt> prompts = getPromptsFromSessionId(sessionId);

        try {
        	final int unusedLifetime = ConfigurationFactory.instance().getConfiguration().getSessionIdUnusedLifetime();
			if ((unusedLifetime > 0 && isPersisted(prompts)) || forcePersistence) {
	            sessionId.setLastUsedAt(new Date());

	            sessionId.setPersisted(true);
	            ldapEntryManager.persist(sessionId);
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
        return updateSessionId(sessionId, updateLastUsedAt, false);
    }

	public boolean updateSessionId(final SessionId sessionId, boolean updateLastUsedAt, boolean forceUpdate) {
		List<Prompt> prompts = getPromptsFromSessionId(sessionId);

		try {
        	final int unusedLifetime = ConfigurationFactory.instance().getConfiguration().getSessionIdUnusedLifetime();
			if ((unusedLifetime > 0 && isPersisted(prompts)) || forceUpdate) {
                if (updateLastUsedAt) {
	                sessionId.setLastUsedAt(new Date());
                }

	            sessionId.setPersisted(true);
	            ldapEntryManager.merge(sessionId);
			}
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

		return true;
	}

    private static boolean isPersisted(List<Prompt> prompts) {
        if (prompts != null && prompts.contains(Prompt.NONE)) {
            final Boolean persistOnPromptNone = ConfigurationFactory.instance().getConfiguration().getSessionIdPersistOnPromptNone();
            return persistOnPromptNone != null && persistOnPromptNone;
        }
        return true;
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
            log.trace(e.getMessage(), e);
        }
        return null;
    }

    public SessionId getSessionId(String sessionId) {
        if (StringHelper.isEmpty(sessionId)) {
            return null;
        }

        String dn = dn(sessionId);
        boolean contains = containsSessionId(dn);
    	if (!contains) {
    		return null;
    	}

        try {
        	final SessionId entity = getSessionByDN(dn);
            log.trace("Try to get session by id: {0} ...", sessionId);
            if (entity != null) {
                log.trace("Session dn: {0}", entity.getDn());

                if (isSessionValid(entity)) {
                    return entity;
                }
            }
        } catch (Exception ex) {
            log.trace(ex.getMessage(), ex);
        }

        log.trace("Failed to get session by id: {0}", sessionId);
        return null;
    }

    public boolean containsSessionId(String dn) {
        try {
            return ldapEntryManager.contains(SessionId.class, dn);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }

        return false;
    }

    private static String getBaseDn() {
        return ConfigurationFactory.instance().getBaseDn().getSessionId();
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

    public void remove(List<SessionId> list) {
        for (SessionId id : list) {
            remove(id);
        }
    }

    public void cleanUpSessions() {
        final int interval = ConfigurationFactory.instance().getConfiguration().getSessionIdUnusedLifetime();
        final int unauthenticatedInterval = ConfigurationFactory.instance().getConfiguration().getSessionIdUnauthenticatedUnusedLifetime();

        remove(getUnauthenticatedIdsOlderThan(unauthenticatedInterval));
        remove(getIdsOlderThan(interval));
    }

    public List<SessionId> getUnauthenticatedIdsOlderThan(int p_intervalInSeconds) {
        try {
            final long dateInPast = new Date().getTime() - TimeUnit.SECONDS.toMillis(p_intervalInSeconds);
            final Filter filter = Filter.create(String.format("&(lastModifiedTime<=%s)(oxState=unauthenticated)", StaticUtils.encodeGeneralizedTime(new Date(dateInPast))));
            return ldapEntryManager.findEntries(getBaseDn(), SessionId.class, filter);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
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

        final long sessionInterval = TimeUnit.SECONDS.toMillis(ConfigurationFactory.instance().getConfiguration().getSessionIdUnusedLifetime());
        final long sessionUnauthenticatedInterval = TimeUnit.SECONDS.toMillis(ConfigurationFactory.instance().getConfiguration().getSessionIdUnauthenticatedUnusedLifetime());

        final long timeSinceLastAccess = System.currentTimeMillis() - sessionId.getLastUsedAt().getTime();
        if (timeSinceLastAccess > sessionInterval && ConfigurationFactory.instance().getConfiguration().getSessionIdUnusedLifetime() != -1) {
            return false;
        }
        if (sessionId.getState() == SessionIdState.UNAUTHENTICATED && timeSinceLastAccess > sessionUnauthenticatedInterval && ConfigurationFactory.instance().getConfiguration().getSessionIdUnauthenticatedUnusedLifetime() != -1) {
            return false;
        }

        return true;
    }

	private List<Prompt> getPromptsFromSessionId(final SessionId sessionId) {
    	String promptParam = sessionId.getSessionAttributes().get("prompt");
		return Prompt.fromString(promptParam, " ");
	}

}
