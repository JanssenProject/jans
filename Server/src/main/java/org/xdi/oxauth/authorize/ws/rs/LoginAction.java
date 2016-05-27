package org.xdi.oxauth.authorize.ws.rs;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

/**
 * @author Javier Rojas Blum
 * @version May 24, 2016
 */
@Name("loginAction")
@Scope(ScopeType.EVENT) // Do not change scope, we try to keep server without http sessions
public class LoginAction {

    @Logger
    private Log log;

    private String loginHint;

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }
}
