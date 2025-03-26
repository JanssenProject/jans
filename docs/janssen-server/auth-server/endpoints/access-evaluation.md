---
tags:
- administration
- auth-server
- access-evaluation
- endpoint
---
# Access Evaluation Endpoint


The Jans-Auth server implements [OpenID AuthZEN Authorization API 1.0 – draft 01](https://openid.github.io/authzen/).
The AuthZEN Authorization API 1.0 specification defines a standardized interface for communication between 
Policy Enforcement Points (PEPs) and Policy Decision Points (PDPs) to facilitate consistent authorization decisions across diverse systems. 
It introduces an Access Evaluation API that allows PEPs to query PDPs about specific access requests, 
enhancing interoperability and scalability in authorization processes. 
The specification is transport-agnostic, with an initial focus on HTTPS bindings, and emphasizes secure, fine-grained, 
and dynamic authorization mechanisms.

The Access Evaluation Endpoint in the AuthZEN specification serves as a mechanism for Policy Enforcement Points (PEPs) 
to request access decisions from a Policy Decision Point (PDP) for specific resources and actions. 
Upon receiving a request, the endpoint evaluates the subject, resource, and action against defined policies to determine 
if access should be granted, denied, or if additional information is needed. 
The endpoint's responses are typically concise, aiming to provide a rapid decision that PEPs can enforce in real-time. 
The goal is to provide a scalable, secure interface for dynamic and fine-grained access control across applications.


URL to access access evaluation endpoint on Janssen Server is listed in both:
 - the response of Janssen Server's well-known [configuration endpoint](./configuration.md) given below.
 - the response of Janssen Server's `/.well-known/authzen-configuration` endpoint.

**OpenID Discovery**
```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

**AuthZEN Discovery**
```text
https://janssen.server.host/jans-auth/.well-known/authzen-configuration
```

`/.well-known/authzen-configuration` allows to publish data specific to AuthZEN only. Response of AuthZEN discovery endpoint can be 
changed via `AccessEvaluationDiscoveryType` custom script. 

**Snippet of AccessEvaluationDiscoveryType**
```java
    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        scriptLogger.info("write to script logger");
        JSONObject response = (JSONObject) responseAsJsonObject;
        response.accumulate("key_from_java", "value_from_script_on_java");
        return true;
    }
```

`access_evaluation_v1_endpoint` claim in the response specifies the URL for access evaluation endpoint. By default, access 
evaluation endpoint looks like below:

```
https://janssen.server.host/jans-auth/restv1/access/v1/evaluation
```

In order to call Access Evaluation Endpoint client must have `access_evaluation` scope. 
If scope is not present AS rejects call with 401 (unauthorized) http status code.
`Authorization` header must contain valid `access_token` with `access_evaluation` scope granted to it.
Otherwise it's possible to use `Basic` token with encoded client credentials if set
`accessEvaluationAllowBasicClientAuthorization` AS configuration property to `true`.

- Bearer token : `Authorization: Bearer <access_token>`
- Basic authorization : `Authorization: Basic <encoded client credentials>`


More information about request and response of the Access Evaluation Endpoint can be found in the OpenAPI specification 
of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml#/access-evaluation).

Sample request
```
POST /jans-auth/restv1/access/v1/evaluation HTTP/1.1
Host: happy-example.gluu.info
Content-Type: application/json
Authorization: Basic M2NjOTdhYWItMDE0Zi00ZWM5LWI4M2EtNTE3MTRlODE3MDMwOmFlYmMwZWFhLWY5N2YtNDU5NS04ZWExLWFlNmU1NDFmNDZjNg==

{
  "subject": {
    "id": "alice@acmecorp.com",
    "type": "super_admin",
    "properties": null
  },
  "resource": {
    "id": "123",
    "type": "account",
    "properties": null
  },
  "action": {
    "name": "can_read",
    "properties": {
      "method": "GET"
    }
  },
  "context": {
    "properties": null
  }
}
```

Sample successful response with `authorization_code`.
```
HTTP/1.1 200
Content-Type: application/json

{
  "decision":true,
  "context": {
    "id":"9e04dd22-e980-4e54-bc04-d64a0c2e1afe",
    "reason_admin":{"reason":"super_admin"},
    "reason_user":null
  }
}
```

Sample error response
```
HTTP/1.1 401 Unauthorized
Content-Type: application/json
Cache-Control: no-store

