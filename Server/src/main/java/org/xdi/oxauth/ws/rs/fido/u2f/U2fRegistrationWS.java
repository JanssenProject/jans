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
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.fido.u2f.RegisterRequestMessageLdap;
import org.xdi.oxauth.model.fido.u2f.U2fErrorResponseType;
import org.xdi.oxauth.model.fido.u2f.exception.BadInputException;
import org.xdi.oxauth.model.fido.u2f.protocol.AuthenticateResponse;
import org.xdi.oxauth.model.fido.u2f.protocol.RegisterRequestMessage;
import org.xdi.oxauth.model.fido.u2f.protocol.RegisterResponse;
import org.xdi.oxauth.model.fido.u2f.protocol.RegisterStatus;
import org.xdi.oxauth.service.fido.u2f.DeviceRegistrationService;
import org.xdi.oxauth.service.fido.u2f.RegistrationService;
import org.xdi.oxauth.util.ServerUtil;

import com.wordnik.swagger.annotations.Api;

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
	private ErrorResponseFactory errorResponseFactory;

	@In
	private RegistrationService u2fRegistrationService;

	@In
	private DeviceRegistrationService deviceRegistrationService;

	@GET
	@Produces({ "application/json" })
	public Response startRegistration(@QueryParam("username") String userName, @QueryParam("application") String appId) {
		try {
			RegisterRequestMessage registerRequestMessage = u2fRegistrationService.builRegisterRequestMessage(appId, userName);
			u2fRegistrationService.storeRegisterRequestMessage(registerRequestMessage);

			// convert manually to avoid possible conflict between resteasy
			// providers, e.g. jettison, jackson
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
		try {
			RegisterResponse registerResponse = ServerUtil.jsonMapperWithWrapRoot().readValue(registerResponseString, RegisterResponse.class);

			String requestId = registerResponse.getRequestId();
			RegisterRequestMessageLdap registerRequestMessageLdap = u2fRegistrationService.getRegisterRequestMessageByRequestId(requestId);
			if (registerRequestMessageLdap == null) {
				throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
						.entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SESSION_EXPIRED)).build());
			}
			u2fRegistrationService.removeRegisterRequestMessage(registerRequestMessageLdap);

			RegisterRequestMessage registerRequestMessage = registerRequestMessageLdap.getRegisterRequestMessage();
			u2fRegistrationService.finishRegistration(registerRequestMessage, registerResponse, userName);

			RegisterStatus registerStatus = new RegisterStatus(Constants.RESULT_SUCCESS, requestId);

			// convert manually to avoid possible conflict between resteasy
			// providers, e.g. jettison, jackson
			final String entity = ServerUtil.asJson(registerStatus);

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

			throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SERVER_ERROR)).build());
		}
	}
}
