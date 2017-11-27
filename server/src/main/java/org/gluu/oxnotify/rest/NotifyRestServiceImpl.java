/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxnotify.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.gluu.oxnotify.model.NotificationResponse;
import org.gluu.oxnotify.model.PushPlatform;
import org.gluu.oxnotify.model.RegisterDeviceResponse;
import org.gluu.oxnotify.model.conf.ClientConfiguration;
import org.gluu.oxnotify.model.sns.ClientData;
import org.gluu.oxnotify.model.sns.CustomUserData;
import org.gluu.oxnotify.service.ApplicationService;
import org.gluu.oxnotify.service.NetworkService;
import org.gluu.oxnotify.service.NotifyService;
import org.slf4j.Logger;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
import com.amazonaws.services.sns.model.InvalidParameterException;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;

/**
 * @author Yuriy Movchan
 * @version Septempber 15, 2017
 */
@Path("/")
public class NotifyRestServiceImpl implements NotifyRestService {

	@Inject
	private Logger log;
	
	@Inject
	private ApplicationService applicationService;

	@Inject
	private NotifyService notifyService;

	@Inject
	private NetworkService networkService;

	@Override
	public Response registerDevice(String authorization, String token, String userData, HttpServletRequest httpRequest) {
		log.debug("Registering new user '{}' device with token '{}'", userData, token);

		ClientConfiguration clientConfiguration = notifyService.processAuthorization(authorization);
		if (clientConfiguration == null) {
			Response response = buildErrorResponse(Response.Status.UNAUTHORIZED, "Failed to authorize client");
			return response;
		}

		ClientData clientData = notifyService.getClientData(clientConfiguration);
		if (clientData == null) {
			Response response = buildErrorResponse(Response.Status.BAD_REQUEST, "Failed to find client");
			return response;
		}

		RegisterDeviceResponse registerDeviceResponse = registerDeviceImpl(clientConfiguration, clientData, token, userData, httpRequest);
		if (registerDeviceResponse == null) {
			Response response = buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to register device");
			return response;
		}

		Response.ResponseBuilder builder = Response.ok();
		disableCache(builder);
		return builder.entity(registerDeviceResponse).build();
	}

	private RegisterDeviceResponse registerDeviceImpl(ClientConfiguration clientConfiguration, ClientData clientData, String token, String userData, HttpServletRequest httpRequest) {
		try {
			log.debug("Preparing for new user '{}' device with token '{}' registration", userData, token);
			AmazonSNS snsClient = clientData.getSnsClient();
			
			// Build custom user data
			CustomUserData customUserData = new CustomUserData(clientConfiguration.getClientId(),
					networkService.getIpAddress(httpRequest), new Date(), Arrays.asList(userData)); 
			log.info("Prepared custom user data for device registration: '{}' ", customUserData);

			CreatePlatformEndpointResult platformEndpointResult;
			String endpointArn = null;
			String requestId = null;
			int statusCode = -1;
			try {
				// Create endpoint for mobile device
				CreatePlatformEndpointRequest platformEndpointRequest = new CreatePlatformEndpointRequest();
				platformEndpointRequest.setCustomUserData(applicationService.asJson(customUserData));
				platformEndpointRequest.setToken(token);
				platformEndpointRequest.setPlatformApplicationArn(clientData.getPlatformApplicationArn());
				platformEndpointResult = snsClient.createPlatformEndpoint(platformEndpointRequest);
				
				endpointArn = platformEndpointResult.getEndpointArn();
				requestId = platformEndpointResult.getSdkResponseMetadata().getRequestId();
				statusCode = platformEndpointResult.getSdkHttpMetadata().getHttpStatusCode();
			} catch (InvalidParameterException ipe) {
				String message = ipe.getErrorMessage();
				Pattern p = Pattern
	                    .compile(".*Endpoint (arn:aws:sns[^ ]+) already exists " +
	                            "with the same Token.*");
	            Matcher m = p.matcher(message);
	            if (m.matches()) {
	                // The endpoint already exists for this token, but with additional custom data that
	                // CreateEndpoint doesn't want to overwrite. Just use the existing endpoint.
	                endpointArn = m.group(1);
	                requestId = ipe.getRequestId();
	                statusCode = HttpStatus.SC_OK;

	                boolean result = updateCustomUserData(snsClient, endpointArn, customUserData);
	                if (!result) {
	                	throw ipe;
	                }
	            } else {
	                // Re-throw exception, the input is actually bad
	                throw ipe;
	            }
			}

			RegisterDeviceResponse registerDeviceResponse = new RegisterDeviceResponse(requestId, statusCode, endpointArn);

			log.info("Registered user '{}' device with status '{}'", userData, registerDeviceResponse);

			return registerDeviceResponse;
		} catch (Exception ex) {
			log.error("Failed to register device", ex);
		}

		return null;
	}

