/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ciba;

import io.jans.as.client.ciba.fcm.FirebaseCloudMessagingClient;
import io.jans.as.client.ciba.fcm.FirebaseCloudMessagingRequest;
import io.jans.as.client.ciba.fcm.FirebaseCloudMessagingResponse;
import io.jans.as.common.util.RedirectUri;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.ciba.CibaEncryptionService;
import io.jans.as.server.service.external.ExternalCibaEndUserNotificationService;
import io.jans.as.server.service.external.context.ExternalCibaEndUserNotificationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.UUID;

import static io.jans.as.model.authorize.AuthorizeRequestParam.ACR_VALUES;
import static io.jans.as.model.authorize.AuthorizeRequestParam.AUTH_REQ_ID;
import static io.jans.as.model.authorize.AuthorizeRequestParam.CLIENT_ID;
import static io.jans.as.model.authorize.AuthorizeRequestParam.NONCE;
import static io.jans.as.model.authorize.AuthorizeRequestParam.PROMPT;
import static io.jans.as.model.authorize.AuthorizeRequestParam.REDIRECT_URI;
import static io.jans.as.model.authorize.AuthorizeRequestParam.RESPONSE_TYPE;
import static io.jans.as.model.authorize.AuthorizeRequestParam.SCOPE;
import static io.jans.as.model.authorize.AuthorizeRequestParam.STATE;

/**
 * @author Javier Rojas Blum
 * @version October 7, 2019
 */
@Stateless
@Named
public class CIBAEndUserNotificationService {

    private final static Logger log = LoggerFactory.getLogger(CIBAEndUserNotificationService.class);

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private CibaEncryptionService cibaEncryptionService;

    @Inject
    private ExternalCibaEndUserNotificationService externalCibaEndUserNotificationService;

    public void notifyEndUser(String scope, String acrValues, String authReqId, String deviceRegistrationToken) {
        try {
            if (externalCibaEndUserNotificationService.isEnabled()) {
                log.debug("CIBA: Authorization request sending to the end user with custom interception scripts");
                ExternalCibaEndUserNotificationContext context = new ExternalCibaEndUserNotificationContext(scope,
                        acrValues, authReqId, deviceRegistrationToken, appConfiguration, cibaEncryptionService);
                log.info("CIBA: Notification sent to the end user, result {}",
                        externalCibaEndUserNotificationService.executeExternalNotifyEndUser(context));
            } else {
                this.notifyEndUserUsingFCM(scope, acrValues, authReqId, deviceRegistrationToken);
            }
        } catch (Exception e) {
            log.info("Error when it was sending the notification to the end user to validate the Ciba authorization", e);
        }
    }

    /**
     * Method responsible to send notifications to the end user using Firebase Cloud Messaging.
     *
     * @param deviceRegistrationToken Device already registered.
     * @param scope                   Scope of the authorization request
     * @param acrValues               Acr values used to the authorzation request
     * @param authReqId               Authentication request id.
     */
    private void notifyEndUserUsingFCM(String scope, String acrValues, String authReqId, String deviceRegistrationToken) {
        String clientId = appConfiguration.getBackchannelClientId();
        String redirectUri = appConfiguration.getBackchannelRedirectUri();
        String url = appConfiguration.getCibaEndUserNotificationConfig().getNotificationUrl();
        String key = cibaEncryptionService.decrypt(appConfiguration.getCibaEndUserNotificationConfig()
                .getNotificationKey(), true);
        String title = "Jans Auth Authentication Request";
        String body = "Client Initiated Backchannel Authentication (CIBA)";

        RedirectUri authorizationRequestUri = new RedirectUri(appConfiguration.getAuthorizationEndpoint());
        authorizationRequestUri.addResponseParameter(CLIENT_ID, clientId);
        authorizationRequestUri.addResponseParameter(RESPONSE_TYPE, "id_token");
        authorizationRequestUri.addResponseParameter(SCOPE, scope);
        authorizationRequestUri.addResponseParameter(ACR_VALUES, acrValues);
        authorizationRequestUri.addResponseParameter(REDIRECT_URI, redirectUri);
        authorizationRequestUri.addResponseParameter(STATE, UUID.randomUUID().toString());
        authorizationRequestUri.addResponseParameter(NONCE, UUID.randomUUID().toString());
        authorizationRequestUri.addResponseParameter(PROMPT, "consent");
        authorizationRequestUri.addResponseParameter(AUTH_REQ_ID, authReqId);

        String clickAction = authorizationRequestUri.toString();

        FirebaseCloudMessagingRequest firebaseCloudMessagingRequest = new FirebaseCloudMessagingRequest(key, deviceRegistrationToken, title, body, clickAction);
        FirebaseCloudMessagingClient firebaseCloudMessagingClient = new FirebaseCloudMessagingClient(url);
        firebaseCloudMessagingClient.setRequest(firebaseCloudMessagingRequest);
        FirebaseCloudMessagingResponse firebaseCloudMessagingResponse = firebaseCloudMessagingClient.exec();

        log.debug("CIBA: firebase cloud messaging result status {}", firebaseCloudMessagingResponse.getStatus());
    }

}