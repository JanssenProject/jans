/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.ciba.BackchannelAuthenticationRequestParam;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.util.QueryBuilder;
import io.jans.as.model.util.Util;

import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Represents a CIBA backchannel authorization request to send to the authorization server.
 *
 * @author Javier Rojas Blum
 * @version May 28, 2020
 */
public class BackchannelAuthenticationRequest extends ClientAuthnRequest {

    private List<String> scope;
    private String clientNotificationToken;
    private List<String> acrValues;
    private String loginHintToken;
    private String idTokenHint;
    private String loginHint;
    private String bindingMessage;
    private String userCode;
    private Integer requestedExpiry;
    private String clientId;
    private String request;
    private String requestUri;

    public BackchannelAuthenticationRequest() {
        setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
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

    public List<String> getAcrValues() {
        return acrValues;
    }

    public void setAcrValues(List<String> acrValues) {
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

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String getQueryString() {
        QueryBuilder builder = QueryBuilder.instance();

        final String scopesAsString = Util.listAsString(scope);
        final String acrValuesAsString = Util.listAsString(acrValues);

        builder.append(BackchannelAuthenticationRequestParam.SCOPE, scopesAsString);
        builder.append(BackchannelAuthenticationRequestParam.CLIENT_NOTIFICATION_TOKEN, clientNotificationToken);
        builder.append(BackchannelAuthenticationRequestParam.ACR_VALUES, acrValuesAsString);
        builder.append(BackchannelAuthenticationRequestParam.LOGIN_HINT_TOKEN, loginHintToken);
        builder.append(BackchannelAuthenticationRequestParam.ID_TOKEN_HINT, idTokenHint);
        builder.append(BackchannelAuthenticationRequestParam.LOGIN_HINT, loginHint);
        builder.append(BackchannelAuthenticationRequestParam.BINDING_MESSAGE, bindingMessage);
        builder.append(BackchannelAuthenticationRequestParam.USER_CODE, userCode);
        builder.appendIfNotNull(BackchannelAuthenticationRequestParam.REQUESTED_EXPIRY, requestedExpiry);
        builder.appendIfNotNull(BackchannelAuthenticationRequestParam.CLIENT_ID, clientId);
        builder.appendIfNotNull(BackchannelAuthenticationRequestParam.REQUEST, request);
        builder.appendIfNotNull(BackchannelAuthenticationRequestParam.REQUEST_URI, requestUri);

        appendClientAuthnToQuery(builder);
        return builder.toString();
    }
}