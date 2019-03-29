/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.gluu.oxauth.service.external.context;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gluu.jsf2.service.FacesService;
import org.gluu.model.SimpleCustomProperty;
import org.gluu.oxauth.authorize.ws.rs.ConsentGatheringSessionService;
import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.common.User;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.service.UserService;

/**
 * @author Yuriy Movchan Date: 10/30/2017
 */
public class ConsentGatheringContext extends ExternalScriptContext {

    private final ConsentGatheringSessionService sessionService;
    private final UserService userService;
    private final FacesService facesService;
    private final AppConfiguration appConfiguration;

    private final Map<String, SimpleCustomProperty> configurationAttributes;
    private final SessionId session;
    private final Map<String, String> pageAttributes;

    public ConsentGatheringContext(Map<String, SimpleCustomProperty> configurationAttributes, HttpServletRequest httpRequest, HttpServletResponse httpResponse, SessionId session,
                            Map<String, String> pageAttributes,
                            ConsentGatheringSessionService sessionService, UserService userService, FacesService facesService, AppConfiguration appConfiguration) {
        super(httpRequest, httpResponse);
        this.configurationAttributes = configurationAttributes;
        this.session = session;
        this.pageAttributes = pageAttributes;
        this.sessionService = sessionService;
        this.userService = userService;
        this.facesService = facesService;
        this.appConfiguration = appConfiguration;
    }

    public Map<String, SimpleCustomProperty> getConfigurationAttributes() {
        return configurationAttributes;
    }

    public User getUser(String... returnAttributes) {
        return sessionService.getUser(httpRequest, returnAttributes);
    }

    public String getUserDn() {
        return sessionService.getUserDn(httpRequest);
    }

    public Client getClient() {
        return sessionService.getClient(session);
    }

    public Map<String, String> getConnectSessionAttributes() {
        SessionId connectSession = sessionService.getConnectSession(httpRequest);
        if (connectSession != null) {
            return new HashMap<String, String>(connectSession.getSessionAttributes());
        }
        return new HashMap<String, String>();
    }

    public boolean isAuthenticated() {
        return getUser() != null;
    }

    public Map<String, String> getPageAttributes() {
        return pageAttributes;
    }

    public Map<String, String[]> getRequestParameters() {
        return httpRequest.getParameterMap();
    }

    public int getStep() {
        return sessionService.getStep(session);
    }

    public void setStep(int step) {
        sessionService.setStep(step, session);
    }

    public void addSessionAttribute(String key, String value) {
        session.getSessionAttributes().put(key, value);
    }

    public void removeSessionAttribute(String key) {
        session.getSessionAttributes().remove(key);
    }

    public Map<String, String> getSessionAttributes() {
        return session.getSessionAttributes();
    }

    /**
     * Must not take any parameters
     */
    public void persist() {
    	session.getSessionAttributes().putAll(this.pageAttributes);

    	sessionService.persist(session);
    }

	public UserService getUserService() {
		return userService;
	}

	public FacesService getFacesService() {
		return facesService;
	}

	public AppConfiguration getAppConfiguration() {
		return appConfiguration;
	}

}
