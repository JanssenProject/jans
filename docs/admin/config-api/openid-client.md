---
tags:
  - administration
  - config-api
  - endpoints
---

# OpenID Connect Client

OpenID Connect Client is Relying Parties(RP) using the OAuth Implicit Flow.

------------------------------------------------------------------------------------------

## Listing existing OpenID Connect clients

<details>
 <summary><code>GET</code> <code><b>/</b></code> <code>(gets list of OpenID Connect clients based on search parameters)</code></summary>

### Parameters

> | name       |  param type | data type      | type      |default value | description                                                                     |
> |------------|-------------|----------------|-----------|--------------|---------------------------------------------------------------------------------|
> | limit      |  query      | integer        | optional  |50            |Search size - max size of the results to return                                  |
> | pattern    |  query      | string         | optional  |N/A           |Comma separated search patter. E.g. `pattern=edu`, `pattern=edu,locale,License`  |
> | startIndex |  query      | integer        | optional  |1             |Index of the first query result                                                  |
> | sortBy     |  query      | string         | optional  |inum          |Field whose value will be used to order the returned response                |
> | sortOrder  |  query      | string         | optional  |ascending     |Search size - max size of the results to return                                  |


### Responses

> | http code     | content-type                      | response                                                            |
> |---------------|-----------------------------------|---------------------------------------------------------------------|
> | `200`         | `application/json`                | `Paginated result`                                                  |
> | `401`         | `application/json`                | `{"code":"401","message":"Unauthorized"}`                           |
> | `500`         | `application/json`                | `{"code":"500","message":"Error msg"}`                              |

### Example cURL

> ```javascript
>  curl -k -i -H "Accept: application/json" -H "Content-Type: application/json" -H "Authorization:Bearer 697479e0-e6f4-453d-bf7a-ddf31b53efba" -X GET http://my.jans.server/jans-config-api/api/v1/openid/clients?limit=3&pattern=test&startIndex=1&includeSource=true
> ```

