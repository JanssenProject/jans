/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.bcauthorize.ws.rs;

import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.util.RedirectUri;
import org.slf4j.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.UUID;

import static org.gluu.oxauth.model.authorize.AuthorizeRequestParam.*;

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