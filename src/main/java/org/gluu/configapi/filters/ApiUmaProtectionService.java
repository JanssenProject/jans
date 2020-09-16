/**
 * 
 */
package org.gluu.configapi.filters;

import io.quarkus.arc.AlternativePriority;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.io.Serializable;

/**
 * @author Mougang T.Gasmyr
 *
 */
@ApplicationScoped
@Named("apiUmaProtectionService")
@AlternativePriority(value = 1)
public class ApiUmaProtectionService /*extends BaseUmaProtectionService*/ implements Serializable {
/*

	private static final long serialVersionUID = -6553095758559902245L;

	@Inject
	private Logger logger;

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

	@SuppressWarnings("resource")
	@Override
	public Response processAuthorization(HttpHeaders headers, ResourceInfo resourceInfo) {
		Response authorizationResponse = null;
		String authorization = headers.getHeaderString("Authorization");
		logger.info("Authorization header {} found", StringUtils.isEmpty(authorization) ? "not" : "");
		try {
			if (appConfiguration.isOxTrustApiTestMode()) {
				logger.info("================OXAUTH-CONFIG-API Test Mode is ACTIVE");
				authorizationResponse = processTestModeAuthorization(authorization);
			} else if (isEnabled()) {
				logger.info("================OXAUTH-CONFIG-API is protected by UMA");
				//authorizationResponse = processUmaAuthorization(authorization, resourceInfo);
			} else {
				logger.info(
						"Please activate UMA or test mode to protect your API endpoints. Read the Gluu API docs to learn more");
				authorizationResponse = getErrorResponse(Response.Status.UNAUTHORIZED, "API not protected");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			authorizationResponse = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		return authorizationResponse;
	}

	private Response processTestModeAuthorization(String token) throws Exception {
		Response response = null;
		if (StringUtils.isNotEmpty(token)) {
			token = token.replaceFirst("Bearer\\s+", "");
			logger.debug("Validating token {}", token);
			String clientInfoEndpoint = openIdService.getOpenIdConfiguration().getClientInfoEndpoint();
			ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
			ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(token);
			if ((clientInfoResponse.getStatus() != Response.Status.OK.getStatusCode())
					|| (clientInfoResponse.getErrorType() != null)) {
				response = getErrorResponse(Status.UNAUTHORIZED, "Invalid token " + token);
				logger.debug("Error validating access token: {}", clientInfoResponse.getErrorDescription());
			}
		} else {
			logger.info("Request is missing authorization header");
			response = getErrorResponse(Status.INTERNAL_SERVER_ERROR, "No authorization header found");
		}
		return response;
	}
*/
}
