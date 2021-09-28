/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.external.context;

import java.util.ArrayList;
import java.util.List;

import io.jans.as.common.model.common.User;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.model.common.IAuthorizationGrant;

/**
 * Holds object required in dynamic scope custom scripts 
 * 
 * @author Yuriy Movchan  Date: 07/01/2015
 */

public class DynamicScopeExternalContext extends ExternalScriptContext {

	private final List<Scope> dynamicScopes;
	private final JsonWebResponse jsonWebResponse;
	private final IAuthorizationGrant authorizationGrant;

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
				scopes.add(scope.getId());
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
