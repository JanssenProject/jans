package org.gluu.oxauth.ciba;

import org.apache.logging.log4j.util.Strings;
import org.gluu.oxauth.interception.CIBADeviceRegistationValidatorInterception;
import org.gluu.oxauth.interception.CIBADeviceRegistrationValidatorInterceptionInterface;
import org.gluu.oxauth.model.error.DefaultErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;
import java.io.Serializable;

import static org.gluu.oxauth.model.ciba.BackchannelAuthenticationErrorResponseType.INVALID_REQUEST;
import static org.gluu.oxauth.model.ciba.BackchannelAuthenticationErrorResponseType.UNKNOWN_USER_ID;

/**
 * @author Javier Rojas Blum
 * @version October 7, 2019
 */
@Interceptor
@CIBADeviceRegistationValidatorInterception
@Priority(Interceptor.Priority.APPLICATION)
public class CIBADeviceRegistrationValidatorInterceptor implements CIBADeviceRegistrationValidatorInterceptionInterface, Serializable {

    private final static Logger log = LoggerFactory.getLogger(CIBADeviceRegistrationValidatorInterceptor.class);

    public CIBADeviceRegistrationValidatorInterceptor() {
        log.info("CIBA Device Registration Validator Interceptor loaded.");
    }

    @AroundInvoke
    public Object validateParams(InvocationContext ctx) {
        log.debug("CIBA: validate device registration params...");

        DefaultErrorResponse errorResponse = null;
        try {
            String idTokenHint = (String) ctx.getParameters()[0];
            String deviceRegistrationToken = (String) ctx.getParameters()[1];

            errorResponse = validateParams(idTokenHint, deviceRegistrationToken);
            ctx.proceed();
        } catch (Exception e) {
            log.error("Failed to validate authorize params.", e);
        }

        return errorResponse;
    }

    @Override
    public DefaultErrorResponse validateParams(String idTokenHint, String deviceRegistrationToken) {
        if (Strings.isBlank(deviceRegistrationToken)) {
            DefaultErrorResponse errorResponse = new DefaultErrorResponse();
            errorResponse.setStatus(Response.Status.BAD_REQUEST.getStatusCode()); // 400
            errorResponse.setType(INVALID_REQUEST);
            errorResponse.setReason("The device registration token cannot be blank.");

            return errorResponse;
        }

        if (Strings.isBlank(idTokenHint)) {
            DefaultErrorResponse errorResponse = new DefaultErrorResponse();
            errorResponse.setStatus(Response.Status.BAD_REQUEST.getStatusCode()); // 400
            errorResponse.setType(UNKNOWN_USER_ID);
            errorResponse.setReason("The id token hint cannot be blank.");

            return errorResponse;
        }

        return null;
    }
}
