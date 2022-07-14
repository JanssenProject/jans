# Application Session Script Guide

## Overview

Authentication scripts can be used to implement complex multi-step, multi-factor authentication workflows.

## Interface

In each script, authentication steps and mechanisms are defined, external APIs can be called, and user experience can be adjusted dynamically based on contextual factors. For example, a fraud detection API can be called in step one. If it indicates unacceptable risk, a second step can be added to prompt the user for a stronger authentication credential.

### Methods

The authentication interception script extends the base script type with methods for `init`, destroy and `getApiVersion`, and also adds the following:

| Method        | `isValidAuthenticationMethod(self, usageType, configurationAttributes)`  |
| :------------- | :------------- |
| **Description**  | This method is used to check if the authentication method is in a valid state. For example we can check there if a 3rd party mechanism is available to authenticate users. As a result it should either return `True` or `False` |
| Method Parameter | `usageType` is `org.gluu.model.AuthenticationScriptUsageType` <br/>`configurationAttributes` is `java.util.Map<String, SimpleCustomProperty>`


### Objects

| First Header  | Second Header |
| ------------- | ------------- |
| Content Cell  | Content Cell  |
| Content Cell  | Content Cell  |

## Common Use Cases

Descriptions of common use cases for this script, including a code snippet for each
