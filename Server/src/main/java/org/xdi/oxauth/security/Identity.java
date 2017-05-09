package org.xdi.oxauth.security;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.session.SessionClient;

@RequestScoped
@Named
public class Identity extends org.xdi.model.security.Identity {

	private static final long serialVersionUID = 2751659008033189259L;

	private SessionState sessionState;

	private User user;
	private SessionClient sessionClient;

	public SessionState getSessionState() {
		return sessionState;
	}

	public void setSessionState(SessionState sessionState) {
		this.sessionState = sessionState;
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

	public SessionClient getSetSessionClient() {
		return sessionClient;
	}

	public void setSetSessionClient(SessionClient setSessionClient) {
		this.sessionClient = setSessionClient;
	}

}
