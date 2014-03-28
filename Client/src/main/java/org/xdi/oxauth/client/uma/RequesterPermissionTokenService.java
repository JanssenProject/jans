package org.xdi.oxauth.client.uma;

import org.xdi.oxauth.model.uma.RequesterPermissionTokenResponse;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

/**
 * The endpoint at which the requester asks the AM to issue an RPT relating to
 * this requesting party, host, and AM.
 * 
 * @author Yuriy Movchan Date: 10/16/2012
 */
public interface RequesterPermissionTokenService {

	@POST
    @Consumes({ UmaConstants.JSON_MEDIA_TYPE })
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	public RequesterPermissionTokenResponse getRequesterPermissionToken(@HeaderParam("Authorization") String authorization,
			@HeaderParam("Host") String host);

}