# Introspection Script Guide

## Overview

Introspection scripts allows to modify response of Introspection Endpoint [spec](https://datatracker.ietf.org/doc/html/rfc7662).

## Interface

### Methods

Introspection script should be associated with client (used for obtaining the token) in order to be run. Otherwise it's possible to set introspectionScriptBackwardCompatibility global AS configuration property to true, in this case AS will run all scripts (ignoring client configuration).

The introspection interception script extends the base script type with the `init`, `destroy` and `getApiVersion` methods but also adds the following method(s):

|Method |`def modifyResponse(self, responseAsJsonObject, context)`|
|:-----|:------|
| Method Paramater| `responseAsJsonObject` is `org.codehaus.jettison.json.JSONObject`<br/> `context` is <br/> `org.gluu.oxauth.service.external.context.ExternalIntrospectionContext`|


### Snippet

    # Returns boolean, true - apply introspection method, false - ignore it.
    # This method is called after introspection response is ready. This method can modify introspection response.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of org.gluu.oxauth.service.external.context.ExternalIntrospectionContext (in https://github.com/GluuFederation/oxauth project, )
    def modifyResponse(self, responseAsJsonObject, context):
        responseAsJsonObject.put("key_from_script", "value_from_script")
        return True
        
Full version of introspection script example can be found [here].(https://github.com/GluuFederation/community-edition-setup/blob/version_4.2.0/static/extension/introspection/introspection.py)

It is also possible to run introspection script during `access_token` creation as JWT. It can be controlled by r`un_introspection_script_before_access_token_as_jwt_creation_and_include_claims` client property which is set to false by default.

If `run_introspection_script_before_access_token_as_jwt_creation_and_include_claims` set to true and a`ccess_token_as_jwt` set to true then introspection script will be run before JWT (`access_token`) is created and all JSON values will be transfered to JWT. Also `context` inside script has additional method which allows to cancel transfering of claims if needed `context.setTranferIntrospectionPropertiesIntoJwtClaims(false)`
        
## Common Use Cases

Descriptions of common use cases for this script, including a code snippet for each
