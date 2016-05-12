/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.xdi.oxauth.ws.rs.fido.u2f;

import com.wordnik.swagger.annotations.Api;
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
import org.xdi.oxauth.model.fido.u2f.DeviceRegistrationResult;
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
import org.xdi.oxauth.service.fido.u2f.ValidationService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.StringHelper;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

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

	@In
	private ValidationService u2fValidationService;

	@GET
	@Produces({ "application/json" })
	public Response startAuthentication(@QueryParam("username") String userName, @QueryParam("keyhandle") String keyHandle, @QueryParam("application") String appId, @QueryParam("session_state") String sessionState) {
		// Parameter username is deprecated. We uses it only to determine is it's one or two step workflow
		try {
			log.debug("Startig authentication with username '{0}', keyhandle '{1}' for appId '{2}' and session_state '{3}'", userName, keyHandle, appId, sessionState);

			if (StringHelper.isEmpty(userName) && StringHelper.isEmpty(keyHandle)) {
				throw new BadInputException(String.format("The request should contains either username or keyhandle"));
			}

			String foundUserInum = null;

			boolean twoStep = StringHelper.isNotEmpty(userName);
			if (twoStep) {
				boolean valid = u2fValidationService.isValidSessionState(userName, sessionState);
				if (!valid) {
					throw new BadInputException(String.format("session_state '%s' is invalid", sessionState));
				}

				foundUserInum = userService.getUserInum(userName);
			} else {
				// Convert to non padding URL base64 string
				String keyHandleWithoutPading = Base64Util.base64urlencode(Base64Util.base64urldecode(keyHandle));

				// In one step we expects empty username and not empty keyhandle
				foundUserInum = u2fAuthenticationService.getUserInumByKeyHandle(appId, keyHandleWithoutPading);
			}

			if (StringHelper.isEmpty(foundUserInum)) {
				throw new BadInputException(String.format("Failed to find user by userName '%s' or keyHandle '%s' in LDAP", userName, keyHandle));
			}

			AuthenticateRequestMessage authenticateRequestMessage = u2fAuthenticationService.buildAuthenticateRequestMessage(appId, foundUserInum);
			u2fAuthenticationService.storeAuthenticationRequestMessage(authenticateRequestMessage, foundUserInum, sessionState);

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
	public Response finishAuthentication(@FormParam("username") String userName, @FormParam("tokenResponse") String authenticateResponseString) {
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

			String foundUserInum = authenticateRequestMessageLdap.getUserInum();
			DeviceRegistrationResult deviceRegistrationResult = u2fAuthenticationService.finishAuthentication(authenticateRequestMessage, authenticateResponse, foundUserInum);

			// If sessionState is not empty update session
			if (StringHelper.isNotEmpty(sessionState)) {
				log.debug("There is session state. Setting session state attributes");

				boolean oneStep = StringHelper.isEmpty(userName);
				userSessionStateService.updateUserSessionStateOnFinishRequest(sessionState, foundUserInum, deviceRegistrationResult, false, oneStep);
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