### Sample Response
> ```javascript
>{
>    "start": 0,
>    "totalEntriesCount": 5,
>    "entriesCount": 3,
>    "entries": [
>        {
>            "dn": "inum=FF81-2D39,ou=clients,o=jans",
>            "clientSecret": "FF81-2D39-jans",
>            "frontChannelLogoutSessionRequired": false,
>            "redirectUris": [
>                "https://jans.server2/jans-auth-rp/home.htm",
>                "https://client.example.com/cb",
>                "https://client.example.com/cb1",
>                "https://client.example.com/cb2"
>            ],
>            "claimRedirectUris": [
>                "https://jans.server2/jans-auth/restv1/uma/gather_claims"
>            ],
>            "responseTypes": [
>                "token",
>                "code",
>                "id_token"
>            ],
>            "grantTypes": [
>                "authorization_code",
>                "implicit",
>                "refresh_token",
>                "client_credentials"
>            ],
>            "applicationType": "web",
>            "clientName": "Jans Test Client (don't remove)",
>            "clientNameLocalized": {},
>            "logoUriLocalized": {},
>            "clientUriLocalized": {},
>            "policyUriLocalized": {},
>            "tosUriLocalized": {},
>            "subjectType": "public",
>            "idTokenSignedResponseAlg": "RS256",
>            "tokenEndpointAuthMethod": "client_secret_basic",
>            "scopes": [
>                "inum=F0C4,ou=scopes,o=jans",
>                "inum=10B2,ou=scopes,o=jans",
>                "inum=764C,ou=scopes,o=jans",
>                "inum=43F1,ou=scopes,o=jans",
>                "inum=341A,ou=scopes,o=jans",
>                "inum=6D99,ou=scopes,o=jans"
>            ],
>            "trustedClient": true,
>            "persistClientAuthorizations": false,
>            "includeClaimsInIdToken": false,
>            "customAttributes": [],
>            "customObjectClasses": [
>                "top"
>            ],
>            "rptAsJwt": false,
>            "accessTokenAsJwt": false,
>            "disabled": false,
>            "attributes": {
>                "runIntrospectionScriptBeforeJwtCreation": false,
>                "keepClientAuthorizationAfterExpiration": false,
>                "allowSpontaneousScopes": false,
>                "backchannelLogoutSessionRequired": false,
>                "parLifetime": 600,
>                "requirePar": false,
>                "jansDefaultPromptLogin": false
>            },
>            "displayName": "Jans Test Client (don't remove)",
>            "authenticationMethod": "client_secret_basic",
>            "tokenBindingSupported": false,
>            "baseDn": "inum=FF81-2D39,ou=clients,o=jans",
>            "inum": "FF81-2D39"
>        },
>        {
>            "dn": "inum=AB77-1A2B,ou=clients,o=jans",
>            "clientSecret": "AB77-1A2B-jans",
>            "frontChannelLogoutSessionRequired": false,
>            "redirectUris": [
>                "https://client.example.com/cb"
>            ],
>            "claimRedirectUris": [
>                "https://jans.server2/jans-auth/restv1/uma/gather_claims"
>            ],
>            "responseTypes": [
>                "code",
>                "id_token"
>            ],
>            "grantTypes": [
>                "authorization_code",
>                "implicit",
>                "refresh_token",
>                "client_credentials"
>            ],
>            "applicationType": "web",
>            "clientName": "Jans Test Resource Server Client (don't remove)",
>            "clientNameLocalized": {},
>            "logoUriLocalized": {},
>            "clientUriLocalized": {},
>            "policyUriLocalized": {},
>            "tosUriLocalized": {},
>            "subjectType": "public",
>            "idTokenSignedResponseAlg": "RS256",
>            "tokenEndpointAuthMethod": "client_secret_basic",
>            "scopes": [
>                "inum=6D99,ou=scopes,o=jans",
>                "inum=7D90,ou=scopes,o=jans"
>            ],
>            "trustedClient": true,
>            "persistClientAuthorizations": false,
>            "includeClaimsInIdToken": false,
>            "customAttributes": [],
>            "customObjectClasses": [
>                "top"
>            ],
>            "rptAsJwt": false,
>            "accessTokenAsJwt": false,
>            "disabled": false,
>            "attributes": {
>                "runIntrospectionScriptBeforeJwtCreation": false,
>                "keepClientAuthorizationAfterExpiration": false,
>                "allowSpontaneousScopes": false,
>                "backchannelLogoutSessionRequired": false,
>                "parLifetime": 600,
>                "requirePar": false,
>                "jansDefaultPromptLogin": false
>            },
>            "displayName": "Jans Test Resource Server Client (don't remove)",
>            "authenticationMethod": "client_secret_basic",
>            "tokenBindingSupported": false,
>            "baseDn": "inum=AB77-1A2B,ou=clients,o=jans",
>            "inum": "AB77-1A2B"
>        },
>        {
>            "dn": "inum=3E20,ou=clients,o=jans",
>            "clientSecret": "3E20-jans",
>            "frontChannelLogoutSessionRequired": false,
>            "redirectUris": [
>                "https://client.example.com/cb"
>            ],
>            "responseTypes": [
>                "code",
>                "id_token"
>            ],
>            "grantTypes": [
>                "authorization_code",
>                "implicit",
>                "refresh_token",
>                "client_credentials"
>            ],
>            "applicationType": "web",
>            "clientName": "Jans Test Requesting Party Client (don't remove)",
>            "clientNameLocalized": {},
>            "logoUriLocalized": {},
>            "clientUriLocalized": {},
>            "policyUriLocalized": {},
>            "tosUriLocalized": {},
>            "subjectType": "public",
>            "idTokenSignedResponseAlg": "RS256",
>            "tokenEndpointAuthMethod": "client_secret_basic",
>            "trustedClient": true,
>            "persistClientAuthorizations": false,
>            "includeClaimsInIdToken": false,
>            "customAttributes": [],
>            "customObjectClasses": [
>                "top"
>            ],
>            "rptAsJwt": false,
>            "accessTokenAsJwt": false,
>            "disabled": false,
>            "attributes": {
>                "runIntrospectionScriptBeforeJwtCreation": false,
>                "keepClientAuthorizationAfterExpiration": false,
>                "allowSpontaneousScopes": false,
>                "backchannelLogoutSessionRequired": false,
>                "parLifetime": 600,
>                "requirePar": false,
>                "jansDefaultPromptLogin": false
>            },
>            "displayName": "Jans Test Requesting Party Client (don't remove)",
>            "authenticationMethod": "client_secret_basic",
>            "tokenBindingSupported": false,
>            "baseDn": "inum=3E20,ou=clients,o=jans",
>            "inum": "3E20"
>        }
>    ]
>}
> ```

