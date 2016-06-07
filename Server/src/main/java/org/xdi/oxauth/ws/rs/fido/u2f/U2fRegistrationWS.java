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
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistrationResult;
import org.xdi.oxauth.model.fido.u2f.RegisterRequestMessageLdap;
import org.xdi.oxauth.model.fido.u2f.U2fConstants;
import org.xdi.oxauth.model.fido.u2f.U2fErrorResponseType;
import org.xdi.oxauth.model.fido.u2f.exception.BadInputException;
import org.xdi.oxauth.model.fido.u2f.protocol.RegisterRequestMessage;
import org.xdi.oxauth.model.fido.u2f.protocol.RegisterResponse;
import org.xdi.oxauth.model.fido.u2f.protocol.RegisterStatus;
import org.xdi.oxauth.service.SessionStateService;
import org.xdi.oxauth.service.UserService;
import org.xdi.oxauth.service.fido.u2f.DeviceRegistrationService;
import org.xdi.oxauth.service.fido.u2f.RegistrationService;
import org.xdi.oxauth.service.fido.u2f.UserSessionStateService;
import org.xdi.oxauth.service.fido.u2f.ValidationService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.StringHelper;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * The endpoint allows to start and finish U2F registration process
 *
 * @author Yuriy Movchan Date: 05/14/2015
 */
@Path("/fido/u2f/registration")
@Api(value = "/fido/u2f/registration", description = "The endpoint at which the U2F device start registration process.")
@Name("u2fRegistrationRestWebService")
public class U2fRegistrationWS {

	@Logger
	private Log log;

	@In
	private UserService userService;

	@In
	private ErrorResponseFactory errorResponseFactory;

	@In
	private RegistrationService u2fRegistrationService;

	@In
	private DeviceRegistrationService deviceRegistrationService;

	@In
	private SessionStateService sessionStateService;

	@In
	private UserSessionStateService userSessionStateService;

	@In
	private ValidationService u2fValidationService;

	@GET
	@Produces({ "application/json" })
	public Response startRegistration(@QueryParam("username") String userName, @QueryParam("application") String appId, @QueryParam("session_state") String sessionState, @QueryParam("enrollment_code") String enrollmentCode) {
		// Parameter username is deprecated. We uses it only to determine is it's one or two step workflow
		try {
			log.debug("Startig registration with username '{0}' for appId '{1}'. session_state '{2}', enrollment_code '{3}'", userName, appId, sessionState, enrollmentCode);

			String userInum = null;

			boolean twoStep = StringHelper.isNotEmpty(userName);
			if (twoStep) {
				boolean removeEnrollment = false;
				if (StringHelper.isNotEmpty(sessionState)) {
					boolean valid = u2fValidationService.isValidSessionState(userName, sessionState);
					if (!valid) {
						throw new BadInputException(String.format("session_state '%s' is invalid", sessionState));
					}
				} else if (StringHelper.isNotEmpty(enrollmentCode)) {
					boolean valid = u2fValidationService.isValidEnrollmentCode(userName, enrollmentCode);
					if (!valid) {
						throw new BadInputException(String.format("enrollment_code '%s' is invalid", enrollmentCode));
					}
					removeEnrollment = true;
				} else {
					throw new BadInputException(String.format("session_state or enrollment_code is mandatory"));
				}
				
				User user = userService.getUser(userName);
				userInum = userService.getUserInum(user);
				if (StringHelper.isEmpty(userInum)) {
					throw new BadInputException(String.format("Failed to find user '%s' in LDAP", userName));
				}
				
				if (removeEnrollment) {
					// We allow to use enrollment code only one time
					user.setAttribute(U2fConstants.U2F_ENROLLMENT_CODE_ATTRIBUTE, (String) null);
					userService.updateUser(user);
				}
			}

			RegisterRequestMessage registerRequestMessage = u2fRegistrationService.builRegisterRequestMessage(appId, userInum);
			u2fRegistrationService.storeRegisterRequestMessage(registerRequestMessage, userInum, sessionState);

			// Convert manually to avoid possible conflict between resteasy providers, e.g. jettison, jackson
			final String entity = ServerUtil.asJson(registerRequestMessage);

			return Response.status(Response.Status.OK).entity(entity).cacheControl(ServerUtil.cacheControl(true)).build();
		} catch (Exception ex) {
			log.error("Exception happened", ex);
			if (ex instanceof WebApplicationException) {
				throw (WebApplicationException) ex;
			}

			throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SERVER_ERROR)).build());
		}
	}

	@POST
	@Produces({ "application/json" })
	public Response finishRegistration(@FormParam("username") String userName, @FormParam("tokenResponse") String registerResponseString) {
		String sessionState = null;
		try {
			log.debug("Finishing registration for username '{0}' with response '{1}'", userName, registerResponseString);

			RegisterResponse registerResponse = ServerUtil.jsonMapperWithWrapRoot().readValue(registerResponseString, RegisterResponse.class);

			String requestId = registerResponse.getRequestId();
			RegisterRequestMessageLdap registerRequestMessageLdap = u2fRegistrationService.getRegisterRequestMessageByRequestId(requestId);
			if (registerRequestMessageLdap == null) {
				throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
						.entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SESSION_EXPIRED)).build());
			}
			u2fRegistrationService.removeRegisterRequestMessage(registerRequestMessageLdap);

			String foundUserInum = registerRequestMessageLdap.getUserInum();

			RegisterRequestMessage registerRequestMessage = registerRequestMessageLdap.getRegisterRequestMessage();
			DeviceRegistrationResult deviceRegistrationResult = u2fRegistrationService.finishRegistration(registerRequestMessage, registerResponse, foundUserInum);

			// If sessionState is not empty update session
			sessionState = registerRequestMessageLdap.getSessionState();
			if (StringHelper.isNotEmpty(sessionState)) {
				log.debug("There is session state. Setting session state attributes");

				boolean oneStep = StringHelper.isEmpty(foundUserInum);
				userSessionStateService.updateUserSessionStateOnFinishRequest(sessionState, foundUserInum, deviceRegistrationResult, true, oneStep);
			}

			RegisterStatus registerStatus = new RegisterStatus(Constants.RESULT_SUCCESS, requestId);

			// Convert manually to avoid possible conflict between resteasy providers, e.g. jettison, jackson
			final String entity = ServerUtil.asJson(registerStatus);

			return Response.status(Response.Status.OK).entity(entity).cacheControl(ServerUtil.cacheControl(true)).build();
		} catch (Exception ex) {
			log.error("Exception happened", ex);

			try {
				// If sessionState is not empty update session
				if (StringHelper.isNotEmpty(sessionState)) {
					log.debug("There is session state. Setting session state status to 'declined'");
					userSessionStateService.updateUserSessionStateOnError(sessionState);
				}
			} catch (Exception ex2) {
				log.error("Failed to update session state status", ex2);
			}

			if (ex instanceof WebApplicationException) {
				throw (WebApplicationException) ex;
			}

			if (ex instanceof BadInputException) {
				throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
						.entity(errorResponseFactory.getErrorResponse(U2fErrorResponseType.INVALID_REQUEST)).build());
			}

			throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SERVER_ERROR)).build());
		}
	}

}
