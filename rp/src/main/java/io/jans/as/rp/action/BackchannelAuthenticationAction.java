/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.rp.action;

import io.jans.as.client.BackchannelAuthenticationClient;
import io.jans.as.client.BackchannelAuthenticationRequest;
import io.jans.as.client.BackchannelAuthenticationResponse;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.util.StringUtils;
import io.jans.as.rp.ciba.CibaFlowState;
import io.jans.as.rp.ciba.CibaRequestSession;
import io.jans.as.rp.service.CibaSessions;
import org.slf4j.Logger;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version December 21, 2019
 */
@Named
@SessionScoped
public class BackchannelAuthenticationAction implements Serializable {

    private static final long serialVersionUID = -5920839613190688979L;

    @Inject
    private Logger log;

    @Inject
    private TokenAction tokenAction;

    @Inject
    private CibaSessions cibaSessions;

    private String backchannelAuthenticationEndpoint;
    private List<String> scope;
    private String clientNotificationToken;
    private String acrValues;
    private String loginHintToken;
    private String idTokenHint;
    private String loginHint;
    private String bindingMessage;
    private String userCode;
    private Integer requestedExpiry;
    private String clientId;
    private String clientSecret;
    private BackchannelTokenDeliveryMode backchannelTokenDeliveryMode;
    private String authReqId;

    private boolean showResults;
    private String requestString;
    private String responseString;
    private String jsonResponse;

    public void exec() {
        BackchannelAuthenticationRequest backchannelAuthenticationRequest = new BackchannelAuthenticationRequest();
        backchannelAuthenticationRequest.setScope(scope);
        backchannelAuthenticationRequest.setClientNotificationToken(clientNotificationToken);
        backchannelAuthenticationRequest.setAcrValues(StringUtils.spaceSeparatedToList(acrValues));
        backchannelAuthenticationRequest.setLoginHintToken(loginHintToken);
        backchannelAuthenticationRequest.setIdTokenHint(idTokenHint);
        backchannelAuthenticationRequest.setLoginHint(loginHint);
        backchannelAuthenticationRequest.setBindingMessage(bindingMessage);
        backchannelAuthenticationRequest.setUserCode(userCode);
        backchannelAuthenticationRequest.setRequestedExpiry(requestedExpiry);

        backchannelAuthenticationRequest.setAuthUsername(clientId);
        backchannelAuthenticationRequest.setAuthPassword(clientSecret);

        BackchannelAuthenticationClient backchannelAuthenticationClient = new BackchannelAuthenticationClient(backchannelAuthenticationEndpoint);
        backchannelAuthenticationClient.setRequest(backchannelAuthenticationRequest);
        BackchannelAuthenticationResponse backchannelAuthenticationResponse = backchannelAuthenticationClient.exec();

        requestString = backchannelAuthenticationClient.getRequestAsString();
        responseString = backchannelAuthenticationClient.getResponseAsString();
        if (backchannelAuthenticationClient.getResponse().getStatus() >= 200
                && backchannelAuthenticationClient.getResponse().getStatus() < 300) {
            jsonResponse = backchannelAuthenticationClient.getResponse().getEntity();
        }

        authReqId = backchannelAuthenticationResponse.getAuthReqId();
        tokenAction.setAuthReqId(authReqId);

        showResults = true;

        // Create session to process between endpoints and the frontend application.
        CibaRequestSession session = new CibaRequestSession();
        session.setState(CibaFlowState.REQUEST_SENT);
        session.setRequest(backchannelAuthenticationRequest);
        session.setResponse(backchannelAuthenticationResponse);
        session.setTokenDeliveryMode(this.backchannelTokenDeliveryMode);
        session.setClientId(clientId);
        session.setClientSecret(clientSecret);
        session.setClientNotificationToken(clientNotificationToken);
        cibaSessions.getSessions().put(backchannelAuthenticationResponse.getAuthReqId(), session);
    }

    public String getRequestStatus() {
        if (cibaSessions.getSessions().containsKey(authReqId)) {
            CibaRequestSession session = cibaSessions.getSessions().get(authReqId);
            switch (session.getState()) {
                case REQUEST_SENT: return "Waiting...";
                case RESPONSE_GOTTEN: return "Response : ";
                default: return "Invalid state";
            }
        } else {
            return null;
        }
    }

    public String getResponsePushFlow() {
        return cibaSessions.getSessions().get(this.authReqId).getCallbackJsonBody();
    }

    public String getResponsePingFlow() {
        if (cibaSessions.getSessions().containsKey(this.authReqId)) {
            return cibaSessions.getSessions().get(this.authReqId).getCallbackJsonBody();
        }
        return null;
    }

    public String getBackchannelAuthenticationEndpoint() {
        return backchannelAuthenticationEndpoint;
    }

    public void setBackchannelAuthenticationEndpoint(String backchannelAuthenticationEndpoint) {
        this.backchannelAuthenticationEndpoint = backchannelAuthenticationEndpoint;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getClientNotificationToken() {
        return clientNotificationToken;
    }

    public void setClientNotificationToken(String clientNotificationToken) {
        this.clientNotificationToken = clientNotificationToken;
    }

    public String getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(String acrValues) {
        this.acrValues = acrValues;
    }

    public String getLoginHintToken() {
        return loginHintToken;
    }

    public void setLoginHintToken(String loginHintToken) {
        this.loginHintToken = loginHintToken;
    }

    public String getIdTokenHint() {
        return idTokenHint;
    }

    public void setIdTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public void setBindingMessage(String bindingMessage) {
        this.bindingMessage = bindingMessage;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public Integer getRequestedExpiry() {
        return requestedExpiry;
    }

    public void setRequestedExpiry(Integer requestedExpiry) {
        this.requestedExpiry = requestedExpiry;
    }

    public boolean isShowResults() {
        return showResults;
    }

    public void setShowResults(boolean showResults) {
        this.showResults = showResults;
    }

    public String getRequestString() {
        return requestString;
    }

    public void setRequestString(String requestString) {
        this.requestString = requestString;
    }

    public String getResponseString() {
        return responseString;
    }

    public void setResponseString(String responseString) {
        this.responseString = responseString;
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

    public BackchannelTokenDeliveryMode getBackchannelTokenDeliveryMode() {
        return backchannelTokenDeliveryMode;
    }

    public void setBackchannelTokenDeliveryMode(BackchannelTokenDeliveryMode backchannelTokenDeliveryMode) {
        this.backchannelTokenDeliveryMode = backchannelTokenDeliveryMode;
    }

    public String getJsonResponse() {
        return jsonResponse;
    }

    public void setJsonResponse(String jsonResponse) {
        this.jsonResponse = jsonResponse;
    }
}
