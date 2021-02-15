/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.auth;

import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.configapi.auth.util.AuthUtil;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.container.ResourceInfo;
import java.io.Serializable;
import java.util.List;

public abstract class AuthorizationService implements Serializable {

	private static final long serialVersionUID = 4012335221233316230L;

	@Inject
	Logger log;

	@Inject
	ConfigurationFactory configurationFactory;

	@Inject
	AuthUtil authUtil;

	public abstract void processAuthorization(String token, String issuer, String tokenType, ResourceInfo resourceInfo, String method,
			String path) throws Exception;

	protected Response getErrorResponse(Response.Status status, String detail) {
		return Response.status(status).entity(detail).build();
	}

	public List<String> getRequestedScopes(String path) {
		return authUtil.getRequestedScopes(path);
	}

	public List<String> getRequestedScopes(ResourceInfo resourceInfo) {
		return authUtil.getRequestedScopes(resourceInfo);
	}

	public boolean validateScope(List<String> authScopes, List<String> resourceScopes) {
		return authUtil.validateScope(authScopes, resourceScopes);
	}

	public List<String> getApiApprovedIssuer() {
		return this.configurationFactory.getApiApprovedIssuer();
	}

	public Jwt parse(String encodedJwt) throws InvalidJwtException {
		log.info("\n\n Jwt string to parse encodedJwt = " + encodedJwt);
		if (StringHelper.isNotEmpty(encodedJwt)) {
			return Jwt.parse(encodedJwt);
		}

		return null;
	}

	public boolean validIssuer(String issuer) throws Exception {
		log.info("\n\n Is Valid Issuer - this.configurationFactory.getApiApprovedIssuer().contains(issuer) = " + this.configurationFactory.getApiApprovedIssuer().contains(issuer));
		return this.configurationFactory.getApiApprovedIssuer().contains(issuer);
	}
}
