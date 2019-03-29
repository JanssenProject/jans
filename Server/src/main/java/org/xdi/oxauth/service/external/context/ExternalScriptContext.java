/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external.context;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.net.util.SubnetUtils;
import org.gluu.oxauth.model.util.Util;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.exception.EntryPersistenceException;
import org.gluu.persist.model.base.CustomEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.util.ServerUtil;

/**
 * Holds object required in custom scripts 
 * 
 * @author Yuriy Movchan  Date: 07/01/2015
 */

public class ExternalScriptContext {

    private static final Logger log = LoggerFactory.getLogger(ExternalScriptContext.class);

    private final PersistenceEntryManager ldapEntryManager;
    protected HttpServletRequest httpRequest;
    protected final HttpServletResponse httpResponse;

    public ExternalScriptContext(HttpServletRequest httpRequest) {
    	this(httpRequest, null);
    }

    public ExternalScriptContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    	this.ldapEntryManager = ServerUtil.getLdapManager();
    	this.httpRequest = httpRequest;
    	this.httpResponse = httpResponse;

    	if (this.httpRequest == null) {
    		FacesContext facesContext = FacesContext.getCurrentInstance();
		    if (facesContext != null) {
			    ExternalContext extCtx = facesContext.getExternalContext();
			    if (extCtx != null) {
			    	this.httpRequest = (HttpServletRequest) extCtx.getRequest();
			    }
		    }
    	}
    }

	public Logger getLog() {
        return log;
    }

    public PersistenceEntryManager getPersistenceEntryManager() {
		return ldapEntryManager;
	}

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }

	public HttpServletResponse getHttpResponse() {
		return httpResponse;
	}

	public String getIpAddress() {
        return httpRequest != null ? httpRequest.getRemoteAddr() : "";
    }

    public boolean isInNetwork(String cidrNotation) {
        final String ip = getIpAddress();
        if (Util.allNotBlank(ip, cidrNotation)) {
            final SubnetUtils utils = new SubnetUtils(cidrNotation);
            return utils.getInfo().isInRange(ip);
        }
        return false;
    }

    protected CustomEntry getEntryByDn(String dn, String ... ldapReturnAttributes) {
		try {
	    	return ldapEntryManager.find(CustomEntry.class, dn, ldapReturnAttributes);
		} catch (EntryPersistenceException epe) {
		    log.error("Failed to find entry '{}'", dn);
		}

		return null;
    }

    protected String getEntryAttributeValue(String dn, String attributeName) {
        final CustomEntry entry = getEntryByDn(dn, attributeName);
        if (entry != null) {
            final String attributeValue = entry.getCustomAttributeValue(attributeName);
            return attributeValue;
        }

        return "";
    }

}