{
  "error": "invalid_token"
}
```


## Configuration Properties

Access Evaluation Endpoint AS configuration:

- **accessEvaluationScriptName** - Access evaluation custom script name. If not set AS falls back to first valid script found in database.
- **accessEvaluationAllowBasicClientAuthorization** - Allow basic client authorization for access evaluation endpoint.

## Custom script  

AS provides `AccessEvaluationType` custom script which must be used to control Access Evaluation Endpoint behaviour.

Use `accessEvaluationScriptName` configuration property to specify custom script.   If not set AS falls back to first valid script found in database.

Main `evaluate` method returns response which grants or denies access. 

Please see following snippet below:

```java
    @Override
    public AccessEvaluationResponse evaluate(AccessEvaluationRequest request, Object scriptContext) {

        ExternalScriptContext context = (ExternalScriptContext) scriptContext;
        // 1. access http request via context.getHttpRequest()
        // 2. access all access evaluation specific data directly with 'request', e.g. request.getSubject()

        // 3. perform custom validation if needed
        validateResource(request.getResource());

        // typically some internal validation must be performed here
        // request data alone must not be trusted, it's just sample to demo script with endpoint
        if ("super_admin".equalsIgnoreCase(request.getSubject().getType())) {
            final ObjectNode reasonAdmin = objectMapper.createObjectNode();
            reasonAdmin.put("reason", "super_admin");

            final AccessEvaluationResponseContext responseContext = new AccessEvaluationResponseContext();
            responseContext.setId(UUID.randomUUID().toString());
            responseContext.setReasonAdmin(reasonAdmin);

            return new AccessEvaluationResponse(true, responseContext);
        }
        return AccessEvaluationResponse.FALSE;
    }
```

More details in [Access Evaluation Custom Script Page](../../developer/scripts/access-evaluation.md).

Full sample script can be found [here](../../../script-catalog/access_evaluation/AccessEvaluation.java)


## Full successful Access Evaluation Flow sample

```
#######################################################
TEST: OpenID Connect Discovery
#######################################################
-------------------------------------------------------
REQUEST:
-------------------------------------------------------
GET /.well-known/webfinger HTTP/1.1?resource=acct%3Aadmin%40happy-example.gluu.info&rel=http%3A%2F%2Fopenid.net%2Fspecs%2Fconnect%2F1.0%2Fissuer HTTP/1.1
Host: happy-example.gluu.info

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 200
Connection: Keep-Alive
Content-Length: 207
Content-Type: application/jrd+json;charset=iso-8859-1
Date: Fri, 08 Nov 2024 17:15:19 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Server: Apache/2.4.52 (Ubuntu)
Set-Cookie: X-Correlation-Id=f8a91ca8-3ebb-48fb-852e-31e40b398b6d; Secure; HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{
    "subject": "acct:admin@happy-example.gluu.info",
    "links": [{
        "rel": "http://openid.net/specs/connect/1.0/issuer",
        "href": "https://happy-example.gluu.info"
    }]
}


