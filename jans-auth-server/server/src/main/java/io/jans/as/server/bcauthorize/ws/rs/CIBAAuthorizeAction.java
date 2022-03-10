/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.bcauthorize.ws.rs;

import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.configuration.AppConfiguration;
import org.slf4j.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.UUID;

import static io.jans.as.model.authorize.AuthorizeRequestParam.CLIENT_ID;
import static io.jans.as.model.authorize.AuthorizeRequestParam.NONCE;
import static io.jans.as.model.authorize.AuthorizeRequestParam.REDIRECT_URI;
import static io.jans.as.model.authorize.AuthorizeRequestParam.RESPONSE_TYPE;
import static io.jans.as.model.authorize.AuthorizeRequestParam.SCOPE;
import static io.jans.as.model.authorize.AuthorizeRequestParam.STATE;

/**
 * @author Javier Rojas Blum
 * @version November 19, 2019
 */
@RequestScoped
@Named("cibaAuthorizeAction")
public class CIBAAuthorizeAction {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    public String getApiKey() {
        return appConfiguration.getCibaEndUserNotificationConfig().getApiKey();
    }

    public String getAuthDomain() {
        return appConfiguration.getCibaEndUserNotificationConfig().getAuthDomain();
    }

    public String getDatabaseURL() {
        return appConfiguration.getCibaEndUserNotificationConfig().getDatabaseURL();
    }

    public String getProjectId() {
        return appConfiguration.getCibaEndUserNotificationConfig().getProjectId();
    }

    public String getStorageBucket() {
        return appConfiguration.getCibaEndUserNotificationConfig().getStorageBucket();
    }

    public String getMessagingSenderId() {
        return appConfiguration.getCibaEndUserNotificationConfig().getMessagingSenderId();
    }

    public String getAppId() {
        return appConfiguration.getCibaEndUserNotificationConfig().getAppId();
    }

    public String getPublicVapidKey() {
        return appConfiguration.getCibaEndUserNotificationConfig().getPublicVapidKey();
    }

    public String getAuthRequest() {
        String authorizationEndpoint = appConfiguration.getAuthorizationEndpoint();
        String clientId = appConfiguration.getBackchannelClientId();
        String redirectUri = appConfiguration.getBackchannelRedirectUri();
        String responseType = "token id_token";
        String scope = "openid";
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();

        RedirectUri authRequest = new RedirectUri(authorizationEndpoint);
        authRequest.addResponseParameter(CLIENT_ID, clientId);
        authRequest.addResponseParameter(REDIRECT_URI, redirectUri);
        authRequest.addResponseParameter(RESPONSE_TYPE, responseType);
        authRequest.addResponseParameter(SCOPE, scope);
        authRequest.addResponseParameter(STATE, state);
        authRequest.addResponseParameter(NONCE, nonce);

        return authRequest.toString();
    }

    public String getBackchannelDeviceRegistrationEndpoint() {
        return appConfiguration.getBackchannelDeviceRegistrationEndpoint();
    }
}