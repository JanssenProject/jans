/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.configuration;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.interception.CIBARegisterClientResponseInterception;
import org.gluu.oxauth.interception.CIBARegisterClientResponseInterceptionInterface;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.util.Util;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;

import static org.gluu.oxauth.model.register.RegisterRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
@Interceptor
@CIBARegisterClientResponseInterception
@Priority(Interceptor.Priority.APPLICATION)
public class CIBARegisterClientResponseInterceptor implements CIBARegisterClientResponseInterceptionInterface, Serializable {

    private final static Logger log = LoggerFactory.getLogger(CIBARegisterClientResponseInterceptor.class);

    @Inject
    private AppConfiguration appConfiguration;

    public CIBARegisterClientResponseInterceptor() {
        log.info("CIBA Register Client Response Interceptor loaded.");
    }

    @AroundInvoke
    public Object updateResponse(InvocationContext ctx) {
        log.debug("CIBA: update registration response.");

        boolean valid = false;
        try {
            JSONObject responseJsonObject = (JSONObject) ctx.getParameters()[0];
            Client client = (Client) ctx.getParameters()[1];
            updateResponse(responseJsonObject, client);
            ctx.proceed();
        } catch (Exception e) {
            log.error("Failed to retrieve params.", e);
        }

        return valid;
    }

    @Override
    public void updateResponse(JSONObject responseJsonObject, Client client) {
        try {
            Util.addToJSONObjectIfNotNull(responseJsonObject, BACKCHANNEL_TOKEN_DELIVERY_MODE.toString(), client.getBackchannelTokenDeliveryMode());
            Util.addToJSONObjectIfNotNull(responseJsonObject, BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT.toString(), client.getBackchannelClientNotificationEndpoint());
            Util.addToJSONObjectIfNotNull(responseJsonObject, BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG.toString(), client.getBackchannelAuthenticationRequestSigningAlg());
            Util.addToJSONObjectIfNotNull(responseJsonObject, BACKCHANNEL_USER_CODE_PARAMETER.toString(), client.getBackchannelUserCodeParameter());
        } catch (JSONException e) {
            log.error("Failed to update response.", e);
        }
    }
}