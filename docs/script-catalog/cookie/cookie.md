---
tags:
  - administration
  - developer
  - script-catalog
---

# Cookie Script 

This script is used to modify the "Set-Cookie" cookie header value. Any cookie created by AS can be modified by using this custom script.

## Interface

### Methods

The cookie interception script extends the base script type with the `init`, `destroy` and `getApiVersion` methods:

| Inherited Methods | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

The `configurationAttributes` parameter is `java.util.Map<String, SimpleCustomProperty>`.

The cookie interception script also adds the following method(s):

|Method |`def modifyCookieHeader(self, cookieName, cookieHeader)`|
|:-----|:------|
| Method Paramater| `cookieName` is `String`<br/> `cookieHeader` is `String` representing header value (e.g. `sessionId=abc123; Expires=Wed, 09 Jun 2021 10:18:14 GMT`)|


## Common Use Case

## Script Type: Java 

### Add/Modify cookie value attribute

```java
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.cookie.CookieType;
import io.jans.service.custom.script.CustomScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class Cookie implements CookieType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public String modifyCookieHeader(String cookieName, String cookieHeader) {

        // append "Secure" attribute to session_id cookie if it's not present yet
        if (cookieName.equals("session_id") && !cookieHeader.contains("Secure")) {
            cookieHeader = cookieHeader + "; Secure";
        }

        return cookieHeader;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Cookie Java script. Initialized successfully");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Cookie Java script. Initialized successfully");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Cookie Java script. Destroyed successfully");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}

```

