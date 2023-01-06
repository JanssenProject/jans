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

Introspection endpoint allows a protected resource to query an OAuth 2.0 authorization server to determine the active 
state of an OAuth 2.0 token and to determine meta-information about this token. This endpoint can be used to introspect
both opaque token (i.e reference tokens) and structured tokens(i.e value tokens). This token conforms to [OAuth2 token
introspection](https://datatracker.ietf.org/doc/html/rfc7662) specifications.

The URI to invoke the introspection endpoint in Janssen Auth Server 
can be found by checking the `introspection_endpoint` claim of the OpenID Connect configuration response, typically deployed at `https://janssen.server.host/.well-known/openid-configuration`

` "introspection_endpoint" : "https://janssen.server.host/jans-auth/restv1/introspection" `

More information about request and response of the clientinfo endpoint can be found in
the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/replace-janssen-version/jans-auth-server/docs/swagger.yaml#/Token/get-introspection).


## Sample GET Request

**Request**

```
curl -X 'GET'   'https://janssen.server.host/jans-auth/restv1/introspection?token=368fea2b-be14-4d30-bd57-bcc4cde2033c&response_as_jwt=false'   -H 'accept: application/json' -H   "Authorization: Bearer 111d51a4-2828-4b47-abce-77034cddcfb5"
```

**Response**

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

**Request**

```
curl -X 'POST' \
  'https://janssen.server.host/jans-auth/restv1/introspection' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'token=eyJra....3ZkB-Ajwg'  
  -H "Authorization: Bearer eyJra...BpKo7g"
```

**Response**

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

## Customising Introspection Endpoint Behaviour

Customizing certain aspects of endpoint behaviour, for example, one can modify claims of an access token as JWT, using
[introspction scripts](../../../script-catalog/introspection/README.md).

When using [Janssen Text-based UI(TUI)](../../config-guide/tui.md) to configure above mentioned client properties, 
navigate to
- accessTokenAsJwt: `Auth Server`->`Clients`->select the client->`Tokens`->`Access Token Type`->Select JWT
- runIntrospectionScriptBeforeJwtCreation: `Auth Server`->`Clients`->select the client->`Tokens`->enable 
`Run Introspection Script before JWT access token creation`

![](../../../assets/image-tui-client-config-token-screen.png)

## Configuration Properties

Introspection endpoint can be further configured using Janssen Server configuration properties listed below. When using
[Janssen Text-based UI(TUI)](../../config-guide/tui.md) to configure the properties,
navigate via `Auth Server`->`Properties`.

- [introspectionEndpoint](../../reference/json/properties/janssenauthserver-properties.md#introspectionendpoint)
- [mtlsIntrospectionEndpoint](../../reference/json/properties/janssenauthserver-properties.md#mtlsintrospectionendpoint)
- [introspectionSkipAuthorization](../../reference/json/properties/janssenauthserver-properties.md#introspectionskipauthorization)
- [introspectionScriptBackwardCompatibility](../../reference/json/properties/janssenauthserver-properties.md#introspectionscriptbackwardcompatibility)
- [introspectionAccessTokenMustHaveUmaProtectionScope](../../reference/json/properties/janssenauthserver-properties.md#introspectionaccesstokenmusthaveumaprotectionscope)
- [introspectionResponseScopesBackwardCompatibility](../../reference/json/properties/janssenauthserver-properties.md#introspectionresponsescopesbackwardcompatibility)

### Testing

1. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/client/src/test/java/io/jans/as/client/ws/rs/IntrospectionWsHttpTest.java)

2. Swagger doc - https://github.com/JanssenProject/jans/blob/main/jans-auth-server/docs/swagger.yaml

3. Useful tool - [jwt.io](https://jwt.io/) to inspect a JWT

### References

1. How is UpdateTokenType script different? - [Article](../../developer/scripts/update-token.md)