</details>


<details>
  <summary><code>GET</code> <code><b>/{inum}</b></code> <code>(gets OpenID Connect client based on inum)</code></summary>

### Parameters

> | name       |  param type | data type      | type      |default value | description                            |
> |------------|-------------|----------------|-----------|--------------|----------------------------------------|
> | `inum`     |  path       | string         | required  | NA           | OpenID Connect client unique idendifier|

### Responses

> | http code     | content-type                      | response                                                            |
> |---------------|-----------------------------------|---------------------------------------------------------------------|
> | `200`         | `application/json        `        | `OpenID Connect client details`                                     |
> | `401`         | `application/json`                | `{"code":"401","message":"Unauthorized"}`                           |
> | `401`         | `application/json`                | `{"code":"404","message":"Not Found"}`                           |
> | `500`         | `application/json`                | `{"code":"500","message":"Error msg"}`                              |

### Example cURL

> ```javascript
>  curl -k -i -H "Accept: application/json" -H "Content-Type: application/json" -H "Authorization:Bearer 697479e0-e6f4-453d-bf7a-ddf31b53efba" -X GET http://my.jans.server/jans-config-api/api/v1/openid/clients/bd27a9f6-7772-4049-bd4f-bf7c651fbe7c
> ```

### Sample Response

> ```javascript
>{
>    "dn": "inum=bd27a9f6-7772-4049-bd4f-bf7c651fbe7c,ou=clients,o=jans",
>    "deletable": false,
>    "clientSecret": "WonHg253UDJmtl7d55z1K0PWWEZ3N9Xg+O33ibJ1JwCVs4ynLhjPxQ==",
>    "frontChannelLogoutSessionRequired": false,
>    "redirectUris": [
>        "https://abc,com"
>    ],
>    "responseTypes": [
>        "code"
>    ],
>    "grantTypes": [
>        "refresh_token",
>        "authorization_code"
>    ],
>    "applicationType": "web",
>    "clientName": "test1234",
>    "clientNameLocalized": {},
>    "logoUriLocalized": {},
>    "clientUriLocalized": {},
>    "policyUriLocalized": {},
>    "tosUriLocalized": {},
>    "subjectType": "public",
>    "tokenEndpointAuthMethod": "client_secret_basic",
>    "scopes": [
>        "inum=43F1,ou=scopes,o=jans",
>        "inum=C17A,ou=scopes,o=jans",
>        "inum=764C,ou=scopes,o=jans"
>    ],
>    "trustedClient": false,
>    "persistClientAuthorizations": false,
>    "includeClaimsInIdToken": false,
>    "customAttributes": [],
>    "customObjectClasses": [
>        "top",
>        "jansClntCustomAttributes"
>    ],
>    "rptAsJwt": false,
>    "accessTokenAsJwt": false,
>    "disabled": false,
>    "attributes": {
>        "runIntrospectionScriptBeforeJwtCreation": false,
>        "keepClientAuthorizationAfterExpiration": false,
>        "allowSpontaneousScopes": false,
>        "backchannelLogoutSessionRequired": false,
>        "parLifetime": 600,
>        "requirePar": false,
>        "jansDefaultPromptLogin": false
>    },
>    "backchannelUserCodeParameter": false,
>    "description": "test1234",
>    "displayName": "test1234",
>    "authenticationMethod": "client_secret_basic",
>    "tokenBindingSupported": false,
>    "baseDn": "inum=bd27a9f6-7772-4049-bd4f-bf7c651fbe7c,ou=clients,o=jans",
>    "inum": "bd27a9f6-7772-4049-bd4f-bf7c651fbe7c"
>}
> ```

