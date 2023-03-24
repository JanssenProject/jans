---
tags:
  - administration
  - config-api
---

# Config-api-configuration

## Overview
[Jans Config Api](https://github.com/JanssenProject/jans/tree/vreplace-janssen-version/jans-config-api) configuration enables to manage application-level configuration.

![](https://github.com/JanssenProject/jans/raw/vreplace-janssen-version/docs/assets/config-api-configuration.png)

### Existing Config-api dynamic configuration

> ```javascript
>{
>  "configOauthEnabled": true,
>  "apiApprovedIssuer": ["https://jans.server2"],
>  "apiProtectionType": "oauth2",
>  "apiClientId": "1800.0a6a17a0-0d3b-4ce5-881c-f98b2f2b75a7",
>  "apiClientPassword": "BlRk0TlvJp8QJg1vs5e1vw==",
>  "endpointInjectionEnabled": false,
>  "authIssuerUrl": "https://jans.server2",
>  "authOpenidConfigurationUrl": "https://jans.server2/.well-known/openid-configuration",
>  "authOpenidIntrospectionUrl": "https://jans.server2/jans-auth/restv1/introspection",
>  "authOpenidTokenUrl": "https://jans.server2/jans-auth/restv1/token",
>  "authOpenidRevokeUrl": "https://jans.server2/jans-auth/restv1/revoke",
>  "smallryeHealthRootPath": "/health-check",
>  "disableJdkLogger":true,
>  "loggingLevel":"INFO",
>  "loggingLayout":"text",
>  "externalLoggerConfiguration":"",
>  "exclusiveAuthScopes": [
>    "jans_stat",
>    "https://jans.io/scim/users.read",
>    "https://jans.io/scim/users.write"
>  ],
>  "corsConfigurationFilters": [
>    {
>      "filterName": "CorsFilter",
>      "corsAllowedOrigins": "*",
>      "corsAllowedMethods": "GET,PUT,POST,DELETE,PATCH,HEAD,OPTIONS",
>      "corsAllowedHeaders": "",
>      "corsExposedHeaders": "",
>      "corsSupportCredentials": true,
>      "corsLoggingEnabled": false,
>      "corsPreflightMaxAge": 1800,
>      "corsRequestDecorate": true,
>      "corsEnabled": true
>    }
>  ],
>  "userExclusionAttributes": [
>    "userPassword"
>  ],
>   "userMandatoryAttributes": [
>	"mail",
>	"displayName",
>	"jansStatus",
>	"userPassword",
>	"givenName"
>  ],
>  "agamaConfiguration": {
>     "mandatoryAttributes": [
>	 "qname",
>	 "source"
>     ],
>	 "optionalAttributes": [
>	 "serialVersionUID",
>	 "enabled"
>     ]
>  }
>}
> ```

## Revision update

`jansRevision` property of the configuration is used to manage any change
![](https://github.com/JanssenProject/jans/raw/vreplace-janssen-version/docs/assets/config-api-configuration-revision)

### Two options to make effect of the changes done to the configuration

1. Restart jans-config-api
2. Increment the `jansRevision` property of the configuration without restarting the application. The timer job will detect the change and fetch the latest configuration from the DB.

## Important attributes

### OAuth authorization

`configOauthEnabled` property can be used to enable or disable the oAuth2 authorization. By default, its set to true.

> ```javascript
>  ...
> "configOauthEnabled": true
>  ...
>```

### Api protection 

`apiProtectionType` property states the protocol used for API authorization. Currently supported value is `oauth2`.

> ```javascript
>  ...
> "apiProtectionType": "oauth2"
>  ...
> ```

### Api protection auth server

`apiApprovedIssuer` property enables to set more than one authorization servers. By default, the current auth-server is set. You can add more server separated by comma.

> ```javascript
>  ...
> apiApprovedIssuer": ["https://jans.server1,https://jans.server2"]
>  ...
> ```

### Logging level 

`loggingLevel` property can be used to the change the logging level to the desired values `(TRACE, DEBUG, INFO, WARN, ERROR)`. By default, the level is set to `INFO`

> ```javascript
>  ...
> "loggingLevel":"DEBUG",
>  ...
>``

### Scopes other than the one defined by config-api

Config API endpoints are oAuth2 protected. These scopes are created while installation. However, there are few endpoints that require scopes defined by other modules like auth-server. 

A list of these scopes is maintained in configuration in order to avoid creation of these scopes during Config API start-up.
`exclusiveAuthScopes` property can be used to the change the logging level to the desired value `(TRACE, DEBUG, INFO, WARN, ERROR)`. By default, the level is set to `INFO`

> ```javascript
>  ...
>  "exclusiveAuthScopes": [
>    "jans_stat",
>    "https://jans.io/scim/users.read",
>    "https://jans.io/scim/users.write"
>  ],
>  ...
> ```

### User - Mandatory and exclusion attributes

#### MandatoryAttributes 

`userMandatoryAttributes` can be used to define mandatory attributes for User while creation and update.

> ```javascript
>  ...
>   "userMandatoryAttributes": [
>	"mail",
>	"displayName",
>	"jansStatus",
>	"userPassword",
>	"givenName"
>  ],
>  ...
> ```

#### Exclusion attributes

`userExclusionAttributes` can be used to define User attributes that are not to be returned  in API response. More attributes that are to be skipped in response can be added to the list.

> ```javascript
>  ...
>  "userExclusionAttributes": [
>    "userPassword"
>  ],
>  ...
> ```


### Agama flow configuration

`agamaConfiguration` stores Agama related configuration used in Agama related endpoints.

`mandatoryAttributes` list defines required attributes for Agama flow creation and update.
`optionalAttributes` list specify the optional attributes.

> ```javascript
>  ...
>  "agamaConfiguration": {
>     "mandatoryAttributes": [
>	 "qname",
>	 "source"
>     ],
>	 "optionalAttributes": [
>	 "serialVersionUID",
>	 "enabled"
>     ]
>  }
>  ...
> ```
