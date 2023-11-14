---
tags:
  - administration
  - auth-server
  - scope
---

## Scopes

Scopes defines width of authorization access. Scopes are associated with authorization and thus with client to limit what scopes can be granted within given client.

Scopes are represented in AS persistence with `jansScope` object and can have following attributes:

- `jansDefScope` - boolean value, specifies whether scope can be associated with client via Dynamic Client Registration (DCR) or not
- `description` - string value, text description of the scope
- `displayName` - string value, display name of the scope
- `inum` - inum identified (which takes part in `dn` key)
- `dn` - distinguished name (inum takes part in `dn`), unique identifier of the entry within persistence
- `jansScopeTyp` - specified type of the scope and can have following values:
  - `oauth` - OAuth 2.0 Scopes for any of their API's. This scope type would only have a description, but no claims. Once a client obtains this token, it may be passed to the backend API (let's say the calendar API).
  - `openid` - Specify what access privileges are being requested for Access Tokens.  The scopes associated with Access Tokens determine what resources will be available when they are used to access OAuth 2.0 protected endpoints. For OpenID Connect, scopes can be used to request that specific sets of information be made available as Claim Values. OpenID Connect defines the following scope values that are used to request Claims:
    - `profile` - This scope value requests access to the End-User's default profile Claims, which are: `name`, `family_name`, `given_name`, `middle_name`, `nickname`, `preferred_username`, `profile`, `picture`, `website`, `gender`, `birthdate`, `zoneinfo`, `locale`, and `updated_at`.
    - `email` - This scope value requests access to the `email` and `email_verified` Claims.
    - `address` - This scope value requests access to the address Claim.
    - `phone` - This scope value requests access to the `phone_number` and `phone_number_verified` Claims.
  The Claims requested by the `profile`, `email`, `address`, and `phone` scope values are returned from the UserInfo Endpoint.
  - `dynamic` - Dynamic scope calls scripts which add claims dynamically.
  - `uma` - UMA scopes
  - `spontaneous` - spontaneous scopes, scopes which are created temporary and match to some `regexp` pattern 
- `creatorId` - string value, specified creator id 
- `creatorTyp` - enum value, specifies type of the creator and can have following values: 
  - `none` - creator type is unknown
  - `client` - scope created by client (e.g. spontaneous scope)
  - `user` - scope created by user
  - `auto` - scope automatically created by AS if it does not exist in persistence (currently used only in UMA case)
- `creatorAttrs` - map which can hold creator attributes
- `creationDate` - date value, specified creation date of the scope
- `jansClaim` - string array, specified id of claims that belongs to this scope
- `jansScrDn` - string array, specified dynamic scope scripts `dn`s. 
- `jansGrpClaims` - boolean value, specifies whether to group claims. 
- `jansId` - string value, id of the claims (used during authorization requests or elsewhere in request/response)
- `jansIconUrl` - string values, specified icon url  
- `jansUmaPolicyScrDn` - string array, specifies UMA policy scripts `dn`s  
- `exp` - date value, specifies expired date
- `del` - boolean value, specified whether scope is deletable and has to be cleaned up by AS. `false` by default, it is `true` for spontaneous scopes which has expiration time and has to be cleaned up.  
- `jansAttrs` - other attributes of the scopes  
  - `spontaneousClientScopes` - string array, specifies client spontaneous scopes which allowed creation of this scope
  - `showInConfigurationEndpoint` - boolean values, specified whether to show scope in discovery page or not 

In AS code, scope object is represented with following [java class](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/persistence-model/src/main/java/io/jans/as/persistence/model/Scope.java)


### Dynamic Client Registration

To enabled scope for DCR please set `jansDefScope` to `true` value. 

Global AS Configuration for DCR
- `dynamicRegistrationScopesParamEnabled` - boolean value specifying whether to enable scopes parameter in dynamic registration.
- `dynamicRegistrationPasswordGrantTypeEnabled` - boolean value specifying whether to enable Password Grant Type during Dynamic Registration. Default value is `false`.
- `dynamicRegistrationAllowedPasswordGrantScopes` - string array, list of grant scopes for dynamic registration

