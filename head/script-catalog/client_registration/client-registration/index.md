# Client Registration scripts

The Jans-Auth server implements the OpenID Connect [Dynamic Client Registration](https://openid.net/specs/openid-connect-registration-1_0.html) specification. This allows developers to register a client with the Authorization Server (AS) without any intervention from the administrator. By default, all clients are given the same default scopes and attributes. Through the use of an interception script, this behavior can be modified. These scripts can be used to analyze the registration request and apply customizations to the registered client. For example, a client can be given specific scopes by analyzing the [Software Statement](https://www.rfc-editor.org/rfc/rfc7591.html#section-2.3) that is sent with the registration request.

## Behavior

By default, Jans server has no dynamic client registration scripts enabled, and all clients are given the same attributes. When a script is added and enabled, the script will run and return a `boolean` after applying logic to the registration request. If `true`, the client is registered and client credentials are returned. Generally, the script is global for all registration requests, and only one script is enabled at a time.

### Registration flow

```
sequenceDiagram
title Client Registration script
autonumber 1
RP->>Jans AS: /register request
Jans AS->>Jans AS: Is there a client registration script enabled?
Jans AS->>Jans AS: run script
note  right of Jans AS: Apply custom logic e.g <br/> Add scopes <br/> Set attributes <br/> validate SSA
Jans AS->>RP: return client credentials
```

## Interface

The client registration script implements the [ClientRegistrationType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/client/ClientRegistrationType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods

| Method header                                                    | Method description                                                                                                                                                                                         |
| ---------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `def init(self, customScript, configurationAttributes)`          | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc                                                                    |
| `def destroy(self, configurationAttributes)`                     | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method                                                                                   |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New methods

| Method header                                                          | Method description                                                                                                 |
| ---------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| `def createClient(self, context)`                                      | Called when the dynamic client registration request is received.                                                   |
| `def updateClient(self, context)`                                      | Called when the PUT method is called on registration endpoint to update client details.                            |
| `def getSoftwareStatementHmacSecret(self, context)`                    | Returns secret key which will be used to validate Software Statement if HMAC algorithm is used (e.g. HS256, HS512) |
| `def getSoftwareStatementJwks(self, context)`                          | Returns JWKS which will be used to validate Software Statement if keys are used (e.g. RS256)                       |
| `def modifyPutResponse(self, responseAsJsonObject, executionContext)`  | Modifies the response from the PUT request to registration endpoint                                                |
| `def modifyReadResponse(self, responseAsJsonObject, executionContext)` | Modifies the response from the GET request to registration endpoint                                                |
| `def modifyPostResponse(self, responseAsJsonObject, executionContext)` | Modifies the response from the POST request to registration endpoint                                               |

### Objects

| Object name               | Object description                                                                                                                                                                   |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `customScript`            | The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java)           |
| `configurationAttributes` | `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes`                                                           |
| `SimpleCustomProperty`    | Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java)                  |
| `context`                 | [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/DynamicClientRegistrationContext.java) |
| `responseAsJsonObject`    | Java JSONObject. [Reference](https://docs.oracle.com/javaee/7/api/javax/json/JsonObject.html)                                                                                        |
| `executionContext`        | [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java)                             |

## Common Use Case: Adding scopes defined in the script configuration parameters

### Script Type: Python

```
# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Janssen
#
# Author: Yuriy Movchan
#

from io.jans.model.custom.script.type.client import ClientRegistrationType
from io.jans.service.cdi.util import CdiUtil
from io.jans.as.service import ScopeService
from io.jans.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList, HashSet

import java

class ClientRegistration(ClientRegistrationType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Client registration. Initialization"

        self.clientRedirectUrisSet = self.prepareClientRedirectUris(configurationAttributes)

        print "Client registration. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Client registration. Destroy"
        print "Client registration. Destroyed successfully"
        return True   

    # Update client entry before persistent it
    # context refers to io.jans.as.server.service.external.context.DynamicClientRegistrationContext - see https://github.com/JanssenProject/jans-auth-server/blob/vreplace-janssen-version/server/src/main/java/io/jans/as/server/service/external/context/DynamicClientRegistrationContext.java#L24
    def createClient(self, context):
        print "Client registration. CreateClient method"
        registerRequest = context.getRegisterRequest()
        configurationAttributes = context.getConfigurationAttibutes()
        client = context.getClient()

        redirectUris = client.getRedirectUris()
        print "Client registration. Redirect Uris: %s" % redirectUris

        addAddressScope = False
        for redirectUri in redirectUris:
            if (self.clientRedirectUrisSet.contains(redirectUri)):
                addAddressScope = True
                break

        print "Client registration. Is add address scope: %s" % addAddressScope

        if addAddressScope:
            currentScopes = client.getScopes()
            print "Client registration. Current scopes: %s" % currentScopes

            scopeService = CdiUtil.bean(ScopeService)
            addressScope = scopeService.getScopeByDisplayName("address")
            newScopes = ArrayHelper.addItemToStringArray(currentScopes, addressScope.getDn())

            print "Client registration. Result scopes: %s" % newScopes
            client.setScopes(newScopes)

        return True

    # Update client entry before persistent it
    # context refers to io.jans.as.server.service.external.context.DynamicClientRegistrationContext - see https://github.com/JanssenProject/jans-auth-server/blob/vreplace-janssen-version/server/src/main/java/io/jans/as/server/service/external/context/DynamicClientRegistrationContext.java#L24
    def updateClient(self, context):
        print "Client registration. UpdateClient method"
        return True

    def getApiVersion(self):
        return 11

    def prepareClientRedirectUris(self, configurationAttributes):
        clientRedirectUrisSet = HashSet()
        if not configurationAttributes.containsKey("client_redirect_uris"):
            return clientRedirectUrisSet

        clientRedirectUrisList = configurationAttributes.get("client_redirect_uris").getValue2()
        if StringHelper.isEmpty(clientRedirectUrisList):
            print "Client registration. The property client_redirect_uris is empty"
            return clientRedirectUrisSet    

        clientRedirectUrisArray = StringHelper.split(clientRedirectUrisList, ",")
        if ArrayHelper.isEmpty(clientRedirectUrisArray):
            print "Client registration. No clients specified in client_redirect_uris property"
            return clientRedirectUrisSet

        # Convert to HashSet to quick search
        i = 0
        count = len(clientRedirectUrisArray)
        while i < count:
            uris = clientRedirectUrisArray[i]
            clientRedirectUrisSet.add(uris)
            i = i + 1

        return clientRedirectUrisSet

    # Returns secret key which will be used to validate Software Statement if HMAC algorithm is used (e.g. HS256, HS512). Invoked if oxauth conf property softwareStatementValidationType=SCRIPT which is default/fallback value.
    # context is reference of io.jans.as.service.external.context.DynamicClientRegistrationContext (in https://github.com/JanssenFederation/oxauth project )
    def getSoftwareStatementHmacSecret(self, context):
        return ""

    # Returns JWKS which will be used to validate Software Statement if keys are used (e.g. RS256). Invoked if oxauth conf property softwareStatementValidationType=SCRIPT which is default/fallback value.
    # context is reference of io.jans.as.service.external.context.DynamicClientRegistrationContext (in https://github.com/JanssenFederation/oxauth project )
    def getSoftwareStatementJwks(self, context):
        return ""

    # cert - java.security.cert.X509Certificate
    # context refers to io.jans.as.server.service.external.context.DynamicClientRegistrationContext - see https://github.com/JanssenProject/jans-auth-server/blob/vreplace-janssen-version/server/src/main/java/io/jans/as/server/service/external/context/DynamicClientRegistrationContext.java#L24
    def isCertValidForClient(self, cert, context):
        return False

        # responseAsJsonObject - is org.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.server.model.common.ExecutionContext
    def modifyPutResponse(self, responseAsJsonObject, executionContext):
        return False

    # responseAsJsonObject - is org.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.server.model.common.ExecutionContext
    def modifyReadResponse(self, responseAsJsonObject, executionContext):
        return False

    # responseAsJsonObject - is org.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.server.model.common.ExecutionContext
    def modifyPostResponse(self, responseAsJsonObject, executionContext):
        return False
```

### Script Type: Java

```
import io.jans.as.common.model.registration.Client;
import io.jans.as.persistence.model.Scope;
import io.jans.as.server.service.ScopeService;
import io.jans.as.server.service.external.context.DynamicClientRegistrationContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.client.ClientRegistrationType;
import io.jans.orm.util.ArrayHelper;
import io.jans.service.cdi.util.CdiUtil;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ClientRegistration implements ClientRegistrationType {
    private static final Logger log = LoggerFactory.getLogger(ClientRegistration.class);

    JSONArray json_array = null;

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        if (!configurationAttributes.containsKey("scope_list")) {
            log.info("Client registration. Initialization failed. Scope List not found.");
            return false;
        }
        String scope_list = configurationAttributes.get("scope_list").getValue2();
        json_array = new JSONArray(scope_list);
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Client registration. Initialization.");
        log.info(configurationAttributes.toString());
        if (!configurationAttributes.containsKey("scope_list")) {
            log.info("Client registration. Initialization failed. Scope List not found.");
            return false;
        }
        String scope_list = configurationAttributes.get("scope_list").getValue2();
        json_array = new JSONArray(scope_list);
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Client registration. Destroy.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }

    @Override
    public boolean createClient(Object context) {
        log.info("Client registration. CreateClient method");
        DynamicClientRegistrationContext regContext = (DynamicClientRegistrationContext) context;
        Client client = regContext.getClient();
        ScopeService scopeService = CdiUtil.bean(ScopeService.class);

        String[] currentScopes = client.getScopes();
        String[] newScopes = currentScopes.clone();

        for (int i = 0; i < json_array.length(); i++) {
            String scopeName = (String) json_array.get(i);
            Scope foundScope = scopeService.getScopeById(scopeName);
            if (foundScope == null) {
                log.info("Client registration. Scope not found");
                return false;
            }
            newScopes = ArrayHelper.addItemToStringArray(newScopes, foundScope.getDn());
        }

        client.setScopes(newScopes);
        log.info("Client registration. Scopes added.");

        return true;
    }

    @Override
    public boolean updateClient(Object context) {
        return true;
    }

    // This method needs to be overridden if client is providing an SSA with HMAC
    @Override
    public String getSoftwareStatementHmacSecret(Object context) {
        return "";
    }

    // This method needs to be overridden if client is providing an SSA and RS256 validation
    @Override
    public String getSoftwareStatementJwks(Object context) {
        return "";
    }

    @Override
    public String getDcrHmacSecret(Object o) {
        return "";
    }

    @Override
    public String getDcrJwks(Object o) {
        return "";
    }

    @Override
    public boolean isCertValidForClient(Object o, Object o1) {
        return false;
    }

    @Override
    public boolean modifyPutResponse(Object responseAsJsonObject, Object executionContext) {
        return true;
    }

    @Override
    public boolean modifyReadResponse(Object responseAsJsonObject, Object executionContext) {
        return true;
    }

    @Override
    public boolean modifyPostResponse(Object responseAsJsonObject, Object executionContext) {
        return true;
    }
}
```

### Sample Scripts

- [OpenBanking](https://docs.jans.io/head/script-catalog/client_registration/OpenBanking/client-registration/index.md)
