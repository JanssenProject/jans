/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