### Customizing scopes

Similar to client registration, scopes can be customized using interception scripts.  

The sample dynamic scope script is [available here](./../../../script-catalog/dynamic_scope/dynamic-permission/dynamic_permission.py).


### Global AS Configuration

Global AS Configurations related to scopes.

- `forceOfflineAccessScopeToEnableRefreshToken` - boolean value specifying whether force offline_access scope to enable refresh_token grant type. Default value is `true`
- `introspectionResponseScopesBackwardCompatibility` - boolean value specifying introspection response backward compatibility mode. Default value is `false`.
- `introspectionAccessTokenMustHaveIntrospectionScope` - if True, rejects introspection requests if access_token does not have the 'introspection' scope in its authorization header. Comparing to 'uma_protection', 'introspection' scope is not allowed for dynamic registration'. Default value is `false`.
- `umaAddScopesAutomatically` - add UMA scopes automatically if it is not registered yet
- `umaGrantAccessIfNoPolicies` - specify whether to grant access to resources if there is no any policies associated with scopes. Default value is `false`.
- `skipAuthorizationForOpenIdScopeAndPairwiseId` - choose whether to skip authorization if a client has an OpenId scope and a pairwise ID, Default value is `false`.
- `activeSessionAuthorizationScope` - string value, specified authorization Scope for active session
- `introspectionAccessTokenMustHaveUmaProtectionScope` - boolean value, if True, rejects introspection requests if access_token does not have the uma_protection scope in its authorization header. Default value is `false`.
- `introspectionAccessTokenMustHaveIntrospectionScope` - boolean value, if True, rejects introspection requests if access_token does not have the 'introspection' scope in its authorization header. Comparing to 'uma_protection', 'introspection' scope is not allowed for dynamic registration'. Default value is `false`.
- `openidScopeBackwardCompatibility` - boolean value, set to false to only allow token endpoint request for openid scope with grant type equals to authorization_code, restrict access to userinfo to scope openid and only return id_token if scope contains openid. Default value is `false`.

### Spontaneous scopes

Spontaneous scopes are scopes with random part. AS supports both OpenID Connect and UMA Spontaneous scopes. 

Global AS Configuration for spontaneous scopes.

- `allowSpontaneousScopes` - boolean value, specifies whether to allow spontaneous scopes
- `spontaneousScopeLifetime` - the lifetime of spontaneous scope in seconds 


#### OpenID Connect Spontaneous scopes

Spontaneous scopes are scopes with random part in it which are not known in advance. For example: `transaction:4685456787`, `pis-552fds` where `4685456787` or `552fds` are generated part of the scope.

Spontaneous scopes are disabled by default and can be enabled per client. There are following client properties available during dynamic registration of the client related to spontaneous scopes:

- `allow_spontaneous_scopes` - OPTIONAL, boolean, false by default. Whether spontaneous scopes are allowed for given client 
- `spontaneous_scopes` - OPTIONAL, array of strings. Regular expressions which should match to scope. If matched scope is allowed. Example: `["^transaction:.+$"]`. It matches `transaction:245` but not `transaction:`.

Dynamic registration example:
```
...
"allow_spontaneous_scopes": true,
"spontaneous_scopes": ["^transaction:.+$"]
...
```

Authorization request example (note `transaction:245` and `transaction:8645` scopes in request)
```
https://example.gluu.org/oxauth/restv1/authorize?response_type=code&scope=openid+profile+transaction%3A245+transaction%3A8645&client_id=c8592b26-8984-484d-8aba-9f475be73af0&redirect_uri=https%3A%2F%2Fexample.gluu.org%2Foxauth-rp%2Fhome.htm&state=2dccaf64-c0b9-4c35-8008-f754ad964c3b&nonce=9cf5c813-578b-44e5-a353-b7446c1b9358
```

If `allow_spontaneous_scopes=true` and `spontaneous_scopes` regular expression has match then spontaneous scope is persisted and allowed to be handled as usual scope.
Spontaneous scope has lifetime and is cleaned up from persistence when expired (and thus not available anymore). Configuration property `spontaneousScopeLifetime` specifies lifetime in seconds.

