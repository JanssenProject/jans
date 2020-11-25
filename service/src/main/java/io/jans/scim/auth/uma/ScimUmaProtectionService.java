/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.scim.auth.uma;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import io.jans.as.client.ClientInfoClient;
import io.jans.as.client.ClientInfoResponse;
import io.jans.scim.model.conf.AppConfiguration;
import io.jans.scim.service.ConfigurationService;
import io.jans.scim.service.OpenIdService;

/**
 * Provides service to protect SCIM UMA Rest service endpoints
 * 
 * @author Yuriy Movchan Date: 12/06/2016
 */
@ApplicationScoped
@Named("scimUmaProtectionService")
@BindingUrls({"/scim"})
public class ScimUmaProtectionService extends BaseUmaProtectionService implements Serializable {

	private static final long serialVersionUID = -5447131971095468865L;

    @Inject
    private Logger log;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private ConfigurationService configurationService;

    @Inject
    private OpenIdService openIdService;

	protected String getClientId() {
		return appConfiguration.getScimUmaClientId();
	}

	protected String getClientKeyStorePassword() {
		return appConfiguration.getScimUmaClientKeyStorePassword();
	}

	protected String getClientKeyStoreFile() {
		return appConfiguration.getScimUmaClientKeyStoreFile();
	}

	protected String getClientKeyId() {
		return appConfiguration.getScimUmaClientKeyId();
	}

	public String getUmaResourceId() {
		return appConfiguration.getScimUmaResourceId();
	}

	public String getUmaScope() {
		return appConfiguration.getScimUmaScope();
	}

	public boolean isEnabled() {
		return isScimEnabled() && isEnabledUmaAuthentication();
	}

	private boolean isScimEnabled() {
		return configurationService.getConfiguration() .isScimEnabled();
	}

    /**
     * This method checks whether the authorization header is present and valid before scim service methods can be actually
     * called.
     * @param headers An object holding HTTP headers
     * @param resourceInfo An object that allows access to request URI information
     * @return A null value if the authorization was successful, otherwise a Response object is returned signaling an
     * authorization error
     */
	public Response processAuthorization(HttpHeaders headers, ResourceInfo resourceInfo){

        //Comment this method body if you want to skip the authorization check and proceed straight to use your SCIM service.
        //This is useful under certain circumstances while doing development
        //log.warn("Bypassing protection TEMPORARILY");

        Response authorizationResponse = null;
        String authorization = headers.getHeaderString("Authorization");
        log.info("==== SCIM Service call intercepted ====");
        log.info("Authorization header {} found", StringUtils.isEmpty(authorization) ? "not" : "");

        try {
            //Test mode may be removed in upcoming versions of Gluu Server...
            if (appConfiguration.isScimTestMode()) {
                log.info("SCIM Test Mode is ACTIVE");
                authorizationResponse = processTestModeAuthorization(authorization);
            }
            else
            if (isEnabled()){
                log.info("SCIM is protected by UMA");
                authorizationResponse = processUmaAuthorization(authorization, resourceInfo);
            }
            else{
                log.info("Please activate UMA or test mode to protect your SCIM endpoints. Read the Gluu SCIM docs to learn more");
                authorizationResponse= getErrorResponse(Response.Status.UNAUTHORIZED, "SCIM API not protected");
            }
        }
        catch (Exception e){
            log.error(e.getMessage(), e);
            authorizationResponse=getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        return authorizationResponse;

    }

    private Response processTestModeAuthorization(String token) throws Exception {

        Response response = null;

        if (StringUtils.isNotEmpty(token)) {
            token=token.replaceFirst("Bearer\\s+","");
            log.debug("Validating token {}", token);

            String clientInfoEndpoint=openIdService.getOpenIdConfiguration().getClientInfoEndpoint();
            ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
            ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(token);

            if ((clientInfoResponse.getStatus() != Response.Status.OK.getStatusCode()) || (clientInfoResponse.getErrorType()!=null)) {
                response=getErrorResponse(Response.Status.UNAUTHORIZED, "Invalid token "+ token);
                log.debug("Error validating access token: {}", clientInfoResponse.getErrorDescription());
            }
        }
        else{
            log.info("Request is missing authorization header");
            //see section 3.12 RFC 7644
            response = getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "No authorization header found");
        }
        return response;

    }

}
