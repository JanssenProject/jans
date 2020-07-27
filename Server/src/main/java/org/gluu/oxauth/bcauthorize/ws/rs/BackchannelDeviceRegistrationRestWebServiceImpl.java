/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.bcauthorize.ws.rs;

import org.gluu.oxauth.audit.ApplicationAuditLogger;
import org.gluu.oxauth.ciba.CIBADeviceRegistrationValidatorService;
import org.gluu.oxauth.model.audit.Action;
import org.gluu.oxauth.model.audit.OAuth2AuditLog;
import org.gluu.oxauth.model.ciba.BackchannelAuthenticationErrorResponseType;
import org.gluu.oxauth.model.common.AuthorizationGrant;
import org.gluu.oxauth.model.common.AuthorizationGrantList;
import org.gluu.oxauth.model.common.User;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.DefaultErrorResponse;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.service.common.UserService;
import org.gluu.oxauth.util.ServerUtil;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import static org.gluu.oxauth.model.ciba.BackchannelAuthenticationErrorResponseType.INVALID_REQUEST;
import static org.gluu.oxauth.model.ciba.BackchannelDeviceRegistrationErrorResponseType.UNKNOWN_USER_ID;

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

        userService.setCustomAttribute(user, "oxAuthBackchannelDeviceRegistrationToken", deviceRegistrationToken);
        userService.updateUser(user);

        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
    }
}