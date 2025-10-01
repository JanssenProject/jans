---
tags:
  - administration
  - developer
  - scripts
---

# Authorization Detail Custom Script (AuthzDetail)



The Jans-Auth server implements [OAuth 2.0 Rich Authorization Requests](https://datatracker.ietf.org/doc/html/rfc9396).
This script is used to control/customize single authorization detail from `authorization_details` array.

## Behavior

In request to Authorization Endpoint and to Token Endpoint RP can specify `authorization_details` request parameter which specifies JSON array.

```json
[
   {
      "type": "demo_authz_detail",
      "actions": [
          "list_accounts",
          "read_balances"
      ],
      "locations": [
          "https://example.com/accounts"
      ],
      "ui_representation": "Read balances and list accounts at https://example.com/accounts"
   },
   {
       "type":"financial-transaction",
       "actions":[
           "withdraw"
       ],
       "identifier":"account-14-32-32-3",
       "currency":"USD",
       "ui_representation": "Withdraw money from account-14-32-32-3"
   }
]
```

`type` defines type of authorization detail. Each such type is represented by AS `AuthzDetailType` custom scripts.
It means that for example above administrator must define two `AuthzDetailType` custom scripts with names: `demo_authz_detail` and `financial-transaction`. 

If `authorization_details` parameter is absent in request then `AuthzDetailType` custom scripts are not invoked. 

`demo_authz_detail` and `financial-transaction` `AuthzDetailType` custom scripts must be provided by administrator.

- `demo_authz_detail` is called for all authorization details with `"type": "demo_authz_detail"`
- `financial-transaction` is called for all authorization details with `"type": "financial-transaction"`

Sample Authorization Request
```
POST /jans-auth/restv1/authorize HTTP/1.1
Host: yuriyz-fond-skink.gluu.info

response_type=code&client_id=7a29bf35-96ec-4bbd-a05c-15e1ff9f07cc&scope=openid+profile+address+email+phone+user_name&redirect_uri=https%3A%2F%2Fyuriyz-relaxed-jawfish.gluu.info%2Fjans-auth-rp%2Fhome.htm&state=6cdc7701-178c-4653-adac-5c1e9c6c4aba&nonce=b9a1ecc4-548e-475c-8b29-f019417e1aef&prompt=&ui_locales=&claims_locales=&acr_values=&request_session_id=false&authorization_details=%5B%0A++%7B%0A++++%22type%22%3A+%22demo_authz_detail%22%2C%0A++++%22actions%22%3A+%5B%0A++++++%22list_accounts%22%2C%0A++++++%22read_balances%22%0A++++%5D%2C%0A++++%22locations%22%3A+%5B%0A++++++%22https%3A%2F%2Fexample.com%2Faccounts%22%0A++++%5D%2C%0A++++%22ui_representation%22%3A+%22Read+balances+and+list+accounts+at+https%3A%2F%2Fexample.com%2Faccounts%22%0A++%7D%0A%5D
```

## Interface
The Authorization Details script implements the [AuthzDetailType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/authzdetails/AuthzDetailType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New methods
| Method header | Method description |
|:-----|:------|
|`def validateDetail(self, context)`| Called when the request is received. Method validates single authorization detail from `authorization_details`. |
|`def getUiRepresentation(self, context)`| Called when single authorization detail from `authorization_details` has to be represented on UI as string. For example on authorization page. |

`validateDetail` method returns true/false which indicates to server whether the validation of single authorization detail (from `authorization_details` array) is passed or failed.
If at least one element from `authorization_details` array fails validation error is returned by AS.

`getUiRepresentation` method returns string and represents single authorization detail as string on UI. Authorization detail can have "ui_representation" json key which makes implementation as simple as following:

```java
 @Override
    public String getUiRepresentation(Object scriptContext) {
        ExternalScriptContext context = (ExternalScriptContext) scriptContext;
        return context.getAuthzDetail().getJsonObject().optString("ui_representation");
    }
```


### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java) |

- Get Authz Detail - `context.getAuthzDetail()`
- Get Authz Detail Type - `context.getAuthzDetail().getType()`
- Get Authz Detail JSON Object (`org.json.JSONObject`) for manipulation - `context.getAuthzDetail().getJsonObject()`
- Get full HTTP Request - `context.getHttpRequest()`


## Simple Use Case: validate authz details is present and return string representation 

### Script Type: Java

```java
/*
 Copyright (c) 2023, Gluu
 Author: Yuriy Z
 */

import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.authzdetails.AuthzDetailType;
import io.jans.service.custom.script.CustomScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class AuthzDetail implements AuthzDetailType {

    private static final Logger log = LoggerFactory.getLogger(AuthzDetail.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    /**
     * All validation logic of single authorization detail must take place in this method.
     * If method returns "false" AS returns error to RP. If "true" processing of request goes on.
     *
     * @param scriptContext script context. Authz detail can be taken as "context.getAuthzDetail()".
     * @return whether single authorization detail is valid or not
     */
    @Override
    public boolean validateDetail(Object scriptContext) {
        ExternalScriptContext context = (ExternalScriptContext) scriptContext;
        return context.getAuthzDetail() != null;
    }

    /**
     * Method returns single authorization detail string representation which is shown on authorization page by AS.
     *
     * @param scriptContext script context. Authz detail can be taken as "context.getAuthzDetail()".
     * @return returns single authorization details string representation which is shown on authorization page by AS.
     */
    @Override
    public String getUiRepresentation(Object scriptContext) {
        ExternalScriptContext context = (ExternalScriptContext) scriptContext;
        return context.getAuthzDetail().getJsonObject().optString("ui_representation");
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized AuthzDetail Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized AuthzDetail Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed AuthzDetail Java custom script.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}

```


## Sample Scripts
- [AuthzDetails](../../../script-catalog/authz_detail/AuthzDetail.java)
