# Identity Assertion Custom Script

The Jans-Auth server supports the [Identity Assertion JWT Authorization Grant (ID-JAG)](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-identity-assertion-authz-grant) profile for Cross-App Access (XAA). This script provides interception points in the ID-JAG issuance flow so operators can customize the JWT payload and the token exchange response without modifying server code.

## When This Script Runs

The script applies to **Step 1** of the ID-JAG flow — the token exchange request where a client presents an existing token (`id_token` or `refresh_token`) and receives a signed ID-JAG:

```
POST /jans-auth/restv1/token
  grant_type=urn:ietf:params:oauth:grant-type:token-exchange
  subject_token=<id_token>
  subject_token_type=urn:ietf:params:oauth:token-type:id_token
  requested_token_type=urn:ietf:params:oauth:token-type:id-jag
  audience=<Resource AS issuer URI>
```

The script is **not** called during Step 2 (JWT Bearer grant at the Resource AS).

## Interface

The Identity Assertion script implements the [IdentityAssertionType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/token/IdentityAssertionType.java) interface.

### Inherited Methods

| Method header                                                    | Method description                                                                       |
| ---------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `def init(self, customScript, configurationAttributes)`          | Called once at script initialization. Use for global setup.                              |
| `def destroy(self, configurationAttributes)`                     | Called once when the script is unloaded. Use to release resources.                       |
| `def getApiVersion(self, configurationAttributes, customScript)` | Return the API version. Include `customScript` only when the version is greater than 10. |

### New Methods

| Method header                                             | Method description                                                                                                                                                                                                          |
| --------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `def modifyIdJagPayload(self, idJagAsJwt, context)`       | Called after built-in ID-JAG claims are set but **before** the JWT is signed. Cast `idJagAsJwt` to `io.jans.as.model.jwt.Jwt` to add, remove, or modify claims. Return `True` to keep the changes; `False` to discard them. |
| `def modifyResponse(self, responseAsJsonObject, context)` | Called after the token exchange response JSON is assembled but **before** it is returned to the client. Cast `responseAsJsonObject` to `org.json.JSONObject`. Return `True` to keep the changes; `False` to discard them.   |

### Objects

| Object name            | Object description                                                                                                                                                                                                                                                    |
| ---------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `idJagAsJwt`           | `io.jans.as.model.jwt.Jwt` — the unsigned ID-JAG. Call `getClaims().setClaim(name, value)` to add claims.                                                                                                                                                             |
| `responseAsJsonObject` | `org.json.JSONObject` — the token exchange response (contains `access_token`, `issued_token_type`, `token_type`).                                                                                                                                                     |
| `context`              | [ExternalScriptContext](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java) — provides access to `getExecutionContext()` (client, HTTP request, audit log). |

## Attaching the Script to a Client

When registering or updating a client, pass the script DN in the `id_jag_script_dns` array:

```
{
  "client_name": "my-app",
  "grant_types": [
    "urn:ietf:params:oauth:grant-type:token-exchange",
    "urn:ietf:params:oauth:grant-type:jwt-bearer"
  ],
  "id_jag_script_dns": [
    "inum=ABCD-1234,ou=scripts,o=jans"
  ]
}
```

Only clients that include a script DN in `id_jag_script_dns` will trigger the script.

## Sample Demo Custom Script

### Script Type: Java

```
/*
 Copyright (c) 2025, Gluu
 Author: Yuriy Z
 */
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.token.IdentityAssertionType;
import io.jans.service.custom.script.CustomScriptManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class IdentityAssertion implements IdentityAssertionType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    /**
     * Called after ID-JAG JWT claims are populated but before the token is signed.
     * Cast idJagAsJwt to Jwt to read or modify claims.
     *
     * @param idJagAsJwt io.jans.as.model.jwt.Jwt — the unsigned ID-JAG JWT
     * @param context    io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true to keep changes, false to discard them
     */
    @Override
    public boolean modifyIdJagPayload(Object idJagAsJwt, Object context) {
        Jwt idJag = (Jwt) idJagAsJwt;
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;

        // Example: add a custom claim to the ID-JAG payload
        idJag.getClaims().setClaim("custom_idp_claim", "value_from_script");

        scriptLogger.info("modifyIdJagPayload executed for client: {}",
                scriptContext.getExecutionContext().getClient().getClientId());
        return true; // return false to discard the changes made above
    }

    /**
     * Called after the token exchange response JSON is built but before it is returned to the client.
     *
     * @param responseAsJsonObject org.json.JSONObject — the token exchange response
     * @param context              io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true to keep changes, false to discard them
     */
    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        JSONObject response = (JSONObject) responseAsJsonObject;

        // Example: add an extra field to the token exchange response
        response.put("x_custom_field", "value_from_script");

        scriptLogger.info("modifyResponse executed, response keys: {}", response.keySet());
        return true; // return false to discard the changes made above
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized IdentityAssertion Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized IdentityAssertion Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed IdentityAssertion Java custom script.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}
```
