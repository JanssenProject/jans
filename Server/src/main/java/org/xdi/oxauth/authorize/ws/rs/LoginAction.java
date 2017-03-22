package org.xdi.oxauth.authorize.ws.rs;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;


/**
 * @author Javier Rojas Blum
 * @version May 24, 2016
 */
@RequestScoped
@Named
public class LoginAction {

    @Inject
    private Logger log;

    private String loginHint;

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

}
