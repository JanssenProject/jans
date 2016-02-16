/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.xdi.oxauth.ws.rs.fido.u2f;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.exception.fido.u2f.DeviceCompromisedException;
import org.xdi.oxauth.exception.fido.u2f.InvalidKeyHandleDeviceException;
import org.xdi.oxauth.exception.fido.u2f.NoEligableDevicesException;
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.fido.u2f.AuthenticateRequestMessageLdap;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;
import org.xdi.oxauth.model.fido.u2f.U2fErrorResponseType;
import org.xdi.oxauth.model.fido.u2f.exception.BadInputException;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateRequestMessage;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateResponse;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateStatus;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.service.UserService;
import org.xdi.oxauth.service.fido.u2f.AuthenticationService;
import org.xdi.oxauth.service.fido.u2f.DeviceRegistrationService;
import org.xdi.oxauth.service.fido.u2f.UserSessionStateService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.StringHelper;

import com.wordnik.swagger.annotations.Api;

/**
 * The endpoint allows to start and finish U2F authentication process
 *
 * @author Yuriy Movchan Date: 05/14/2015
 */
@Path("/fido/u2f/authentication")
@Api(value = "/fido/u2f/registration", description = "The endpoint at which the application U2F device start registration process.")
@Name("u2fAuthenticationRestWebService")
public class U2fAuthenticationWS {

	@Logger
	private Log log;

	@In
	private ErrorResponseFactory errorResponseFactory;

	@In
	private UserService userService;

	@In
	private AuthenticationService u2fAuthenticationService;

	@In
	private DeviceRegistrationService deviceRegistrationService;

	@In
	private UserSessionStateService userSessionStateService;

	@GET
	@Produces({ "application/json" })
	public Response startAuthentication(@QueryParam("username") String userName, @QueryParam("keyhandle") String keyHandle, @QueryParam("application") String appId, @QueryParam("session_state") String sessionState) {
		try {
			log.debug("Startig authentication with username '{0}', keyhandle '{1}' for appId '{2}' and session_state '{3}'", userName, keyHandle, appId, sessionState);

			if (StringHelper.isEmpty(userName) && StringHelper.isEmpty(keyHandle)) {
				throw new BadInputException(String.format("The request should contains either username or keyhandle"));
			}

			boolean oneStep = StringHelper.isEmpty(userName);
			String foundUserName = userName;
			if (oneStep) {
				// Convert to non padding URL base64 string
				String keyHandleWithoutPading = Base64Util.base64urlencode(Base64Util.base64urldecode(keyHandle));

				// In one step we expects empty username and not empty keyhandle
				String userInum = u2fAuthenticationService.getUserInumByKeyHandle(appId, keyHandleWithoutPading);
				foundUserName = userService.getUserNameByInum(userInum);
			}

			AuthenticateRequestMessage authenticateRequestMessage = u2fAuthenticationService.buildAuthenticateRequestMessage(appId, foundUserName);
			u2fAuthenticationService.storeAuthenticationRequestMessage(authenticateRequestMessage, foundUserName, sessionState);

			// convert manually to avoid possible conflict between resteasy
			// providers, e.g. jettison, jackson
			final String entity = ServerUtil.asJson(authenticateRequestMessage);

			return Response.status(Response.Status.OK).entity(entity).cacheControl(ServerUtil.cacheControl(true)).build();
		} catch (Exception ex) {
			log.error("Exception happened", ex);
			if (ex instanceof WebApplicationException) {
				throw (WebApplicationException) ex;
			}

			if ((ex instanceof NoEligableDevicesException) || (ex instanceof InvalidKeyHandleDeviceException)) {
				throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
						.entity(errorResponseFactory.getErrorResponse(U2fErrorResponseType.NO_ELIGABLE_DEVICES)).build());
			}

			throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SERVER_ERROR)).build());
		}
	}

	@POST
	@Produces({ "application/json" })
	public Response finishAuthentication(@FormParam("username") String userName, @FormParam("keyhandle") String keyHandle, @FormParam("tokenResponse") String authenticateResponseString) {
		String sessionState = null;
		try {
			log.debug("Finishing authentication for username '{0}' with response '{1}'", userName, authenticateResponseString);

			AuthenticateResponse authenticateResponse = ServerUtil.jsonMapperWithWrapRoot().readValue(authenticateResponseString, AuthenticateResponse.class);

			String requestId = authenticateResponse.getRequestId();
			AuthenticateRequestMessageLdap authenticateRequestMessageLdap = u2fAuthenticationService.getAuthenticationRequestMessageByRequestId(requestId);
			if (authenticateRequestMessageLdap == null) {
				throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
						.entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SESSION_EXPIRED)).build());
			}
			sessionState = authenticateRequestMessageLdap.getSessionState();
			u2fAuthenticationService.removeAuthenticationRequestMessage(authenticateRequestMessageLdap);

			AuthenticateRequestMessage authenticateRequestMessage = authenticateRequestMessageLdap.getAuthenticateRequestMessage();

			boolean oneStep = StringHelper.isEmpty(userName);
			String foundUserName = authenticateRequestMessageLdap.getUserName();

			DeviceRegistration deviceRegistration = u2fAuthenticationService.finishAuthentication(authenticateRequestMessage, authenticateResponse, foundUserName);

			// If sessionState is not empty update session
			if (StringHelper.isNotEmpty(sessionState)) {
				log.debug("There is session state. Setting session state attributes");
				userSessionStateService.updateUserSessionStateOnFinishRequest(sessionState, foundUserName, deviceRegistration, false, oneStep);
			}

			AuthenticateStatus authenticationStatus = new AuthenticateStatus(Constants.RESULT_SUCCESS, requestId);

			// convert manually to avoid possible conflict between resteasy
			// providers, e.g. jettison, jackson
			final String entity = ServerUtil.asJson(authenticationStatus);

			return Response.status(Response.Status.OK).entity(entity).cacheControl(ServerUtil.cacheControl(true)).build();
		} catch (Exception ex) {
			log.error("Exception happened", ex);
			if (ex instanceof WebApplicationException) {
				throw (WebApplicationException) ex;
			}

			try {
				// If sessionState is not empty update session
				if (StringHelper.isNotEmpty(sessionState)) {
					log.debug("There is session state. Setting session state status to 'declined'");
					userSessionStateService.updateUserSessionStateOnError(sessionState);
				}
			} catch (Exception ex2) {
				log.error("Failed to update session state status", ex2);
			}

			if (ex instanceof BadInputException) {
				throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
						.entity(errorResponseFactory.getErrorResponse(U2fErrorResponseType.INVALID_REQUEST)).build());
			}

			if (ex instanceof DeviceCompromisedException) {
				DeviceRegistration deviceRegistration = ((DeviceCompromisedException) ex).getDeviceRegistration();
				try {
					deviceRegistrationService.disableUserDeviceRegistration(deviceRegistration);
				} catch (Exception ex2) {
					log.error("Failed to mark device '{0}' as compomised", ex2, deviceRegistration.getId());
				}
				throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
						.entity(errorResponseFactory.getErrorResponse(U2fErrorResponseType.DEVICE_COMPROMISED)).build());
			}

			throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SERVER_ERROR)).build());
		}
	}
}
