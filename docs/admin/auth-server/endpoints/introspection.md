---
tags:
  - administration
  - auth-server
  - endpoint
  - introspection
  - accessTokenAsJwt
  - introspectionScriptBackwardCompatibility
---

# Overview

Introspection endpoint allows a protected resource to determine the active state of an OAuth 2.0 token and to determine 
meta-information about this token. This endpoint can be used to introspect
both opaque token (i.e. reference tokens) and structured tokens(i.e. value tokens). This endpoint conforms to [OAuth2 token
introspection](https://datatracker.ietf.org/doc/html/rfc7662) specifications.

The URI to invoke the introspection endpoint in Janssen Server 
can be found by checking the `introspection_endpoint` claim of the OpenID Connect configuration response, typically deployed at `https://janssen.server.host/.well-known/openid-configuration`

 "introspection_endpoint" : "https://janssen.server.host/jans-auth/restv1/introspection" `

More information about request and response of the Introspection endpoint can be found in
the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml#/Token/get-introspection).

**Request parameters**
- `token` - REQUIRED.  The string value of the token.  For access tokens, this is the "access_token" value returned from the token endpoint
- `token_type_hint` - OPTIONAL.  A hint about the type of the token submitted for introspection. Not used in current implementation of the AS. 
- `response_as_jwt` - OPTIONAL. Boolean value with default value false. If true, returns introspection response as JWT (signed based on client configuration used for authentication to Introspection Endpoint).

## Sample GET Request

### Request

```
curl -X 'GET'   'https://janssen.server.host/jans-auth/restv1/introspection?token=368fea2b-be14-4d30-bd57-bcc4cde2033c&response_as_jwt=false'   -H 'accept: application/json' -H   "Authorization: Bearer 111d51a4-2828-4b47-abce-77034cddcfb5"
```

### Response

```
{
    "sub": "",
    "iss": "https://janssen.server.host",
    "active": true,
    "token_type": "Bearer",
    "client_id": "1800.df1bb233-10b8-40ed-bbb9-07da50892a35",
    "aud": "1800.df1bb233-10b8-40ed-bbb9-07da50892a35",
    "nbf": null,
    "scope": "https://jans.io/oauth/config/scripts.write",
    "acr_values": null,
    "cnf": null,
    "exp": 1668705523,
    "iat": 1668705223,
    "jti": null,
    "username": null
}
```
## Sample POST Request

### Request

```
curl -X 'POST' \
  'https://janssen.server.host/jans-auth/restv1/introspection' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'token=eyJra....3ZkB-Ajwg'  
  -H "Authorization: Bearer eyJra...BpKo7g"
```

### Response

```
{
    "sub": "",
    "iss": "https://janssen.server.host",
    "active": true,
    "token_type": "Bearer",
    "client_id": "3000.5829c1f8-7554-41ab-a7d6-3513a5e9c4ad",
    "aud": "3000.5829c1f8-7554-41ab-a7d6-3513a5e9c4ad",
    "nbf": null,
    "scope": "",
    "acr_values": null,
    "cnf": null,
    "exp": 1668941216,
    "iat": 1668781885,
    "jti": null,
    "username": null
}
```

## Response as JWT


Response is returned as JWT if `Accept` header has value `application/token-introspection+jwt` or otherwise if explicit endpoint parameter `response_as_jwt` is set to `true`.

```
POST /introspect HTTP/1.1
Host: as.example.com
Accept: application/token-introspection+jwt
```  

Sample decoded JWT payload
```json
{
  "iss":"https://as.example.com/",
  "aud":"https://rs.example.com/resource",
  "iat":1514797892,
  "token_introspection":
     {
        "active":true,
        "iss":"https://as.example.com/",
        "aud":"https://rs.example.com/resource",
        "iat":1514797822,
        "exp":1514797942,
        "client_id":"paiB2goo0a",
        "scope":"read write dolphin",
        "sub":"Z5O3upPC88QrAjx00dis",
        "birthdate":"1982-02-01",
        "given_name":"John",
        "family_name":"Doe",
        "jti":"t1FoCCaZd4Xv4ORJUWVUeTZfsKhW30CQCrWDDjwXy6w"
     }
} 
```

Sample response (line breaks in payload is for convenience)

```
HTTP/1.1 200 OK
Content-Type: application/token-introspection+jwt

eyJraWQiOiJ3RzZEIiwidHlwIjoidG9rZW4taW50cm9zcGVjdGlvbitqd3QiLCJhbGc
iOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2FzLmV4YW1wbGUuY29tLyIsImF1ZCI6I
mh0dHBzOi8vcnMuZXhhbXBsZS5jb20vcmVzb3VyY2UiLCJpYXQiOjE1MTQ3OTc4OTIs
InRva2VuX2ludHJvc3BlY3Rpb24iOnsiYWN0aXZlIjp0cnVlLCJpc3MiOiJodHRwczo
vL2FzLmV4YW1wbGUuY29tLyIsImF1ZCI6Imh0dHBzOi8vcnMuZXhhbXBsZS5jb20vcm
Vzb3VyY2UiLCJpYXQiOjE1MTQ3OTc4MjIsImV4cCI6MTUxNDc5Nzk0MiwiY2xpZW50X
2lkIjoicGFpQjJnb28wYSIsInNjb3BlIjoicmVhZCB3cml0ZSBkb2xwaGluIiwic3Vi
IjoiWjVPM3VwUEM4OFFyQWp4MDBkaXMiLCJiaXJ0aGRhdGUiOiIxOTgyLTAyLTAxIiw
iZ2l2ZW5fbmFtZSI6IkpvaG4iLCJmYW1pbHlfbmFtZSI6IkRvZSIsImp0aSI6InQxRm
9DQ2FaZDRYdjRPUkpVV1ZVZVRaZnNLaFczMENRQ3JXRERqd1h5NncifX0.przJMU5Gh
mNzvwtt1Sr-xa9xTkpiAg5IshbQsRiRVP_7eGR1GHYrNwQh84kxOkHCyje2g5WSRcYo
sGEVIiC-eoPJJ-qBwqwSlgx9JEeCDw2W5DjrblOI_N0Jvsq_dUeOyoWVMqlOydOBhKN
Y0smBrI4NZvEExucOm9WUJXMuJtvq1gBes-0go5j4TEv9sOP9uu81gqWTr_LOo6pgT0
tFFyZfWC4kbXPXiQ2YT6mxCiQRRNM-l9cBdF6Jx6IOrsfFhBuYdYQ_mlL19HgDDOFal
eyqmru6lKlASOsaE8dmLSeKcX91FbG79FKN8un24iwIDCbKT9xlUFl54xWVShNDFA
```

## Disabling The Endpoint Using Feature Flag

`/introspection` endpoint can be enabled or disable using [END_SESSION feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#introspection).
Use [Janssen Text-based UI(TUI)](../../config-guide/jans-tui/README.md) or [Janssen command-line interface](../../config-guide/jans-cli/README.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or
disable `INTROSPECTION` flag as required.

![](../../../assets/image-tui-enable-components.png)

## Configuration Properties

Introspection endpoint can be further configured using Janssen Server configuration properties listed below. When using
[Janssen Text-based UI(TUI)](../../config-guide/jans-tui/README.md) to configure the properties,
navigate via `Auth Server`->`Properties`.

- [introspectionEndpoint](../../reference/json/properties/janssenauthserver-properties.md#introspectionendpoint)
- [mtlsIntrospectionEndpoint](../../reference/json/properties/janssenauthserver-properties.md#mtlsintrospectionendpoint)
- [introspectionSkipAuthorization](../../reference/json/properties/janssenauthserver-properties.md#introspectionskipauthorization)
- [introspectionScriptBackwardCompatibility](../../reference/json/properties/janssenauthserver-properties.md#introspectionscriptbackwardcompatibility)
- [introspectionAccessTokenMustHaveUmaProtectionScope](../../reference/json/properties/janssenauthserver-properties.md#introspectionaccesstokenmusthaveumaprotectionscope)
- [introspectionAccessTokenMustHaveIntrospectionScope](../../reference/json/properties/janssenauthserver-properties.md#introspectionaccesstokenmusthaveintrospectionscope)
- [introspectionResponseScopesBackwardCompatibility](../../reference/json/properties/janssenauthserver-properties.md#introspectionresponsescopesbackwardcompatibility)

There difference between `introspectionAccessTokenMustHaveUmaProtectionScope` and `introspectionAccessTokenMustHaveIntrospectionScope` is that `uma_protection` scope is enabled for Dynamic Client Registration while `introspection` scope is not. Thus if set `introspectionAccessTokenMustHaveIntrospectionScope` to `true` value allows disable access to Introspection Endpoint to all clients which does not have explicitly granted `introspection` scope. 

## Customising Introspection Endpoint Behaviour using Custom script:

Customizing certain aspects of endpoint behaviour, for example, one can modify claims of an access token as JWT, using
[introspection scripts](../../../script-catalog/introspection/README.md). Use
[update token](../../developer/scripts/update-token.md) introspection script for transformation of claims and values in
id-token and access-token.

Configure below-mentioned client properties to enable usage of introspection scripts.
When using [Janssen Text-based UI(TUI)](../../config-guide/jans-tui/README.md) to configure these client properties,
navigate to

- _accessTokenAsJwt_:

`Auth Server`->`Clients`->select the client->`Tokens`->`Access Token Type`->Select JWT

- _runIntrospectionScriptBeforeJwtCreation_:

`Auth Server`->`Clients`->select the client->`Tokens`->enable `Run Introspection Script before JWT access token creation`

![](../../../assets/image-tui-client-config-token-screen.png)

### References for custom scripts
1. Interface - [IntrospectionType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/introspection/IntrospectionType.java)
2. [Introspection scripts](../../../script-catalog/introspection/README.md)
3. [Introspection script vs Update Token Script](https://github.com/JanssenProject/jans/blob/main/docs/admin/developer/scripts/update-token.md#introspectiontype-script-vs-updatetokentype-script)
