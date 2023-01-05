/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.model.security.protect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@RequestScoped
@Named
public class AuthenticationAttemptList implements Serializable {

    private static final long serialVersionUID = -1841823297081861148L;

    private List<AuthenticationAttempt> authenticationAttempts = new ArrayList<AuthenticationAttempt>();

    public final List<AuthenticationAttempt> getAuthenticationAttempts() {
        return authenticationAttempts;
    }

    public final void setAuthenticationAttempts(List<AuthenticationAttempt> authenticationAttempts) {
        this.authenticationAttempts = authenticationAttempts;
    }

}
