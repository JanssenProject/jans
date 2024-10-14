---
tags:
  - administration
  - developer
  - scripts
  - ConsentGathering
---

## Overview
OAuth 2.0 allows providers to prompt users for consent before releasing their personal information to a client (application). The standard consent process is binary: approve or deny. Using the consent gathering interception script, the consent flow can be customized to meet unique business requirements, for instance to support payment authorization, where you need to present transactional information, or where you need to step-up authentication to add security.

## Interface
The consent gathering script implements the [ConsentGathering](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/authz/ConsentGatheringType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods
| Method header | Method description |
|:-----|:------|
| `def authorize(self, step, consentContext)` | Main consent-gather method. Must return True (if consent gathered successfully) or False (if failed). |
| `def getNextStep(self, step, context)` |  |
| `def getStepsCount(self, context)` | Return total number of consent gathering steps |
| `def getPageForStep(self, step, context)` | Returns the consent page corresponding to the current step of consent gathering |

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`SimpleCustomProperty`| Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java) |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ConsentGatheringContext.java) |

## Use case: Dummy Consent Gathering Form
This script has been adapted from the Gluu Server [sample consent gathering script](https://github.com/GluuFederation/oxAuth/blob/master/Server/integrations/authz/ConsentGatheringSample.py).
### Script Type: Python
```python
from io.jans.model.custom.script.type.authz import ConsentGatheringType
import random

class ConsentGathering(ConsentGatheringType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Consent-Gathering. Initializing ..."
        print "Consent-Gathering. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Consent-Gathering. Destroying ..."
        print "Consent-Gathering. Destroyed successfully"

        return True

    def getAuthenticationMethodClaims(self, requestParameters):
        return None

    def getApiVersion(self):
        return 11

    # All user entered values can be access via Map<String, String> context.getPageAttributes()
    def authorize(self, step, context):
        print "Consent-Gathering. Authorizing..."

        if step == 1:
            allowButton = context.getRequestParameters().get("authorizeForm:allowButton")
            if (allowButton != None) and (len(allowButton) > 0):
                print "Consent-Gathering. Authorization success for step 1"
                return True

            print "Consent-Gathering. Authorization declined for step 1"
        elif step == 2:
            allowButton = context.getRequestParameters().get("authorizeForm:allowButton")
            if (allowButton != None) and (len(allowButton) > 0):
                print "Consent-Gathering. Authorization success for step 2"
                return True

            print "Consent-Gathering. Authorization declined for step 2"

        return False

    def getNextStep(self, step, context):
        return -1

    def prepareForStep(self, step, context):
        if not context.isAuthenticated():
            print "User is not authenticated. Aborting authorization flow ..."
            return False

        if step == 2:
            pageAttributes = context.getPageAttributes()

            # Generate random consent gathering request
            consentRequest = "Requested transaction #%s approval for the amount of sum $ %s.00" % ( random.randint(100000, 1000000), random.randint(1, 100) )
            pageAttributes.put("consent_request", consentRequest)
            return True

        return True

    def getStepsCount(self, context):
        return 2

    def getPageForStep(self, step, context):
        if step == 1:
            return "/authz/authorize.xhtml"
        elif step == 2:
            return "/authz/transaction.xhtml"

        return ""
```

### Script Type: Java
```java
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.authz.ConsentGatheringType;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.as.server.service.external.context.ConsentGatheringContext;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsentGathering implements ConsentGatheringType {

    private static final Logger log = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Consent gathering. Initializing...");
        log.info("Consent gathering. Initialized");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Consent gathering. Initializing...");
        log.info("Consent gathering. Initialized");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Consent gathering. Destroying...");
        log.info("Consent gathering. Destroyed.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }

    @Override
    public boolean authorize(int step, Object consentContext) {
        log.info("Consent gathering. Authorizing...");
        ConsentGatheringContext gatheringContext = (ConsentGatheringContext) consentContext;
        String[] allowButton = gatheringContext.getRequestParameters().get("authorizeForm:allowButton");
        if (step == 1) {
            if (allowButton != null && allowButton.length > 0) {
                log.info("Consent gathering. Authorization success for step 1");
                return true;
            }
            log.info("Consent gathering. Authorization declined for step 1");
        }
        else if (step == 2) {
            if (allowButton != null && allowButton.length > 0) {
                log.info("Consent gathering. Authorization success for step 2");
                return true;
            }
            log.info("Consent gathering. Authorization declined for step 2");
        }
        return false;
    }

    @Override
    public int getNextStep(int step, Object consentContext) {
        return -1;
    }

    @Override
    public boolean prepareForStep(int step, Object consentContext) {
        ConsentGatheringContext gatheringContext = (ConsentGatheringContext) consentContext;
        if (!gatheringContext.isAuthenticated()) {
            log.info("User is not authenticated. Aborting authorization flow...");
            return false;
        }
        if(step == 2) {
            Map<String, String> pageAttributes = gatheringContext.getPageAttributes();

            String consentRequest = "This is a random consent request";
            pageAttributes.put("consent_request", consentRequest);
            return true;
        }
        return false;
    }

    @Override
    public int getStepsCount(Object consentContext) {
        return 2;
    }

    @Override
    public String getPageForStep(int step, Object consentContext) {
        if(step == 1) {
            return "/authz/authorize.xhtml";
        } else if (step == 2) {
            return "return \"/authz/transaction.xhtml\"";
        }
        return "";
    }
}
```