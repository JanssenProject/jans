---
tags:
  - administration
  - auth-server
  - endpoint
  - DCR
  - dynamic client registration
---

# Client Registration Endpoint

### Dynamic Client Registration (DCR)

Dynamic client registration refers to the process by which a client submits a registration request to the Authorization 
server and how that request is served by the Authorization server. It is explained in the following specifications:

- [For OpenID Connect relying parties](https://openid.net/specs/openid-connect-registration-1_0.html)
- [For OAuth 2.0 client (without OpenID Connect features)](https://tools.ietf.org/html/rfc7591)
- [Client management (CRUD) operations](https://tools.ietf.org/html/rfc7592)
- [OpenBanking OpenID Dynamic Client Registration](https://openbanking.atlassian.net/wiki/spaces/DZ/pages/36667724/OpenBanking+OpenID+Dynamic+Client+Registration+Specification+-+v1.0.0-rc2#OpenBankingOpenIDDynamicClientRegistrationSpecification-v1.0.0-rc2-ClientRegistrationRequest) 

### Client Registration endpoint
The URI to dynamically register a client to a Janssen Auth Server can be found by checking the `registration_endpoint` 
claim of the OpenID Connect configuration reponse, typically deployed at 

```text
https://<my.jans.server>/.well-known/openid-configuration
```

### Configuring Janssen AS to allow clients to dynamically register
The Janssen Authorization server will serve a DCR request if the following configuration parameters are set:

1. `dynamicRegistrationEnabled` : `true` or `false`
2. `dynamicRegistrationExpirationTime` : Expiration time in seconds for clients created with dynamic registration, 0 or -1 means never expire
3. `dcrForbidExpirationTimeInRequest` : Boolean value specifying whether to allow to set client's expiration time in seconds during dynamic registration.. Default value is `false`.

**Client expiration**

Client expiration is set based on `dynamicRegistrationExpirationTime` AS configuration property or otherwise 
if `dcrForbidExpirationTimeInRequest` is `false` then it can be requested in Dynamic Client Registration Request via `lifetime` parameter 
which expected value in seconds. 

Configure the Janssen AS using steps explained in the [link](#curl-commands-to-configure-jans-auth-server)

### Client registration Requests

Client registration request primarily does two things: Communicate client metadata to the authorization server, and 
optionally authenticate. 

There are different ways in which the client metadata is communicated to the authorization server using
dynamic client registration requests.

- [Using request body](#using-request-body)
- [Using signed request object (JWT)](#using-signed-request-object-jwt)
- [Using software statement](#using-software-statement)

#### Using request body

A minimal DCR request with only mandatory parameters, looks like the one below.

 ```shell
 curl -X POST -k -H 'Content-Type: application/json' -i 'https://my.jans.server/jans-auth/restv1/register' \
              --data '{"redirect_uris": ["https://my.jans.client/page"]}'
 ```

If the registration is successful, Janssen Server will respond with http response code `201` with JSON body as shown
in example below:

```json
{
    "allow_spontaneous_scopes": false,
    "application_type": "web",
    "rpt_as_jwt": false,
    "registration_client_uri": "https://my.jans.server/jans-auth/restv1/register?client_id=85192707-a38c-496c-806c-ef01a6a3ae4a",
    "tls_client_auth_subject_dn": "",
    "run_introspection_script_before_jwt_creation": false,
    "registration_access_token": "eaee20de-54ce-4217-b960-6b72b55e6cab",
    "client_id": "85192707-a38c-496c-806c-ef01a6a3ae4a",
    "token_endpoint_auth_method": "client_secret_basic",
    "scope": "profile work_phone phone user_name device_sso openid permission uma_protection address email clientinfo org_name offline_access https://jans.io/auth/ssa.portal test https://jans.io/auth/ssa.admin https://jans.io/auth/ssa.developer",
    "client_secret": "4148f812-92d6-4245-80e0-243524b3b6a4",
    "client_id_issued_at": 1678700818,
    "backchannel_logout_uri": "",
    "backchannel_logout_session_required": false,
    "client_name": "my.jans.client",
    "par_lifetime": 600,
    "lifetime": 3600,
    "spontaneous_scopes": [],
    "id_token_signed_response_alg": "RS256",
    "access_token_as_jwt": false,
    "grant_types": [
        "authorization_code",
        "refresh_token"
    ],
    "subject_type": "pairwise",
    "keep_client_authorization_after_expiration": false,
    "require_par": false,
    "redirect_uris": ["https://my.jans.client/page"],
    "redirect_uris_regex": "",
    "additional_audience": [],
    "frontchannel_logout_session_required": false,
    "client_secret_expires_at": 1678787218,
    "access_token_signing_alg": "RS256",
    "response_types": ["code"]
}
```

In a typical DCR request the client or developer calls the client registration endpoint with a set of client metadata 
as specified in [RFC7591](https://www.rfc-editor.org/rfc/rfc7591.html#page-8)

```shell
curl -X POST -k -i 'https://my.jans.server/jans-auth/restv1/register'  \
     --data '{ \
                   "redirect_uris": ["https://client.example.org/cb"] \
               "client_id": "c3BhdRkqfX", \
               "client_secret": "bd136123eeffeef234235805d", \
               "grant_types": ["authorization_code", "refresh_token"], \
               "token_endpoint_auth_method": "client_secret_basic", \
               "jwks_uri": "https://client.example.org/my_public_keys.jwks", \
               "client_name": "My Example", \
               "client_name#fr": "Mon Exemple" \
         }' 
```
#### Using signed request object (JWT)

In some use-cases like FAPI implementation, DCR request payload is a JWT.

Example:

```shell
curl -X POST -k -H 'Content-Type: application/jwt' \
    -H 'Accept: application/json' \
    -i 'https://my-jans-server/jans-auth/restv1/register'  \
    --data 'eyJraWQiOiJrWTIyZXBUT......ueOg2HkjpggwAEP84jq9Q'
```

When such will be the nature of client registration requests, the following configuration properties should be set in 
the authorization server:

- `dcrSignatureValidationEnabled` - enables DCR signature validation
- `dcrSignatureValidationJwksUri` - specifies JWKS URI for all DCR's validations.
- `dcrSignatureValidationJwks` - specifies JWKS for all DCR's validations.

Configure the Janssen AS using steps explained in the [link](#curl-commands-to-configure-jans-auth-server)

#### Using software statement

A signed assertion from a trusted party, a Software statement or Software Statement Assertion (SSA), is used to 
dynamically register clients to an Authorization server.

Example:

```shell
curl -X POST https://my.jans.server/jans-auth/restv1/register \
       -H "Content-Type: application/json" \
   --data-binary @- <<DATA \
{ "redirect_uris": [ "https://client.example.org/cab1" ], \
  "software_statement":"eyJ0eXAi........j3ouyeYOv8", \
  "jwks_uri":"https://my.portal/portal/jwks" \
} DATA
```

To enable client registrations using Software statements, the AS should be configured using the following configuration 
parameters:

 - `softwareStatementValidationType` - The value of this variable is one of the following:
      - NONE - validation is skipped for software statement
      - SCRIPT - (default), invokes `getSoftwareStatementJwks` of dynamic registration script which has to return jwks.
      - JWKS - claim name within software statement that has inlined JWKS
      - JWKS_URI - claim name within software statement that points to JWKS URI that should lead to keys.
 - `dcrSignatureValidationSoftwareStatementJwksURIClaim` - specifies claim name inside software statement that should 
point to JWKS URI.
 - `dcrSignatureValidationSoftwareStatementJwksClaim` - specifies claim name inside software statement. Value of claim 
should point to inlined JWKS.
 - `trustedSsaIssuers` - map of trusted SSA issuers with configuration (e.g. automatically granted scopes). When empty - no issuers validation is performed. When not empty - AS forces validation and each SSA must match at least one entry from list.
   - `automaticallyGrantedScopes` - automatically granted scopes for trusted issuer
    ```json
    {
      "https://trusted.as.com": {
        "automaticallyGrantedScopes": [
          "a",
          "b"
        ]
      },
      "https://another-trusted.as.com": {
        "automaticallyGrantedScopes": [
          "d"
        ]
      }
    }
    ``` 

Configure the AS using steps explained in the [link](#curl-commands-to-configure-jans-auth-server)

#### Special mention about FAPI:

In case of a typical [client registration request in FAPI implementation]( https://openbankinguk.github.io/dcr-docs-pub/v3.3/dynamic-client-registration.html), 
the request object which is a signed JWT (as seen in point 3) is also called an SSA (Software statement Assertion) or 
DCR payload. This SSA can contain the software_statement inside it which is also a signed JWT. Each of the JWTs, the 
outer JWT called the SSA and the inner JWT called the software_statement are signed by different entities - the TPP and 
OBIE respectively.

### Security Pointers

If `dynamicRegistrationEnabled` is enabled in the Authorization Server, assess the following points to minimize 
potential exposure of sensitive personal data:

1. `trustedClientEnabled` and `dynamicRegistrationPersistClientAuthorizations` properties determine whether clients 
are trusted and if consent should be sought from the user before releasing their personal data to the RP
2. `dynamicRegistrationScopesParamEnabled` controls whether default scopes are globally enabled. 
If `dynamicRegistrationScopesParamEnabled` is `true` then scopes defined as default will be automatically added to any 
dynamically registered client entry without consent of OP's administrator. Therefore, make an informed decision before 
setting this field to `true`.

### CURL commands to configure Jans-auth server

Jans-auth server is configured using [Jans Config Api](https://github.com/JanssenProject/jans/tree/main/jans-config-api) :

#### Obtain the access token

```shell
curl -u "put_client_id_here:put_config_api_client_secret_here" https://<your.jans.server>/jans-auth/restv1/token \
     -d  "grant_type=client_credentials&scope=https://jans.io/oauth/jans-auth-server/config/properties.write"
```
   
#### Patch jans-auth server configurations

Patch jans-auth server configurations to reflect `anExampleConfigField` with the value `anExampleConfigField_value`

```shell
curl -X PATCH -k -H 'Content-Type: application/json-patch+json' \
 -i 'https://<your.jans.server>/jans-config-api/api/v1/jans-auth-server/config' \
 -H "Authorization: Bearer put_access_token_here" --data '[
    {
     "op": "add",
     "path": "anExampleConfigField",
     "value": "anExampleConfigField_value"
     }
    ]'
```

For example, to patch the jans-auth server configurations to reflect `dynamicRegistrationEnabled` with value as `true`

```shell
curl -X PATCH -k -H 'Content-Type: application/json-patch+json' \
-i 'https://<your.jans.server>/jans-config-api/api/v1/jans-auth-server/config' \
-H "Authorization: Bearer put_access_token_here" --data '[
 {
  "op": "add",
  "path": "dynamicRegistrationEnabled",
  "value": "true"
  }
 ]'
```

### Client metadata

Command to obtain metadata schema

```shell
jans cli --schema /components/schemas/Client
```

Output:

```json
{
  "dn": "string",
  "inum": "string",
  "displayName": "string",
  "clientSecret": "string",
  "frontChannelLogoutUri": "string",
  "frontChannelLogoutSessionRequired": true,
  "registrationAccessToken": "string",
  "clientIdIssuedAt": "2022-08-31T10:39:31.385Z",
  "clientSecretExpiresAt": "2022-08-31T10:39:31.385Z",
  "redirectUris": [
    "https://client.example.org/cb"
  ],
  "claimRedirectUris": [
    "string"
  ],
  "responseTypes": [
    "code"
  ],
  "grantTypes": [
    "authorization_code"
  ],
  "applicationType": "web",
  "contacts": [
    "string"
  ],
  "idTokenTokenBindingCnf": "string",
  "logoUri": "string",
  "clientUri": "string",
  "policyUri": "string",
  "tosUri": "string",
  "jwksUri": "string",
  "jwks": "{ \"keys\" : [ { \"e\" : \"AQAB\", \"n\" : \"gmlDX_mgMcHX..\" ] }",
  "sectorIdentifierUri": "string",
  "subjectType": "pairwise",
  "idTokenSignedResponseAlg": "HS256",
  "idTokenEncryptedResponseAlg": "RSA1_5",
  "idTokenEncryptedResponseEnc": "A128CBC+HS256",
  "userInfoSignedResponseAlg": "HS256",
  "userInfoEncryptedResponseAlg": "RSA1_5",
  "userInfoEncryptedResponseEnc": "A128CBC+HS256",
  "requestObjectSigningAlg": "HS256",
  "requestObjectEncryptionAlg": "RSA1_5",
  "requestObjectEncryptionEnc": "A128CBC+HS256",
  "tokenEndpointAuthMethod": "client_secret_basic",
  "tokenEndpointAuthSigningAlg": "HS256",
  "defaultMaxAge": 1000000,
  "requireAuthTime": true,
  "defaultAcrValues": [
    "string"
  ],
  "initiateLoginUri": "string",
  "postLogoutRedirectUris": [
    "https://client.example.org/logout/page1",
    "https://client.example.org/logout/page2",
    "https://client.example.org/logout/page3"
  ],
  "requestUris": [
    "string"
  ],
  "scopes": [
    "read write dolphin"
  ],
  "claims": [
    "string"
  ],
  "trustedClient": false,
  "lastAccessTime": "2022-08-31T10:39:31.385Z",
  "lastLogonTime": "2022-08-31T10:39:31.385Z",
  "persistClientAuthorizations": true,
  "includeClaimsInIdToken": false,
  "refreshTokenLifetime": 100000000,
  "accessTokenLifetime": 100000000,
  "customAttributes": [
    {
      "name": "name, displayName, birthdate, email",
      "multiValued": true,
      "values": [
        "string"
      ]
    }
  ],
  "customObjectClasses": [
    "string"
  ],
  "rptAsJwt": true,
  "accessTokenAsJwt": true,
  "accessTokenSigningAlg": "HS256",
  "disabled": false,
  "authorizedOrigins": [
    "string"
  ],
  "softwareId": "4NRB1-0XZABZI9E6-5SM3R",
  "softwareVersion": "2.1",
  "softwareStatement": "string",
  "attributes": {
    "tlsClientAuthSubjectDn": "string",
    "runIntrospectionScriptBeforeAccessTokenAsJwtCreationAndIncludeClaims": true,
    "keepClientAuthorizationAfterExpiration": true,
    "allowSpontaneousScopes": true,
    "spontaneousScopes": [
      "string"
    ],
    "spontaneousScopeScriptDns": [
      "string"
    ],
    "updateTokenScriptDns": [
      "string"
    ],
    "backchannelLogoutUri": [
      "string"
    ],
    "backchannelLogoutSessionRequired": true,
    "additionalAudience": [
      "string"
    ],
    "postAuthnScripts": [
      "string"
    ],
    "consentGatheringScripts": [
      "string"
    ],
    "introspectionScripts": [
      "string"
    ],
    "rptClaimsScripts": [
      "string"
    ],
    "ropcScripts": [
      "string"
    ],
    "parLifetime": 0,
    "requirePar": true,
    "jansAuthSignedRespAlg": "string",
    "jansAuthEncRespAlg": "string",
    "jansAuthEncRespEnc": "string",
    "jansSubAttr": "string",
    "redirectUrisRegex": "string",
    "jansAuthorizedAcr": [
      "string"
    ],
    "jansDefaultPromptLogin": true
  },
  "backchannelTokenDeliveryMode": "poll",
  "backchannelClientNotificationEndpoint": "string",
  "backchannelAuthenticationRequestSigningAlg": "RS256",
  "backchannelUserCodeParameter": true,
  "expirationDate": "2022-08-31T10:39:31.385Z",
  "deletable": false,
  "jansId": "string",
  "description": "string"
}
```

### Signed DCR and SSA validation

In OpenBanking case DCR (Dynamic Client Request) is signed and must contain SSA (Software Statement Assertion) inside it.

Non-Normative Example:
```curl
POST /register HTTP/1.1
Content-Type: application/jwt
Accept: application/json
Host: auth.bankone.com

eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJJREFtYX...
```

Decoded DCR Example:

```json
{
    "typ": "JWT",
    "alg": "ES256",
    "kid": "ABCD1234"
}
{
    "iss": "Amazon TPPID",
    "iat": 1492760444,
    "exp": 1524296444,
    "aud": "https://authn.gluu.org",
    "scope": "openid makepayment",
    "token_endpoint_auth_method": "private_key_jwt",
    "grant_types": ["authorization_code", "refresh_token", "client_credentials"],
    "response_types": ["code"],
    "id_token_signed_response_alg": "ES256",
    "request_object_signing_alg": "ES256",
    "software_id": "65d1f27c-4aea-4549-9c21-60e495a7a86f",
    "software_statement":  "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJlbXB0eSIsInN1YiI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UiLCJhZG1pbiI6dHJ1ZSwic2NvcGUiOiJzY29wZTEgc2NvcGUyIiwiY2xhaW1zIjoiY2xhaW0xIGNsYWltMiIsImlhdCI6MTY2OTgwNjc2MywiZXhwIjoxNjY5ODEwMzYzfQ.db0WQh2lmHkNYCWT8tSW684hqWTPJDTElppy42XM_lc"
}
{
    Signature
}
``` 

AS has `dcrSsaValidationConfigs` configuration value which holds json array. It can be used to validated both DCR and SSA.

Single item of this array has following properties:

* **id** - REQUIRED primary key for the entity
* **type** - REQUIRED either `ssa` or `dcr`
* **displayName** - Human friendly name in case we build an admin GUI for this  
* **description** - Human friendly details 
* **scope** - For SSA only -- list of allowed scopes the issuer can enable the client to request automatically. If not present, all scopes are allowed.
* **allowed_claims** - Any claims not listed in this list will be dropped. If not present, all claims are allowed.
* **jwks** - Public key 
* **jwks_uri** - URI of public key
* **issuers** - For MTLS, list of issuers trusted 
* **configuration_endpoint** - points to discovery page, e.g. `https://examle.com/.well-known/openid-configuration`
* **configuration_endpoint_claim** - e.g. `ssa_jwks_endpoint`
* **shared_secret** -  for MTLS HMAC

One of `jwks`, `jwks_uri` or `issuers` or `configuration_endpoint` is required.

Non-normative example of `dcrSsaValidationConfigs`

```json
[ {
  "id" : "735ee1c0-895d-4398-9c1b-9ad852257cc0",
  "type" : "DCR",
  "scopes" : [ "read", "write" ],
  "allowedClaims" : [ "exp", "iat" ],
  "jwks" : "{jwks here}",
  "issuers" : [ "Acme" ],
  "sharedSecret" : "secret"
}, {
  "id" : "7907cd0b-0f9f-4b4f-aaa2-0d0614546246",
  "type" : "SSA",
  "scopes" : [ "my_read", "my_write" ],
  "allowedClaims" : [ "test_exp", "test_iat" ],
  "jwks" : "{jwks here}",
  "issuers" : [ "jans-auth" ],
  "sharedSecret" : "secret"
}, {
  "id" : "1e95c9b1-04d0-4440-9362-88f4e1e62d76",
  "type" : "SSA",
  "jwks" : "{jwks here}",
  "issuers" : [ "empty" ],
  "sharedSecret" : "secret"
} ]
```

#### Signed DCR validation

DCR can be validated with two approaches.

**Via `dcrSsaValidationConfigs` configuration property - RECOMMENDED**

When create entry in `dcrSsaValidationConfigs` configuration property :
- `type` MUST be equal to `DCR` value
- `issuers` MUST have value which equals to `iss` claim in DCR.

**Via other configuration properties**

* **trustedSsaIssuers** - map of trusted SSA issuers with configuration (e.g. automatically granted scopes). When empty - no issuers validation is performed. When not empty - AS forces validation and each SSA must match at least one entry from list.
  * **automaticallyGrantedScopes** - automatically granted scopes for trusted issuer
    ```json
    {
      "https://trusted.as.com": {
        "automaticallyGrantedScopes": [
          "a",
          "b"
        ]
      },
      "https://another-trusted.as.com": {
        "automaticallyGrantedScopes": [
          "d"
        ]
      }
    }
    ``` 
* **dcrSignatureValidationJwks** - specifies JWKS for DCR's validations
* **dcrSignatureValidationJwksUri** - specifies JWKS URI for  DCR's validations
* **dcrSignatureValidationSharedSecret** - if HMAC is used, this is the shared secret
* **dcrSignatureValidationEnabled** - boolean value enables DCR signature validation. Default is false
* **dcrSignatureValidationSoftwareStatementJwksURIClaim** - specifies claim name inside software statement. Value of claim should point to JWKS URI
* **dcrSignatureValidationSoftwareStatementJwksClaim** - specifies claim name inside software statement. Value of claim should point to inlined JWKS 
* **dcrAuthorizationWithClientCredentials** - boolean value indicating if DCR authorization to be performed using client credentials

#### SSA Validation

SSA is validated based on `softwareStatementValidationType` which is enum.

1. **softwareStatementValidationType**=*builtin* - validation is performed against `dcrSsaValidationConfigs` configuration property, where
   1. `type` MUST be equal to `SSA` value
   1. `issuers` MUST have value which equals to `iss` claim in DCR.
1. **softwareStatementValidationType**=*script* - jwks and hmac secret are returned by dynamic client registration script
1. **softwareStatementValidationType**=*jwks_uri*, allows to specify jwks_uri claim name from software_statement. Claim name specified by `softwareStatementValidationClaimName` configuration property.
1. **softwareStatementValidationType**=*jwks*, allows to specify jwks claim name from software_statement. Claim name specified by `softwareStatementValidationClaimName` configuration property.
1. **softwareStatementValidationType**=*none*, no validation.


### Customizing the behavior of the AS using Interception script
Janssen's allows developers to register a client with the Authorization Server (AS) without any intervention by the administrator. By default, all clients are given the same default scopes and attributes. Through the use of an interception script, this behavior can be modified. These scripts can be used to analyze the registration request and apply customizations to the registered client. For example, a client can be given specific scopes by analyzing the [Software Statement](https://www.rfc-editor.org/rfc/rfc7591.html#section-2.3) that is sent with the registration request.

Further reading [here](../../developer/scripts/client-registration.md)

### The Use of Attestation in Dynamic Client Registration

AS supports "The Use of Attestation in OAuth 2.0 Dynamic Client Registration" [specification draft](https://www.ietf.org/id/draft-tschofenig-oauth-attested-dclient-reg-00.html).

Specification draft does not define exact attestation request/response formats. 
Thus AS supports Attestation calls to Verifier via `ClientRegistrationType` custom script type.

Use `createClient` method of `ClientRegistrationType` to prevent client creation if attestation result does not satisfy expectation.

```python
    def createClient(self, context):
        print "Client registration. CreateClient method"
        registerRequest = context.getRegisterRequest()
        configurationAttributes = context.getConfigurationAttibutes()
        client = context.getClient()
        
        # getting evidence as string or as JWT
        evidenceAsString = registerRequest.getEvidence()
        evidenceAsJwt = context.getEvidence()

        # following code depends on attestation client used to make calls to Verifier
        attestationResult = attestationClient.attestation(evidence)
        if attestationResult.isFail:
            print "Attestation result forbids client creation"
            return False
            
        print "Attestation result is OK"
        return True            
```  

**Configuration options**
- `dcrAttestationEvidenceRequired` - Boolean value indicating if DCR attestation evidence is required. Default value is `false`.

If `evidence` request parameter is not present and `dcrAttestationEvidenceRequired` is `true` AS returns `stale_evidence` error:

```
HTTP/1.1 400 Bad Request
Content-Type: application/json
Cache-Control: no-store
Pragma: no-cache

{
  "error": "stale_evidence",
  "error_description": "The provided evidence is not current.",
  "nonce": "lBjvTtuPbpzIaqyiAOOkrIol3WmflPUUepzUXNDFuUgMKUL"
}
```

It is common to get `nonce` value from Attestation Server. In this case set `dcrAttestationEvidenceRequired=false`
and throw error from custom script.

```python
    def createClient(self, context):
        evidenceAsString = registerRequest.getEvidence()
        
        if StringHelper.isEmpty(evidenceAsString)
            nonceFromVerifier = attestationClient.requestNonce()
            context.createStaleEvidenceWebApplicationException(nonceFromVerifier)
            return False

        ...
            
        print "Attestation result is OK"
        return True            
```  

`context.createStaleEvidenceWebApplicationException(nonceFromVerifier)` leads to `stale_evidence` error creation, 
see example above (`nonceFromVerifier` must be string value).

In case goal is to create own custom error, please use standard `context.createWebApplicationException` methods and return `False` from script to prevent client creation.

Example
```python
    entity = <construct error message>
    context.createWebApplicationException(400, entity)
```

### Dynamic registration custom attributes


### CRUD Operations

Janssen Server allows client management through client configuration endpoint 
[(RFC 7592)](https://www.rfc-editor.org/rfc/rfc7592). 

[JSON response](#using-request-body) to client registration request contains `registration_client_uri` and
`registration_access_token` data elements. The URI mentioned using `registration_client_uri` provides functionality to
read, update and delete the client. This endpoint is a protected endpoint where request has to be authenticated using
access token `registration_access_token`.

#### Read client metadata 

A read request to the same client that got created in [earlier](#using-request-body) section should be as shown in the 
example below. HTTP method `GET` is used to send the request which signifies that it is a request to `read` client 
metadata.

```shell
curl -k -H 'Authorization: Bearer eaee20de-54ce-4217-b960-6b72b55e6cab' \
-i 'https://my.jans.server/jans-auth/restv1/register?client_id=85192707-a38c-496c-806c-ef01a6a3ae4a'
```

JSON response from Janssen Server will contain the current state of client metadata. 

#### Update client metadata

Client metadata can be updated by sending request with `PUT` HTTP method to `registration_client_uri`. Janssen Server 
replaces current metadata with the one sent with update request as outlined in 
[the specification](https://www.rfc-editor.org/rfc/rfc7592#section-2.2) 

#### Delete Client

Client can be deleted by sending a request using `DELETE` method to `registration_client_uri`.

A successful delete action will invalidate the "client_id", "client_secret", and "registration_access_token" for this 
client, thereby preventing the "client_id" from being used at either the authorization endpoint or token endpoint of the
authorization server.

### Internationalization for Client metadata
