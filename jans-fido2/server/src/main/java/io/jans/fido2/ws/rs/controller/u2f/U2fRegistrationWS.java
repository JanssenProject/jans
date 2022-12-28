/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.ws.rs.controller.u2f;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.fido2.model.u2f.U2fErrorResponseType;
import io.jans.fido2.model.u2f.exception.BadInputException;
import io.jans.fido2.model.u2f.exception.RegistrationNotAllowed;
import io.jans.fido2.model.u2f.protocol.RegisterRequestMessage;
import io.jans.fido2.model.u2f.protocol.RegisterResponse;
import io.jans.fido2.model.u2f.protocol.RegisterStatus;
import io.jans.as.common.model.session.SessionId;
import io.jans.fido2.service.SessionIdService;
import io.jans.fido2.service.external.ExternalAuthenticationService;
import io.jans.fido2.service.u2f.util.Constants;
import io.jans.fido2.model.u2f.DeviceRegistration;
import io.jans.fido2.model.u2f.DeviceRegistrationResult;
import io.jans.fido2.model.u2f.RegisterRequestMessageLdap;
import io.jans.fido2.service.u2f.DeviceRegistrationService;
import io.jans.fido2.service.u2f.RegistrationService;
import io.jans.fido2.service.u2f.UserSessionIdService;
import io.jans.fido2.service.u2f.ValidationService;
import io.jans.fido2.model.u2f.util.ServerUtil;
import io.jans.model.custom.script.conf.CustomScriptConfiguration;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
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

import java.util.List;

