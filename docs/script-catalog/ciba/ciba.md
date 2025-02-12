---
tags:
  - administration
  - developer
  - script-catalog
---

# CIBA End User Notification Script (EndUserNotification)

## Overview

The Jans-Auth server implements [OAuth 2.0 Rich Authorization Requests](https://datatracker.ietf.org/doc/html/rfc9396).
This script is used to control/customize cache refresh.



## Interface
The CIBA end user script implements the [EndUserNotificationType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/ciba/EndUserNotificationType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New methods
| Method header | Method description |
|:-----|:------|
|`notifyEndUser(self, context)`| Returns boolean true or false depending on the process, if the notification is sent successfully or not|

### Script Type: Python

```
from io.jans.as.client.fcm import FirebaseCloudMessagingResponse
from io.jans.as.client.fcm import FirebaseCloudMessagingClient
from io.jans.as.client.fcm import FirebaseCloudMessagingRequest
from io.jans.as.util import RedirectUri
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

```


## Sample Scripts
- [EndUserNotification](../../../script-catalog/ciba/end-user-notification/end_user_notification.py)


## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).