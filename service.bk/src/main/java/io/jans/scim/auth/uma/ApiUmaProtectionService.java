/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */
package io.jans.scim.auth.uma;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.gluu.config.oxtrust.AppConfiguration;
import org.gluu.oxauth.client.ClientInfoClient;
import org.gluu.oxauth.client.ClientInfoResponse;
import org.slf4j.Logger;

import io.jans.scim.service.OpenIdService;

/**
 * Provides service to protect APIs Rest service endpoints with UMA scope.
 * 
 * @author Dmitry Ognyannikov
 */
@ApplicationScoped
@Named("apiUmaProtectionService")
@BindingUrls({ "/api/v1" })
public class ApiUmaProtectionService extends BaseUmaProtectionService implements Serializable {

	private static final long serialVersionUID = 362749692619005003L;

	@Inject
	private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private OpenIdService openIdService;

	@Override
	protected String getClientId() {
		return appConfiguration.getApiUmaClientId();
	}

	@Override
	protected String getClientKeyStorePassword() {
		return appConfiguration.getApiUmaClientKeyStorePassword();
	}

	@Override
	protected String getClientKeyStoreFile() {
		return appConfiguration.getApiUmaClientKeyStoreFile();
	}

	@Override
	protected String getClientKeyId() {
		return appConfiguration.getApiUmaClientKeyId();
	}

	@Override
	public String getUmaResourceId() {
		return appConfiguration.getApiUmaResourceId();
	}

	@Override
	public String getUmaScope() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(appConfiguration.getApiUmaScopes()[0]);
		buffer.append(" ");
		buffer.append(appConfiguration.getApiUmaScopes()[1]);
		return buffer.toString();
	}

	@Override
	public boolean isEnabled() {
		return isEnabledUmaAuthentication();
	}

	@Override
	public Response processAuthorization(HttpHeaders headers, ResourceInfo resourceInfo) {
		Response authorizationResponse = null;
		String authorization = headers.getHeaderString("Authorization");
		log.info("==== API Service call intercepted ====");
		log.info("Authorization header {} found", StringUtils.isEmpty(authorization) ? "not" : "");
		try {
			if (appConfiguration.isOxTrustApiTestMode()) {
				log.info("API Test Mode is ACTIVE");
				authorizationResponse = processTestModeAuthorization(authorization);
			} else if (isEnabled()) {
				log.info("API is protected by UMA");
				authorizationResponse = processUmaAuthorization(authorization, resourceInfo);
			} else {
				log.info(
						"Please activate UMA or test mode to protect your API endpoints. Read the Gluu API docs to learn more");
				authorizationResponse = getErrorResponse(Response.Status.UNAUTHORIZED, "API not protected");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			authorizationResponse = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		return authorizationResponse;
	}

	private Response processTestModeAuthorization(String token) throws Exception {
		Response response = null;
		if (StringUtils.isNotEmpty(token)) {
			token = token.replaceFirst("Bearer\\s+", "");
			log.debug("Validating token {}", token);
			String clientInfoEndpoint = openIdService.getOpenIdConfiguration().getClientInfoEndpoint();
			ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
			ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(token);
			if ((clientInfoResponse.getStatus() != Response.Status.OK.getStatusCode()) || (clientInfoResponse.getErrorType() != null)) {
				response = getErrorResponse(Status.UNAUTHORIZED, "Invalid token " + token);
				log.debug("Error validating access token: {}", clientInfoResponse.getErrorDescription());
			}
		} else {
			log.info("Request is missing authorization header");
			response = getErrorResponse(Status.INTERNAL_SERVER_ERROR, "No authorization header found");
		}
		return response;
	}

}
