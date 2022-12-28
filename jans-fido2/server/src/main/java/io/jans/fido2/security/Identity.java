/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.security;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.session.SessionId;
import io.jans.fido2.model.session.SessionClient;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Named;
import jakarta.interceptor.Interceptor;

/**
 * @version August 9, 2017
 */
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 20)
@RequestScoped
@Named
public class Identity extends io.jans.model.security.Identity {

    private static final long serialVersionUID = 2751659008033189259L;

    private SessionId sessionId;

    private User user;
    private SessionClient sessionClient;

    public SessionId getSessionId() {
        return sessionId;
    }

    public void setSessionId(SessionId sessionId) {
        this.sessionId = sessionId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setSessionClient(SessionClient sessionClient) {
        this.sessionClient = sessionClient;
    }

    public SessionClient getSessionClient() {
        return sessionClient;
    }

}