OpenID Connect Configuration
-------------------------------------------------------
REQUEST:
-------------------------------------------------------
GET /.well-known/openid-configuration HTTP/1.1 HTTP/1.1
Host: happy-example.gluu.info

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 200
Connection: Keep-Alive
Content-Length: 7715
Content-Type: application/json
Date: Fri, 08 Nov 2024 17:15:19 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Server: Apache/2.4.52 (Ubuntu)
Set-Cookie: X-Correlation-Id=474307e2-ed02-404e-bf35-a2bc60bf3421; Secure; HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{
  "request_parameter_supported" : true,
  "pushed_authorization_request_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/par",
  "introspection_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "introspection_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/introspection",
  "claims_parameter_supported" : false,
  "status_list_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/status_list",
  "issuer" : "https://happy-example.gluu.info",
  "userinfo_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "id_token_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "access_token_signing_alg_values_supported" : [ "none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "authorization_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/authorize",
  "service_documentation" : "http://jans.org/docs",
  "authorization_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "introspection_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "claims_supported" : [ "street_address", "country", "zoneinfo", "birthdate", "role", "gender", "formatted", "user_name", "phone_mobile_number", "preferred_username", "locale", "inum", "updated_at", "post_office_box", "nickname", "preferred_language", "email", "website", "email_verified", "profile", "locality", "room_number", "phone_number_verified", "given_name", "middle_name", "picture", "name", "phone_number", "postal_code", "region", "family_name", "jansAdminUIRole" ],
  "ssa_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/ssa",
  "token_endpoint_auth_methods_supported" : [ "client_secret_basic", "client_secret_post", "client_secret_jwt", "private_key_jwt", "tls_client_auth", "self_signed_tls_client_auth" ],
  "tls_client_certificate_bound_access_tokens" : true,
  "response_modes_supported" : [ "fragment", "query.jwt", "query", "fragment.jwt", "jwt", "form_post.jwt", "form_post" ],
  "backchannel_logout_session_supported" : true,
  "token_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/token",
  "response_types_supported" : [ "code token", "code", "code id_token", "code token id_token", "token id_token", "token", "id_token" ],
  "tx_token_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "authorization_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "backchannel_token_delivery_modes_supported" : [ "poll", "ping", "push" ],
  "dpop_signing_alg_values_supported" : [ "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "request_uri_parameter_supported" : true,
  "backchannel_user_code_parameter_supported" : false,
  "grant_types_supported" : [ "client_credentials", "urn:ietf:params:oauth:grant-type:device_code", "refresh_token", "implicit", "password", "authorization_code", "urn:ietf:params:oauth:grant-type:uma-ticket", "urn:ietf:params:oauth:grant-type:token-exchange" ],
  "ui_locales_supported" : [ "en", "bg", "de", "es", "fr", "it", "ru", "tr" ],
  "prompt_values_supported" : [ "none", "login", "consent", "select_account", "create" ],
  "userinfo_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/userinfo",
  "access_evaluation_v1_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/access/v1/evaluation",
  "authorization_challenge_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/authorization_challenge",
  "op_tos_uri" : "https://happy-example.gluu.info/tos",
  "require_request_uri_registration" : false,
  "id_token_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "frontchannel_logout_session_supported" : true,
  "authorization_signing_alg_values_supported" : [ "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "claims_locales_supported" : [ "en" ],
  "clientinfo_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/clientinfo",
  "request_object_signing_alg_values_supported" : [ "none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "request_object_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "global_token_revocation_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/global-token-revocation",
  "introspection_signing_alg_values_supported" : [ "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "tx_token_signing_alg_values_supported" : [ "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "session_revocation_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/revoke_session",
  "check_session_iframe" : "https://happy-example.gluu.info/jans-auth/opiframe.htm",
  "scopes_supported" : [ "address", "introspection", "role", "access_evaluation", "https://jans.io/auth/ssa.admin", "online_access", "openid", "clientinfo", "user_name", "profile", "uma_protection", "revoke_any_token", "global_token_revocation", "https://jans.io/scim/users.write", "revoke_session", "device_sso", "https://jans.io/scim/users.read", "phone", "mobile_phone", "offline_access", "authorization_challenge", "https://jans.io/oauth/lock/audit.write", "email", "https://jans.io/oauth/lock/audit.readonly" ],
  "backchannel_logout_supported" : true,
  "acr_values_supported" : [ "simple_password_auth" ],
  "archived_jwks_uri" : "https://happy-example.gluu.info/jans-auth/restv1/jwks/archived",
  "request_object_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "device_authorization_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/device_authorization",
  "display_values_supported" : [ "page", "popup" ],
  "tx_token_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "userinfo_signing_alg_values_supported" : [ "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "require_pushed_authorization_requests" : false,
  "claim_types_supported" : [ "normal" ],
  "userinfo_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "end_session_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/end_session",
  "revocation_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/revoke",
  "backchannel_authentication_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/bc-authorize",
  "token_endpoint_auth_signing_alg_values_supported" : [ "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "frontchannel_logout_supported" : true,
  "jwks_uri" : "https://happy-example.gluu.info/jans-auth/restv1/jwks",
  "subject_types_supported" : [ "public", "pairwise" ],
  "id_token_signing_alg_values_supported" : [ "none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "registration_endpoint" : "https://happy-example.gluu.info/jans-auth/restv1/register",
  "id_token_token_binding_cnf_values_supported" : [ "tbh" ]
}


#######################################################
TEST: accessEvaluation_whenSubjectTypeIsAcceptedByScript_shouldGrantAccess
#######################################################
-------------------------------------------------------
REQUEST:
-------------------------------------------------------
POST /jans-auth/restv1/register HTTP/1.1
Host: happy-example.gluu.info
Content-Type: application/json
Accept: application/json

{
  "grant_types" : [ "authorization_code", "refresh_token" ],
  "subject_type" : "public",
  "application_type" : "web",
  "scope" : "access_evaluation openid profile address email phone user_name",
  "minimum_acr_priority_list" : [ ],
  "redirect_uris" : [ "https://happy-example.gluu.info/jans-auth-rp/home.htm", "https://client.example.com/cb", "https://client.example.com/cb1", "https://client.example.com/cb2" ],
  "client_name" : "access_evaluation test",
  "additional_audience" : [ ],
  "response_types" : [ "code", "id_token" ]
}

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 201
Cache-Control: no-store
Connection: Keep-Alive
Content-Length: 1664
Content-Type: application/json
Date: Fri, 08 Nov 2024 17:15:20 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Pragma: no-cache
Server: Apache/2.4.52 (Ubuntu)
Set-Cookie: X-Correlation-Id=d7035723-e472-4cac-84c5-ef19f14fcc09; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{
    "allow_spontaneous_scopes": false,
    "application_type": "web",
    "rpt_as_jwt": false,
    "registration_client_uri": "https://happy-example.gluu.info/jans-auth/restv1/register?client_id=3cc97aab-014f-4ec9-b83a-51714e817030",
    "tls_client_auth_subject_dn": "",
    "run_introspection_script_before_jwt_creation": false,
    "registration_access_token": "bcf42a29-d534-4ed4-a4aa-eceb4e50f472",
    "client_id": "3cc97aab-014f-4ec9-b83a-51714e817030",
    "token_endpoint_auth_method": "client_secret_basic",
    "scope": "openid access_evaluation",
    "client_secret": "aebc0eaa-f97f-4595-8ea1-ae6e541f46c6",
    "client_id_issued_at": 1731086120,
    "backchannel_logout_session_required": false,
    "client_name": "access_evaluation test",
    "par_lifetime": 600,
    "spontaneous_scopes": [],
    "id_token_signed_response_alg": "RS256",
    "access_token_as_jwt": false,
    "grant_types": [
        "refresh_token",
        "authorization_code"
    ],
    "subject_type": "public",
    "authorization_details_types": [],
    "additional_token_endpoint_auth_methods": [],
    "keep_client_authorization_after_expiration": false,
    "require_par": false,
    "redirect_uris": [
        "https://client.example.com/cb2",
        "https://client.example.com/cb1",
        "https://client.example.com/cb",
        "https://happy-example.gluu.info/jans-auth-rp/home.htm"
    ],
    "redirect_uris_regex": "",
    "additional_audience": [],
    "frontchannel_logout_session_required": false,
    "client_secret_expires_at": 0,
    "access_token_signing_alg": "RS256",
    "response_types": [
        "code",
        "id_token"
    ]
}

-------------------------------------------------------
REQUEST:
-------------------------------------------------------
POST /jans-auth/restv1/access/v1/evaluation HTTP/1.1
Host: happy-example.gluu.info
Content-Type: application/json
Authorization: Basic M2NjOTdhYWItMDE0Zi00ZWM5LWI4M2EtNTE3MTRlODE3MDMwOmFlYmMwZWFhLWY5N2YtNDU5NS04ZWExLWFlNmU1NDFmNDZjNg==

{"subject":{"id":"alice@acmecorp.com","type":"super_admin","properties":null},"resource":{"id":"123","type":"account","properties":null},"action":{"name":"can_read","properties":{"method":"GET"}},"context":{"properties":null}}

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 200
Connection: Keep-Alive
Content-Length: 132
Content-Type: application/json
Date: Fri, 08 Nov 2024 17:15:21 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Server: Apache/2.4.52 (Ubuntu)
Set-Cookie: X-Correlation-Id=d4f99d9f-5b94-4863-a020-73f6fb62c5e8; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{"decision":true,"context":{"id":"9e04dd22-e980-4e54-bc04-d64a0c2e1afe","reason_admin":{"reason":"super_admin"},"reason_user":null}}

```
