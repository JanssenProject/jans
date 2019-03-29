package org.gluu.model.security.protect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

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
