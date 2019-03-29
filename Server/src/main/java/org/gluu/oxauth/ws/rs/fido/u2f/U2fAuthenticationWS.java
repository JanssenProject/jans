/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.gluu.oxauth.ws.rs.fido.u2f;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.gluu.oxauth.exception.fido.u2f.DeviceCompromisedException;
import org.gluu.oxauth.exception.fido.u2f.InvalidKeyHandleDeviceException;
import org.gluu.oxauth.exception.fido.u2f.NoEligableDevicesException;
import org.gluu.oxauth.model.config.Constants;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.fido.u2f.AuthenticateRequestMessageLdap;
import org.gluu.oxauth.model.fido.u2f.DeviceRegistration;
import org.gluu.oxauth.model.fido.u2f.DeviceRegistrationResult;
import org.gluu.oxauth.model.fido.u2f.U2fErrorResponseType;
import org.gluu.oxauth.model.fido.u2f.exception.BadInputException;
import org.gluu.oxauth.model.fido.u2f.protocol.AuthenticateRequestMessage;
import org.gluu.oxauth.model.fido.u2f.protocol.AuthenticateResponse;
import org.gluu.oxauth.model.fido.u2f.protocol.AuthenticateStatus;
import org.gluu.oxauth.model.util.Base64Util;
import org.gluu.oxauth.service.UserService;
import org.gluu.oxauth.service.fido.u2f.AuthenticationService;
import org.gluu.oxauth.service.fido.u2f.DeviceRegistrationService;
import org.gluu.oxauth.service.fido.u2f.UserSessionIdService;
import org.gluu.oxauth.service.fido.u2f.ValidationService;
import org.gluu.oxauth.util.ServerUtil;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

import com.wordnik.swagger.annotations.Api;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * The endpoint allows to start and finish U2F authentication process
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@Path("/fido/u2f/authentication")
@Api(value = "/fido/u2f/registration", description = "The endpoint at which the application U2F device start registration process.")
public class U2fAuthenticationWS {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private UserService userService;

    @Inject
    private AuthenticationService u2fAuthenticationService;

    @Inject
    private DeviceRegistrationService deviceRegistrationService;

    @Inject
    private UserSessionIdService userSessionIdService;

    @Inject
    private ValidationService u2fValidationService;

    @GET
    @Produces({"application/json"})
    public Response startAuthentication(@QueryParam("username") String userName, @QueryParam("keyhandle") String keyHandle, @QueryParam("application") String appId, @QueryParam("session_id") String sessionId) {
        // Parameter username is deprecated. We uses it only to determine is it's one or two step workflow
        try {
            if (appConfiguration.getDisableU2fEndpoint()) {
                return Response.status(Status.FORBIDDEN).build();
            }

            log.debug("Startig authentication with username '{}', keyhandle '{}' for appId '{}' and session_id '{}'", userName, keyHandle, appId, sessionId);

            if (StringHelper.isEmpty(userName) && StringHelper.isEmpty(keyHandle)) {
                throw new BadInputException("The request should contains either username or keyhandle");
            }

            String foundUserInum = null;

            boolean twoStep = StringHelper.isNotEmpty(userName);
            if (twoStep) {
                boolean valid = u2fValidationService.isValidSessionId(userName, sessionId);
                if (!valid) {
                    throw new BadInputException(String.format("session_id '%s' is invalid", sessionId));
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
            u2fAuthenticationService.storeAuthenticationRequestMessage(authenticateRequestMessage, foundUserInum, sessionId);

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
    @Produces({"application/json"})
    public Response finishAuthentication(@FormParam("username") String userName, @FormParam("tokenResponse") String authenticateResponseString) {
        String sessionId = null;
        try {
            if (appConfiguration.getDisableU2fEndpoint()) {
                return Response.status(Status.FORBIDDEN).build();
            }

            log.debug("Finishing authentication for username '{}' with response '{}'", userName, authenticateResponseString);

            AuthenticateResponse authenticateResponse = ServerUtil.jsonMapperWithWrapRoot().readValue(authenticateResponseString, AuthenticateResponse.class);

            String requestId = authenticateResponse.getRequestId();
            AuthenticateRequestMessageLdap authenticateRequestMessageLdap = u2fAuthenticationService.getAuthenticationRequestMessageByRequestId(requestId);
            if (authenticateRequestMessageLdap == null) {
                throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SESSION_EXPIRED)).build());
            }
            sessionId = authenticateRequestMessageLdap.getSessionId();
            u2fAuthenticationService.removeAuthenticationRequestMessage(authenticateRequestMessageLdap);

            AuthenticateRequestMessage authenticateRequestMessage = authenticateRequestMessageLdap.getAuthenticateRequestMessage();

            String foundUserInum = authenticateRequestMessageLdap.getUserInum();
            DeviceRegistrationResult deviceRegistrationResult = u2fAuthenticationService.finishAuthentication(authenticateRequestMessage, authenticateResponse, foundUserInum);

            // If sessionId is not empty update session
            if (StringHelper.isNotEmpty(sessionId)) {
                log.debug("There is session id. Setting session id attributes");

                boolean oneStep = StringHelper.isEmpty(userName);
                userSessionIdService.updateUserSessionIdOnFinishRequest(sessionId, foundUserInum, deviceRegistrationResult, false, oneStep);
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
                // If sessionId is not empty update session
                if (StringHelper.isNotEmpty(sessionId)) {
                    log.debug("There is session id. Setting session id status to 'declined'");
                    userSessionIdService.updateUserSessionIdOnError(sessionId);
                }
            } catch (Exception ex2) {
                log.error("Failed to update session id status", ex2);
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
                    log.error("Failed to mark device '{}' as compomised", ex2, deviceRegistration.getId());
                }
                throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponseFactory.getErrorResponse(U2fErrorResponseType.DEVICE_COMPROMISED)).build());
            }

            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SERVER_ERROR)).build());
        }
    }
}
