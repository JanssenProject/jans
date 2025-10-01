/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ciba;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.util.Util;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;

import static io.jans.as.model.register.RegisterRequestParam.BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG;
import static io.jans.as.model.register.RegisterRequestParam.BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT;
import static io.jans.as.model.register.RegisterRequestParam.BACKCHANNEL_TOKEN_DELIVERY_MODE;
import static io.jans.as.model.register.RegisterRequestParam.BACKCHANNEL_USER_CODE_PARAMETER;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
@Stateless
@Named
public class CIBARegisterClientResponseService {

    private final static Logger log = LoggerFactory.getLogger(CIBARegisterClientResponseService.class);

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