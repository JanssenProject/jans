/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.action;

import java.io.Serializable;

import javax.faces.bean.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;

/**
 * @author Javier Rojas Blum Date: 02.22.2012
 */
@Named("checkSessionAction")
@SessionScoped
public class CheckSessionAction implements Serializable {

	private static final long serialVersionUID = 6513115606594205672L;

	@Inject
    private transient Logger log;

    private String checkSessionEndpoint;

    public String getCheckSessionEndpoint() {
        return checkSessionEndpoint;
    }

    public void setCheckSessionEndpoint(String checkSessionEndpoint) {
        this.checkSessionEndpoint = checkSessionEndpoint;
    }
}