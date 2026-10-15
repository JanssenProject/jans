---
tags:
  - administration
  - developer
  - script-catalog
---

# Application Session Script Guide

The Application Session interception script allows developers to customize session lifecycle behavior in Janssen Auth Server. It provides callback methods that can execute custom logic when a session starts, ends, or generates an event, and when the active sessions response is processed.

## Purpose

The Application Session script is useful when custom processing is required during session management.

Common use cases include:

- **Session validation:** Apply custom checks when a user session starts. For example, the sample script checks for an existing session-related entry before allowing a new session.
- **Session termination:** Perform additional processing or write logs when a session ends.
- **Session event handling:** Respond to supported session events, such as an authenticated session event.
- **Active session response customization:** Inspect or modify the data returned by the `/session/active` endpoint.


## Source of Script

The Application Session script implements the [`ApplicationSessionType`](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/session/ApplicationSessionType.java) Java interface.

The Janssen repository provides a [sample Python implementation](https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/application_session/sample-script/SampleScript.py) demonstrating the available callbacks and a custom session validation rule.

## Interface

The ApplicationSessionType interface extends the base custom script interface and defines the callback methods available to Application Session scripts.

### Inherited Methods

| Method                                                    | Description                                                                                                                                                                                         |
|:-----------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `def init(self, customScript, configurationAttributes)`          | Called during script initialization. Can be used to initialize objects and resources.                                              |
| `def destroy(self, configurationAttributes)`                     | Called when the script is destroyed. Can be used to release initialized resources.                                                            |
| `def getApiVersion(self)` | Returns the API version supported by the script implementation. |

### Application Session Methods

| Method                                                             | Description                                                           | Return Type |
|:--------------------------------------------------------------------------|------------------------------------------------------------------------------|-------------|
| `def startSession(self, httpRequest, sessionId, configurationAttributes)` | Called when a session starts. Allows custom session validation and processing. | `boolean` |
| `def endSession(self, httpRequest, sessionId, configurationAttributes)`   | Called when a session ends. Allows custom processing or logging. | `boolean` |     
| `def onEvent(self, event)`                                                | Called when a session event occurs. | `void` |
| `def modifyActiveSessionsResponse(self, jsonArray, context)`              | Called while processing the `/session/active` endpoint, allowing modification of the active sessions response. | `boolean` |

The Boolean methods return `True` or `False`. The effect of these return values depends on the callback and the server's handling of its result. The `onEvent()` method does not return a Boolean value.

### Objects

| Object               |Description                                                                                                                                                                                                               |
|:--------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `customScript`            | Custom script configuration object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java)                                            |
| `context`                 | Context object provided to the active sessions response callback. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java) |
| `sessionId`               | Session object containing session information and attributes. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/common/src/main/java/io/jans/as/common/model/session/SessionId.java)                                               |
| `event`                   | Session event object containing event-related information. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/session/SessionEvent.java)                   |
| `httpRequest`             | `jakarta.servlet.http.HttpServletRequest`- HTTP request associated with the callback.           |
| `jsonArray`               | `org.json.JSONArray` - Active sessions response data.                                                                                           |
| `configurationAttributes` | Map of configuration properties available to the script. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java)                  |
