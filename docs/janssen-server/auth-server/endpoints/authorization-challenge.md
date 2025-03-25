---
tags:
- administration
- auth-server
- authorization-challenge
- endpoint
---
# Authorization Challenge
## Overview

Authorization Challenge Endpoint allows first-party native client obtain authorization code which later can be exchanged on access token.
This can provide an entirely browserless OAuth 2.0 experience suited for native applications.

This endpoint conforms to [OAuth 2.0 for First-Party Applications](https://www.ietf.org/archive/id/draft-parecki-oauth-first-party-apps-02.html) specifications.

URL to access authorization challenge endpoint on Janssen Server is listed in the response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`authorization_challenge_endpoint` claim in the response specifies the URL for authorization challenge endpoint. By default, authorization 
challenge endpoint looks like below:

```
https://janssen.server.host/jans-auth/restv1/authorize-challenge
```

In order to call Authorization Challenge Endpoint client must have `authorization_challenge` scope. 
If scope is not present AS rejects call with 401 (unauthorized) http status code. 

Authorization Challenge Endpoint supports Proof Key for Code Exchange (PKCE).

More information about request and response of the authorization challenge endpoint can be found in the OpenAPI specification 
of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml#/authorize-challenge).

Sample request
```
POST /authorize HTTP/1.1
Host: server.example.com
Content-Type: application/x-www-form-urlencoded

login_hint=%2B1-310-123-4567&scope=profile
&client_id=bb16c14c73415
```

Sample successful response with `authorization_code`.
```
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
Cache-Control: no-store

{
  "authorization_code": "uY29tL2F1dGhlbnRpY"
}
```

Sample error response
```
HTTP/1.1 401 Unauthorized
Content-Type: application/json
Cache-Control: no-store

{
  "error": "username_required"
}
```


## Configuration Properties

Authorization Challenge Endpoint AS configuration:

- **authorizationChallengeEndpoint** - The authorization challenge endpoint URL
- **authorizationChallengeDefaultAcr** - Authorization Challenge Endpoint Default ACR if no value is specified in acr_values request parameter. Default value is `default_challenge`.
- **authorizationChallengeShouldGenerateSession** - Boolean value specifying whether to generate session_id (AS object and cookie) during authorization at Authorization Challenge Endpoint. Default value is `false`.
- **mtlsAuthorizationChallengeEndpoint** - URL for Mutual TLS (mTLS) Client Authentication and Certificate-Bound Access Tokens (MTLS) Authorization Challenge Endpoint.

## Custom script  

AS provides `AuthorizationChallengeType` custom script which must be used to control Authorization Challenge Endpoint behaviour.

If request does not have `acr_values` specified and script name falls back to `default_challenge` which is available and enabled during installation.
Default script name can be changed via `authorizationChallengeDefaultAcr` configuration property. 

Main method return true/false which indicates to server whether to issue `authorization_code` in response or not.

If parameters is not present then error has to be created and `false` returned.
If all is good script has to return `true` and it's strongly recommended to set user `context.getExecutionContext().setUser(user);` so AS can keep tracking what exactly user is authenticated.

Please see following snippet below:

```java
    public boolean authorize(Object scriptContext) {
        ExternalScriptContext context = (ExternalScriptContext) scriptContext;

        // 1. validate all required parameters are present
        final String username = getParameterOrCreateError(context, USERNAME_PARAMETER);
        if (StringUtils.isBlank(username)) {
            return false;
        }

        final String password = getParameterOrCreateError(context, PASSWORD_PARAMETER);
        if (StringUtils.isBlank(password)) {
            return false;
        }

        scriptLogger.trace("All required parameters are present");

        // 2. main authorization logic, if ok -> set authorized user into "context.getExecutionContext().setUser(user);" and return true
        UserService userService = CdiUtil.bean(UserService.class);
        PersistenceEntryManager entryManager = CdiUtil.bean(PersistenceEntryManager.class);

        final User user = userService.getUser(username);
        if (user == null) {
            scriptLogger.trace("User is not found by username {}", username);
            createError(context, "username_invalid");
            return false;
        }

        final boolean ok = entryManager.authenticate(user.getDn(), User.class, password);
        if (ok) {
            context.getExecutionContext().setUser(user); // <- IMPORTANT : without user set, user relation will not be associated with token
            scriptLogger.trace("User {} is authenticated successfully.", username);
            return true;
        }

        // 3. not ok -> set error which explains what is wrong and return false
        scriptLogger.trace("Failed to authenticate user {}. Please check username and password.", username);
        createError(context, "username_or_password_invalid");
        return false;
    }
```

More details in [Authorization Challenge Custom Script Page](../../developer/scripts/authorization-challenge.md).

Full sample script can be found [here](../../../script-catalog/authorization_challenge/AuthorizationChallenge.java)

## Auth session

Auth session is optional. AS does not return it by default. 
It's possible to pass in request `use_auth_session=true` which makes AS return it in error response.
If it is desired to use `auth_session` and don't pass `client_id` (or other parameters) in next request, 
it should be put in attributes of `auth_session` object. 
`auth_session` object lifetime is set by `authorizationChallengeSessionLifetimeInSeconds` AS configuration property. 
If `authorizationChallengeSessionLifetimeInSeconds` is not set then value falls back to `86400` seconds.

Example
```java
String clientId = context.getHttpRequest().getParameter("client_id");
authorizationChallengeSessionObject.getAttributes().getAttributes().put("client_id", clientId);
``` 

AS automatically validates DPoP if it is set during auth session creation.
Thus it's recommended to set `jkt` of the auth session if DPoP is used.
```java
final String dpop = context.getHttpRequest().getHeader(DpopService.DPOP);
if (StringUtils.isNotBlank(dpop)) {
    authorizationChallengeSessionObject.getAttributes().setJkt(getDpopJkt(dpop));
}
```

Full sample script can be found [here](../../../script-catalog/authorization_challenge/AuthorizationChallenge.java)

## Web session

Authorization challenge script is first-party flow and thus web session is not created by default. 
However there can be cases when such session has to be created. Please set **authorizationChallengeShouldGenerateSession** configuration property to **true**
to force session creation. 

In case it is needed to prepare session with specific data, it is possible to create session
in script and set it into context. Example:

```java
SessionIdService sessionIdService = CdiUtil.bean(SessionIdService.class);
Identity identityService = CdiUtil.bean(Identity.class);

Map<String, String> sessionStore = new HashMap<String, String>();
sessionStore.put("login_id_token",login_id_token);
sessionStore.put("login_access_token",login_access_token);
sessionStore.put("transaction_status","PENDING");
SessionId sessionId = sessionIdService.generateAuthenticatedSessionId(context.getHttpRequest(), user.getDn(), sessionStore);

context.getExecutionContext().setAuthorizationChallengeSessionId(sessionId);
scriptLogger.trace("Created Authorization challenge session successfully");
``` 

## Multi-step example

Sometimes it's required to send data sequentially. Step by step. Calls to Authorization Challenge Endpoint must have 
`use_auth_session=true` parameter to force tracking data between request. 
 
Lets consider example when RP first sends `username` and then in next request `OTP`.

```text
POST /jans-auth/restv1/authorize-challenge HTTP/1.1
Host: server.example.com
Content-Type: application/x-www-form-urlencoded

username=alice
&scope=photos
&client_id=bb16c14c73415
```
 
AS accepts `username` and returns back error with `auth_session`.

```text
HTTP/1.1 401 Unauthorized
Content-Type: application/json
Cache-Control: no-store

{
  "error": "otp_required",
  "auth_session": "ce6772f5e07bc8361572f"
}
```

In next call RP can send OTP and `auth_session` (AS matches user from `auth_session`)

```text
POST /jans-auth/restv1/authorize-challenge HTTP/1.1
Host: server.example.com
Content-Type: application/x-www-form-urlencoded

otp=ccnnju667d&auth_session=ce6772f5e07bc8361572f
```

In custom script it's easy to code what data has to be kept in `auth_session`.

```text
    private void createError(ExternalScriptContext context, String errorCode) {
        String deviceSessionPart = prepareDeviceSessionSubJson(context);

        final String entity = String.format("{\"error\": \"%s\"%s}", errorCode, deviceSessionPart);
        context.createWebApplicationException(401, entity);
    }

    private String prepareDeviceSessionSubJson(ExternalScriptContext context) {
        DeviceSession authorizationChallengeSessionObject = context.getAuthzRequest().getDeviceSessionObject();
        if (authorizationChallengeSessionObject != null) {
            prepareDeviceSession(context, authorizationChallengeSessionObject);
            return String.format(",\"auth_session\":\"%s\"", authorizationChallengeSessionObject.getId());
        } else if (context.getAuthzRequest().isUseDeviceSession()) {
            authorizationChallengeSessionObject = prepareDeviceSession(context, null);
            return String.format(",\"auth_session\":\"%s\"", authorizationChallengeSessionObject.getId());
        }
        return "";
    }

    private DeviceSession prepareDeviceSession(ExternalScriptContext context, DeviceSession authorizationChallengeSessionObject) {
        DeviceSessionService deviceSessionService = CdiUtil.bean(DeviceSessionService.class);
        boolean newSave = authorizationChallengeSessionObject == null;
        if (newSave) {
            authorizationChallengeSessionObject = deviceSessionService.newDeviceSession();
        }

        String username = context.getHttpRequest().getParameter(USERNAME_PARAMETER);
        if (StringUtils.isNotBlank(username)) {
            authorizationChallengeSessionObject.getAttributes().getAttributes().put(USERNAME_PARAMETER, username);
        }

        String otp = context.getHttpRequest().getParameter(OTP_PARAMETER);
        if (StringUtils.isNotBlank(otp)) {
            authorizationChallengeSessionObject.getAttributes().getAttributes().put(OTP_PARAMETER, otp);
        }
        
        String clientId = context.getHttpRequest().getParameter("client_id");
        if (StringUtils.isNotBlank(clientId)) {
            authorizationChallengeSessionObject.getAttributes().getAttributes().put("client_id", clientId);
        }
        
        String acrValues = context.getHttpRequest().getParameter("acr_values");
        if (StringUtils.isNotBlank(acrValues)) {
            authorizationChallengeSessionObject.getAttributes().getAttributes().put("acr_values", acrValues);
        }

        if (newSave) {
            deviceSessionService.persist(authorizationChallengeSessionObject);
        } else {
            deviceSessionService.merge(authorizationChallengeSessionObject);
        }

        return authorizationChallengeSessionObject;
    }
```


More details in [Authorization Challenge Custom Script Page](../../developer/scripts/authorization-challenge.md).

Full multi-step sample script can be found [here](../../../script-catalog/authorization_challenge/multi_step/AuthorizationChallenge.java)



## Full successful Authorization Challenge Flow sample

```
OpenID Connect Configuration
-------------------------------------------------------
REQUEST:
-------------------------------------------------------
GET /.well-known/openid-configuration HTTP/1.1 HTTP/1.1
Host: yuriyz-fond-skink.gluu.info

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 200
Connection: Keep-Alive
Content-Length: 6244
Content-Type: application/json
Date: Thu, 10 Aug 2023 11:53:04 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Server: Apache/2.4.41 (Ubuntu)
Set-Cookie: X-Correlation-Id=fa097a44-5568-48aa-9390-1880616e5a69; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{
  "request_parameter_supported" : true,
  "pushed_authorization_request_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/par",
  "introspection_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/introspection",
  "claims_parameter_supported" : false,
  "issuer" : "https://yuriyz-fond-skink.gluu.info",
  "userinfo_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "id_token_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "access_token_signing_alg_values_supported" : [ "none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "authorization_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/authorize",
  "service_documentation" : "http://jans.org/docs",
  "authorization_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "claims_supported" : [ "street_address", "country", "zoneinfo", "birthdate", "role", "gender", "formatted", "user_name", "phone_mobile_number", "preferred_username", "locale", "inum", "updated_at", "post_office_box", "nickname", "preferred_language", "email", "website", "email_verified", "profile", "locality", "phone_number_verified", "room_number", "given_name", "middle_name", "picture", "name", "phone_number", "postal_code", "region", "family_name", "jansAdminUIRole" ],
  "ssa_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/ssa",
  "token_endpoint_auth_methods_supported" : [ "client_secret_basic", "client_secret_post", "client_secret_jwt", "private_key_jwt", "tls_client_auth", "self_signed_tls_client_auth" ],
  "tls_client_certificate_bound_access_tokens" : true,
  "response_modes_supported" : [ "query", "jwt", "query.jwt", "form_post.jwt", "form_post", "fragment", "fragment.jwt" ],
  "backchannel_logout_session_supported" : true,
  "token_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/token",
  "response_types_supported" : [ "code", "code id_token token", "id_token", "code id_token", "token", "id_token token", "code token" ],
  "authorization_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "backchannel_token_delivery_modes_supported" : [ "poll", "ping", "push" ],
  "dpop_signing_alg_values_supported" : [ "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "request_uri_parameter_supported" : true,
  "backchannel_user_code_parameter_supported" : false,
  "grant_types_supported" : [ "client_credentials", "urn:ietf:params:oauth:grant-type:uma-ticket", "urn:ietf:params:oauth:grant-type:device_code", "urn:ietf:params:oauth:grant-type:token-exchange", "implicit", "authorization_code", "password", "refresh_token" ],
  "ui_locales_supported" : [ "en", "bg", "de", "es", "fr", "it", "ru", "tr" ],
  "userinfo_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/userinfo",
  "authorization_challenge_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/authorize-challenge",
  "op_tos_uri" : "https://yuriyz-fond-skink.gluu.info/tos",
  "require_request_uri_registration" : false,
  "id_token_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "frontchannel_logout_session_supported" : true,
  "authorization_signing_alg_values_supported" : [ "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "claims_locales_supported" : [ "en" ],
  "clientinfo_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/clientinfo",
  "request_object_signing_alg_values_supported" : [ "none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "request_object_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "key_from_java" : "value_from_script_on_java",
  "session_revocation_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/revoke_session",
  "check_session_iframe" : "https://yuriyz-fond-skink.gluu.info/jans-auth/opiframe.htm",
  "scopes_supported" : [ "address", "introspection", "https://jans.io/auth/ssa.admin", "online_access", "openid", "user_name", "clientinfo", "profile", "uma_protection", "permission", "https://jans.io/scim/users.write", "revoke_session", "https://jans.io/scim/users.read", "device_sso", "phone", "mobile_phone", "offline_access", "email" ],
  "backchannel_logout_supported" : true,
  "acr_values_supported" : [ "simple_password_auth" ],
  "request_object_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "device_authorization_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/device_authorization",
  "display_values_supported" : [ "page", "popup" ],
  "userinfo_signing_alg_values_supported" : [ "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "require_pushed_authorization_requests" : false,
  "claim_types_supported" : [ "normal" ],
  "userinfo_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "end_session_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/end_session",
  "revocation_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/revoke",
  "backchannel_authentication_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/bc-authorize",
  "token_endpoint_auth_signing_alg_values_supported" : [ "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "frontchannel_logout_supported" : true,
  "jwks_uri" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/jwks",
  "subject_types_supported" : [ "public", "pairwise" ],
  "id_token_signing_alg_values_supported" : [ "none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "registration_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/register",
  "id_token_token_binding_cnf_values_supported" : [ "tbh" ]
}


#######################################################
TEST: authorizationChallengeFlow
#######################################################
-------------------------------------------------------
REQUEST:
-------------------------------------------------------
POST /jans-auth/restv1/register HTTP/1.1
Host: yuriyz-fond-skink.gluu.info
Content-Type: application/json
Accept: application/json

{
  "grant_types" : [ "authorization_code", "refresh_token" ],
  "subject_type" : "public",
  "application_type" : "web",
  "scope" : "openid profile address email phone user_name",
  "minimum_acr_priority_list" : [ ],
  "redirect_uris" : [ "https://yuriyz-fond-skink.gluu.info/jans-auth-rp/home.htm", "https://client.example.com/cb", "https://client.example.com/cb1", "https://client.example.com/cb2" ],
  "client_name" : "jans test app",
  "additional_audience" : [ ],
  "response_types" : [ "code", "id_token" ]
}

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 201
Cache-Control: no-store
Connection: Keep-Alive
Content-Length: 1633
Content-Type: application/json
Date: Thu, 10 Aug 2023 11:53:05 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Pragma: no-cache
Server: Apache/2.4.41 (Ubuntu)
Set-Cookie: X-Correlation-Id=81dc6c45-7831-4738-b169-b087ee9a6bd6; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{
    "allow_spontaneous_scopes": false,
    "application_type": "web",
    "rpt_as_jwt": false,
    "registration_client_uri": "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/register?client_id=999e13b8-f4a2-4fed-ad3c-6c88bd2c92ea",
    "tls_client_auth_subject_dn": "",
    "run_introspection_script_before_jwt_creation": false,
    "registration_access_token": "28a50db3-b6d1-4054-a259-ef7168afa760",
    "client_id": "999e13b8-f4a2-4fed-ad3c-6c88bd2c92ea",
    "token_endpoint_auth_method": "client_secret_basic",
    "scope": "openid",
    "client_secret": "f6364c5c-295d-4e6e-bb40-6ad3a47b2119",
    "client_id_issued_at": 1691668385,
    "backchannel_logout_uri": "",
    "backchannel_logout_session_required": false,
    "client_name": "jans test app",
    "par_lifetime": 600,
    "spontaneous_scopes": [],
    "id_token_signed_response_alg": "RS256",
    "access_token_as_jwt": false,
    "grant_types": [
        "authorization_code",
        "refresh_token"
    ],
    "subject_type": "public",
    "additional_token_endpoint_auth_methods": [],
    "keep_client_authorization_after_expiration": false,
    "require_par": false,
    "redirect_uris": [
        "https://client.example.com/cb2",
        "https://client.example.com/cb1",
        "https://client.example.com/cb",
        "https://yuriyz-fond-skink.gluu.info/jans-auth-rp/home.htm"
    ],
    "redirect_uris_regex": "",
    "additional_audience": [],
    "frontchannel_logout_session_required": false,
    "client_secret_expires_at": 1691704385,
    "access_token_signing_alg": "RS256",
    "response_types": [
        "code",
        "id_token"
    ]
}

-------------------------------------------------------
REQUEST:
-------------------------------------------------------
POST /jans-auth/restv1/authorize-challenge HTTP/1.1
Host: yuriyz-fond-skink.gluu.info

client_id=999e13b8-f4a2-4fed-ad3c-6c88bd2c92ea&scope=openid+profile+address+email+phone+user_name&state=b4a41b29-51c8-4354-9c8c-fda38b4dbd43&nonce=3a56f8d0-f78e-4b15-857c-3e792801be68&prompt=&ui_locales=&claims_locales=&acr_values=&request_session_id=false&password=secret&username=admin

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 200
Cache-Control: no-transform, no-store
Connection: Keep-Alive
Content-Length: 61
Content-Type: application/json
Date: Thu, 10 Aug 2023 11:53:06 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Server: Apache/2.4.41 (Ubuntu)
Set-Cookie: X-Correlation-Id=3aa95eb7-73e2-40ae-9303-34adf30a1a05; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{"authorization_code":"9e3dc65b-937a-49c2-bdff-41fbc1a352d0"}

Successfully obtained authorization code 9e3dc65b-937a-49c2-bdff-41fbc1a352d0 at Authorization Challenge Endpoint
-------------------------------------------------------
REQUEST:
-------------------------------------------------------
POST /jans-auth/restv1/token HTTP/1.1
Host: yuriyz-fond-skink.gluu.info
Content-Type: application/x-www-form-urlencoded
Authorization: Basic OTk5ZTEzYjgtZjRhMi00ZmVkLWFkM2MtNmM4OGJkMmM5MmVhOmY2MzY0YzVjLTI5NWQtNGU2ZS1iYjQwLTZhZDNhNDdiMjExOQ==

grant_type=authorization_code&code=9e3dc65b-937a-49c2-bdff-41fbc1a352d0&redirect_uri=https%3A%2F%2Fyuriyz-fond-skink.gluu.info%2Fjans-auth-rp%2Fhome.htm

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 200
Cache-Control: no-store
Connection: Keep-Alive
Content-Length: 1250
Content-Type: application/json
Date: Thu, 10 Aug 2023 11:53:06 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Pragma: no-cache
Server: Apache/2.4.41 (Ubuntu)
Set-Cookie: X-Correlation-Id=3eb3c205-6206-4a70-98fb-75bf81757976; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{"access_token":"d87aa8d2-fefb-4d16-a775-9b9d27f73bfc","refresh_token":"505314a7-05f5-4c9f-8900-ccb0685dea17","id_token":"eyJraWQiOiJjb25uZWN0XzI1OGZmMmFiLWE4ODQtNDIxNy1iNmQ4LTJhMGI2NDhmOTcxZF9zaWdfcnMyNTYiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzI1NiJ9.eyJhdF9oYXNoIjoiUC04RktlejhlNHROTURTbVlGeHV5dyIsInN1YiI6IjI1Nzg0ZDQ5LTg0ZjMtNGIyNi1hZWUyLTEwNDkzMzM5MjMyZCIsImFtciI6W10sImlzcyI6Imh0dHBzOi8veXVyaXl6LWZvbmQtc2tpbmsuZ2x1dS5pbmZvIiwibm9uY2UiOiIzYTU2ZjhkMC1mNzhlLTRiMTUtODU3Yy0zZTc5MjgwMWJlNjgiLCJqYW5zT3BlbklEQ29ubmVjdFZlcnNpb24iOiJvcGVuaWRjb25uZWN0LTEuMCIsImF1ZCI6Ijk5OWUxM2I4LWY0YTItNGZlZC1hZDNjLTZjODhiZDJjOTJlYSIsInJhbmRvbSI6ImJmYmI5OTBmLWNkYTEtNGM3OC1hNjM4LWFhN2NiMjc5MjU3MiIsImFjciI6ImRlZmF1bHRfY2hhbGxlbmdlIiwiY19oYXNoIjoiTnFoSGFIenZZYjYxeDFackQwUEZVdyIsImF1dGhfdGltZSI6MTY5MTY2ODM4NiwiZXhwIjoxNjkxNjcxOTg2LCJncmFudCI6ImF1dGhvcml6YXRpb25fY29kZSIsImlhdCI6MTY5MTY2ODM4Nn0.QTUmzJaHtbPGjrV4E0MUn_fU1On44B6-_7pT0Dz_cY29s_KajGLfin3G_WsYmZA--ysyRLAmdK_X5C3W-wpkpDJ8906vuZST5547lSJGOZ45_VFv7XnTmBip3zRQOmrlxdU6OQ5Vmj3xMON_NQ-ckEUSNr65xWTAPmOQoncGYp8s-TO7ethyx6UyDTnW8d1YiXWCUYfQDQ8d5wCPHnfoYAsZCs_f0xaBUmaiwvUL3ckiXgMr2yHjWKWQuezlbjJk7ODu2cgoAzs3IWMonaixIJeeJJcOvFB4SPTnbToJe7ISvvsZTEwrLWW_E_LgTUEDqHbeWyeQI8WqDa9EOwMEFw","token_type":"Bearer","expires_in":299}

-------------------------------------------------------
REQUEST:
-------------------------------------------------------
POST /jans-auth/restv1/token HTTP/1.1
Host: yuriyz-fond-skink.gluu.info
Content-Type: application/x-www-form-urlencoded
Authorization: Basic OTk5ZTEzYjgtZjRhMi00ZmVkLWFkM2MtNmM4OGJkMmM5MmVhOmY2MzY0YzVjLTI5NWQtNGU2ZS1iYjQwLTZhZDNhNDdiMjExOQ==

grant_type=refresh_token&refresh_token=505314a7-05f5-4c9f-8900-ccb0685dea17

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 200
Cache-Control: no-store
Connection: Keep-Alive
Content-Length: 166
Content-Type: application/json
Date: Thu, 10 Aug 2023 11:53:08 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Pragma: no-cache
Server: Apache/2.4.41 (Ubuntu)
Set-Cookie: X-Correlation-Id=88a6b7a5-3230-4f0f-b859-09df77a5c67a; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{"access_token":"572f6422-caf9-496a-a6be-4ab39c872816","refresh_token":"e93b6576-5297-4d9d-a92b-3276d90a75e4","scope":"openid","token_type":"Bearer","expires_in":299}

-------------------------------------------------------
REQUEST:
-------------------------------------------------------
GET /jans-auth/restv1/userinfo HTTP/1.1 HTTP/1.1
Host: yuriyz-fond-skink.gluu.info
Authorization: Bearer 572f6422-caf9-496a-a6be-4ab39c872816

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 200
Cache-Control: no-store, private
Connection: Keep-Alive
Content-Length: 46
Content-Type: application/json;charset=utf-8
Date: Thu, 10 Aug 2023 11:53:08 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Pragma: no-cache
Server: Apache/2.4.41 (Ubuntu)
Set-Cookie: X-Correlation-Id=390c7a63-fe06-48a5-b3bf-2549267ba9b0; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{"sub":"25784d49-84f3-4b26-aee2-10493339232d"}

```

## Authorization Challenge Flow sample with invalid user

```
OpenID Connect Configuration
-------------------------------------------------------
REQUEST:
-------------------------------------------------------
GET /.well-known/openid-configuration HTTP/1.1 HTTP/1.1
Host: yuriyz-fond-skink.gluu.info

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 200
Connection: Keep-Alive
Content-Length: 6244
Content-Type: application/json
Date: Thu, 10 Aug 2023 11:57:01 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Server: Apache/2.4.41 (Ubuntu)
Set-Cookie: X-Correlation-Id=79c5fed3-d69a-4fdf-af88-fce550cd1819; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{
  "request_parameter_supported" : true,
  "pushed_authorization_request_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/par",
  "introspection_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/introspection",
  "claims_parameter_supported" : false,
  "issuer" : "https://yuriyz-fond-skink.gluu.info",
  "userinfo_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "id_token_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "access_token_signing_alg_values_supported" : [ "none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "authorization_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/authorize",
  "service_documentation" : "http://jans.org/docs",
  "authorization_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "claims_supported" : [ "street_address", "country", "zoneinfo", "birthdate", "role", "gender", "formatted", "user_name", "phone_mobile_number", "preferred_username", "locale", "inum", "updated_at", "post_office_box", "nickname", "preferred_language", "email", "website", "email_verified", "profile", "locality", "phone_number_verified", "room_number", "given_name", "middle_name", "picture", "name", "phone_number", "postal_code", "region", "family_name", "jansAdminUIRole" ],
  "ssa_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/ssa",
  "token_endpoint_auth_methods_supported" : [ "client_secret_basic", "client_secret_post", "client_secret_jwt", "private_key_jwt", "tls_client_auth", "self_signed_tls_client_auth" ],
  "tls_client_certificate_bound_access_tokens" : true,
  "response_modes_supported" : [ "query", "jwt", "query.jwt", "form_post.jwt", "form_post", "fragment", "fragment.jwt" ],
  "backchannel_logout_session_supported" : true,
  "token_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/token",
  "response_types_supported" : [ "code", "code id_token token", "id_token", "code id_token", "token", "id_token token", "code token" ],
  "authorization_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "backchannel_token_delivery_modes_supported" : [ "poll", "ping", "push" ],
  "dpop_signing_alg_values_supported" : [ "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "request_uri_parameter_supported" : true,
  "backchannel_user_code_parameter_supported" : false,
  "grant_types_supported" : [ "client_credentials", "urn:ietf:params:oauth:grant-type:uma-ticket", "urn:ietf:params:oauth:grant-type:device_code", "urn:ietf:params:oauth:grant-type:token-exchange", "implicit", "authorization_code", "password", "refresh_token" ],
  "ui_locales_supported" : [ "en", "bg", "de", "es", "fr", "it", "ru", "tr" ],
  "userinfo_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/userinfo",
  "authorization_challenge_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/authorize-challenge",
  "op_tos_uri" : "https://yuriyz-fond-skink.gluu.info/tos",
  "require_request_uri_registration" : false,
  "id_token_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "frontchannel_logout_session_supported" : true,
  "authorization_signing_alg_values_supported" : [ "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "claims_locales_supported" : [ "en" ],
  "clientinfo_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/clientinfo",
  "request_object_signing_alg_values_supported" : [ "none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "request_object_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "key_from_java" : "value_from_script_on_java",
  "session_revocation_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/revoke_session",
  "check_session_iframe" : "https://yuriyz-fond-skink.gluu.info/jans-auth/opiframe.htm",
  "scopes_supported" : [ "address", "introspection", "https://jans.io/auth/ssa.admin", "online_access", "openid", "user_name", "clientinfo", "profile", "uma_protection", "permission", "https://jans.io/scim/users.write", "revoke_session", "https://jans.io/scim/users.read", "device_sso", "phone", "mobile_phone", "offline_access", "email" ],
  "backchannel_logout_supported" : true,
  "acr_values_supported" : [ "simple_password_auth" ],
  "request_object_encryption_enc_values_supported" : [ "A128CBC+HS256", "A256CBC+HS512", "A128GCM", "A256GCM" ],
  "device_authorization_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/device_authorization",
  "display_values_supported" : [ "page", "popup" ],
  "userinfo_signing_alg_values_supported" : [ "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "require_pushed_authorization_requests" : false,
  "claim_types_supported" : [ "normal" ],
  "userinfo_encryption_alg_values_supported" : [ "RSA1_5", "RSA-OAEP", "A128KW", "A256KW" ],
  "end_session_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/end_session",
  "revocation_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/revoke",
  "backchannel_authentication_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/bc-authorize",
  "token_endpoint_auth_signing_alg_values_supported" : [ "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "frontchannel_logout_supported" : true,
  "jwks_uri" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/jwks",
  "subject_types_supported" : [ "public", "pairwise" ],
  "id_token_signing_alg_values_supported" : [ "none", "HS256", "HS384", "HS512", "RS256", "RS384", "RS512", "ES256", "ES384", "ES512", "ES512", "PS256", "PS384", "PS512" ],
  "registration_endpoint" : "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/register",
  "id_token_token_binding_cnf_values_supported" : [ "tbh" ]
}


#######################################################
TEST: authorizationChallengeFlow
#######################################################
-------------------------------------------------------
REQUEST:
-------------------------------------------------------
POST /jans-auth/restv1/register HTTP/1.1
Host: yuriyz-fond-skink.gluu.info
Content-Type: application/json
Accept: application/json

{
  "grant_types" : [ "authorization_code", "refresh_token" ],
  "subject_type" : "public",
  "application_type" : "web",
  "scope" : "openid profile address email phone user_name",
  "minimum_acr_priority_list" : [ ],
  "redirect_uris" : [ "https://yuriyz-fond-skink.gluu.info/jans-auth-rp/home.htm", "https://client.example.com/cb", "https://client.example.com/cb1", "https://client.example.com/cb2" ],
  "client_name" : "jans test app",
  "additional_audience" : [ ],
  "response_types" : [ "code", "id_token" ]
}

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 201
Cache-Control: no-store
Connection: Keep-Alive
Content-Length: 1633
Content-Type: application/json
Date: Thu, 10 Aug 2023 11:57:02 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Pragma: no-cache
Server: Apache/2.4.41 (Ubuntu)
Set-Cookie: X-Correlation-Id=7045173c-9a96-418a-86ed-47a09749b004; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{
    "allow_spontaneous_scopes": false,
    "application_type": "web",
    "rpt_as_jwt": false,
    "registration_client_uri": "https://yuriyz-fond-skink.gluu.info/jans-auth/restv1/register?client_id=d93a5129-1546-4b9b-bf8c-ea19e36ea2c8",
    "tls_client_auth_subject_dn": "",
    "run_introspection_script_before_jwt_creation": false,
    "registration_access_token": "67aa99de-e977-4562-955d-6292f2c95df4",
    "client_id": "d93a5129-1546-4b9b-bf8c-ea19e36ea2c8",
    "token_endpoint_auth_method": "client_secret_basic",
    "scope": "openid",
    "client_secret": "f921c89c-57f0-4a91-baaa-036a4a22737b",
    "client_id_issued_at": 1691668622,
    "backchannel_logout_uri": "",
    "backchannel_logout_session_required": false,
    "client_name": "jans test app",
    "par_lifetime": 600,
    "spontaneous_scopes": [],
    "id_token_signed_response_alg": "RS256",
    "access_token_as_jwt": false,
    "grant_types": [
        "authorization_code",
        "refresh_token"
    ],
    "subject_type": "public",
    "additional_token_endpoint_auth_methods": [],
    "keep_client_authorization_after_expiration": false,
    "require_par": false,
    "redirect_uris": [
        "https://client.example.com/cb2",
        "https://client.example.com/cb1",
        "https://client.example.com/cb",
        "https://yuriyz-fond-skink.gluu.info/jans-auth-rp/home.htm"
    ],
    "redirect_uris_regex": "",
    "additional_audience": [],
    "frontchannel_logout_session_required": false,
    "client_secret_expires_at": 1691704622,
    "access_token_signing_alg": "RS256",
    "response_types": [
        "code",
        "id_token"
    ]
}

-------------------------------------------------------
REQUEST:
-------------------------------------------------------
POST /jans-auth/restv1/authorize-challenge HTTP/1.1
Host: yuriyz-fond-skink.gluu.info

client_id=d93a5129-1546-4b9b-bf8c-ea19e36ea2c8&scope=openid+profile+address+email+phone+user_name&state=4f925a8d-287a-4cba-a174-04d2e56109df&nonce=84c9b6dd-635c-4ca4-bba1-35c53c51a339&prompt=&ui_locales=&claims_locales=&acr_values=&request_session_id=false&password=secret&username=invalidUser

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 401
Cache-Control: no-transform, no-store
Connection: Keep-Alive
Content-Length: 29
Content-Type: application/json
Date: Thu, 10 Aug 2023 11:57:02 GMT
Expires: Thu, 01 Jan 1970 00:00:00 GMT
Keep-Alive: timeout=5, max=100
Server: Apache/2.4.41 (Ubuntu)
Set-Cookie: X-Correlation-Id=4c89e007-6c77-43da-a67f-b7ee1ff0e60a; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{"error": "username_invalid"}

``` 