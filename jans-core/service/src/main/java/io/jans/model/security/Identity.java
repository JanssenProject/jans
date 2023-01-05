/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.security;

import java.io.Serializable;
import java.security.Principal;
import java.security.acl.Group;
import java.util.HashMap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.interceptor.Interceptor;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.slf4j.Logger;

import io.jans.model.security.event.Authenticated;

@RequestScoped
@Named
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 10)
public class Identity implements Serializable {

    private static final long serialVersionUID = 3751659008033189259L;

    public static final String EVENT_LOGIN_SUCCESSFUL = "org.jboss.seam.security.loginSuccessful";

    public static final String ROLES_GROUP = "Roles";

    @Inject
    private Logger log;

    @Inject
    private Credentials credentials;

    @Inject
    private Event<String> event;

    private Principal principal;

    private HashMap<String, Object> workingParameters;

    private Subject subject;

    @PostConstruct
    public void create() {
        this.subject = new Subject();
    }

    /**
     * Simple check that returns true if the user is logged in, without attempting
     * to authenticate
     *
     * @return true if the user is logged in
     */
    public boolean isLoggedIn() {
        // If there is a principal set, then the user is logged in.
        return getPrincipal() != null;
    }

    /**
     * Will attempt to authenticate quietly if the user's credentials are set and
     * they haven't authenticated already. A quiet authentication doesn't throw any
     * exceptions if authentication fails.
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
     * @return String returns "loggedIn" if user is authenticated, or null if not.
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
     * Attempts a quiet login, suppressing any login exceptions and not creating any
     * faces messages. This method is intended to be used primarily as an internal
     * API call, however has been made public for convenience.
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

    public Subject getSubject() {
        return subject;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * Resets all security state and credentials
     */
    public void unAuthenticate() {
        this.principal = null;
        this.credentials.clear();
        this.subject = new Subject();
    }

    public void logout() {
        if (isLoggedIn()) {
            unAuthenticate();
        }
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
        return getWorkingParameters().containsKey(name);
    }

    public Object getWorkingParameter(String name) {
        return getWorkingParameters().get(name);
    }

    public void setWorkingParameter(String name, Object value) {
        getWorkingParameters().put(name, value);
    }

    /**
     * Adds a role to the authenticated user.
     *
     * @param role
     *            The name of the role to add
     */
    public boolean addRole(String role) {
        if (role == null || "".equals(role)) {
            return false;
        }

        if (!isLoggedIn()) {
            return false;
        } else {
            for (Group sg : getSubject().getPrincipals(Group.class)) {
                if (ROLES_GROUP.equals(sg.getName())) {
                    return sg.addMember(new Role(role));
                }
            }

            SimpleGroup roleGroup = new SimpleGroup(ROLES_GROUP);
            roleGroup.addMember(new Role(role));
            getSubject().getPrincipals().add(roleGroup);
            return true;
        }
    }

    public boolean hasRole(String role) {
        tryLogin();

        for (Group sg : getSubject().getPrincipals(Group.class)) {
            if (ROLES_GROUP.equals(sg.getName())) {
                return sg.isMember(new Role(role));
            }
        }
        return false;
    }

}
