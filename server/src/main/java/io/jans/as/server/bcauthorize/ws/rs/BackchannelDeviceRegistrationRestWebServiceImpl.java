/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.bcauthorize.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.DefaultErrorResponse;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.server.audit.ApplicationAuditLogger;
import io.jans.as.server.ciba.CIBADeviceRegistrationValidatorService;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.util.ServerUtil;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import static io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType.INVALID_REQUEST;
import static io.jans.as.model.ciba.BackchannelDeviceRegistrationErrorResponseType.UNKNOWN_USER_ID;

/**
 * Implementation for request backchannel device registration through REST web services.
 *
 * @author Javier Rojas Blum
 * @version October 7, 2019
 */
@Path("/")
public class BackchannelDeviceRegistrationRestWebServiceImpl implements BackchannelDeviceRegistrationRestWebService {

    @Inject
    private Logger log;

    @Inject
    private ApplicationAuditLogger applicationAuditLogger;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private UserService userService;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private CIBADeviceRegistrationValidatorService cibaDeviceRegistrationValidatorService;

    @Override
    public Response requestBackchannelDeviceRegistrationPost(
            String idTokenHint, String deviceRegistrationToken,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext securityContext) {

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.BACKCHANNEL_DEVICE_REGISTRATION);

        // ATTENTION : please do not add more parameter in this debug method because it will not work with Seam 2.2.2.Final,
        // there is limit of 10 parameters (hardcoded), see: org.jboss.seam.core.Interpolator#interpolate
        log.debug("Attempting to request backchannel device registration: "
                        + "idTokenHint = {}, deviceRegistrationToken = {}, isSecure = {}",
                idTokenHint, deviceRegistrationToken, securityContext.isSecure());

        Response.ResponseBuilder builder = Response.ok();

        if (!appConfiguration.getCibaEnabled()) {
            log.warn("Trying to register a CIBA device, however CIBA config is disabled.");
            builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode());
            builder.entity(errorResponseFactory.getErrorAsJson(INVALID_REQUEST));
            return builder.build();
        }

        DefaultErrorResponse cibaDeviceRegistrationValidation = cibaDeviceRegistrationValidatorService.validateParams(
                idTokenHint, deviceRegistrationToken);
        if (cibaDeviceRegistrationValidation != null) {
            builder = Response.status(cibaDeviceRegistrationValidation.getStatus());
            builder.entity(errorResponseFactory.errorAsJson(
                    cibaDeviceRegistrationValidation.getType(), cibaDeviceRegistrationValidation.getReason()));
            return builder.build();
        }

        User user = null;

        AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByIdToken(idTokenHint);
        if (authorizationGrant == null) {
            builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
            builder.entity(errorResponseFactory.getErrorAsJson(BackchannelAuthenticationErrorResponseType.UNKNOWN_USER_ID));
            return builder.build();
        }

        user = authorizationGrant.getUser();

        if (user == null) {
            builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
            builder.entity(errorResponseFactory.getErrorAsJson(UNKNOWN_USER_ID));
            return builder.build();
        }

        userService.setCustomAttribute(user, "jansBackchannelDeviceRegistrationTkn", deviceRegistrationToken);
        userService.updateUser(user);

        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
    }
}