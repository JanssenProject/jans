# Introspection Script Guide

## Overview

OAuth reference tokens don't convey any information. They are by their very nature, unguessable strings that meet the prescribed OAuth security guidelines for lack of order or predictability (entropy). However, there is still a large amount of data that may be attached to a token, such as:-
 - its current validity,
 - approved scopes, 
 - information about the context in which the token was issued. 

Access token data is essential for the resource server to evaluate policies that determine whether or not to allow the request. Token introspection therefore enables a client to trade an OAuth reference token for its JSON equivalent by making a request per [OAuth 2.0 Token Introspection Guide - (RFC 7662)](https://datatracker.ietf.org/doc/html/rfc7662). 

The use of Introspection scripts allows the ability to modify the response of Introspection Endpoint [spec](https://datatracker.ietf.org/doc/html/rfc7662) and provide additional information in the JSON response.

## Interface

In order to be run and Introspection script should be associated with an OpenID Client (used for obtaining the token). Another way of doing this is by setting the `introspectionScriptBackwardCompatibility` global Auth Server JSON Configuration Property to true. In this case the Auth Server will run all scripts and will do so by ignoring client configuration.

### Methods

The introspection interception script extends the base script type with the methods -

| Method | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | **Inherited Method** This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | **Inherited Method** This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | **Inherited Method** The `getApiVersion` method allows API changes in order to do transparent migration from an old script to a new API. **NOTE**: - Only include the customScript variable if the value for `getApiVersion` is greater than 10 |
| `def modifyResponse(self, responseAsJsonObject, context)` | This method is called after the introspection response is ready. This method can modify the introspection response.<br/>`responseAsJsonObject` is `org.codehaus.jettison.json.JSONObject`<br/> `context` is `io.jans.as.service.external.context.ExternalIntrospectionContext` |

The `configurationAttributes` parameter is `java.util.Map<String, SimpleCustomProperty>`. 

    configurationAttributes = new HashMap<String, SimpleCustomProperty>();
    configurationAttributes.put("Location Type", new SimpleCustomProperty("location_type", "ldap", "Storage Location for the script"));

### Snippet

    # Returns boolean, true - apply introspection method, false - ignore it.
    # This method is called after introspection response is ready. This method can modify the introspection response.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.server.service.external.context.ExternalIntrospectionContext (in https://github.com/JanssenProject project)
    def modifyResponse(self, responseAsJsonObject, context):
        responseAsJsonObject.accumulate("key_from_script", "value_from_script")
        return True
        
**Note - The preferred way to modify an access token is with the Update Token script.**

It is also possible to run an introspection script during `access_token` creation as JWT. It can be controlled by `run_introspection_script_before_jwt_creation` OpenID Client property which is set to false by default. 

If OpenID Client properties `run_introspection_script_before_jwt_creation` and `access_token_as_jwt` are set to true then an introspection script will be run before JWT (`access_token`) is created and all JSON values will be transfered to JWT. Also `context` inside the script has additional method which allows you to cancel transfering of claims if needed `context.setTranferIntrospectionPropertiesIntoJwtClaims(false)`
        
## Common Use Cases

## Script Type: Python

### [Retrieve Grant, Session and User Details from Access Token](introspection-custom-parameters/introspection_custom_params.py)

The sample code snippet shows how to work backwards from an AccessToken to Grant, Session and User information.

## Script Type: Java

### [Retrieve Grant, Session and User Details from Access Token](introspection-custom-parameters/introspection_custom_params.java)

The sample code snippet shows how to work backwards from an AccessToken to Grant, Session and User information.
