/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.action;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;

/**
 * @author Javier Rojas Blum Date: 02.22.2012
 */
@Name("checkSessionAction")
@Scope(ScopeType.SESSION)
@AutoCreate
public class CheckSessionAction {

    @Logger
    private Log log;

    private String checkSessionEndpoint;

    public String getCheckSessionEndpoint() {
        return checkSessionEndpoint;
    }

    public void setCheckSessionEndpoint(String checkSessionEndpoint) {
        this.checkSessionEndpoint = checkSessionEndpoint;
    }
}