</details>

------------------------------------------------------------------------------------------

## Creating new OpenID Connect client

<details>
  <summary><code>POST</code> <code><b>/{inum}</b></code> <code>(creates a new OpenID Connect client)</code></summary>

### Parameters

> | name       |  param type | data type      | type      |default value | description                            |
> |------------|-------------|----------------|-----------|--------------|----------------------------------------|
> | None       |  request    | object (JSON)  | required  | NA           | OpenID Connect client json                         |

### Responses

> | http code     | content-type                      | response                                                            |
> |---------------|-----------------------------------|---------------------------------------------------------------------|
> | `201`         | `application/json        `        | `OpenID Connect client json`                                                 |
> | `401`         | `application/json`                | `{"code":"401","message":"Unauthorized"}`                           |
> | `500`         | `application/json`                | `{"code":"500","message":"Error msg"}`                              |

### Example cURL

> ```javascript
>  curl -X POST -k -H 'Content-Type: application/json' -H 'Authorization: Bearer ba9b8810-7a2b-4e4a-a18a-689d7eacf7d1' -i 'https://my.jans.server/jans-config-api/api/v1/openid/clients' --data @post.json
> ```

### Sample Request

> ```javascript
>{
>  "clientName": "test1234",
>  "description": "test1234",
>  "expirable": [],
>  "softwareSection": false,
>  "cibaSection": false,
>  "backchannelUserCodeParameter": false,
>  "redirectUris": [
>    "https://abc,com"
>  ],
>  "claimRedirectUris": [],
>  "authorizedOrigins": [],
>  "requestUris": [],
>  "postLogoutRedirectUris": [],
>  "responseTypes": [],
>  "grantTypes": [],
>  "scopes": [
>    "inum=43F1,ou=scopes,o=jans",
>    "inum=C17A,ou=scopes,o=jans",
>    "inum=764C,ou=scopes,o=jans"
>  ],
>  "attributes": {
>    "tlsClientAuthSubjectDn": null,
>    "runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims": false,
>    "keepClientAuthorizationAfterExpiration": false,
>    "allowSpontaneousScopes": false,
>    "backchannelLogoutSessionRequired": false,
>    "backchannelLogoutUri": [],
>    "rptClaimsScripts": [],
>    "consentGatheringScripts": [],
>    "spontaneousScopeScriptDns": [],
>    "introspectionScripts": [],
>    "postAuthnScripts": [],
>    "additionalAudience": [],
>    "spontaneousScopes": [],
>    "redirectUrisRegex": "",
>    "parLifetime": "",
>    "requirePar": false,
>    "jansDefaultPromptLogin": false,
>    "authorizedAcrValues": [],
>    "updateTokenScriptDns": [],
>    "ropcScripts": [],
>    "jansAuthSignedRespAlg": "",
>    "jansAuthEncRespAlg": "",
>    "jansAuthEncRespEnc": ""
>  },
>  "tlsClientAuthSubjectDn": null,
>  "frontChannelLogoutSessionRequired": false,
>  "runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims": false,
>  "backchannelLogoutSessionRequired": false,
>  "keepClientAuthorizationAfterExpiration": false,
>  "allowSpontaneousScopes": false,
>  "spontaneousScopes": [],
>  "introspectionScripts": [],
>  "spontaneousScopeScriptDns": [],
>  "consentGatheringScripts": [],
>  "redirectUrisRegex": "",
>  "parLifetime": "",
>  "requirePar": false,
>  "updateTokenScriptDns": [],
>  "ropcScripts": [],
>  "jansAuthSignedRespAlg": "",
>  "jansAuthEncRespAlg": "",
>  "jansAuthEncRespEnc": "",
>  "postAuthnScripts": [],
>  "rptClaimsScripts": [],
>  "additionalAudience": [],
>  "backchannelLogoutUri": [],
>  "jansDefaultPromptLogin": false,
>  "authorizedAcrValues": [],
>  "customObjectClasses": [],
>  "requireAuthTime": false,
>  "trustedClient": false,
>  "persistClientAuthorizations": false,
>  "includeClaimsInIdToken": false,
>  "rptAsJwt": false,
>  "accessTokenAsJwt": false,
>  "disabled": false,
>  "action_message": "test1234test1234"
>}
> ```


