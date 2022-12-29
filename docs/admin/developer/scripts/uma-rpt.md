---
tags:
  - administration
  - developer
  - scripts
---

## Overview
This is a special script for UMA. It allows an admin to protect UMA scopes with policies. It is possible to add more than one UMA policy to an UMA scope. On requesting access to a specified resource, the application should call specified UMA policies in order to grant or deny access.

## Interface
The UMA RPT Authorization Policy script implements the [UmaRptPolicyType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/uma/UmaRptPolicyType.java) interface. This extends methods from the base script type in addition to adding new method:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods
| Method header | Method description |
|:-----|:------|
| `def getRequiredClaims(self, authorizationContext)` | Returns required claims definitions. This method must provide definition of all claims that is used in 'authorize' method. Return empty array `[]` if no claims should be gathered. Note : name in both places must match. `%1$s` - placeholder for issuer. It uses standard Java Formatter, docs : https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html |
| `def authorize(self, authorizationContext)` | Main authorization method. Must return True or False. |
| `def getClaimsGatheringScriptName(self, authorizationContext)` | Returns name of the Claims-Gathering script which will be invoked if need_info error is returned. Return blank/empty string if claims gathering flow is not involved. |

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`SimpleCustomProperty`| Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java) |
| `context` | [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/uma/authorization/UmaAuthorizationContext.java)
| `ClaimDefinition` | [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/model/src/main/java/io/jans/model/uma/ClaimDefinition.java) |

## Use case: Request Country and City Policies
This script was adapted from the Gluu Server [UMA RPT Authorization Script](https://gluu.org/docs/gluu-server/4.4/admin-guide/sample-uma-authorization-script.py).

### Script Type: Python
```python
from io.jans.model.custom.script.type.uma import UmaRptPolicyType
from io.jans.model.uma import ClaimDefinitionBuilder
from java.lang import String

class UmaRptPolicy(UmaRptPolicyType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "RPT Policy. Initializing ..."
        print "RPT Policy. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "RPT Policy. Destroying ..."
        print "RPT Policy. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 1

    # Returns required claims definitions.
    # This method must provide definition of all claims that is used in 'authorize' method.
    # Return empty array `[]` if no claims should be gathered.
    # Note : name in both places must match.
    # %1$s - placeholder for issuer. It uses standard Java Formatter, docs : https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html
    def getRequiredClaims(self, context): 
        json = """[
        {
            "issuer" : [ "%1$s" ],
            "name" : "country",
            "claim_token_format" : [ "http://openid.net/specs/openid-connect-core-1_0.html#IDToken" ],
            "claim_type" : "string",
            "friendly_name" : "country"
        },
        {
            "issuer" : [ "%1$s" ],
            "name" : "city",
            "claim_token_format" : [ "http://openid.net/specs/openid-connect-core-1_0.html#IDToken" ],
            "claim_type" : "string",
            "friendly_name" : "city"
        }
        ]"""
        context.addRedirectUserParam("customUserParam1", "value1") # pass some custom parameters to need_info uri. It can be removed if you don't need custom parameters.
        return ClaimDefinitionBuilder.build(String.format(json, context.getIssuer()))

    # Main authorization method. Must return True or False.
    def authorize(self, context): 
        print "RPT Policy. Authorizing ..."

        if context.getClaim("country") == 'US' and context.getClaim("city") == 'NY':
            print "Authorized successfully!"
            return True

        return False

    # Returns name of the Claims-Gathering script which will be invoked if need_info error is returned. Return blank/empty string if claims gathering flow is not involved.
    def getClaimsGatheringScriptName(self, context): 
        context.addRedirectUserParam("customUserParam2", "value2") # pass some custom parameters to need_info uri. It can be removed if you don't need custom parameters.
        return "sampleClaimsGathering"
```

### Script Type: Java
```java
import java.util.List;
import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.uma.UmaRptPolicyType;
import io.jans.model.uma.ClaimDefinition;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.as.server.uma.authorization.UmaAuthorizationContext;
import io.jans.model.uma.ClaimDefinitionBuilder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UmaRptPolicy implements UmaRptPolicyType {
	
	private static final Logger log = LoggerFactory.getLogger(CustomScriptManager.class);

	@Override
	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("UMA RPT Policy Authorization. Initializing...");
        log.info("UMA RPT Policy Authorization. Initialized");
		return true;
	}

	@Override
	public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("UMA RPT Policy Authorization. Initializing...");
        log.info("UMA RPT Policy Authorization. Initialized");
        return true;
	}

	@Override
	public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("UMA RPT Policy Authorization. Destroying...");
        log.info("UMA RPT Policy Authorization. Destroyed.");
        return true;
	}

	@Override
	public int getApiVersion() {
		return 11;
	}

	@Override
	public List<ClaimDefinition> getRequiredClaims(Object authorizationContext) {
		/*  needs to be a valid JSON string
	     *  Sample: [ { "issuer" : [ "https://example.com" ], "name" :
	     * "country", "claim_token_format" : [
	     * "http://openid.net/specs/openid-connect-core-1_0.html#IDToken" ],
	     * "claim_type" : "string", "friendly_name" : "country" } ]
	     *
	     */
		String json = "";
		UmaAuthorizationContext authContext = (UmaAuthorizationContext) authorizationContext;
		authContext.addRedirectUserParam("customUserParam1", "value1");
		return ClaimDefinitionBuilder.build(String.format(json, authContext.getIssuer()));
	}

	@Override
	public boolean authorize(Object authorizationContext) {
		log.info("UMA RPT Policy Authorization. Authorizing...");
		UmaAuthorizationContext authContext = (UmaAuthorizationContext) authorizationContext;
		if (authContext.getClaim("country").equals("US") && authContext.getClaim("city").equals("NY")) {
			log.info("Authorized successfully!");
			return true;
		}
		return false;
	}

	@Override
	public String getClaimsGatheringScriptName(Object authorizationContext) {
		UmaAuthorizationContext authContext = (UmaAuthorizationContext) authorizationContext;
		// pass some custom parameters to need_info uri. It can be removed if you don't need custom parameters.
		authContext.addRedirectUserParam("customUserParam2", "value2"); 
		return "sampleClaimsGathering";
	}

}

```