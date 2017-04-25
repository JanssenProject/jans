package org.xdi.oxauth.security;

import org.slf4j.Logger;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.session.SessionClient;
import org.xdi.oxauth.security.event.Authenticated;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.login.LoginException;
import java.io.Serializable;
import java.security.Principal;
import java.util.HashMap;

@RequestScoped
@Named
public class Identity implements Serializable {

	private static final long serialVersionUID = 3751659008033189259L;

	public static final String EVENT_LOGIN_SUCCESSFUL = "org.jboss.seam.security.loginSuccessful";

	@Inject
	private Logger log;

	@Inject
	private Credentials credentials;

	@Inject
	private Event<String> event;

	private Principal principal;

	private SessionState sessionState;

	private User user;
	private SessionClient sessionClient;

	private HashMap<String, Object> workingParameters;

	/**
	 * Simple check that returns true if the user is logged in, without
	 * attempting to authenticate
	 * 
	 * @return true if the user is logged in
	 */
	public boolean isLoggedIn() {
		// If there is a principal set, then the user is logged in.
		return getPrincipal() != null;
	}

	/**
	 * Will attempt to authenticate quietly if the user's credentials are set
	 * and they haven't authenticated already. A quiet authentication doesn't
	 * throw any exceptions if authentication fails.
	 * 
	 * @return true if the user is logged in, false otherwise
	 */
	public boolean tryLogin() {
		if ((getPrincipal() == null) && credentials.isSet()) {
			quietLogin();
		}

		return isLoggedIn();
	}

	/**
	 * Attempts to authenticate the user.
	 * 
	 * @return String returns "loggedIn" if user is authenticated, or null if
	 *         not.
	 */
	public String login() {
		try {
			if (isLoggedIn()) {
				return "loggedIn";
			}

			authenticate();

			if (!isLoggedIn()) {
				throw new LoginException();
			}

			if (log.isDebugEnabled()) {
				log.debug("Login successful for: " + credentials.getUsername());
			}

			event.select(Authenticated.Literal.INSTANCE).fire(EVENT_LOGIN_SUCCESSFUL);
			return "loggedIn";
		} catch (LoginException ex) {
			credentials.invalidate();

			if (log.isDebugEnabled()) {
				log.debug("Login failed for: " + credentials.getUsername(), ex);
			}
		}

		return null;
	}

	/**
	 * Attempts a quiet login, suppressing any login exceptions and not creating
	 * any faces messages. This method is intended to be used primarily as an
	 * internal API call, however has been made public for convenience.
	 */
	public void quietLogin() {
		try {
			if (!isLoggedIn()) {
				if (credentials.isSet()) {
					authenticate();
				}
			}
		} catch (LoginException ex) {
			credentials.invalidate();
		}
	}

	public synchronized void authenticate() throws LoginException {
		// If we're already authenticated, then don't authenticate again
		if (!isLoggedIn() && !credentials.isInvalid()) {
			principal = new SimplePrincipal(credentials.getUsername());
			credentials.setPassword(null);
		}
	}

	public void acceptExternallyAuthenticatedPrincipal(Principal principal) {
		this.principal = principal;
	}

	public Principal getPrincipal() {
		return principal;
	}

	public Credentials getCredentials() {
		return credentials;
	}

	/**
	 * Resets all security state and credentials
	 */
	public void unAuthenticate() {
		principal = null;
		credentials.clear();
	}

	public void logout() {
		if (isLoggedIn()) {
			unAuthenticate();
		}
	}

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

	private synchronized void initWorkingParamaters() {
		if (this.workingParameters == null) {
			this.workingParameters = new HashMap<String, Object>();
		}
	}

	public HashMap<String, Object> getWorkingParameters() {
		initWorkingParamaters();
		return workingParameters;
	}

	public boolean isSetWorkingParameter(String name) {
		return this.workingParameters.containsKey(name);
	}

	public Object getWorkingParameter(String name) {
		return this.workingParameters.get(name);
	}

	public void setWorkingParameter(String name, Object value) {
		this.workingParameters.put(name, value);
	}

}
