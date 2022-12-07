---
tags:
  - administration
  - configuration
  - cli
  - interactive
---

# OpenID Connect Configuration

!!! Important
    The interactive mode of the CLI will be deprecated upon the full release of the Configuration TUI in the coming months.

> Prerequisite: Know how to use the Janssen CLI in [interactive mode](im-index.md)

OpenID Connect Interactive Mode supports the following list of actions:

```text
OAuth - OpenID Connect - Clients
--------------------------------
1 Gets list of OpenID Connect clients
2 Create new OpenId connect client
3 Update OpenId Connect client
4 Get OpenId Connect Client by Inum
5 Delete OpenId Connect client
6 Update modified properties of OpenId Connect client by Inum
```
Using Janssen CLI, the Administrator can easily `create/update/delete` OpenID Connect without any interruption.

## Get list of OpenID Connect clients

By selecting option '1' you will get a list of OpenID Connect clients.
You may enter `limit[50]` and `pattern` to filter in searching.

```
Gets list of OpenID Connect clients
-----------------------------------

«Search size - max size of the results to return. Type: integer»
limit  [50]: 

«Search pattern. Type: string»
pattern: 
Calling Api with parameters: {'limit': 50}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/openid/clients.readonly
[
  {
    "dn": "inum=1801.d361f68d-8200-4ba2-a0bb-ca7fea79e805,ou=clients,o=jans",
    "inum": "1801.d361f68d-8200-4ba2-a0bb-ca7fea79e805",
    "clientSecret": "KfwZeAfq4jrL",
    "frontChannelLogoutUri": null,
    "frontChannelLogoutSessionRequired": false,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": [
      "https://testjans.gluu.com/admin-ui",
      "http//:localhost:4100"
    ],
    "claimRedirectUris": null,
    "responseTypes": [
      "code"
    ],
    "grantTypes": [
      "authorization_code",
      "refresh_token",
      "client_credentials"
    ],
    "applicationType": "web",
    "contacts": null,
    "clientName": "Jans Config Api Client",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": null,
    "sectorIdentifierUri": null,
    "subjectType": "pairwise",
    "idTokenSignedResponseAlg": "RS256",
    "idTokenEncryptedResponseAlg": null,
    "idTokenEncryptedResponseEnc": null,
    "userInfoSignedResponseAlg": null,
    "userInfoEncryptedResponseAlg": null,
    "userInfoEncryptedResponseEnc": null,
    "requestObjectSigningAlg": null,
    "requestObjectEncryptionAlg": null,
    "requestObjectEncryptionEnc": null,
    "tokenEndpointAuthMethod": "client_secret_basic",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": null,
    "requestUris": null,
    "scopes": [
      "inum=1800.F6E877,ou=scopes,o=jans",
      "inum=1800.D4F3E7,ou=scopes,o=jans",
      "inum=1800.2FD7EF,ou=scopes,o=jans",
      "inum=1800.97B23C,ou=scopes,o=jans",
      "inum=1800.8FC2C7,ou=scopes,o=jans",
      "inum=1800.1FFDF2,ou=scopes,o=jans",
      "inum=1800.5CF44C,ou=scopes,o=jans",
      "inum=1800.CCA518,ou=scopes,o=jans",
      "inum=1800.E62D6E,ou=scopes,o=jans",
      "inum=1800.11CB33,ou=scopes,o=jans",
      "inum=1800.781FA2,ou=scopes,o=jans",
      "inum=1800.ADAD8F,ou=scopes,o=jans",
      "inum=1800.40F22F,ou=scopes,o=jans",
      "inum=1800.7619BA,ou=scopes,o=jans",
      "inum=1800.E0DAF5,ou=scopes,o=jans",
      "inum=1800.7F45B0,ou=scopes,o=jans",
      "inum=1800.778C57,ou=scopes,o=jans",
      "inum=1800.E39293,ou=scopes,o=jans",
      "inum=1800.939483,ou=scopes,o=jans",
      "inum=1800.0ED2E8,ou=scopes,o=jans",
      "inum=1800.66CA59,ou=scopes,o=jans",
      "inum=1800.A4DBE5,ou=scopes,o=jans",
      "inum=1800.9AF358,ou=scopes,o=jans",
      "inum=1800.478CCF,ou=scopes,o=jans",
      "inum=1800.450A9A,ou=scopes,o=jans",
      "inum=1800.27A193,ou=scopes,o=jans",
      "inum=1800.3971D5,ou=scopes,o=jans",
      "inum=1800.891693,ou=scopes,o=jans",
      "inum=1800.A35DFD,ou=scopes,o=jans",
      "inum=1800.3516DE,ou=scopes,o=jans",
      "inum=F0C4,ou=scopes,o=jans",
      "inum=764C,ou=scopes,o=jans",
      "inum=10B2,ou=scopes,o=jans"
    ],
    "claims": null,
    "trustedClient": false,
    "lastAccessTime": null,
    "lastLogonTime": null,
    "persistClientAuthorizations": true,
    "includeClaimsInIdToken": false,
    "refreshTokenLifetime": null,
    "accessTokenLifetime": null,
    "customAttributes": [],
    "customObjectClasses": [
      "top"
    ],
    "rptAsJwt": false,
    "accessTokenAsJwt": false,
    "accessTokenSigningAlg": "RS256",
    "disabled": false,
    "authorizedOrigins": null,
    "softwareId": null,
    "softwareVersion": null,
    "softwareStatement": null,
    "attributes": {
      "tlsClientAuthSubjectDn": null,
      "runIntrospectionScriptBeforeJwtCreation": false,
      "keepClientAuthorizationAfterExpiration": false,
      "allowSpontaneousScopes": false,
      "spontaneousScopes": null,
      "spontaneousScopeScriptDns": null,
      "backchannelLogoutUri": null,
      "backchannelLogoutSessionRequired": false,
      "additionalAudience": null,
      "postAuthnScripts": null,
      "consentGatheringScripts": null,
      "introspectionScripts": null,
      "rptClaimsScripts": null
    },
    "backchannelTokenDeliveryMode": null,
    "backchannelClientNotificationEndpoint": null,
    "backchannelAuthenticationRequestSigningAlg": null,
    "backchannelUserCodeParameter": null,
    "expirationDate": null,
    "deletable": false,
    "jansId": null
  },
  {
    "dn": "inum=1001.0e964ce7-7670-44a4-a2d1-d0a5f689a34f,ou=clients,o=jans",
    "inum": "1001.0e964ce7-7670-44a4-a2d1-d0a5f689a34f",
    "clientSecret": "4OJLToBXav0P",
    "frontChannelLogoutUri": "https://testjans.gluu.com/identity/ssologout.htm",
    "frontChannelLogoutSessionRequired": true,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": [
      "https://testjans.gluu.com/identity/scim/auth",
      "https://testjans.gluu.com/identity/authcode.htm",
      "https://testjans.gluu.com/jans-auth/restv1/uma/gather_claims?authentication=true"
    ],
    "claimRedirectUris": [
      "https://testjans.gluu.com/jans-auth/restv1/uma/gather_claims"
    ],
    "responseTypes": [
      "code"
    ],
    "grantTypes": [
      "authorization_code",
      "implicit",
      "refresh_token"
    ],
    "applicationType": "web",
    "contacts": null,
    "clientName": "oxTrust Admin GUI",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": null,
    "sectorIdentifierUri": null,
    "subjectType": "public",
    "idTokenSignedResponseAlg": "HS256",
    "idTokenEncryptedResponseAlg": null,
    "idTokenEncryptedResponseEnc": null,
    "userInfoSignedResponseAlg": null,
    "userInfoEncryptedResponseAlg": null,
    "userInfoEncryptedResponseEnc": null,
    "requestObjectSigningAlg": null,
    "requestObjectEncryptionAlg": null,
    "requestObjectEncryptionEnc": null,
    "tokenEndpointAuthMethod": "client_secret_basic",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": [
      "https://testjans.gluu.com/identity/finishlogout.htm"
    ],
    "requestUris": null,
    "scopes": [
      "inum=F0C4,ou=scopes,o=jans",
      "inum=10B2,ou=scopes,o=jans",
      "inum=764C,ou=scopes,o=jans"
    ],
    "claims": null,
    "trustedClient": true,
    "lastAccessTime": null,
    "lastLogonTime": null,
    "persistClientAuthorizations": false,
    "includeClaimsInIdToken": false,
    "refreshTokenLifetime": null,
    "accessTokenLifetime": null,
    "customAttributes": [],
    "customObjectClasses": [
      "top"
    ],
    "rptAsJwt": false,
    "accessTokenAsJwt": false,
    "accessTokenSigningAlg": null,
    "disabled": false,
    "authorizedOrigins": null,
    "softwareId": null,
    "softwareVersion": null,
    "softwareStatement": null,
    "attributes": {
      "tlsClientAuthSubjectDn": null,
      "runIntrospectionScriptBeforeJwtCreation": false,
      "keepClientAuthorizationAfterExpiration": false,
      "allowSpontaneousScopes": false,
      "spontaneousScopes": null,
      "spontaneousScopeScriptDns": null,
      "backchannelLogoutUri": null,
      "backchannelLogoutSessionRequired": false,
      "additionalAudience": null,
      "postAuthnScripts": null,
      "consentGatheringScripts": null,
      "introspectionScripts": null,
      "rptClaimsScripts": null
    },
    "backchannelTokenDeliveryMode": null,
    "backchannelClientNotificationEndpoint": null,
    "backchannelAuthenticationRequestSigningAlg": null,
    "backchannelUserCodeParameter": null,
    "expirationDate": null,
    "deletable": false,
    "jansId": null
  },
  {
    "dn": "inum=1201.d71e6b84-b637-4e26-b8d3-34c80934c097,ou=clients,o=jans",
    "inum": "1201.d71e6b84-b637-4e26-b8d3-34c80934c097",
    "clientSecret": "0vFoEhc7Zut2",
    "frontChannelLogoutUri": null,
    "frontChannelLogoutSessionRequired": false,
    "registrationAccessToken": null,
    "clientIdIssuedAt": null,
    "clientSecretExpiresAt": null,
    "redirectUris": null,
    "claimRedirectUris": null,
    "responseTypes": null,
    "grantTypes": [
      "client_credentials"
    ],
    "applicationType": "native",
    "contacts": null,
    "clientName": "SCIM client",
    "idTokenTokenBindingCnf": null,
    "logoUri": null,
    "clientUri": null,
    "policyUri": null,
    "tosUri": null,
    "jwksUri": null,
    "jwks": null,
    "sectorIdentifierUri": null,
    "subjectType": "pairwise",
    "idTokenSignedResponseAlg": null,
    "idTokenEncryptedResponseAlg": null,
    "idTokenEncryptedResponseEnc": null,
    "userInfoSignedResponseAlg": null,
    "userInfoEncryptedResponseAlg": null,
    "userInfoEncryptedResponseEnc": null,
    "requestObjectSigningAlg": null,
    "requestObjectEncryptionAlg": null,
    "requestObjectEncryptionEnc": null,
    "tokenEndpointAuthMethod": "client_secret_basic",
    "tokenEndpointAuthSigningAlg": null,
    "defaultMaxAge": null,
    "requireAuthTime": false,
    "defaultAcrValues": null,
    "initiateLoginUri": null,
    "postLogoutRedirectUris": null,
    "requestUris": null,
    "scopes": [
      "inum=1200.841184,ou=scopes,o=jans",
      "inum=1200.98DDA5,ou=scopes,o=jans",
      "inum=1200.F40A49,ou=scopes,o=jans",
      "inum=1200.B609F0,ou=scopes,o=jans",
      "inum=1200.492980,ou=scopes,o=jans",
      "inum=1200.F7EC4A,ou=scopes,o=jans",
      "inum=1200.280C97,ou=scopes,o=jans",
      "inum=1200.E236BB,ou=scopes,o=jans",
      "inum=1200.DC0FDE,ou=scopes,o=jans",
      "inum=1200.2483ED,ou=scopes,o=jans"
    ],
    "claims": null,
    "trustedClient": false,
    "lastAccessTime": null,
    "lastLogonTime": null,
    "persistClientAuthorizations": false,
    "includeClaimsInIdToken": false,
    "refreshTokenLifetime": null,
    "accessTokenLifetime": null,
    "customAttributes": [],
    "customObjectClasses": [
      "top"
    ],
    "rptAsJwt": false,
    "accessTokenAsJwt": false,
    "accessTokenSigningAlg": "RS256",
    "disabled": false,
    "authorizedOrigins": null,
    "softwareId": null,
    "softwareVersion": null,
    "softwareStatement": null,
    "attributes": {
      "tlsClientAuthSubjectDn": null,
      "runIntrospectionScriptBeforeJwtCreation": false,
      "keepClientAuthorizationAfterExpiration": false,
      "allowSpontaneousScopes": false,
      "spontaneousScopes": null,
      "spontaneousScopeScriptDns": null,
      "backchannelLogoutUri": null,
      "backchannelLogoutSessionRequired": false,
      "additionalAudience": null,
      "postAuthnScripts": null,
      "consentGatheringScripts": null,
      "introspectionScripts": null,
      "rptClaimsScripts": null
    },
    "backchannelTokenDeliveryMode": null,
    "backchannelClientNotificationEndpoint": null,
    "backchannelAuthenticationRequestSigningAlg": null,
    "backchannelUserCodeParameter": null,
    "expirationDate": null,
    "deletable": false,
    "jansId": null
  }
]

Selection: 

```

