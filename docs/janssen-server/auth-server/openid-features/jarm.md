---
tags:
  - administration
  - auth-server
  - openidc
  - feature
---
# JARM
JWT Secured Authorization Response Mode (JARM) is an OpenID/OAuth extension that returns the authorization response as a JWT (`response` parameter) instead of plain URL parameters like `code` or `access_token`. See the specification for protocol details: [OAuth 2.0 JWT Secured Authorization Response Mode (JARM)](https://openid.net/specs/oauth-v2-jarm.html).

In Janssen Server, JARM is supported at the authorization endpoint through these response modes:

- `query.jwt`
- `fragment.jwt`
- `form_post.jwt`
- `jwt` (shortcut mode)

For `response_mode=jwt`, Janssen follows the JARM default mapping:

- `response_type=code` -> `query.jwt`
- response types that include `token` and/or `id_token` -> `fragment.jwt`

This means `jwt` acts as a shorthand, and Janssen automatically chooses where the `response=<JWT>` parameter is delivered based on response type:

- **Code flow only**: returned via query component (`query.jwt`)
- **Implicit or hybrid-style flows** (any response type containing `token` or `id_token`): returned via fragment component (`fragment.jwt`)


## Configure JARM in Janssen Server

JARM configuration has two layers:

1. **Authorization Server capabilities** (global)
2. **Client metadata** (per RP/client)

### 1) Authorization Server capabilities

These global properties define supported JARM response modes and allowed signing/encryption algorithms:

- [responseModesSupported](../../reference/json/properties/janssenauthserver-properties.md#responsemodessupported)
- [authorizationSigningAlgValuesSupported](../../reference/json/properties/janssenauthserver-properties.md#authorizationsigningalgvaluessupported)
- [authorizationEncryptionAlgValuesSupported](../../reference/json/properties/janssenauthserver-properties.md#authorizationencryptionalgvaluessupported)
- [authorizationEncryptionEncValuesSupported](../../reference/json/properties/janssenauthserver-properties.md#authorizationencryptionencvaluessupported)

You can configure them using the [Janssen TUI](../../config-guide/config-tools/jans-tui/README.md) by navigating to `Auth Server` -> `Properties`, or through Config API/JSON configuration management.

### 2) Client metadata (per client)

To make a client use JARM, configure client metadata through Dynamic Client Registration or client management APIs/UI:

- `authorization_signed_response_alg` (JWS algorithm; `none` is not accepted)
- `authorization_encrypted_response_alg` (optional, for JWE)
- `authorization_encrypted_response_enc` (optional, for JWE content encryption)

At runtime:

- If only `authorization_signed_response_alg` is set, Janssen returns a **signed JARM JWT**
- If encryption fields are set, Janssen returns an **encrypted JWT** (or nested signed+encrypted JWT when signing is also configured)

You can configure them using the [Janssen TUI](../../config-guide/config-tools/jans-tui/README.md).

## Important endpoints and discovery metadata

### Authorization endpoint

JARM is used on the standard authorization endpoint:

```text
https://<your-domain>/jans-auth/restv1/authorize
```

Include `response_mode` in the authorization request, for example:

```text
response_mode=query.jwt
```

or:

```text
response_mode=jwt
```

### OpenID configuration endpoint

Use discovery to confirm runtime support:

```text
https://<your-domain>/jans-auth/.well-known/openid-configuration
```

Important discovery claims for JARM:

- `response_modes_supported`
- `authorization_signing_alg_values_supported`
- `authorization_encryption_alg_values_supported`
- `authorization_encryption_enc_values_supported`

### JWKS endpoint

Clients validate JWS signatures from the authorization response JWT with AS keys from:

```text
https://<your-domain>/jans-auth/restv1/jwks
```

## Request and response behavior

### Authorization request

Typical parameters are the same as standard authorization requests, with added JARM response mode selection:

- `client_id`
- `redirect_uri`
- `response_type`
- `scope`
- `state`
- `nonce` (required for relevant OIDC/implicit-hybrid flows)
- `response_mode=query.jwt|fragment.jwt|form_post.jwt|jwt`

### Authorization response

Instead of plain fields in query/fragment/form body, Janssen returns:

- `response=<JWT>`

The JWT includes standard authorization response data such as success or error fields (for example `code`, `state`, `error`) as JWT claims.

## FAPI note

When [fapiCompatibility](../../reference/json/properties/janssenauthserver-properties.md#fapicompatibility) is enabled, Janssen applies stricter checks. In this mode:

- `response_type=code` must use `response_mode=jwt`
- `response_mode=query` is rejected

This aligns with the stricter JARM/FAPI behavior enforced by authorization request validation.



## Why use JARM and use cases

JARM helps clients validate authorization responses cryptographically and reduce risks from front-channel tampering.

Common benefits and typical Janssen JARM use cases:

- Response integrity via JWS signatures
- Optional response confidentiality via JWE encryption
- Better fit for high-assurance profiles (including FAPI-style deployments)
- Consistent JWT-based validation model at the client side

JARM is specifically useful in:

- **Financial/regulated ecosystems** where signed (and often encrypted) front-channel responses are required
- **Hybrid and implicit-style response handling** where front-channel payload protection is important
- **Multi-party integrations** where response authenticity must be independently verifiable by clients
- **Security hardening programs** that standardize on JWT validation patterns across OAuth/OIDC responses

## Related documentation

- [Authorization Endpoint](../endpoints/authorization.md)
- [PAR (Pushed Authorization Requests)](../oauth-features/par.md)
- [Janssen Auth Server Configuration Properties](../../reference/json/properties/janssenauthserver-properties.md)

