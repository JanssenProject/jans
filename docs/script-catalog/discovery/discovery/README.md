# Discovery Script Guide

## Overview

The Authorization Server Metadata spec [RFC8414](https://datatracker.ietf.org/doc/html/rfc8414) (also known as OAuth Discovery) defines a format for clients to use to look up the information needed to interact with a particular OAuth server. This includes things like:-
- finding the authorization endpoint,
- listing the supported scopes and client authentication mechanisms.

The ["OpenID Connect Discovery 1.0"](https://openid.net/specs/openid-connect-discovery-1_0.html) defines the metadata in such a way that is compatible with OpenID Connect Discovery while being applicable to a wider set of OAuth 2.0 use cases.  

This is intentionally parallel to the way that ["OAuth 2.0 Dynamic Client Registration Protocol" RFC7591](https://datatracker.ietf.org/doc/html/rfc8414) defines the dynamic client registration mechanisms "OpenID Connect Dynamic Client Registration 1.0 OpenID.Registration so that is compatible with it.

The metadata for an authorization server is retrieved from a know location as a JSON object [RFC8259](https://datatracker.ietf.org/doc/html/rfc8259) , which defines its endpoint locations and authorization server capabilities.

This metadata can be passed either:-
- in a self-asserted fashion from the server origin via HTTPS 
- or as a set of signed metadata values represented as claims in a JSON Web Token [JWT](https://www.rfc-editor.org/info/rfc7519).  

In the JWT case, the issuer is vouching for the validity of the data coming from the authorization server.  This is analogous to the role that the Software Statement plays in OAuth Dynamic Client Registration RFC7591.

**Note:** The means by which the client chooses an authorization server is out of scope.  In some cases, the issuer identifier may be manually configured into the client.  In other cases, it may be dynamically discovered, for instance, through the use of [WebFinger](https://datatracker.ietf.org/doc/html/rfc7033).

Discovery script allows to modify response of OpenID Connect Discovery [RFC8414](https://datatracker.ietf.org/doc/html/rfc8414).

### Methods

The discovery interception script extends the base script type with the methods -

| Method | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | **Inherited Method** This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | **Inherited Method** This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | **Inherited Method** The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |
| `def modifyResponse(self, responseAsJsonObject, context)` | This method is called after discovery response is ready. This method can modify discovery response.<br/>`responseAsJsonObject` is `org.codehaus.jettison.json.JSONObject`<br/> `context` is `io.jans.as.server.model.common.ExecutionContext` |

The `configurationAttributes` parameter is `java.util.Map<String, SimpleCustomProperty>`.

    configurationAttributes = new HashMap<String, SimpleCustomProperty>();
    configurationAttributes.put("Location Type", new SimpleCustomProperty("location_type", "ldap", "Storage Location for the script"));

### Snippet

    # Returns boolean, true - apply discovery method, false - ignore it.
    # This method is called after discovery response is ready. This method can modify discovery response.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.server.model.common.ExecutionContext (in https://github.com/JanssenProject project)
    def modifyResponse(self, responseAsJsonObject, context):
        responseAsJsonObject.accumulate("key_from_script", "value_from_script")
        return True
        
## Common Use Cases

## Script Type: Python

### [Add a value (Client IP Address) and Filter out a value from Discovery Response](scripts/Custom_OpenID_Config.py)

The sample code snippet shows how to add and filter out a value from Discovery Response.


## Script Type: Java

### [Add a value (Client IP Address) and Filter out a value from Discovery Response](scripts/Custom_OpenID_Config.java)

The sample code snippet shows how to add and filter out a value from Discovery Response.
