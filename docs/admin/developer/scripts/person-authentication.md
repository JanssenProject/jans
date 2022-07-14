# Application Session Script Guide

## Overview

Authentication scripts can be used to implement complex multi-step, multi-factor authentication workflows.

## Interface

In each script, authentication steps and mechanisms are defined, external APIs can be called, and user experience can be adjusted dynamically based on contextual factors. For example, a fraud detection API can be called in step one. If it indicates unacceptable risk, a second step can be added to prompt the user for a stronger authentication credential.

### Methods

The authentication interception script extends the base script type with methods for `init`, destroy and `getApiVersion`, and also adds the following:

| Method        | `isValidAuthenticationMethod(self, usageType, configurationAttributes)`  |
| :------------- | :------------- |
| **Description**  | This method is used to check if the authentication method is in a valid state. For example we can check there if a 3rd party mechanism is available to authenticate users. As a result it should either return `True` or `False`  |
| Method Parameter | `usageType` is `org.gluu.model.AuthenticationScriptUsageType` <br/>`configurationAttributes` is `java.util.Map<String, SimpleCustomProperty>`|

| Method        | `def getAlternativeAuthenticationMethod(self, usageType, configurationAttributes)`  |
| :------------- | :------------- |
| **Description**  | This method is called only if the current authentication method is in an invalid state. Hence authenticator calls it only if `isValidAuthenticationMethod` returns False. As a result it should return the reserved authentication method name |
| Method Parameter | `usageType` is `org.gluu.model.AuthenticationScriptUsageType` <br/>`configurationAttributes` is `java.util.Map<String, SimpleCustomProperty>`|


| Method        | `def authenticate(self, configurationAttributes, requestParameters, step)`  |
| :------------- | :------------- |
| **Description**  | This method is the key method within the person authentication script. It checks if the user has passed the specified step or not. As a result it should either return `True` or `False` |
| Method Parameter | `requestParameters` is `java.util.Map<String, String[]>` <br/>`step` is java integer <br/>`configurationAttributes is java.util.Map<String, SimpleCustomProperty>`

| Method        | `def prepareForStep(self, configurationAttributes, requestParameters, step)`  |
| :------------- | :------------- |
| **Description**  | This method can be used to prepare variables needed to render the login page and store them in an according event context. As a result it should either return `True` or `False` |
| Method Parameter | `requestParameters` is `java.util.Map<String, String[]>` <br/>`step` is java integer <br/>`configurationAttributes is java.util.Map<String, SimpleCustomProperty>`

| Method        | `def getCountAuthenticationSteps(self, configurationAttributes)`  |
| :------------- | :------------- |
| **Description**  | This method should return an integer value with the number of steps in the authentication workflow |
| Method <br/>Parameter | `configurationAttributes is java.util.Map<String, SimpleCustomProperty>`


| Method        | `def getExtraParametersForStep(self, configurationAttributes, step)`  |
| :------------- | :------------- |
| **Description**  | This method provides a way to notify the authenticator that it should store specified event context parameters event in the oxAuth session. It is needed in a few cases, for example when an authentication script redirects the user to a 3rd party authentication system and expects the workflow to resume after that. As a result it should return a java array of strings |
| Method Parameter | `configurationAttributes` is `java.util.Map<String, SimpleCustomProperty>` <br/> `step` is a java integer


### Objects

| First Header  | Second Header |
| ------------- | ------------- |
| Content Cell  | Content Cell  |
| Content Cell  | Content Cell  |

## Common Use Cases

Descriptions of common use cases for this script, including a code snippet for each
