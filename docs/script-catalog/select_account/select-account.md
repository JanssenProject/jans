---
tags:
  - administration
  - developer
  - script-catalog
  - SelectAccount
---

# Select Account

A person may have several accounts on a single Jans Auth Server instance. For example, it is common to have several Gmail accounts. Jans Auth Server uses two cookies to track which accounts are associated with a browser: `session_id` and `current_sessions`.

Please check [Multiple Sessions In One Browser Overview](../../janssen-server/auth-server/session-management/multiple-sessions-one-browser.md)



Select Account interception script can be used to customize account selection behavior. E.g. redirect to external page or change text representation of sessions on page.

## Interface
The select account script implements the [SelectAccountType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/selectaccount/SelectAccountType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods

| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods

| Method header | Method description |
|:-----|:------|
| `def getSelectAccountPage(self, context)` | This method is called to return path to custom select account page (e.g. /customs/page/path/myselectaccount.xhtml) |
| `def prepare(self, context)` | This method is called before select account page is loaded. It can be used to prevent loading or redirect to external url. |
| `def getAccountDisplayName(self, context)` |  This method is used to customize text representation of the session shown on the page. Returns display name for given session id (https://github.com/JanssenProject/jans/blob/main/jans-auth-server/common/src/main/java/io/jans/as/common/model/session/SessionId.java). |
| `def onSelect(self, context)` | This method is called on session selection and can be used to forbid selection. |

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`SimpleCustomProperty`| Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java) |
|`context`| Execution Context [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java) |

## Use case: Dummy Select Account example script (does not have impact on built-in account selection)

[Select Account script example](https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/select_account/select-account/select_account.py).

### Script Type: Python

```python
# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2020, Janssen
#
# Author: Yuriy Z
#

from io.jans.model.custom.script.type.selectaccount import SelectAccountType
from java.lang import String

class SelectAccount(SelectAccountType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "SelectAccount script. Initializing ..."
        print "SelectAccount script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "SelectAccount script. Destroying ..."
        print "SelectAccount script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns path to select account page (e.g. /customs/page/path/myselectaccount.xhtml)
    # If none or empty string is returned, AS uses built-in page.
    # (Note: Custom page can be also put into `custom/pages/selectAccount.xhtml` and used without custom script.)
    # context is reference of io.jans.as.server.model.common.ExecutionContext( https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java )
    def getSelectAccountPage(self, context):
        return ""

    # This method is called before select account page is loaded.
    # It is good place to make preparation processing.
    # E.g. check whether it is ok to land on select account page or maybe redirect to external page.
    # Return True - continue loading of the page
    # Return False - stop loading and show error page
    # context is reference of io.jans.as.server.model.common.ExecutionContext( https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java )
    def prepare(self, context):
        return True

    # Returns display name for given session id (https://github.com/JanssenProject/jans/blob/main/jans-auth-server/common/src/main/java/io/jans/as/common/model/session/SessionId.java).
    # Session can be accessed via context.getSessionId()
    # Typical use is: context.getSessionId().getUser().getAttribute("myDisplayName")
    # Returns string. If blank string is returned, AS will return built-in implementation to return display name
    # which is context.getSessionId().getUser().getAttribute("displayName")
    # context is reference of io.jans.as.server.model.common.ExecutionContext( https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java )
    def getAccountDisplayName(self, context):
        return ""

    # This method is called on session selection.
    # Selected session can be accessed as context.getSessionId()
    # Return True - continue session selection
    # Return False - stop session selection (forbid it)
    # context is reference of io.jans.as.server.model.common.ExecutionContext( https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java )
    def onSelect(self, context):
        return True
```

### Script Type: Java

```java

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class DummySelectAccountType implements SelectAccountType {

    @Override
    public String getSelectAccountPage(Object context) {
        return "";
    }

    @Override
    public boolean prepare(Object context) {
        return true;
    }

    @Override
    public String getAccountDisplayName(Object context) {
        return "";
    }

    @Override
    public boolean onSelect(Object context) {
        return false;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public int getApiVersion() {
        return 1;
    }
}

```