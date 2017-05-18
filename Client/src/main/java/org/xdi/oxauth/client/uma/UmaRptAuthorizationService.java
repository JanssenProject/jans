/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.uma;

import org.xdi.oxauth.model.uma.RptAuthorizationResponse;
import org.xdi.oxauth.model.uma.RptAuthorizationRequest;
import org.xdi.oxauth.model.uma.UmaConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

/**
 * The endpoint at which the requester asks for RPT authorization.
 * 
 * @author Yuriy Zabrovarnyy
 */
public interface UmaRptAuthorizationService {

	@POST
	@Consumes({ UmaConstants.JSON_MEDIA_TYPE })
	@Produces({ UmaConstants.JSON_MEDIA_TYPE })
	RptAuthorizationResponse requestRptAuthorization(@HeaderParam("Authorization") String authorization,
													 @HeaderParam("Host") String amHost,
													 RptAuthorizationRequest rptAuthorizationRequest);

}