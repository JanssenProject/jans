# Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
# Copyright (c) 2020, Janssen Project
#
# Author: Milton BO
#
#

from org.gluu.oxauth.client.fcm import FirebaseCloudMessagingResponse
from org.gluu.oxauth.client.fcm import FirebaseCloudMessagingClient
from org.gluu.oxauth.client.fcm import FirebaseCloudMessagingRequest
from io.jans.as.server.util import RedirectUri
from io.jans.model.custom.script.type.ciba import EndUserNotificationType
from java.lang import String
from java.util import UUID

class EndUserNotification(EndUserNotificationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Firebase EndUserNotification script. Initializing ..."
        print "Firebase EndUserNotification script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Firebase EndUserNotification script. Destroying ..."
        print "Firebase EndUserNotification script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    # Returns boolean true or false depending on the process, if the notification
    # is sent successfully or not.
    def notifyEndUser(self, context):
        print 'Sending push notification using Firebase Cloud Messaging'
        appConfiguration = context.getAppConfiguration()
        encryptionService = context.getEncryptionService()
        clientId = appConfiguration.getBackchannelClientId()
        redirectUri = appConfiguration.getBackchannelRedirectUri()
        url = appConfiguration.getCibaEndUserNotificationConfig().getNotificationUrl()
        key = encryptionService.decrypt(appConfiguration.getCibaEndUserNotificationConfig().getNotificationKey(), True)
        to = context.getDeviceRegistrationToken()
        title = "oxAuth Authentication Request"
        body = "Client Initiated Backchannel Authentication (CIBA)"

        authorizationRequestUri = RedirectUri(appConfiguration.getAuthorizationEndpoint())
        authorizationRequestUri.addResponseParameter("client_id", clientId)
        authorizationRequestUri.addResponseParameter("response_type", "id_token")
        authorizationRequestUri.addResponseParameter("scope", context.getScope())
        authorizationRequestUri.addResponseParameter("acr_values", context.getAcrValues())
        authorizationRequestUri.addResponseParameter("redirect_uri", redirectUri)
        authorizationRequestUri.addResponseParameter("state", UUID.randomUUID().toString())
        authorizationRequestUri.addResponseParameter("nonce", UUID.randomUUID().toString())
        authorizationRequestUri.addResponseParameter("prompt", "consent")
        authorizationRequestUri.addResponseParameter("auth_req_id", context.getAuthReqId())

        clickAction = authorizationRequestUri.toString()

        firebaseCloudMessagingRequest = FirebaseCloudMessagingRequest(key, to, title, body, clickAction)
        firebaseCloudMessagingClient = FirebaseCloudMessagingClient(url)
        firebaseCloudMessagingClient.setRequest(firebaseCloudMessagingRequest)
        firebaseCloudMessagingResponse = firebaseCloudMessagingClient.exec()

        responseStatus = firebaseCloudMessagingResponse.getStatus()
        print "CIBA: firebase cloud messaging result status " + str(responseStatus)
        return (responseStatus >= 200 and responseStatus < 300 )