## Create a New OpenID Client

To create a new OpenID client, you need to enter '2' from OpenID Menu.
It will ask to enter below information:

- frontChannelLogoutSessionRequired[false, true]
- applicationType[web, native]
- clientName
- subjectType[pairwise, public]
- includeClaimsInIdToken[false, true]
- Populate optional fields?[y, n]

If you enter `y` to **Populate optional fields?** then you will get a lot of optional fields are listed below:

```
Populate optional fields? y
Optional Fields:
1 clientSecret
2 frontChannelLogoutUri
3 registrationAccessToken
4 clientIdIssuedAt
5 clientSecretExpiresAt
6 redirectUris
7 claimRedirectUris
8 responseTypes
9 grantTypes
10 contacts
11 idTokenTokenBindingCnf
12 logoUri
13 clientUri
14 policyUri
15 tosUri
16 jwksUri
17 jwks
18 sectorIdentifierUri
19 idTokenSignedResponseAlg
20 idTokenEncryptedResponseAlg
21 idTokenEncryptedResponseEnc
22 userInfoSignedResponseAlg
23 userInfoEncryptedResponseAlg
24 userInfoEncryptedResponseEnc
25 requestObjectSigningAlg
26 requestObjectEncryptionAlg
27 requestObjectEncryptionEnc
28 tokenEndpointAuthMethod
29 tokenEndpointAuthSigningAlg
30 defaultMaxAge
31 requireAuthTime
32 defaultAcrValues
33 initiateLoginUri
34 postLogoutRedirectUris
35 requestUris
36 scopes
37 claims
38 trustedClient
39 lastAccessTime
40 lastLogonTime
41 persistClientAuthorizations
42 refreshTokenLifetime
43 accessTokenLifetime
44 customAttributes
45 customObjectClasses
46 rptAsJwt
47 accessTokenAsJwt
48 accessTokenSigningAlg
49 disabled
50 authorizedOrigins
51 softwareId
52 softwareVersion
53 softwareStatement
54 attributes
55 backchannelTokenDeliveryMode
56 backchannelClientNotificationEndpoint
57 backchannelAuthenticationRequestSigningAlg
58 backchannelUserCodeParameter
59 expirationDate
60 deletable
61 jansId

«c: continue, #: populate filed. »

Selection: 1

«The client secret.  The client MAY omit the parameter if the client secret is an empty string. Type: string»
clientSecret: aabbccdd

«c: continue, #: populate filed. »
Selection: c

Obtained Data:

{
  "dn": null,
  "inum": null,
  "clientSecret": "aabbccdd",
  "frontChannelLogoutUri": null,
  "frontChannelLogoutSessionRequired": false,
  "registrationAccessToken": null,
  "clientIdIssuedAt": null,
  "clientSecretExpiresAt": null,
  "redirectUris": null,
  "claimRedirectUris": null,
  "responseTypes": null,
  "grantTypes": null,
  "applicationType": "web",
  "contacts": null,
  "clientName": "newOID",
  "idTokenTokenBindingCnf": null,
  "logoUri": null,
  "clientUri": null,
  "policyUri": null,
  "tosUri": null,
  "jwksUri": null,
  "jwks": null,
  "sectorIdentifierUri": null,
  "subjectType": "pairwise",
  "idTokenSignedResponseAlg": null,
  "idTokenEncryptedResponseAlg": null,
  "idTokenEncryptedResponseEnc": null,
  "userInfoSignedResponseAlg": null,
  "userInfoEncryptedResponseAlg": null,
  "userInfoEncryptedResponseEnc": null,
  "requestObjectSigningAlg": null,
  "requestObjectEncryptionAlg": null,
  "requestObjectEncryptionEnc": null,
  "tokenEndpointAuthMethod": null,
  "tokenEndpointAuthSigningAlg": null,
  "defaultMaxAge": null,
  "requireAuthTime": null,
  "defaultAcrValues": null,
  "initiateLoginUri": null,
  "postLogoutRedirectUris": null,
  "requestUris": null,
  "scopes": null,
  "claims": null,
  "trustedClient": false,
  "lastAccessTime": null,
  "lastLogonTime": null,
  "persistClientAuthorizations": null,
  "includeClaimsInIdToken": false,
  "refreshTokenLifetime": null,
  "accessTokenLifetime": null,
  "customAttributes": null,
  "customObjectClasses": null,
  "rptAsJwt": null,
  "accessTokenAsJwt": null,
  "accessTokenSigningAlg": null,
  "disabled": false,
  "authorizedOrigins": null,
  "softwareId": null,
  "softwareVersion": null,
  "softwareStatement": null,
  "attributes": null,
  "backchannelTokenDeliveryMode": null,
  "backchannelClientNotificationEndpoint": null,
  "backchannelAuthenticationRequestSigningAlg": null,
  "backchannelUserCodeParameter": null,
  "expirationDate": null,
  "deletable": false,
  "jansId": null
}

Continue? y

Getting access token for scope https://jans.io/oauth/config/openid/clients.write
Please wait while posting data ...

{
  "dn": "inum=1929a64c-6f67-4399-bdd3-6a8d44cc04ae,ou=clients,o=jans",
  "inum": "1929a64c-6f67-4399-bdd3-6a8d44cc04ae",
  "clientSecret": "B3ziSqU8gWTAXICdYfNxw2cP4LmwDqrG1koRqzFxQc0=",
  "frontChannelLogoutUri": null,
  "frontChannelLogoutSessionRequired": false,
  "registrationAccessToken": null,
  "clientIdIssuedAt": null,
  "clientSecretExpiresAt": null,
  "redirectUris": null,
  "claimRedirectUris": null,
  "responseTypes": null,
  "grantTypes": [],
  "applicationType": "web",
  "contacts": null,
  "clientName": "newOID",
  "idTokenTokenBindingCnf": null,
  "logoUri": null,
  "clientUri": null,
  "policyUri": null,
  "tosUri": null,
  "jwksUri": null,
  "jwks": null,
  "sectorIdentifierUri": null,
  "subjectType": "pairwise",
  "idTokenSignedResponseAlg": null,
  "idTokenEncryptedResponseAlg": null,
  "idTokenEncryptedResponseEnc": null,
  "userInfoSignedResponseAlg": null,
  "userInfoEncryptedResponseAlg": null,
  "userInfoEncryptedResponseEnc": null,
  "requestObjectSigningAlg": null,
  "requestObjectEncryptionAlg": null,
  "requestObjectEncryptionEnc": null,
  "tokenEndpointAuthMethod": null,
  "tokenEndpointAuthSigningAlg": null,
  "defaultMaxAge": null,
  "requireAuthTime": false,
  "defaultAcrValues": null,
  "initiateLoginUri": null,
  "postLogoutRedirectUris": null,
  "requestUris": null,
  "scopes": null,
  "claims": null,
  "trustedClient": false,
  "lastAccessTime": null,
  "lastLogonTime": null,
  "persistClientAuthorizations": false,
  "includeClaimsInIdToken": false,
  "refreshTokenLifetime": null,
  "accessTokenLifetime": null,
  "customAttributes": [],
  "customObjectClasses": [
    "top"
  ],
  "rptAsJwt": false,
  "accessTokenAsJwt": false,
  "accessTokenSigningAlg": null,
  "disabled": false,
  "authorizedOrigins": null,
  "softwareId": null,
  "softwareVersion": null,
  "softwareStatement": null,
  "attributes": {
    "tlsClientAuthSubjectDn": null,
    "runIntrospectionScriptBeforeJwtCreation": false,
    "keepClientAuthorizationAfterExpiration": false,
    "allowSpontaneousScopes": false,
    "spontaneousScopes": null,
    "spontaneousScopeScriptDns": null,
    "backchannelLogoutUri": null,
    "backchannelLogoutSessionRequired": false,
    "additionalAudience": null,
    "postAuthnScripts": null,
    "consentGatheringScripts": null,
    "introspectionScripts": null,
    "rptClaimsScripts": null
  },
  "backchannelTokenDeliveryMode": null,
  "backchannelClientNotificationEndpoint": null,
  "backchannelAuthenticationRequestSigningAlg": null,
  "backchannelUserCodeParameter": null,
  "expirationDate": null,
  "deletable": false,
  "jansId": null
}

Selection: 
```

