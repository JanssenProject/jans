package org.xdi.oxauth.authorize.ws.rs;

import javax.enterprise.context.ApplicationScoped;
import org.apache.log4j.Logger;
import javax.inject.Named;
import org.jboss.seam.annotations.Scope;


/**
 * @author Javier Rojas Blum
 * @version May 24, 2016
 */
@Named("loginAction")
@RequestScoped // Do not change scope, we try to keep server without http sessions
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
