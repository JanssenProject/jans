/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.bcauthorize.ws.rs;

import com.wordnik.swagger.annotations.Api;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.gluu.oxauth.audit.ApplicationAuditLogger;
import org.gluu.oxauth.ciba.CIBADeviceRegistrationValidatorProxy;
import org.gluu.oxauth.ciba.CIBASupportProxy;
import org.gluu.oxauth.model.audit.Action;
import org.gluu.oxauth.model.audit.OAuth2AuditLog;
import org.gluu.oxauth.model.common.User;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.DefaultErrorResponse;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.service.UserService;
import org.gluu.oxauth.util.ServerUtil;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import static org.gluu.oxauth.model.ciba.BackchannelDeviceRegistrationErrorResponseType.ACCESS_DENIED;
import static org.gluu.oxauth.model.ciba.BackchannelDeviceRegistrationErrorResponseType.UNKNOWN_USER_ID;

/**
 * Implementation for request backchannel device registration through REST web services.
 *
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
@Path("/")
@Api(value = "/oxauth/bc-deviceRegistration", description = "Backchannel Device Registration Endpoint")
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
    private CIBASupportProxy cibaSupportProxy;

    @Inject
    private CIBADeviceRegistrationValidatorProxy cibaDeviceRegistrationValidatorProxy;

    @Override
    public Response requestBackchannelDeviceRegistrationPost(
            String loginHint, String deviceRegistrationToken,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse, SecurityContext securityContext) {

        OAuth2AuditLog oAuth2AuditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.BACKCHANNEL_DEVICE_REGISTRATION);
        oAuth2AuditLog.setUsername(loginHint);

        // ATTENTION : please do not add more parameter in this debug method because it will not work with Seam 2.2.2.Final,
        // there is limit of 10 parameters (hardcoded), see: org.jboss.seam.core.Interpolator#interpolate
        log.debug("Attempting to request backchannel device registration: "
                        + "loginHint = {}, deviceRegistrationToken = {}, isSecure = {}",
                loginHint, deviceRegistrationToken, securityContext.isSecure());

        Response.ResponseBuilder builder = Response.ok();

        if (!cibaSupportProxy.isCIBASupported()) {
            builder = Response.status(Response.Status.FORBIDDEN.getStatusCode()); // 403
            builder.entity(errorResponseFactory.errorAsJson(
                    ACCESS_DENIED,
                    "The CIBA (Client Initiated Backchannel Authentication) is not enabled in the server."));
            return builder.build();
        }

        User user = null;
        if (Strings.isNotBlank(loginHint)) { // TODO: Do not use login_hint
            user = userService.getUniqueUserByAttributes(appConfiguration.getBackchannelLoginHintClaims(), loginHint);
        }
        if (user == null) {
            builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
            builder.entity(errorResponseFactory.getErrorAsJson(UNKNOWN_USER_ID));
            return builder.build();
        }

        DefaultErrorResponse cibaDeviceRegistrationValidation = cibaDeviceRegistrationValidatorProxy.validateParams(
                loginHint);
        if (cibaDeviceRegistrationValidation != null) {
            builder = Response.status(cibaDeviceRegistrationValidation.getStatus());
            builder.entity(errorResponseFactory.errorAsJson(
                    cibaDeviceRegistrationValidation.getType(), cibaDeviceRegistrationValidation.getReason()));
            return builder.build();
        }

        userService.setCustomAttribute(user, "oxAuthBackchannelDeviceRegistrationToken", deviceRegistrationToken);
        userService.updateUser(user);

        applicationAuditLogger.sendMessage(oAuth2AuditLog);
        return builder.build();
    }
}