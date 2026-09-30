# Post Authentication

After the browser has a session, if a person visits the website, the requesting party can obtain a code without the user having to authenticate or authorize. In some cases, it is desirable to insert custom business logic before granting the code or tokens from the authorization endpoint. Post Authentication script allows to force re-authentication or re-authorization (even if client is "Pre-authorized" or client authorization persistence is on).

## Interface

The post authentication script implements the [PostAuthnType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/postauthn/PostAuthnType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods

| Method header                                                    | Method description                                                                                                                                                                                         |
| ---------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `def init(self, customScript, configurationAttributes)`          | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc                                                                    |
| `def destroy(self, configurationAttributes)`                     | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method                                                                                   |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods

| Method header                              | Method description                                                                                                                                                                                                                                                                                                                            |
| ------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `def forceReAuthentication(self, context)` | This method is called during Authorization Request at Authorization Endpoint. If True is returned, session is set as unauthenticated and user is send for authentication.                                                                                                                                                                     |
| `def forceAuthorization(self, context)`    | This method is called during Authorization Request at Authorization Endpoint. If True is returned user is send for Authorization. By default if client is "Pre-Authorized" or "Client Persist Authorizations" is on, authorization is skipped. This script has higher priority and can cancel Pre-Authorization and persisted authorizations. |

### Objects

| Object name               | Object description                                                                                                                                                           |
| ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `customScript`            | The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java)   |
| `configurationAttributes` | `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes`                                                   |
| `SimpleCustomProperty`    | Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java)          |
| `context`                 | [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalPostAuthnContext.java) |

## Use case: Dummy Post Authentication script (does not force re-authentication)

This was adapted from [Gluu Post Authentication script example](https://github.com/GluuFederation/oxAuth/blob/master/Server/integrations/postauthn/postauthn.py).

### Script Type: Python

```
# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2020, Janssen
#
# Author: Yuriy Zabrovarnyy
#
#

from io.jans.model.custom.script.type.postauthn import PostAuthnType
from java.lang import String

class PostAuthn(PostAuthnType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Post Authn script. Initializing ..."
        print "Post Authn script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Post Authn script. Destroying ..."
        print "Post Authn script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # This method is called during Authorization Request at Authorization Endpoint.
    # If True is returned, session is set as unauthenticated and user is send for authentication.
    # Note :
    # context is reference of io.jans.as.service.external.context.ExternalPostAuthnContext(in https://github.com/JanssenFederation/oxauth project, )
    def forceReAuthentication(self, context):
        return False

    # This method is called during Authorization Request at Authorization Endpoint.
    # If True is returned user is send for Authorization. By default if client is "Pre-Authorized" or "Client Persist Authorizations" is on, authorization is skipped.
    # This script has higher priority and can cancel Pre-Authorization and persisted authorizations.
    # Note :
    # context is reference of io.jans.as.service.external.context.ExternalPostAuthnContext(in https://github.com/JanssenFederation/oxauth project, )
    # To get session object just call context.getSession()
    def forceAuthorization(self, context):
        return False
```

### Script Type: Java

```
import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.postauthn.PostAuthnType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostAuthn implements PostAuthnType {

        private static final Logger log = LoggerFactory.getLogger(PostAuthn.class);

        @Override
        public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Post Authentication. Initializing...");
        log.info("Post Authentication. Initialized");
        return true;
        }

        @Override
        public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Post Authentication. Initializing...");
        log.info("Post Authentication. Initialized");
        return true;
        }

        @Override
        public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Post Authentication. Destroying...");
        log.info("Post Authentication. Destroyed.");
        return true;
        }

        @Override
        public int getApiVersion() {
                return 11;
        }

        @Override
        public boolean forceReAuthentication(Object context) {
                return false;
        }

        @Override
        public boolean forceAuthorization(Object context) {
                return false;
        }

}
```
