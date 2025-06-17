---
tags:
  - administration
  - developer
  - script-catalog
---

# Client Authentication Custom Script



AS support different types of client authentications such as :

- client_secret_basic
- client_secret_post
- client_secret_jwt
- private_key_jwt

Sometimes it's convenient to customize default AS client authentication process. For this reason Client Authentication custom script was introduced. 

If script successfully authenticated client, it should return it in `authenticateClient`. 
If client is not returned then AS performs built-in authentication.

## Interface
The Client Authentication script implements the [ClientAuthnType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/clientauthn/ClientAuthnType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New methods
| Method header | Method description |
|:-----|:------|
|`def authenticateClient(self, context)`| Called when the request is received. |

`authenticateClient` method returns authenticated `Client` object or null if authentication failed.


### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalClientAuthnContext.java) |


## Sample script which demonstrates basic client authentication 

### Script Type: Java

```java
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.config.Constants;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.external.context.ExternalClientAuthnContext;
import io.jans.as.server.service.token.TokenService;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.client.ClientAuthnType;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.custom.script.CustomScriptManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Yuriy Z
 */
public class ClientAuthn implements ClientAuthnType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public Object authenticateClient(Object clientAuthnContext) {
        final ExternalClientAuthnContext context = (ExternalClientAuthnContext) clientAuthnContext;

        final HttpServletRequest request = context.getHttpRequest();
        final HttpServletResponse response = context.getHttpResponse();

        String authorization = request.getHeader(Constants.AUTHORIZATION);
        if (!StringUtils.startsWith(authorization, "Basic")) {
            context.sendUnauthorizedError();
            return null;
        }

        TokenService tokenService = CdiUtil.bean(TokenService.class);
        ClientService clientService = CdiUtil.bean(ClientService.class);

        String base64Token = tokenService.getBasicToken(authorization);
        String token = new String(Base64.decodeBase64(base64Token), StandardCharsets.UTF_8);

        int delim = token.indexOf(":");

        if (delim != -1) {
            String clientId = URLDecoder.decode(token.substring(0, delim), StandardCharsets.UTF_8);
            String clientSecret = URLDecoder.decode(token.substring(delim + 1), StandardCharsets.UTF_8);

            final boolean authenticated = clientService.authenticate(clientId, clientSecret);
            if (authenticated) {
                final Client client = clientService.getClient(clientId);
                scriptLogger.debug("Successfully performed basic client authentication, clientId: {}", clientId);
                return client;
            }
        }

        context.sendUnauthorizedError();
        return null;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized ClientAuthn Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized ClientAuthn Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed ClientAuthn Java custom script.");
        return false;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}

```


## Sample Scripts
- [ClientAuthentication](../../../script-catalog/client_authn/ClientAuthn.java)
