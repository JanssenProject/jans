/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external.context;

import java.util.List;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.net.util.SubnetUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.gluu.site.ldap.persistence.exception.EntryPersistenceException;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.xdi.ldap.model.CustomEntry;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.token.JsonWebResponse;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.util.ServerUtil;

/**
 * Holds object required in dynamic scope custom scripts 
 * 
 * @author Yuriy Movchan  Date: 07/01/2015
 */

public class DynamicScopeExternalContext extends ExternalScriptContext {

	private List<String> dynamicScopes;
	private JsonWebResponse jsonWebResponse;
	private User user;

    public DynamicScopeExternalContext(List<String> dynamicScopes, JsonWebResponse jsonWebResponse, User user) {
    	super(null);

    	this.dynamicScopes = dynamicScopes;
    	this.jsonWebResponse = jsonWebResponse;
    	this.user = user;
    }

	public List<String> getDynamicScopes() {
		return dynamicScopes;
	}

	public void setDynamicScopes(List<String> dynamicScopes) {
		this.dynamicScopes = dynamicScopes;
	}

	public JsonWebResponse getJsonWebResponse() {
		return jsonWebResponse;
	}

	public void setJsonWebResponse(JsonWebResponse jsonWebResponse) {
		this.jsonWebResponse = jsonWebResponse;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

}