## Update an OpenID Client by its inum

If anything you want to update of an OpenID client, you can choose option '3' and enter the `inum` of the OpenID client. Here I've used the `inum=1929a64c-6f67-4399-bdd3-6a8d44cc04ae` of the above OpenID client. After then, You will get a list of fields to choose which one you are going to update.
Here is what you can see in return:

```
Get OpenId Connect Client by Inum
---------------------------------

«inum. Type: string»
inum: 1929a64c-6f67-4399-bdd3-6a8d44cc04ae
Calling Api with parameters: {'inum': '1929a64c-6f67-4399-bdd3-6a8d44cc04ae'}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/openid/clients.readonly
Fields:
 1 accessTokenAsJwt
 2 accessTokenLifetime
 3 accessTokenSigningAlg
 4 applicationType
 5 attributes
 6 authorizedOrigins
 7 backchannelAuthenticationRequestSigningAlg
 8 backchannelClientNotificationEndpoint
 9 backchannelTokenDeliveryMode
10 backchannelUserCodeParameter
11 claimRedirectUris
12 claims
13 clientIdIssuedAt
14 clientName
15 clientSecret
16 clientSecretExpiresAt
17 clientUri
18 contacts
19 customAttributes
20 customObjectClasses
21 defaultAcrValues
22 defaultMaxAge
23 deletable
24 disabled
25 expirationDate
26 frontChannelLogoutSessionRequired
27 frontChannelLogoutUri
28 grantTypes
29 idTokenEncryptedResponseAlg
30 idTokenEncryptedResponseEnc
31 idTokenSignedResponseAlg
32 idTokenTokenBindingCnf
33 includeClaimsInIdToken
34 initiateLoginUri
35 inum
36 jansId
37 jwks
38 jwksUri
39 lastAccessTime
40 lastLogonTime
41 logoUri
42 persistClientAuthorizations
43 policyUri
44 postLogoutRedirectUris
45 redirectUris
46 refreshTokenLifetime
47 registrationAccessToken
48 requestObjectEncryptionAlg
49 requestObjectEncryptionEnc
50 requestObjectSigningAlg
51 requestUris
52 requireAuthTime
53 responseTypes
54 rptAsJwt
55 scopes
56 sectorIdentifierUri
57 softwareId
58 softwareStatement
59 softwareVersion
60 subjectType
61 tokenEndpointAuthMethod
62 tokenEndpointAuthSigningAlg
63 tosUri
64 trustedClient
65 userInfoEncryptedResponseAlg
66 userInfoEncryptedResponseEnc
67 userInfoSignedResponseAlg

«q: quit, v: view, s: save, l: list fields #: update filed. »
Selection: 
```

