---
tags:
  - administration
  - developer
  - script-catalog
  - fido2
  - Fido2Interception
---


# Fido2 Extension


FIDO2 implements attestation and assertion with next endpoints:

```
https://<myjans-server>/jans-fido2/restv1/attestation/options   (register)
https://<myjans-server>/jans-fido2/restv1/attestation/result   (verify registration)
https://<myjans-server>/jans-fido2/restv1/assertion/options   (authenticate)
https://<myjans-server>/jans-fido2/restv1/assertion/result   (verify authentication)
```
each one can be customized just in the moment the endpoint is called, just before to execute the method as usual. You could execute validations, verifications, and modify the params received.

## Interface
The Fido2 extension script implements the [Fido2ExtensionType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/fido2/Fido2ExtensionType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods
| Method header | Method description |
|:-----|:------|
| `def registerAttestationStart(self, paramAsJsonNode, context)` | This method is called in Attestation register endpoint before start the registration process. Method 'throwBadRequestException' could be used to create a BadRequest Exception Response. |
| `def registerAttestationFinish(self, paramAsJsonNode, context)` | This method is called in Attestation register endpoint after start the registration process. Method 'throwBadRequestException' could be used to create a BadRequest Exception Response. |
| `def verifyAttestationStart(self, paramAsJsonNode, context)` | This method is called in Attestation verify endpoint before finish the registration verification process. Method 'throwBadRequestException' could be used to create a BadRequest Exception Response. |
| `def verifyAttestationFinish(self, paramAsJsonNode, context)` | This method is called in Attestation verify endpoint after finish the registration verification process. Method 'throwBadRequestException' could be used to create a BadRequest Exception Response. |
| `def authenticateAssertionStart(self, paramAsJsonNode, context)` | This method is called in Assertion authenticate endpoint before start the authentication process. Method 'throwBadRequestException' could be used to create a BadRequest Exception Response. |
| `def authenticateAssertionFinish(self, paramAsJsonNode, context)` | This method is called in Assertion authenticate endpoint after start the authentication process. Method 'throwBadRequestException' could be used to create a BadRequest Exception Response. |
| `def verifyAssertionStart(self, paramAsJsonNode, context)` | This method is called in Assertion verify endpoint before finish the authentication verification process. Method 'throwBadRequestException' could be used to create a BadRequest Exception Response. |
| `def verifyAssertionFinish(self, paramAsJsonNode, context)` | This method is called in Assertion verify endpoint after finish the authentication verification process. Method 'throwBadRequestException' could be used to create a BadRequest Exception Response. |

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`SimpleCustomProperty`| Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java) |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-fido2/server/src/main/java/io/jans/fido2/service/external/context/ExternalFido2Context.java) |

## Use case: Dummy Fido2 Interception Form
This script has been adapted from the Jans Server [sample fido2 interception script](https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/fido2_extension/Fido2ExtensionSample.py).
### Script Type: Python
```python
--8<-- "script-catalog/fido2_extension/Fido2ExtensionSample.py"
```

### Script Type: Java
```java
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.fido2.Fido2ExtensionType;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.fido2.service.external.context.ExternalFido2Context;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.ThreadContext;
import org.jboss.resteasy.spi.NoLogWebApplicationException;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fido2Extension implements Fido2ExtensionType {

    private static final Logger log = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Fido2Interception Script. Initializing...");
        log.info("Fido2Interception Script. Initialized");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Fido2Interception Script. Initializing...");
        log.info("Fido2Interception Script. Initialized");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Fido2Interception Script. Destroying...");
        log.info("Fido2Interception Script. Destroyed.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }

    @Override
    public boolean registerAttestationStart(JsonNode paramsAsJsonNode, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception Script. Attestation Register (start)...");

        return true;
    }

    @Override
    public boolean registerAttestationFinish(JsonNode paramsAsJsonNode, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception Script. Attestation Register (finish)...");

        return true;
    }

    @Override
    public boolean verifyAttestationStart(JsonNode paramsAsJsonNode, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception Script. Attestation Verify (start)...");

        return true;
    }

    @Override
    public boolean verifyAttestationFinish(JsonNode paramsAsJsonNode, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception Script. Attestation Verify (finish)...");

        return true;
    }

    @Override
    public boolean authenticateAssertionStart(JsonNode paramsAsJsonNode, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception Script. Assertion Authenticate (start)...");

        JsonNode jsonNodeUser = paramsAsJsonNode.get("username");
        if (jsonNodeUser != null) {
            log.info("Fido2Interception. printing username: {}", jsonNodeUser.asText());
            if (jsonNodeUser.asText().equals("test_user")) {
                throwBadRequestException("Fido2Interception interceptAuthenticateAssertion : test_user", "Description Error from script : test_user", context);
            }
        }

        return true;
    }

    @Override
    public boolean authenticateAssertionFinish(JsonNode paramsAsJsonNode, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception Script. Assertion Authenticate (finish)...");

        return true;
    }

    @Override
    public boolean verifyAssertionStart(JsonNode paramsAsJsonNode, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception Script. Assertion Verify (start)...");

        return true;
    }

    @Override
    public boolean verifyAssertionFinish(JsonNode paramsAsJsonNode, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception Script. Assertion Verify (finish)...");

        return true;
    }

    private void throwBadRequestException(String title, String message, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception. setting Bad request exception");
        WebApplicationException errorClaimException = Fido2ErrorResponseFactory.createBadRequestException(Fido2ErrorResponseType.BAD_REQUEST_INTERCEPTION, title, message, ThreadContext.get(Constants.CORRELATION_ID_HEADER));
        context.setWebApplicationException((NoLogWebApplicationException) errorClaimException);
    }
}
```
