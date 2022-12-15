/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ws.rs.fido.u2f;

import io.jans.as.common.service.common.UserService;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.fido.u2f.U2fErrorResponseType;
import io.jans.as.model.fido.u2f.exception.BadInputException;
import io.jans.as.model.fido.u2f.protocol.AuthenticateRequestMessage;
import io.jans.as.model.fido.u2f.protocol.AuthenticateResponse;
import io.jans.as.model.fido.u2f.protocol.AuthenticateStatus;
import io.jans.as.model.util.Base64Util;
import io.jans.as.server.exception.fido.u2f.DeviceCompromisedException;
import io.jans.as.server.exception.fido.u2f.InvalidKeyHandleDeviceException;
import io.jans.as.server.exception.fido.u2f.NoEligableDevicesException;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.model.fido.u2f.AuthenticateRequestMessageLdap;
import io.jans.as.server.model.fido.u2f.DeviceRegistration;
import io.jans.as.server.model.fido.u2f.DeviceRegistrationResult;
import io.jans.as.server.service.fido.u2f.AuthenticationService;
import io.jans.as.server.service.fido.u2f.DeviceRegistrationService;
import io.jans.as.server.service.fido.u2f.UserSessionIdService;
import io.jans.as.server.service.fido.u2f.ValidationService;
import io.jans.as.server.util.ServerUtil;
import io.jans.util.StringHelper;
import org.slf4j.Logger;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
/**
 * The endpoint allows to start and finish U2F authentication process
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@Path("/fido/u2f/authentication")
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
            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.U2F);

            log.debug("Starting authentication with username '{}', keyhandle '{}' for appId '{}' and session_id '{}'", userName, keyHandle, appId, sessionId);

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
            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.U2F);

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
