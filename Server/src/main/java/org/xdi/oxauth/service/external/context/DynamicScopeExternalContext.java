/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.external.context;

import java.util.ArrayList;
import java.util.List;

import org.oxauth.persistence.model.Scope;
import org.xdi.oxauth.model.common.IAuthorizationGrant;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.token.JsonWebResponse;

/**
 * Holds object required in dynamic scope custom scripts 
 * 
 * @author Yuriy Movchan  Date: 07/01/2015
 */

public class DynamicScopeExternalContext extends ExternalScriptContext {

	private List<Scope> dynamicScopes;
	private JsonWebResponse jsonWebResponse;
	private IAuthorizationGrant authorizationGrant;

    public DynamicScopeExternalContext(List<Scope> dynamicScopes, JsonWebResponse jsonWebResponse, IAuthorizationGrant authorizationGrant) {
    	super(null);

    	this.dynamicScopes = dynamicScopes;
    	this.jsonWebResponse = jsonWebResponse;
    	this.authorizationGrant = authorizationGrant;
    }

	/**
	 * This method is used by scripts.
	 * @return dynamic scopes as string
	 *
	 */
	public List<String> getDynamicScopes() {
		List<String> scopes = new ArrayList<String>();
		if (dynamicScopes != null) {
			for (Scope scope : dynamicScopes) {
				scopes.add(scope.getDisplayName());
			}
		}
		return scopes;
	}

	public List<Scope> getScopes() {
		return dynamicScopes;
	}

	public JsonWebResponse getJsonWebResponse() {
		return jsonWebResponse;
	}

	public IAuthorizationGrant getAuthorizationGrant() {
		return authorizationGrant;
	}

	public User getUser() {
		return authorizationGrant.getUser();
	}

}
