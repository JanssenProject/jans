/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.action;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

/**
 * @author Javier Rojas Blum Date: 02.22.2012
 */
@Named
@SessionScoped
public class CheckSessionAction implements Serializable {

	private static final long serialVersionUID = 6513115606594205672L;

	@Inject
    private Logger log;

    private String checkSessionEndpoint;

    public String getCheckSessionEndpoint() {
        return checkSessionEndpoint;
    }

    public void setCheckSessionEndpoint(String checkSessionEndpoint) {
        this.checkSessionEndpoint = checkSessionEndpoint;
    }
}