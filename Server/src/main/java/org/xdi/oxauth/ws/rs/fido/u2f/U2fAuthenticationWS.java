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
import org.xdi.oxauth.exception.fido.u2f.NoEligableDevicesException;
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.fido.u2f.AuthenticateRequestMessageLdap;
import org.xdi.oxauth.model.fido.u2f.U2fErrorResponseType;
import org.xdi.oxauth.model.fido.u2f.exception.BadInputException;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateRequestMessage;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateResponse;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateStatus;
import org.xdi.oxauth.service.fido.u2f.AuthenticationService;
import org.xdi.oxauth.service.fido.u2f.DeviceRegistrationService;
import org.xdi.oxauth.util.ServerUtil;

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
	private AuthenticationService u2fAuthenticationService;

	@In
	private DeviceRegistrationService deviceRegistrationService;

	@GET
	@Produces({ "application/json" })
	public Response startAuthentication(@QueryParam("username") String userName, @QueryParam("application") String appId) {
		try {
			AuthenticateRequestMessage authenticateRequestMessage = u2fAuthenticationService.buildAuthenticateRequestMessage(appId, userName);
			u2fAuthenticationService.storeAuthenticationRequestMessage(authenticateRequestMessage);

			// convert manually to avoid possible conflict between resteasy
			// providers, e.g. jettison, jackson
			final String entity = ServerUtil.asJson(authenticateRequestMessage);

			return Response.status(Response.Status.OK).entity(entity).cacheControl(ServerUtil.cacheControl(true)).build();
		} catch (Exception ex) {
			log.error("Exception happened", ex);
			if (ex instanceof WebApplicationException) {
				throw (WebApplicationException) ex;
			}

			if (ex instanceof NoEligableDevicesException) {
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
		try {
			AuthenticateResponse authenticateResponse = ServerUtil.jsonMapperWithWrapRoot().readValue(authenticateResponseString, AuthenticateResponse.class);

			String requestId = authenticateResponse.getRequestId();
			AuthenticateRequestMessageLdap authenticateRequestMessageLdap = u2fAuthenticationService.getAuthenticationRequestMessageByRequestId(requestId);
			if (authenticateRequestMessageLdap == null) {
				throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
						.entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SESSION_EXPIRED)).build());
			}
			u2fAuthenticationService.removeAuthenticationRequestMessage(authenticateRequestMessageLdap);

			AuthenticateRequestMessage authenticateRequestMessage = authenticateRequestMessageLdap.getAuthenticateRequestMessage();
			u2fAuthenticationService.finishAuthentication(authenticateRequestMessage, authenticateResponse, userName);

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

			if (ex instanceof BadInputException) {
				throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
						.entity(errorResponseFactory.getErrorResponse(U2fErrorResponseType.INVALID_REQUEST)).build());
			}

			if (ex instanceof DeviceCompromisedException) {
				throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
						.entity(errorResponseFactory.getErrorResponse(U2fErrorResponseType.DEVICE_COMPROMISED)).build());
			}

			throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SERVER_ERROR)).build());
		}
	}
}
