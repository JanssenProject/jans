---
tags:
  - administration
  - developer
  - scripts
  - fido2
  - Fido2Interception
---

## Overview
FIDO2 implements attestation and assertion with next endpoints:  

```
https://<myjans-server>/jans-fido2/restv1/attestation/options   (register) 
https://<myjans-server>/jans-fido2/restv1/attestation/result   (verify registration)
https://<myjans-server>/jans-fido2/restv1/assertion/options   (authenticate)
https://<myjans-server>/jans-fido2/restv1/assertion/result   (verify authentication)
```
each one can be customized just in the moment the endpoint is called, just before to execute the method as usual. You could execute validations, verifications, and modify the params received.

## Interface
The Fido2 interception script implements the [Fido2Interception](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/authz/Fido2InterceptionType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods
| Method header | Method description |
|:-----|:------|
| `def interceptRegisterAttestation(self, step, consentContext)` | Main consent-gather method. Must return True (if consent gathered successfully) or False (if failed). |
| `def interceptVerifyAttestation(self, step, context)` |  |
| `def interceptAuthenticateAssertion(self, context)` | Return total number of consent gathering steps |
| `def interceptVerifyAssertion(self, step, context)` | Returns the consent page corresponding to the current step of consent gathering |

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`SimpleCustomProperty`| Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java) |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/fido2/service/external/context/ExternalFido2InterceptionContext.java) |

## Use case: Dummy Fido2 Interception Form
This script has been adapted from the Jans Server [sample fido2 interception script](https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/fido2_interception/fido2-interception/Fido2Interception_Script.py).
### Script Type: Python
```python
from io.jans.service.cdi.util import CdiUtil
from io.jans.model.custom.script.type.auth import Fido2InterceptionType
from io.jans.fido2.service.operation import AttestationService
from io.jans.util import StringHelper

import java

class Fido2Interception(Fido2InterceptionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript,  configurationAttributes):
        print "Fido2Interception. Initialization"
        print "Fido2Interception. Initialized successfully"
        return True   

    def destroy(self, configurationAttributes):
        print "Fido2Interception. Destroy"
        print "Fido2Interception. Destroyed successfully"
        return True
               
    def getApiVersion(self):
        return 11

    #This method is called during Attestation register endpoint, just before to start the registration process
    def interceptRegisterAttestation(self, paramAsJsonNode, context):
	print "Fido2Interception. interceptRegisterAttestation"
	attestationService = CdiUtil.bean(AttestationService)
        return True
	
    #This method is called during Attestation verify enpoint, just before to start the verification process	
    def interceptVerifyAttestation(self, paramAsJsonNode, context):
	print "Fido2Interception. interceptVerifyAttestation"
	attestationService = CdiUtil.bean(AttestationService)
        return True
	
    #This method is called during Assertion authenticate enpoint, just before to start the authentication process		
    def interceptAuthenticateAssertion(self, paramAsJsonNode, context):
	print "Fido2Interception. interceptAuthenticateAssertion"
	assertionService = CdiUtil.bean(AssertionService)
        return True
	
    #This method is called during Assertion verify enpoint, just before to start the verification process		
    def interceptVerifyAssertion(self, paramAsJsonNode, context):
	print "Fido2Interception. interceptVerifyAssertion"
	assertionService = CdiUtil.bean(AssertionService)
        return True
```

### Script Type: Java
```java
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.auth.Fido2InterceptionType;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.fido2.service.external.context.ExternalFido2InterceptionContext;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Fido2Interception implements Fido2InterceptionType {

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
    public boolean interceptRegisterAttestation(JsonNode paramsAsJsonNode, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception Script. Attestation Register...");
        
        return false;
    }

    @Override
    public boolean interceptVerifyAttestation(JsonNode paramsAsJsonNode, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception Script. Attestation Register...");
        
        return false;
    }

    @Override
    public boolean interceptAuthenticateAssertion(JsonNode paramsAsJsonNode, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception Script. Assertion Authenticate...");
        
        return false;
    }
    
    @Override
    public boolean interceptVerifyAssertion(JsonNode paramsAsJsonNode, ExternalFido2InterceptionContext context) {
        log.info("Fido2Interception Script. Assertion Verify...");
        
        return false;
    }
}
```
