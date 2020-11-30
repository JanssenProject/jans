/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.security;

import io.jans.as.common.model.common.User;
import io.jans.as.server.model.common.SessionId;
import io.jans.as.server.model.session.SessionClient;

import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Named;
import javax.interceptor.Interceptor;

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
