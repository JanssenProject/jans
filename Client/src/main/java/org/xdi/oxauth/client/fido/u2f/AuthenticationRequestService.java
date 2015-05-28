/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.fido.u2f;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateRequestMessage;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateStatus;

/**
 * The endpoint allows to start and finish U2F authentication process
 * 
 * @author Yuriy Movchan Date: 05/27/2015
 */
public interface AuthenticationRequestService {

	@GET
	@Produces({ "application/json" })
	public AuthenticateRequestMessage startAuthentication(@QueryParam("username") String userName, @QueryParam("application") String appId);

	@POST
	@Produces({ "application/json" })
	public AuthenticateStatus finishAuthentication(@FormParam("username") String userName, @FormParam("tokenResponse") String authenticateResponseString);

}