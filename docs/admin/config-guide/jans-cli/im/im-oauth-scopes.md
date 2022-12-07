---
tags:
  - administration
  - configuration
  - cli
  - interactive
---

# OAuth Scopes

!!! Important
    The interactive mode of the CLI will be deprecated upon the full release of the Configuration TUI in the coming months.
    
> Prerequisite: Know how to use the Janssen CLI in [interactive mode](im-index.md)

In OAuth, scopes are used to specify the extent of access. For an OpenID Connect sign-in flow, scopes correspond to the release of user claims.
`jans-cli` supports the following operations through Interactive Mode.

```
OAuth - Scopes
--------------
1 Gets list of Scopes
2 Create Scope
3 Updates existing Scope
4 Get Scope by Inum
5 Delete Scope
6 Update modified attributes of existing Scope by Inum

Selection: 
```

## Gets list of Scopes

To view the current list of scopes of the Janssen Server, choose the first option from the following menu. It will ask to enter `type`, `limit` & `pattern` to filter in searching. You may skip by pressing `'Enter'` key to get all the scopes of the server. In my case, I have set `limit` upto 5.

```
«Scope type. Type: string»
type: 

«Search size - max size of the results to return. Type: integer»
limit  [50]: 5

«Search pattern. Type: string»
pattern: 
Calling Api with parameters: {'limit': 5}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/scopes.readonly
[
  {
    "dn": "inum=F0C4,ou=scopes,o=jans",
    "inum": "F0C4",
    "displayName": "authenticate_openid_connect",
    "id": "openid",
    "iconUrl": null,
    "description": "Authenticate using OpenID Connect.",
    "scopeType": "openid",
    "claims": null,
    "defaultScope": true,
    "groupClaims": null,
    "dynamicScopeScripts": null,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    },
    "umaType": false,
    "deletable": false,
    "expirationDate": null
  },
  {
    "dn": "inum=43F1,ou=scopes,o=jans",
    "inum": "43F1",
    "displayName": "view_profile",
    "id": "profile",
    "iconUrl": null,
    "description": "View your basic profile info.",
    "scopeType": "openid",
    "claims": [
      "inum=2B29,ou=attributes,o=jans",
      "inum=0C85,ou=attributes,o=jans",
      "inum=B4B0,ou=attributes,o=jans",
      "inum=A0E8,ou=attributes,o=jans",
      "inum=5EC6,ou=attributes,o=jans",
      "inum=B52A,ou=attributes,o=jans",
      "inum=64A0,ou=attributes,o=jans",
      "inum=EC3A,ou=attributes,o=jans",
      "inum=3B47,ou=attributes,o=jans",
      "inum=3692,ou=attributes,o=jans",
      "inum=98FC,ou=attributes,o=jans",
      "inum=A901,ou=attributes,o=jans",
      "inum=36D9,ou=attributes,o=jans",
      "inum=BE64,ou=attributes,o=jans",
      "inum=6493,ou=attributes,o=jans"
    ],
    "defaultScope": false,
    "groupClaims": null,
    "dynamicScopeScripts": null,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    },
    "umaType": false,
    "deletable": false,
    "expirationDate": null
  },
  {
    "dn": "inum=D491,ou=scopes,o=jans",
    "inum": "D491",
    "displayName": "view_phone_number",
    "id": "phone",
    "iconUrl": null,
    "description": "View your phone number.",
    "scopeType": "openid",
    "claims": [
      "inum=B17A,ou=attributes,o=jans",
      "inum=0C18,ou=attributes,o=jans"
    ],
    "defaultScope": false,
    "groupClaims": null,
    "dynamicScopeScripts": null,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    },
    "umaType": false,
    "deletable": false,
    "expirationDate": null
  },
  {
    "dn": "inum=C17A,ou=scopes,o=jans",
    "inum": "C17A",
    "displayName": "view_address",
    "id": "address",
    "iconUrl": null,
    "description": "View your address.",
    "scopeType": "openid",
    "claims": [
      "inum=27DB,ou=attributes,o=jans",
      "inum=2A3D,ou=attributes,o=jans",
      "inum=6609,ou=attributes,o=jans",
      "inum=6EEB,ou=attributes,o=jans",
      "inum=BCE8,ou=attributes,o=jans",
      "inum=D90B,ou=attributes,o=jans",
      "inum=E6B8,ou=attributes,o=jans",
      "inum=E999,ou=attributes,o=jans"
    ],
    "defaultScope": false,
    "groupClaims": true,
    "dynamicScopeScripts": null,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    },
    "umaType": false,
    "deletable": false,
    "expirationDate": null
  },
  {
    "dn": "inum=764C,ou=scopes,o=jans",
    "inum": "764C",
    "displayName": "view_email_address",
    "id": "email",
    "iconUrl": null,
    "description": "View your email address.",
    "scopeType": "openid",
    "claims": [
      "inum=8F88,ou=attributes,o=jans",
      "inum=CAE3,ou=attributes,o=jans"
    ],
    "defaultScope": false,
    "groupClaims": null,
    "dynamicScopeScripts": null,
    "umaAuthorizationPolicies": null,
    "attributes": {
      "spontaneousClientId": null,
      "spontaneousClientScopes": null,
      "showInConfigurationEndpoint": true
    },
    "umaType": false,
    "deletable": false,
    "expirationDate": null
  }
]

Selection: 

```

## Creating Scopes

You can create a scope through the command line interface.
It will ask to enter value for each property.


```

Selection: 2

«A human-readable name of the scope. Type: string»
displayName: testScope

«The base64url encoded id. Type: string»
id: tScope

«A human-readable string describing the scope. Type: string»
description: creating scope

«The scopes type associated with Access Tokens determine what resources will. Type: string»
scopeType: openid

Populate optional fields? n
Obtained Data:

{
  "dn": null,
  "inum": null,
  "displayName": "testScope",
  "id": "tScope",
  "iconUrl": null,
  "description": "creating scope",
  "scopeType": "openid",
  "claims": null,
  "defaultScope": null,
  "groupClaims": null,
  "dynamicScopeScripts": null,
  "umaAuthorizationPolicies": null,
  "attributes": null,
  "umaType": false,
  "deletable": false,
  "expirationDate": null
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/scopes.write
Please wait while posting data ...

{
  "dn": "inum=070daa9e-4a8f-423a-8681-f578673a2781,ou=scopes,o=jans",
  "inum": "070daa9e-4a8f-423a-8681-f578673a2781",
  "displayName": "testScope",
  "id": "tScope",
  "iconUrl": null,
  "description": "creating scope",
  "scopeType": "openid",
  "claims": null,
  "defaultScope": null,
  "groupClaims": null,
  "dynamicScopeScripts": null,
  "umaAuthorizationPolicies": null,
  "attributes": {
    "spontaneousClientId": null,
    "spontaneousClientScopes": null,
    "showInConfigurationEndpoint": true
  },
  "umaType": false,
  "deletable": false,
  "expirationDate": null
}

Selection: 
```