/**
 * The endpoint allows to start and finish U2F registration process
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@ApplicationScoped
@Path("/fido/u2f/registration")
public class U2fRegistrationWS {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private UserService userService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private RegistrationService u2fRegistrationService;

    @Inject
    private DeviceRegistrationService deviceRegistrationService;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private UserSessionIdService userSessionIdService;

    @Inject
    private ValidationService u2fValidationService;

    @Inject
    private ExternalAuthenticationService service;

    @GET
    @Produces({"application/json"})
    public Response startRegistration(@QueryParam("username") String userName, @QueryParam("application") String appId, @QueryParam("session_id") String sessionId, @QueryParam("enrollment_code") String enrollmentCode) {
        // Parameter username is deprecated. We uses it only to determine is it's one or two step workflow
        try {
            //errorResponseFactory.validateComponentEnabled(ComponentType.U2F);

            log.debug("Starting registration with username '{}' for appId '{}'. session_id '{}', enrollment_code '{}'", userName, appId, sessionId, enrollmentCode);

            String userInum = null;

            boolean sessionBasedEnrollment = false;
            boolean twoStep = StringHelper.isNotEmpty(userName);
            if (twoStep) {
                boolean removeEnrollment = false;
                if (StringHelper.isNotEmpty(sessionId)) {
                    boolean valid = u2fValidationService.isValidSessionId(userName, sessionId);
                    if (!valid) {
                        throw new BadInputException(String.format("session_id '%s' is invalid", sessionId));
                    }
                    sessionBasedEnrollment = true;
                } else if (StringHelper.isNotEmpty(enrollmentCode)) {
                    boolean valid = u2fValidationService.isValidEnrollmentCode(userName, enrollmentCode);
                    if (!valid) {
                        throw new BadInputException(String.format("enrollment_code '%s' is invalid", enrollmentCode));
                    }
                    removeEnrollment = true;
                } else {
                    throw new BadInputException("session_id or enrollment_code is mandatory");
                }

                User user = userService.getUser(userName);
                userInum = userService.getUserInum(user);
                if (StringHelper.isEmpty(userInum)) {
                    throw new BadInputException(String.format("Failed to find user '%s' in LDAP", userName));
                }

                if (removeEnrollment) {
                    // We allow to use enrollment code only one time
                    user.setAttribute(io.jans.as.model.fido.u2f.U2fConstants.U2F_ENROLLMENT_CODE_ATTRIBUTE, "", false);
                    userService.updateUser(user);
                }
            }

            if (sessionBasedEnrollment) {
                List<DeviceRegistration> deviceRegistrations = deviceRegistrationService.findUserDeviceRegistrations(userInum, appId);
                if (deviceRegistrations.size() > 0 && !isCurrentAuthenticationLevelCorrespondsToU2fLevel(sessionId)) {
                    throw new RegistrationNotAllowed(String.format("It's not possible to start registration with user_name and session_id because user '%s' has already enrolled device", userName));
                }
            }

            RegisterRequestMessage registerRequestMessage = u2fRegistrationService.builRegisterRequestMessage(appId, userInum);
            u2fRegistrationService.storeRegisterRequestMessage(registerRequestMessage, userInum, sessionId);

            // Convert manually to avoid possible conflict between resteasy providers, e.g. jettison, jackson
            final String entity = ServerUtil.asJson(registerRequestMessage);

            return Response.status(Response.Status.OK).entity(entity).cacheControl(ServerUtil.cacheControl(true)).build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);
            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            if (ex instanceof RegistrationNotAllowed) {
                throw new WebApplicationException(Response.status(Response.Status.NOT_ACCEPTABLE)
                        .entity(errorResponseFactory.getErrorResponse(io.jans.as.model.fido.u2f.U2fErrorResponseType.REGISTRATION_NOT_ALLOWED)).build());
            }

            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getJsonErrorResponse(io.jans.as.model.fido.u2f.U2fErrorResponseType.SERVER_ERROR)).build());
        }
    }

    @POST
    @Produces({"application/json"})
    public Response finishRegistration(@FormParam("username") String userName, @FormParam("tokenResponse") String registerResponseString) {
        String sessionId = null;
        try {
            errorResponseFactory.validateFeatureEnabled(FeatureFlagType.U2F);

            log.debug("Finishing registration for username '{}' with response '{}'", userName, registerResponseString);

            RegisterResponse registerResponse = ServerUtil.jsonMapperWithWrapRoot().readValue(registerResponseString, RegisterResponse.class);

            String requestId = registerResponse.getRequestId();
            RegisterRequestMessageLdap registerRequestMessageLdap = u2fRegistrationService.getRegisterRequestMessageByRequestId(requestId);
            if (registerRequestMessageLdap == null) {
                throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponseFactory.getJsonErrorResponse(io.jans.as.model.fido.u2f.U2fErrorResponseType.SESSION_EXPIRED)).build());
            }
            u2fRegistrationService.removeRegisterRequestMessage(registerRequestMessageLdap);

            String foundUserInum = registerRequestMessageLdap.getUserInum();

            RegisterRequestMessage registerRequestMessage = registerRequestMessageLdap.getRegisterRequestMessage();
            DeviceRegistrationResult deviceRegistrationResult = u2fRegistrationService.finishRegistration(registerRequestMessage, registerResponse, foundUserInum);

            // If sessionId is not empty update session
            sessionId = registerRequestMessageLdap.getSessionId();
            if (StringHelper.isNotEmpty(sessionId)) {
                log.debug("There is session id. Setting session id attributes");

                boolean oneStep = StringHelper.isEmpty(foundUserInum);
                userSessionIdService.updateUserSessionIdOnFinishRequest(sessionId, foundUserInum, deviceRegistrationResult, true, oneStep);
            }

            RegisterStatus registerStatus = new RegisterStatus(Constants.RESULT_SUCCESS, requestId);

            // Convert manually to avoid possible conflict between resteasy providers, e.g. jettison, jackson
            final String entity = ServerUtil.asJson(registerStatus);

            return Response.status(Response.Status.OK).entity(entity).cacheControl(ServerUtil.cacheControl(true)).build();
        } catch (Exception ex) {
            log.error("Exception happened", ex);

            try {
                // If sessionId is not empty update session
                if (StringHelper.isNotEmpty(sessionId)) {
                    log.debug("There is session id. Setting session id status to 'declined'");
                    userSessionIdService.updateUserSessionIdOnError(sessionId);
                }
            } catch (Exception ex2) {
                log.error("Failed to update session id status", ex2);
            }

            if (ex instanceof WebApplicationException) {
                throw (WebApplicationException) ex;
            }

            if (ex instanceof BadInputException) {
                throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN)
                        .entity(errorResponseFactory.getErrorResponse(io.jans.as.model.fido.u2f.U2fErrorResponseType.INVALID_REQUEST)).build());
            }

            throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorResponseFactory.getJsonErrorResponse(U2fErrorResponseType.SERVER_ERROR)).build());
        }
    }

    private boolean isCurrentAuthenticationLevelCorrespondsToU2fLevel(String session) {
        SessionId sessionId = sessionIdService.getSessionId(session);
        if (sessionId == null)
            return false;

        String acrValuesStr = sessionIdService.getAcr(sessionId);
        if (acrValuesStr == null)
            return false;

        CustomScriptConfiguration u2fScriptConfiguration = service.getCustomScriptConfigurationByName("u2f");
        if (u2fScriptConfiguration == null)
            return false;

        String[] acrValuesArray = acrValuesStr.split(" ");
        for (String acrValue : acrValuesArray) {
            CustomScriptConfiguration currentScriptConfiguration = service.getCustomScriptConfigurationByName(acrValue);
            if (currentScriptConfiguration == null)
                continue;

            if (currentScriptConfiguration.getLevel() >= u2fScriptConfiguration.getLevel())
                return true;
        }

        return false;
    }

}