</details>

------------------------------------------------------------------------------------------

## Updating existing OpenID Connect client

<details>
  <summary><code>PUT</code> <code><b>/{inum}</b></code> <code>(updates an existings OpenID Connect client)</code></summary>

### Parameters

> | name       |  param type | data type      | type      |default value | description                            |
> |------------|-------------|----------------|-----------|--------------|----------------------------------------|
> | None       |  request    | object (JSON)  | required  | NA           | OpenID Connect client json                         |

### Responses

> | http code     | content-type                      | response                                                                      |
> |---------------|-----------------------------------|-------------------------------------------------------------------------------|
> | `200`         | `application/json        `        | `OpenID Connect client json`                                                  |
> | `404`         | `application/json`                | `{"code":"404","message":"The requested OpenID Connect client doesn't exist"}`|
> | `401`         | `application/json`                | `{"code":"401","message":"Unauthorized"}`                                     |
> | `500`         | `application/json`                | `{"code":"500","message":"Error msg"}`                                        |

### Example cURL

> ```javascript
>  curl -X PUT -k -H 'Content-Type: application/json' -H 'Authorization: Bearer ba9b8810-7a2b-4e4a-a18a-689d7eacf7d1' -i 'https://my.jans.server/jans-config-api/api/v1/openid/clients' --data @put.json
> ```

### Sample Request

> ```javascript
>{
>    "dn": "inum=bd27a9f6-7772-4049-bd4f-bf7c651fbe7c,ou=clients,o=jans",
>    "deletable": false,
>    "clientSecret": "c0b5ce54-d1e0-4a22-999a-8bcd055a3bc2",
>    "frontChannelLogoutSessionRequired": false,
>    "redirectUris": [
>        "https://abc,com"
>    ],
>    "responseTypes": [
>        "code"
>    ],
>    "grantTypes": [
>        "refresh_token",
>        "authorization_code"
>    ],
>    "applicationType": "web",
>    "clientName": "test1234",
>    "clientNameLocalized": {},
>    "logoUriLocalized": {},
>    "clientUriLocalized": {},
>    "policyUriLocalized": {},
>    "tosUriLocalized": {},
>    "subjectType": "public",
>    "tokenEndpointAuthMethod": "client_secret_basic",
>    "scopes": [
>        "inum=43F1,ou=scopes,o=jans",
>        "inum=C17A,ou=scopes,o=jans",
>        "inum=764C,ou=scopes,o=jans"
>    ],
>    "trustedClient": false,
>    "persistClientAuthorizations": false,
>    "includeClaimsInIdToken": false,
>    "customAttributes": [],
>    "customObjectClasses": [
>        "top",
>        "jansClntCustomAttributes"
>    ],
>    "rptAsJwt": false,
>    "accessTokenAsJwt": false,
>    "disabled": false,
>    "attributes": {
>        "runIntrospectionScriptBeforeJwtCreation": false,
>        "keepClientAuthorizationAfterExpiration": false,
>        "allowSpontaneousScopes": false,
>        "backchannelLogoutSessionRequired": false,
>        "parLifetime": 600,
>        "requirePar": false,
>        "jansDefaultPromptLogin": false
>    },
>    "backchannelUserCodeParameter": false,
>    "description": "test1234",
>    "displayName": "test1234",
>    "authenticationMethod": "client_secret_basic",
>    "tokenBindingSupported": false,
>    "baseDn": "inum=bd27a9f6-7772-4049-bd4f-bf7c651fbe7c,ou=clients,o=jans",
>    "inum": "bd27a9f6-7772-4049-bd4f-bf7c651fbe7c"
>}
> ```

