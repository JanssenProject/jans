# Person Authentication Guide

## Overview

Authentication scripts can be used to implement complex multi-step, multi-factor authentication workflows.

## Interface

In each script, authentication steps and mechanisms are defined, external APIs can be called, and user experience can be adjusted dynamically based on contextual factors. For example, a fraud detection API can be called in step one. If it indicates unacceptable risk, a second step can be added to prompt the user for a stronger authentication credential.

### Methods

The authentication interception script extends the base script type with methods for `init`, destroy and `getApiVersion`, and also adds the following:

| Method        | `isValidAuthenticationMethod(self, usageType, configurationAttributes)`|
| :------------- | :------------- |
| **Description**  | This method is used to check if the authentication method is in a valid state. For example we can check there if a 3rd party mechanism is available to authenticate users. As a result it should either return `True` or `False`|
| Method Parameter | `usageType` is `org.gluu.model.AuthenticationScriptUsageType` <br/>`configurationAttributes` is `java.util.Map<String, SimpleCustomProperty>`|

| Method        | `def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes)`|
| :------------- | :------------- |
| **Description**  | This method is called only if the current authentication method is in an invalid state. Hence authenticator calls it only if `isValidAuthenticationMethod` returns False. As a result it should return the reserved authentication method name |
| Method Parameter | `usageType` is `org.gluu.model.AuthenticationScriptUsageType` <br/>`configurationAttributes` is `java.util.Map<String, SimpleCustomProperty>`|

| Method        | `def authenticate(self, configurationAttributes, requestParameters, step)`|
| :------------- | :------------- |
| **Description**  | This method is the key method within the person authentication script. It checks if the user has passed the specified step or not. As a result it should either return `True` or `False` |
| Method Parameter | `requestParameters` is `java.util.Map<String, String[]>` <br/>`step` is java integer <br/>`configurationAttributes is java.util.Map<String, SimpleCustomProperty>`|

| Method        | `def prepareForStep(self, configurationAttributes, requestParameters, step)`|
| :------------- | :------------- |
| **Description**  | This method can be used to prepare variables needed to render the login page and store them in an according event context. As a result it should either return `True` or `False` |
| Method Parameter | `requestParameters` is `java.util.Map<String, String[]>` <br/>`step` is java integer <br/>`configurationAttributes is java.util.Map<String, SimpleCustomProperty>`|

| Method        | `def getCountAuthenticationSteps(self, configurationAttributes)`|
| :------------- | :------------- |
| **Description**  | This method should return an integer value with the number of steps in the authentication workflow|
| Method <br/>Parameter | `configurationAttributes is java.util.Map<String, SimpleCustomProperty>`|


| Method        | `def getExtraParametersForStep(self, configurationAttributes, step)`|
| :------------- | :------------- |
| **Description**  | This method provides a way to notify the authenticator that it should store specified event context parameters event in the oxAuth session. It is needed in a few cases, for example when an authentication script redirects the user to a 3rd party authentication system and expects the workflow to resume after that. As a result it should return a java array of strings |
| Method Parameter | `configurationAttributes` is `java.util.Map<String, SimpleCustomProperty>` <br/> `step` is a java integer|

| Method        | `def getPageForStep(self, configurationAttributes, step)`|
| :------------- | :------------- |
| **Description**  | This method allows the admin to render a required page for a specified authentication step. It should return a string value with a path to an XHTML page. If the return value is empty or null, the authenticator should render the default log in page `/login.xhtml`|
| Method Parameter | `configurationAttributes` is `java.util.Map<String, SimpleCustomProperty>` <br/> `step` is a java integer|

| Method        | `def logout(self, configurationAttributes, requestParameters)`|
| :------------- | :------------- |
| **Description**  | This method is not mandatory. It can be used in cases when you need to execute specific logout logic in the authentication script when oxAuth receives an end session request to the `/oxauth/logout.htm `endpoint (which receives the same set of parameters than the usual `end_session` endpoint). This method should return `True` or `False`; when `False` oxAuth stops processing the end session request workflow.<br/> <br/> If `getApiVersion()` returns "3" for this script, `logout()` will call an external end session API at a 3rd party service before terminating sessions at Gluu Server. To do so it calls `getLogoutExternalUrl()` method and redirects the user agent to the URL returned by such method (note at this point Gluu's sessions are not yet killed). After the 3rd-party service has completed its end session routines, it must re-direct user back to `/oxauth/logout.htm` again with empty URL query string - that's enough for oxAuth to recognize it as a continuation of the extended logout flow, restore the original URL query string, and send user to `/oxauth/end_session` to complete it|
| Method Parameter | `configurationAttributes` is `java.util.Map<String, SimpleCustomProperty>` <br/> `requestParameters` is `java.util.Map<String, String[]>`|


### Objects

Every deployment of the Gluu Server includes a number of pre-written authentication scripts out-of-the-box. Learn more in the [authentication guide.](https://gluu.org/docs/gluu-server/4.3/authn-guide/intro/)

For a complete list of pre-written, open source authentication scripts, view our [server integrations.](https://github.com/GluuFederation/oxAuth/tree/master/Server/integrations)

 - View a [sample Authentication Script.](https://gluu.org/docs/gluu-server/4.3/admin-guide/sample-authentication-script.py)

## Common Use Cases

 Descriptions of common use cases for this script, including a code snippet for each
