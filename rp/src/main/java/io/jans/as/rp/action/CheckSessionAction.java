/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.rp.action;

import org.slf4j.Logger;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

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