In addition there is spontaneous scope interception scripts which give additional flexibility. 
The sample spontaneous scope script is [available here](./../../../script-catalog/spontaneous_scope/spontaneous-scope/spontaneous_scope.py).

#### UMA Spontaneous scopes

Sometimes it's required to handle scope by pattern. For example we wish to grant access for particular user based on path
```
/user/1
...
/user/n
```
In this case we can't register resources for each user since it is dynamic. If lets say we have 1 million users we don't want to register 1 million resources.
It can be handled with spontaneous scopes which works via regular expressions.

1. Allow spontaneous scopes for client via `allow_spontaneous_scopes` client property.
2. Register resource with scope `^/user/.+$`
3. Register and assign UMA RPT Authorization Policies to `^/user/.+$` 
4. RS should sent explicit scope in permission during ticket registration, e.g. `/user/1` (AS validates whether `/user/1` matches regexp `^/user/.+$`). After validation AS persists `/user/1` scope and during RPT creation puts permission with explicit scope.

In this way AS can validate and persist scopes dynamically. Spontaneous scopes have lifetime which is controlled by `spontaneousScopeLifetime` global oxauth configuration property.

**Ticket registration**

RS registers tickets with all scopes mentioned in "data" (we need all scopes in order to evaluate expression, all or nothing principle)

```json

{  
   "resource_id":"112210f47de98100",
   "resource_scopes":[  
       "http://photoz.example.com/dev/actions/all",
       "http://photoz.example.com/dev/actions/add",
       "http://photoz.example.com/dev/actions/internalClient"
   ]
}
```

### UMA Scopes

UMA 2 scopes are used to grant a client permission to do an action on a protected resource. Different scopes can grant access to the same action. For example, a "read" action can be allowed with scope "read" or "all". 

For some actions the Resource Server (RS) may want multiple scopes at the same time. For instance, a "read" action should only be allowed if the authorization request includes the "read" **and** "all" scopes. UMA 2 scopes are bound to resources and are used to fetch policies that check whether the specified user or client should have access to the resource. 

The scopes are described in JSON and have the following properties:

- name
- icon\_uri

An example of the scope JSON is given below:

```
{
  "name": "Add photo",
  "icon_uri": "https://<hostname>/icons/add_photo_scope.png"
}
```

!!! Note
    The scope JSON may contain custom properties.

The following is an example what an UMA 2 Scope URL may look like:

```
https://<hostname>/uma/scopes/view
```

!!! Note
    The scope endpoint has to be present in UMA configuration to make it discoverable.

The `ldif` for both an internal and external scope is given below:

**Sample ldif**
```
dn: inum=@!1111!8990!BF80,ou=scopes,ou=uma,o=@!1111,o=gluu
displayName: View
inum: @!1111!8990!BF80
objectClass: oxAuthUmaScopeDescription
objectClass: top
oxId: View
oxIconUrl: http://<hostname>/uma/icons/view_scope.png
```

#### UMA Scope Expressions 

UMA 2 Scope expressions is Gluu invented extension of UMA 2 which gives flexible way to 
combine scopes and thus propose more robust way to grant access.

**Register resource with `scope_expression`**

RS registers resource 

!!! Note
        new `scope_expression` field, `resource_scopes` is ignored in this case

```json
{  
   "resource_scopes":[],
   "description":"Collection of digital photographs",
   "icon_uri":"http://www.example.com/icons/flower.png",
   "name":"Photo Album",
   "type":"http://www.example.com/rsrcs/photoalbum",
   "scope_expression": {
      "rule": {
         "and": [
            {
               "or": [
                   {"var": 0},
                   {"var": 1}
               ]
            },
            {"var": 2}
         ]
      },
      "data": [
         "http://photoz.example.com/dev/actions/all",
         "http://photoz.example.com/dev/actions/add",
         "http://photoz.example.com/dev/actions/internalClient"
      ]
   }
}

```

### Statistic Endpoint

Global AS Configuration for statistic endpoint

- `statAuthorizationScope` - scope required for Statistical Authorization. Default value is `jans_stat`

## Have questions in the meantime?

You can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).