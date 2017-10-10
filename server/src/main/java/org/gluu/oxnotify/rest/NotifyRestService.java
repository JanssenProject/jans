/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxnotify.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Yuriy Movchan
 * @version Septempber 15, 2017
 */
public interface NotifyRestService {

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/register")
	Response registerDevice(@HeaderParam("Authorization") String authorization, @FormParam("token") String token,
			@FormParam("user_data") String userData, @Context HttpServletRequest httpRequest);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/notify")
	Response sendNotification(@HeaderParam("Authorization") String authorization, @FormParam("enpoint") String endpoint,
			@FormParam("message") String message, @Context HttpServletRequest httpRequest);

}