- __q__ to quit 
- __v__ to view each attribute with updated data
- __l__ to get list of fields 
- __#__ to update filed attribute
- __id__ number of an attribute to identify which one you want to update



## Get OpenID client by its inum

`inum` is a unique identity of an OpenID client. You can use `inum` of an OpenID client to get more details.

In my case, I'm using the `inum` of the above created OpenID client:

```
Get OpenId Connect Client by Inum
---------------------------------

«inum. Type: string»
inum: 1929a64c-6f67-4399-bdd3-6a8d44cc04ae
Calling Api with parameters: {'inum': '1929a64c-6f67-4399-bdd3-6a8d44cc04ae'}
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/openid/clients.readonly
{
  "dn": "inum=1929a64c-6f67-4399-bdd3-6a8d44cc04ae,ou=clients,o=jans",
  "inum": "1929a64c-6f67-4399-bdd3-6a8d44cc04ae",
  "clientSecret": "CpTCvlZZsQDWShGrMXFBzQ==",
  "frontChannelLogoutUri": null,
  "frontChannelLogoutSessionRequired": false,
  "registrationAccessToken": null,
  "clientIdIssuedAt": null,
  "clientSecretExpiresAt": null,
  "redirectUris": null,
  "claimRedirectUris": null,
  "responseTypes": null,
  "grantTypes": [],
  "applicationType": "web",
  "contacts": null,
  "clientName": "newOID",
  "idTokenTokenBindingCnf": null,
  "logoUri": null,
  "clientUri": null,
  "policyUri": null,
  "tosUri": null,
  "jwksUri": null,
  "jwks": null,
  "sectorIdentifierUri": null,
  "subjectType": "pairwise",
  "idTokenSignedResponseAlg": null,
  "idTokenEncryptedResponseAlg": null,
  "idTokenEncryptedResponseEnc": null,
  "userInfoSignedResponseAlg": null,
  "userInfoEncryptedResponseAlg": null,
  "userInfoEncryptedResponseEnc": null,
  "requestObjectSigningAlg": null,
  "requestObjectEncryptionAlg": null,
  "requestObjectEncryptionEnc": null,
  "tokenEndpointAuthMethod": null,
  "tokenEndpointAuthSigningAlg": null,
  "defaultMaxAge": null,
  "requireAuthTime": false,
  "defaultAcrValues": null,
  "initiateLoginUri": null,
  "postLogoutRedirectUris": null,
  "requestUris": null,
  "scopes": null,
  "claims": null,
  "trustedClient": false,
  "lastAccessTime": null,
  "lastLogonTime": null,
  "persistClientAuthorizations": false,
  "includeClaimsInIdToken": false,
  "refreshTokenLifetime": null,
  "accessTokenLifetime": null,
  "customAttributes": [],
  "customObjectClasses": [
    "top"
  ],
  "rptAsJwt": false,
  "accessTokenAsJwt": false,
  "accessTokenSigningAlg": null,
  "disabled": false,
  "authorizedOrigins": null,
  "softwareId": null,
  "softwareVersion": null,
  "softwareStatement": null,
  "attributes": {
    "tlsClientAuthSubjectDn": null,
    "runIntrospectionScriptBeforeJwtCreation": false,
    "keepClientAuthorizationAfterExpiration": false,
    "allowSpontaneousScopes": false,
    "spontaneousScopes": null,
    "spontaneousScopeScriptDns": null,
    "backchannelLogoutUri": null,
    "backchannelLogoutSessionRequired": false,
    "additionalAudience": null,
    "postAuthnScripts": null,
    "consentGatheringScripts": null,
    "introspectionScripts": null,
    "rptClaimsScripts": null
  },
  "backchannelTokenDeliveryMode": null,
  "backchannelClientNotificationEndpoint": null,
  "backchannelAuthenticationRequestSigningAlg": null,
  "backchannelUserCodeParameter": null,
  "expirationDate": null,
  "deletable": false,
  "jansId": null
}

Selection: 
```
