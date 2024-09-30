---
tags:
  - administration
  - developer
  - scripts
  - IdGenerator
---

## Overview
By default Janssen Auth Server uses an internal method to generate unique identifiers for new person/client, etc. entries. In most cases the format of the ID is:

`'!' + idType.getInum() + '!' + four_random_HEX_characters + '.' + four_random_HEX_characters.`

The ID generation script enables an admin to implement custom ID generation rules.

## Interface
The ID Generator script implements the [IdGeneratorType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/id/IdGeneratorType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods
| Method header | Method description |
|:-----|:------|
| `def generateId(self, appId, idType, idPrefix, configurationAttributes)` | `appId` is application ID, `idType` is ID Type, `idPrefix` is ID Prefix |

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |

## Use case: Sample ID generator
This script has been adapted from the Gluu Server [sample ID generator script](https://gluu.org/docs/gluu-server/4.4/admin-guide/sample-id-generation-script.py).

### Script Type: Python
```python
from io.jans.model.custom.script.type.id import IdGeneratorType

import java

class IdGenerator(IdGeneratorType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Id generator. Initialization"
        print "Id generator. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Id generator. Destroy"
        print "Id generator. Destroyed successfully"
        return True   

    def getApiVersion(self):
        return 1

    # Id generator init method
    # appId is application Id
    # idType is Id Type
    # idPrefix is Id Prefix
    def generateId(self, appId, idType, idPrefix, configurationAttributes):
        print "Id generator. Generate Id"
        print "Id generator. Generate Id. AppId: '", appId, "', IdType: '", idType, "', IdPrefix: '", idPrefix, "'"

        # Return None or empty string to trigger default Id generation method
        return None

```


### Script Type: Java
```java
import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.id.IdGeneratorType;
import io.jans.service.custom.script.CustomScriptManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndSession implements EndSessionType {

    private static final Logger log = LoggerFactory.getLogger(CustomScriptManager.class);

	@Override
	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("ID Generator. Initializing...");
        log.info("ID Generator. Initialized");
        return true;
	}

	@Override
	public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("ID Generator. Initializing...");
        log.info("ID Generator. Initialized");
        return true;
	}

	@Override
	public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("ID Generator. Destroying...");
        log.info("ID Generator. Destroyed.");
        return true;
	}

	@Override
	public int getApiVersion() {
		return 11;
	}

    @Override
    public String generateId(String appId, String idType, String idPrefix, Map<String SimpleCustomProperty> configurationAttributes) {
        log.info("ID Generator. Generate Id");
        // Return None or empty string to trigger default Id generation method
        return "";
    }
}

```