/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.interception.CIBAAuthorizeParamsValidatorInterception;
import org.gluu.oxauth.interception.CIBAAuthorizeParamsValidatorInterceptionInterface;
import org.gluu.oxauth.model.common.BackchannelTokenDeliveryMode;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.DefaultErrorResponse;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.gluu.oxauth.model.ciba.BackchannelAuthenticationErrorResponseType.*;

/**
 * @author Javier Rojas Blum
 * @version October 7, 2019
 */
@Interceptor
@CIBAAuthorizeParamsValidatorInterception
@Priority(Interceptor.Priority.APPLICATION)
public class CIBAAuthorizeParamsValidatorInterceptor implements CIBAAuthorizeParamsValidatorInterceptionInterface, Serializable {

    private final static Logger log = LoggerFactory.getLogger(CIBAAuthorizeParamsValidatorInterceptor.class);

    @Inject
    private AppConfiguration appConfiguration;

    public CIBAAuthorizeParamsValidatorInterceptor() {
        log.info("CIBA Authorize Params Validator Interceptor loaded.");
    }

    @AroundInvoke
    public Object validateParams(InvocationContext ctx) {
        log.debug("CIBA: validate authorize params...");

        DefaultErrorResponse errorResponse = null;
        try {
            List<String> scopeList = (List<String>) ctx.getParameters()[0];
            String clientNotificationToken = (String) ctx.getParameters()[1];
            BackchannelTokenDeliveryMode tokenDeliveryMode = (BackchannelTokenDeliveryMode) ctx.getParameters()[2];
            String loginHintToken = (String) ctx.getParameters()[3];
            String idTokenHint = (String) ctx.getParameters()[4];
            String loginHint = (String) ctx.getParameters()[5];
            String bindingMessage = (String) ctx.getParameters()[6];
            Boolean backchannelUserCodeParameter = (Boolean) ctx.getParameters()[7];
            String userCode = (String) ctx.getParameters()[8];
            errorResponse = validateParams(
                    scopeList, clientNotificationToken, tokenDeliveryMode,
                    loginHintToken, idTokenHint, loginHint, bindingMessage,
                    backchannelUserCodeParameter, userCode);
            ctx.proceed();
        } catch (Exception e) {
            log.error("Failed to validate authorize params.", e);
        }

        return errorResponse;
    }

    @Override
    public DefaultErrorResponse validateParams(
            List<String> scopeList, String clientNotificationToken, BackchannelTokenDeliveryMode tokenDeliveryMode,
            String loginHintToken, String idTokenHint, String loginHint, String bindingMessage,
            Boolean backchannelUserCodeParameter, String userCode) {

        if (tokenDeliveryMode == null) {
            DefaultErrorResponse errorResponse = new DefaultErrorResponse();
            errorResponse.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
            errorResponse.setType(UNAUTHORIZED_CLIENT);
            errorResponse.setReason(
                    "Clients registering to use CIBA must indicate a token delivery mode.");

            return errorResponse;
        }

        if (scopeList == null || !scopeList.contains("openid")) {
            DefaultErrorResponse errorResponse = new DefaultErrorResponse();
            errorResponse.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
            errorResponse.setType(INVALID_SCOPE);
            errorResponse.setReason(
                    "CIBA authentication requests must contain the openid scope value.");

            return errorResponse;
        }

        if (!validateOneParamNotBlank(loginHintToken, idTokenHint, loginHint)) {
            DefaultErrorResponse errorResponse = new DefaultErrorResponse();
            errorResponse.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
            errorResponse.setType(INVALID_REQUEST);
            errorResponse.setReason(
                    "It is required that the Client provides one (and only one) of the hints in the authentication " +
                            "request, that is login_hint_token, id_token_hint or login_hint.");

            return errorResponse;
        }

        if (tokenDeliveryMode == BackchannelTokenDeliveryMode.PING || tokenDeliveryMode == BackchannelTokenDeliveryMode.PUSH) {
            if (Strings.isBlank(clientNotificationToken)) {
                DefaultErrorResponse errorResponse = new DefaultErrorResponse();
                errorResponse.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
                errorResponse.setType(INVALID_REQUEST);
                errorResponse.setReason(
                        "The client notification token is required if the Client is registered to use Ping or Push modes.");

                return errorResponse;
            }
        }

        if(Strings.isNotBlank(bindingMessage)) {
            final Pattern pattern = Pattern.compile(appConfiguration.getBackchannelBindingMessagePattern());
            if (!pattern.matcher(bindingMessage).matches()) {
                DefaultErrorResponse errorResponse = new DefaultErrorResponse();
                errorResponse.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
                errorResponse.setType(INVALID_BINDING_MESSAGE);
                errorResponse.setReason("The provided binding message is unacceptable. It must match the pattern: " + pattern.pattern());

                return errorResponse;
            }
        }

        if (backchannelUserCodeParameter != null && backchannelUserCodeParameter) {
            if (Strings.isBlank(userCode)) {
                DefaultErrorResponse errorResponse = new DefaultErrorResponse();
                errorResponse.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
                errorResponse.setType(INVALID_USER_CODE);
                errorResponse.setReason("The user code is required.");

                return errorResponse;
            }
        }

        return null;
    }

    private boolean validateOneParamNotBlank(String... params) {
        List<String> notBlankParams = new ArrayList<>();

        for (String param : params) {
            if (Strings.isNotBlank(param)) {
                notBlankParams.add(param);
            }
        }

        return notBlankParams.size() == 1;
    }
}