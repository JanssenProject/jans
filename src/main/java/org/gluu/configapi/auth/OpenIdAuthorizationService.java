package org.gluu.configapi.auth;


import org.gluu.oxauth.client.ClientInfoClient;
import org.gluu.oxauth.client.ClientInfoResponse;
import org.gluu.configapi.service.OpenIdService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.Serializable;

import org.slf4j.Logger;

import org.apache.commons.lang.StringUtils;

@ApplicationScoped
@Named("openIdAuthorizationService")
public class OpenIdAuthorizationService extends AuthorizationService implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    Logger logger;
    
    @Inject
    OpenIdService openIdService;    


    public Response processAuthorization(String token, ResourceInfo resourceInfo) throws Exception {
        logger.info(" OpenIdAuthorizationService::processAuthorization() - token  = " + token
                + " , resourceInfo = " + resourceInfo+" , openIdService = "+openIdService);
        Response response = null;
        if (StringUtils.isNotEmpty(token)) {
            token = token.replaceFirst("Bearer\\s+", "");
            logger.debug("Validating token {}", token);
            String clientInfoEndpoint = openIdService.getOpenIdConfiguration().getClientInfoEndpoint();
            ClientInfoClient clientInfoClient = new ClientInfoClient(clientInfoEndpoint);
            ClientInfoResponse clientInfoResponse = clientInfoClient.execClientInfo(token);
            if ((clientInfoResponse.getStatus() != Response.Status.OK.getStatusCode()) || (clientInfoResponse.getErrorType() != null)) {
                response = getErrorResponse(Status.UNAUTHORIZED, "Invalid token " + token);
                logger.debug("Error validating access token: {}", clientInfoResponse.getErrorDescription());
            }
        } else {
            logger.info("Request is missing authorization header");
            response = getErrorResponse(Status.INTERNAL_SERVER_ERROR, "No authorization header found");
        }
        return response;
    }
     

}