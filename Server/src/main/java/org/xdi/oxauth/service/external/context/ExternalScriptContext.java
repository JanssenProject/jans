/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external.context;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.net.util.SubnetUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.ldap.model.CustomEntry;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.util.ServerUtil;

/**
 * Holds object required in custom scripts 
 * 
 * @author Yuriy Movchan  Date: 07/01/2015
 */

public class ExternalScriptContext {

    private static final Log log = Logging.getLog(ExternalScriptContext.class);

    private LdapEntryManager ldapEntryManager;
    protected HttpServletRequest httpRequest;

    public ExternalScriptContext(HttpServletRequest httpRequest) {
    	this.ldapEntryManager = ServerUtil.getLdapManager();
    	this.httpRequest = httpRequest;
    	
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

    public Log getLog() {
        return log;
    }

    public HttpServletRequest getHttpRequest() {
        return httpRequest;
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
		    log.error("Failed to find entry '{0}'", dn);
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
