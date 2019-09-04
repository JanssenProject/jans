/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.bcauthorize.ws.rs;

import org.gluu.jsf2.message.FacesMessages;
import org.gluu.jsf2.service.FacesService;
import org.gluu.oxauth.ciba.CIBAPushTokenDeliveryProxy;
import org.gluu.oxauth.model.authorize.ScopeChecker;
import org.gluu.oxauth.model.common.*;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.model.util.StringUtils;
import org.gluu.oxauth.service.AuthorizeService;
import org.gluu.oxauth.service.ClientService;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Javier Rojas Blum
 * @version September 4, 2019
 */
@RequestScoped
@Named("cibaAuthorizeAction")
public class CIBAAuthorizeAction {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ClientService clientService;

    @Inject
    private AuthorizeService authorizeService;

    @Inject
    private ScopeChecker scopeChecker;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private FacesService facesService;

    @Inject
    private FacesMessages facesMessages;

    @Inject
    private CIBAPushTokenDeliveryProxy cibaPushTokenDeliveryProxy;

    private String authorizationRequestId;

    private Client client;

    private CIBAGrant authorizationGrant;

    public void loadAuthorizationGrant() {
        if (authorizationRequestId != null && !authorizationRequestId.isEmpty() && authorizationGrant == null) {
            authorizationGrant = authorizationGrantList.getCIBAGrant(authorizationRequestId);
        }
    }

    public Client getClient() {
        if (authorizationGrant != null && client == null) {
            String clientId = authorizationGrant.getClientId();

            client = clientService.getClient(clientId);
        }

        return client;
    }

    public List<Scope> getScopes() {
        Set<String> scope = new HashSet<>();

        if (authorizationGrant != null) {
            scope = authorizationGrant.getScopes();
        }

        Set<String> grantedScopes = scopeChecker.checkScopesPolicy(getClient(), StringUtils.implode(scope, " "));
        String allowedScope = StringUtils.implode(grantedScopes, " ");
        return authorizeService.getScopes(allowedScope);
    }

    public void permissionGranted() {
        loadAuthorizationGrant();

        if ((authorizationRequestId == null) || authorizationRequestId.isEmpty()) {
            log.error("Permission denied. auth_req_id should be not empty.");
            permissionDenied();
            return;
        }

        if (getClient() == null) {
            log.error("Permission denied. Failed to find the client in LDAP.");
            permissionDenied();
            return;
        }

        authorizationGrant.setUserAuthorization(true);
        authorizationGrant.save();

        if (authorizationGrant.getClient().getBackchannelTokenDeliveryMode() == BackchannelTokenDeliveryMode.PUSH) {

            RefreshToken refreshToken = authorizationGrant.createRefreshToken();
            log.debug("Issuing refresh token: {}", refreshToken.getCode());

            //AccessToken accessToken = authorizationGrant.createAccessToken(request.getHeader("X-ClientCert"), new ExecutionContext(request, response)); // create token after scopes are checked
            AccessToken accessToken = authorizationGrant.createAccessToken(null, new ExecutionContext(null, null));
            log.debug("Issuing access token: {}", accessToken.getCode());

            IdToken idToken = authorizationGrant.createIdToken(
                    null, null, accessToken, refreshToken,
                    null, authorizationGrant, false, null);

            cibaPushTokenDeliveryProxy.pushTokenDelivery(
                    authorizationGrant.getCIBAAuthenticationRequestId().getCode(),
                    authorizationGrant.getClient().getBackchannelClientNotificationEndpoint(),
                    authorizationGrant.getClientNotificationToken(),
                    accessToken.getCode(),
                    refreshToken.getCode(),
                    idToken.getCode(),
                    accessToken.getExpiresIn()
            );
        }

        facesMessages.add(FacesMessage.SEVERITY_INFO, "Permission granted.");
        facesService.redirect("/ciba/home.xhtml");
    }

    public void permissionDenied() {
        loadAuthorizationGrant();

        authorizationGrant.setUserAuthorization(false);
        authorizationGrant.save();

        facesMessages.add(FacesMessage.SEVERITY_INFO, "Permission denied.");
        facesService.redirect("/ciba/home.xhtml");
    }

    public String getAuthorizationRequestId() {
        return authorizationRequestId;
    }

    public void setAuthorizationRequestId(String authorizationRequestId) {
        this.authorizationRequestId = authorizationRequestId;
    }

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

    public String getBackchannelDeviceRegistrationEndpoint() {
        return appConfiguration.getBackchannelDeviceRegistrationEndpoint();
    }
}