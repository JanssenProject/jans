package org.xdi.oxauth.client.uma;

import org.jboss.resteasy.client.ClientResponse;
import org.xdi.oxauth.model.uma.AuthorizationResponse;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

/**
 * The endpoint at which the requester asks for authorizationto have a new permission.
 * 
 * @author Yuriy Movchan Date: 10/25/2012
 */
public interface AuthorizationRequestService {

	@POST
	@Consumes({ UmaConstants.JSON_MEDIA_TYPE })
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public ClientResponse<AuthorizationResponse> requestRptPermissionAuthorization(@HeaderParam("Authorization") String authorization,
			@HeaderParam("Host") String amHost, RptAuthorizationRequest rptAuthorizationRequest);

}