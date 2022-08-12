/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.authorize.ws.rs.ConsentGatheringSessionService;
import io.jans.as.common.model.session.SessionId;
import io.jans.jsf2.service.FacesService;
import io.jans.model.SimpleCustomProperty;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

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
