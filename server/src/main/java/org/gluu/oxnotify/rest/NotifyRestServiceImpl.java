/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxnotify.rest;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.gluu.oxnotify.model.NotificationResponse;
import org.gluu.oxnotify.model.PushPlatform;
import org.gluu.oxnotify.model.RegisterDeviceResponse;
import org.gluu.oxnotify.model.sns.ClientData;
import org.gluu.oxnotify.service.NotifyService;
import org.slf4j.Logger;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

/**
 * @author Yuriy Movchan
 * @version Septempber 15, 2017
 */
@Path("/")
public class NotifyRestServiceImpl implements NotifyRestService {

	@Inject
	private Logger log;

	@Inject
	private NotifyService notifyService;

	@Override
	public Response registerDevice(String authorization, String token, String userData) {
		log.debug("Registering new user '{}' device with token '{}'", userData, token);

		boolean authorized = notifyService.processAuthorization(authorization);
		if (!authorized) {
			Response response = buildErrorResponse(Response.Status.BAD_REQUEST, "Failed to authorize client");
			return response;
		}

		ClientData clientData = notifyService.getClientData(authorization);
		if (!authorized) {
			Response response = buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to prepare client");
			return response;
		}

		RegisterDeviceResponse registerDeviceResponse = registerDeviceImpl(clientData, token, userData);
		if (registerDeviceResponse == null) {
			Response response = buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, "Failed to register device");
			return response;
		}

		Response.ResponseBuilder builder = Response.ok();
		disableCache(builder);
		return builder.entity(registerDeviceResponse).build();
	}

	public RegisterDeviceResponse registerDeviceImpl(ClientData clientData, String token, String userData) {
		try {

			log.debug("Preparing for new user '{}' device with token '{}' registration", userData, token);
			AmazonSNS snsClient = clientData.getSnsClient();

			// Create endpoint for mobile device
			CreatePlatformEndpointRequest platformEndpointRequest = new CreatePlatformEndpointRequest();
			platformEndpointRequest.setCustomUserData(userData);
			platformEndpointRequest.setToken(token);
			platformEndpointRequest.setPlatformApplicationArn(clientData.getPlatformApplicationArn());

			CreatePlatformEndpointResult platformEndpointResult = snsClient
					.createPlatformEndpoint(platformEndpointRequest);

			RegisterDeviceResponse registerDeviceResponse = new RegisterDeviceResponse(
					platformEndpointResult.getSdkResponseMetadata().getRequestId(),
					platformEndpointResult.getSdkHttpMetadata().getHttpStatusCode(),
					platformEndpointResult.getEndpointArn());

			log.info("Registered user '{}' device with status '{}'", registerDeviceResponse);

			return registerDeviceResponse;
		} catch (Exception ex) {
			log.error("Failed to register device", ex);
		}

		return null;
	}

	@Override
	public Response sendNotification(String authorization, String endpoint, String message) {
		log.debug("Sending notification '{}' to endpoint '{}'", message, endpoint);

		boolean authorized = notifyService.processAuthorization(authorization);
		if (!authorized) {
			Response response = buildErrorResponse(Response.Status.BAD_REQUEST, "Failed to authorize client");
			return response;
		}

		ClientData clientData = notifyService.getClientData(authorization);
		if (!authorized) {
			Response response = buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to prepare client");
			return response;
		}

		NotificationResponse notificationResponse = sendNotificationImpl(clientData, endpoint, message);
		if (notificationResponse == null) {
			Response response = buildErrorResponse(Response.Status.SERVICE_UNAVAILABLE, "Failed to send notification");
			return response;
		}

		Response.ResponseBuilder builder = Response.ok();
		disableCache(builder);

		return builder.entity(notificationResponse).build();
	}

	public NotificationResponse sendNotificationImpl(ClientData clientData, String endpoint, String message) {
		try {
			log.debug("Preparing to send to device endpoint '{}'", endpoint);
			AmazonSNS snsClient = clientData.getSnsClient();

			PushPlatform platform = clientData.getPlatform();
			Map<String, String> messageMap = new HashMap<String, String>();
			messageMap.put(platform.toString(), message);

			final ObjectMapper objectMapper = new ObjectMapper();
			String pushMessage = objectMapper.writeValueAsString(messageMap);

			PublishRequest publishRequest = new PublishRequest();
			publishRequest.setMessageStructure("json");
			publishRequest.setTargetArn(endpoint);
			publishRequest.setMessage(pushMessage);

			PublishResult publishResult = snsClient.publish(publishRequest);

			NotificationResponse notificationResponse = new NotificationResponse(
					publishResult.getSdkResponseMetadata().getRequestId(),
					publishResult.getSdkHttpMetadata().getHttpStatusCode(), publishResult.getMessageId());

			log.info("Send notification to device endpoint '{}' with status '{}'", endpoint, notificationResponse);

			return notificationResponse;
		} catch (Exception ex) {
			log.error("Failed to send notification", ex);
		}

		return null;
	}

	private Response buildErrorResponse(Response.Status status, String message, Object... params) {
		Response.ResponseBuilder builder = Response.status(status);
		disableCache(builder);

		String formattedMessage = String.format(message, params);
		builder.entity(formattedMessage);
		log.error(message);

		return builder.build();
	}

	private void disableCache(Response.ResponseBuilder builder) {
		CacheControl cacheControl = new CacheControl();
		cacheControl.setNoTransform(false);
		cacheControl.setNoStore(true);
		builder.cacheControl(cacheControl);
		builder.header("Pragma", "no-cache");
	}

}