	private boolean updateCustomUserData(AmazonSNS snsClient, String endpoint, CustomUserData newCustomUserData) throws IOException {
		log.debug("Adding custom user data '{}' to endpoint '{}'", newCustomUserData, endpoint);

		// Load existing attributes
		GetEndpointAttributesRequest getEndpointAttributesRequest = new GetEndpointAttributesRequest()
				.withEndpointArn(endpoint);

		GetEndpointAttributesResult getEndpointAttributesResult = snsClient.getEndpointAttributes(getEndpointAttributesRequest);

		// Parse loaded custom user data
		CustomUserData customUserData;
		try {
			String customUserDataString = getEndpointAttributesResult.getAttributes().get("CustomUserData");
			customUserData = applicationService.jsonToObject(customUserDataString, CustomUserData.class);
		} catch (Exception ex) {
			log.warn("Failed to add new app data '{}'", newCustomUserData.getAppUserData());
			log.warn("Failed to convert JSON to object", ex);

			return false;
		}

		// Union existing app data with new app data
		List<String> newAppUserData = new ArrayList<String>();
		newAppUserData.addAll(customUserData.getAppUserData());
		newAppUserData.addAll(newCustomUserData.getAppUserData());
		customUserData.setAppUserData(newAppUserData);
		
		// Update modification date
		customUserData.setModificationDate(newCustomUserData.getCreationDate());

		// Update custom user data
		SetEndpointAttributesRequest setEndpointAttributesRequest = new SetEndpointAttributesRequest()
				.withEndpointArn(endpoint).addAttributesEntry("CustomUserData", applicationService.asJson(customUserData));

		snsClient.setEndpointAttributes(setEndpointAttributesRequest);

		log.info("Added custom user data '{}' to endpoint '{}'", newCustomUserData, endpoint);
		
		return true;
	}

	@Override
	public Response sendNotification(String authorization, String endpoint, String message, HttpServletRequest httpRequest) {
		log.debug("Sending notification '{}' to endpoint '{}'", message, endpoint);

		ClientConfiguration clientConfiguration = notifyService.processAuthorization(authorization);
		if (clientConfiguration == null) {
			Response response = buildErrorResponse(Response.Status.UNAUTHORIZED, "Failed to authorize client");
			return response;
		}

		ClientData clientData = notifyService.getClientData(clientConfiguration);
		if (clientData == null) {
			Response response = buildErrorResponse(Response.Status.BAD_REQUEST, "Failed to find client");
			return response;
		}

		NotificationResponse notificationResponse = sendNotificationImpl(clientConfiguration, clientData, endpoint, message, httpRequest);
		if (notificationResponse == null) {
			Response response = buildErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to send notification");
			return response;
		}

		Response.ResponseBuilder builder = Response.ok();
		disableCache(builder);

		return builder.entity(notificationResponse).build();
	}

	private NotificationResponse sendNotificationImpl(ClientConfiguration clientConfiguration, ClientData clientData, String endpoint, String message, HttpServletRequest httpRequest) {
		try {
			log.debug("Preparing to send to device endpoint '{}'", endpoint);
			AmazonSNS snsClient = clientData.getSnsClient();

			log.info("Prepared message to send from clienId '{}' with clientIp '{}'", clientConfiguration.getClientId(),
					networkService.getIpAddress(httpRequest));

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
