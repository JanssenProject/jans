/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.ciba.BackchannelAuthenticationRequestParam;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;

import static io.jans.as.model.ciba.BackchannelAuthenticationResponseParam.AUTH_REQ_ID;
import static io.jans.as.model.ciba.BackchannelAuthenticationResponseParam.EXPIRES_IN;
import static io.jans.as.model.ciba.BackchannelAuthenticationResponseParam.INTERVAL;

/**
 * Encapsulates functionality to make backchannel authentication request calls to an authorization server via REST Services.
 *
 * @author Javier Rojas Blum
 * @version September 4, 2019
 */
public class BackchannelAuthenticationClient extends BaseClient<BackchannelAuthenticationRequest, BackchannelAuthenticationResponse> {

    private static final Logger LOG = Logger.getLogger(BackchannelAuthenticationClient.class);

    /**
     * Constructs a backchannel authentication client by providing a REST url where the
     * backchannel authentication service is located.
     *
     * @param url The REST Service location.
     */
    public BackchannelAuthenticationClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    /**
     * Executes the call to the REST Service and processes the response.
     *
     * @return The authorization response.
     */
    public BackchannelAuthenticationResponse exec() {
        BackchannelAuthenticationResponse response = null;

        try {
            initClient();
            response = execInternal();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return response;
    }

    private BackchannelAuthenticationResponse execInternal() throws Exception {
        final String scopesAsString = Util.listAsString(getRequest().getScope());
        final String acrValuesAsString = Util.listAsString(getRequest().getAcrValues());

        if (StringUtils.isNotBlank(scopesAsString)) {
            requestForm.param(BackchannelAuthenticationRequestParam.SCOPE, scopesAsString);
        }
        if (StringUtils.isNotBlank(getRequest().getClientNotificationToken())) {
            requestForm.param(BackchannelAuthenticationRequestParam.CLIENT_NOTIFICATION_TOKEN, getRequest().getClientNotificationToken());
        }
        if (StringUtils.isNotBlank(acrValuesAsString)) {
            requestForm.param(BackchannelAuthenticationRequestParam.ACR_VALUES, acrValuesAsString);
        }
        if (StringUtils.isNotBlank(getRequest().getLoginHintToken())) {
            requestForm.param(BackchannelAuthenticationRequestParam.LOGIN_HINT_TOKEN, getRequest().getLoginHintToken());
        }
        if (StringUtils.isNotBlank(getRequest().getIdTokenHint())) {
            requestForm.param(BackchannelAuthenticationRequestParam.ID_TOKEN_HINT, getRequest().getIdTokenHint());
        }
        if (StringUtils.isNotBlank(getRequest().getLoginHint())) {
            requestForm.param(BackchannelAuthenticationRequestParam.LOGIN_HINT, getRequest().getLoginHint());
        }
        if (StringUtils.isNotBlank(getRequest().getBindingMessage())) {
            requestForm.param(BackchannelAuthenticationRequestParam.BINDING_MESSAGE, getRequest().getBindingMessage());
        }
        if (StringUtils.isNotBlank(getRequest().getUserCode())) {
            requestForm.param(BackchannelAuthenticationRequestParam.USER_CODE, getRequest().getUserCode());
        }
        if (getRequest().getRequestedExpiry() != null) {
            requestForm.param(BackchannelAuthenticationRequestParam.REQUESTED_EXPIRY, getRequest().getRequestedExpiry().toString());
        }
        if (StringUtils.isNotBlank(getRequest().getClientId())) {
            requestForm.param(BackchannelAuthenticationRequestParam.CLIENT_ID, getRequest().getClientId());
        }
        if (StringUtils.isNotBlank(getRequest().getRequest())) {
            requestForm.param(BackchannelAuthenticationRequestParam.REQUEST, getRequest().getRequest());
        }
        if (StringUtils.isNotBlank(getRequest().getRequestUri())) {
            requestForm.param(BackchannelAuthenticationRequestParam.REQUEST_URI, getRequest().getRequestUri());
        }

        Builder clientRequest = webTarget.request();
        applyCookies(clientRequest);

        // Prepare request parameters
        clientRequest.header("Content-Type", request.getContentType());
        if (request.getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_BASIC && request.hasCredentials()) {
            clientRequest.header("Authorization", "Basic " + request.getEncodedCredentials());
        }

        new ClientAuthnEnabler(clientRequest, requestForm).exec(getRequest());

        // Call REST Service and handle response
        clientResponse = clientRequest.buildPost(Entity.form(requestForm)).invoke();

        setResponse(new BackchannelAuthenticationResponse(clientResponse));
        if (StringUtils.isNotBlank(response.getEntity())) {
            JSONObject jsonObj = new JSONObject(response.getEntity());

            if (jsonObj.has(AUTH_REQ_ID)) {
                getResponse().setAuthReqId(jsonObj.getString(AUTH_REQ_ID));
            }
            if (jsonObj.has(EXPIRES_IN)) {
                getResponse().setExpiresIn(jsonObj.getInt(EXPIRES_IN));
            }
            if (jsonObj.has(INTERVAL)) {
                getResponse().setInterval(jsonObj.getInt(INTERVAL));
            }
        }

        return getResponse();
    }
}