</details>

------------------------------------------------------------------------------------------

## Patching existing OpenID Connect client

<details>
  <summary><code>PATCH</code> <code><b>/{inum}</b></code> <code>(patches an existing OpenID Connect client)</code></summary>

### Parameters

> | name       |  param type | data type          | type      |default value | description                             |
> |------------|-------------|--------------------|-----------|--------------|-----------------------------------------|
> | inum       |  path       | string             | required  | NA           | OpenID Connect client unique idendifier |
> | None       |  request    | json-patch object  | required  | NA           | json-patch request                      |


### Responses

> | http code     | content-type                      | response                                                               |
> |---------------|-----------------------------------|------------------------------------------------------------------------|
> | `200`         | `application/json        `        | `OpenID Connect client details`                                                    |
> | `404`         | `application/json`                | `{"code":"404","message":"The requested <inum> doesn't exist"}`        |
> | `401`         | `application/json`                | `{"code":"401","message":"Unauthorized"}`                              |
> | `500`         | `application/json`                | `{"code":"500","message":"Error msg"}`                                 |

### Example cURL

> ```javascript
>  curl -X PATCH -k -H 'Content-Type: application/json-patch+json' -H 'Authorization: Bearer ba9b8810-7a2b-4e4a-a18a-689d7eacf7d1' -i 'https://my.jans.server/jans-config-api/api/v1/openid/clients/f8c1a111-0919-47e8-a4d4-f7c18f73a644' --data @patch.json
> ```

### Sample Request

> ```javascript
> [{ "op": "replace", "path": "/responseTypes", "value":["code","token"]}] 
> ```

</details>

------------------------------------------------------------------------------------------

## Deleting existing OpenID Connect client

<details>
  <summary><code>DELETE</code> <code><b>/{inum}</b></code> <code>(deletes an existings OpenID Connect client)</code></summary>

### Parameters

> | name       |  param type | data type          | type      |default value | description                             |
> |------------|-------------|--------------------|-----------|--------------|-----------------------------------------|
> | inum       |  path       | string             | required  | NA           | OpenID Connect client unique idendifier |


### Responses

> | http code     | content-type                      | response                                                               |
> |---------------|-----------------------------------|------------------------------------------------------------------------|
> | `204`         | `application/json        `        | `No Content`                                                    |
> | `404`         | `application/json`                | `{"code":"404","message":"The requested <inum> doesn't exist"}`        |
> | `401`         | `application/json`                | `{"code":"401","message":"Unauthorized"}`                              |
> | `500`         | `application/json`                | `{"code":"500","message":"Error msg"}`                                 |

### Example cURL

> ```javascript
>  curl -X DELETE -k -H 'Content-Type: application/json' -H 'Authorization: Bearer ba9b8810-7a2b-4e4a-a18a-689d7eacf7d1' -i 'https://my.jans.server/jans-config-api/api/v1/openid/clients/f8c1a111-0919-47e8-a4d4-f7c18f73a644'
> ```

### Sample Request
> None

</details>
------------------------------------------------------------------------------------------
