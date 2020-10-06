/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package io.jans.notify.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.jans.notify.model.NotificationResponse;
import io.jans.notify.model.RegisterDeviceResponse;

/**
 * Notification endpoint allows to register device and send notification
 * 
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
public interface NotifyClientService {

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/register")
	RegisterDeviceResponse registerDevice(@HeaderParam("Authorization") String authorization, @FormParam("token") String token,
			@FormParam("user_data") String userData);

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/notify")
	NotificationResponse sendNotification(@HeaderParam("Authorization") String authorization, @FormParam("enpoint") String endpoint,
			@FormParam("message") String message);

}
