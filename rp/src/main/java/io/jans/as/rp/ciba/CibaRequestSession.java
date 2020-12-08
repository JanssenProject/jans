/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.rp.ciba;

import io.jans.as.client.BackchannelAuthenticationRequest;
import io.jans.as.client.BackchannelAuthenticationResponse;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;

public class CibaRequestSession {

    private BackchannelAuthenticationRequest request;
    private BackchannelAuthenticationResponse response;
    private CibaFlowState state;
    private BackchannelTokenDeliveryMode tokenDeliveryMode;
    private String clientId;
    private String clientSecret;
    private String clientNotificationToken;
    private String callbackJsonBody;

    public CibaRequestSession() {

    }

    public CibaRequestSession(BackchannelAuthenticationRequest request, BackchannelAuthenticationResponse response,
                              CibaFlowState state) {
        this.request = request;
        this.response = response;
        this.state = state;
    }

    public BackchannelAuthenticationRequest getRequest() {
        return request;
    }

    public void setRequest(BackchannelAuthenticationRequest request) {
        this.request = request;
    }

    public BackchannelAuthenticationResponse getResponse() {
        return response;
    }

    public void setResponse(BackchannelAuthenticationResponse response) {
        this.response = response;
    }

    public CibaFlowState getState() {
        return state;
    }

    public void setState(CibaFlowState state) {
        this.state = state;
    }

    public BackchannelTokenDeliveryMode getTokenDeliveryMode() {
        return tokenDeliveryMode;
    }

    public void setTokenDeliveryMode(BackchannelTokenDeliveryMode tokenDeliveryMode) {
        this.tokenDeliveryMode = tokenDeliveryMode;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientNotificationToken() {
        return clientNotificationToken;
    }

    public void setClientNotificationToken(String clientNotificationToken) {
        this.clientNotificationToken = clientNotificationToken;
    }

    public String getCallbackJsonBody() {
        return callbackJsonBody;
    }

    public void setCallbackJsonBody(String callbackJsonBody) {
        this.callbackJsonBody = callbackJsonBody;
    }

    @Override
    public String toString() {
        return "CibaRequestSession{" +
                "request=" + request +
                ", response=" + response +
                ", state=" + state +
                ", tokenDeliveryMode=" + tokenDeliveryMode +
                '}';
    }
}
