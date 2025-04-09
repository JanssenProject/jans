---
tags:
  - cedarling
  - jwt-validation
---

# Cedarling JWT Validation

Cedarling performs JWT (JSON Web Token) validation as part of its authorization workflow. This process ensures that only tokens from trusted issuers, with valid signatures and required claims, are used to make policy decisions. Optional revocation and trust-mode checks add further assurance and control.

Learn more about each part of the validation process:

- [Signature Validation](#jwt-signature-validation): Verifies the token's origin using trusted issuer keys.
- [Content Validation](#jwt-content-validation): Ensures required claims like `exp` or `client_id` are present.
- [JWT Revocation (W.I.P)](#jwt-revocation-wip): Checks if a token has been explicitly revoked.
- [JWT Validation Flow Diagram](#jwt-validation-flow-diagram): Visual overview of Cedarling's validation logic.
- [Related Bootstrap Properties](#related-bootstrap-properties): [Bootstrap properties](./cedarling-properties.md) that are related to JWT validation.

## JWT Signature Validation

At startup, Cedarling fetches public keys from trusted identity providers (IDPs) defined in the [policy store](./cedarling-policy-store.md). These keys are used to validate the signature of incoming JWTs.

### Example Policy Store

Cedarling only validates tokens issued by [trusted issuers](./cedarling-policy-store.md#trusted-issuer-schema) listed in the policy store. Tokens from issuers not listed in the policy store will be **ignored** and will not be used for [entity creation](./cedarling-entities.md).

To allow an Access token like the one below to be used for authorization:

```json
{
  "iss": "https://test.issuer.com",
  "aud": "abc123",
  "exp": 1234567890
}
```

You **MUST** define a trusted issuer in you policy store with a matching `openid_configuration_endpoint` (same host as the token's `iss` claim):

```json
{
  // ... other fields have been omitted for brevity
  "trusted_issuers": {
    "name": "my trusted issuer",
    "description": "an IDP that i trust",
    "openid_configuration_endpoint": "https://test.issuer.com/.well-known/openid-configuration", // this endpoint has the same host as the token's `iss` claim
    "tokens_metadata": { 
      "access_token": { 
        // metadata specific to this token type
      }
    }
  }
}
```

Additionally, only tokens **explicitly named** in the tokens_metadata section will be validated. All other tokens will be ignored.

### Validation Requirements

In summary, for a token to be validated by Cedarling, two conditions must be met:

1. The `iss` (Issuer) claim must match the **host** of an `openid_configuration_endpoint` in the policy store.
2. The token must be provided under a **token name** defined in the corresponding `tokens_metadata`

  ```js
  // Example authorize call
  cedarling.authorize({
    "tokens": {
      "access_token": "<access_token>", // will be validated and used for entity creation
      "id_token": "<id_token>",         // will be ignored unless defined in tokens_metadata
    },
    // ...
  })
  ```

## JWT Content Validation

Cedarling also supports validating the contents of a JWT by enforcing the presence of required claims. These requirements are defined in the `tokens_metadata` section of the policy store.

### Example

```json
{
  // ... other fields have been omitted for brevity
  "trusted_issuers": {
    "name": "my trusted issuer",
    "description": "an IDP that i trust",
    "openid_configuration_endpoint": "https://test.issuer.com/.well-known/openid-configuration", // this endpoint has the same host as the token's `iss` claim
    "tokens_metadata": { 
      "access_token": {
        "required_claims": ["exp", "client_id"]
      }
    }
  }
}
```

The above configuration means that any access_token must contain both the exp and client_id claims, or it will be rejected. Additionally, *registered claims*[^registered-claims] like the `exp` will also be validated.

[^registered-claims]: Registered claims like `exp`, `iat`, `iss`, etc., are defined in [`RFC 7519, Section 4.1`] (https://datatracker.ietf.org/doc/html/rfc7519#section-4.1)

### Id Token Trust Mode

Cedarling supports an optional strict trust mode for validating relationships between different token types—primarily ID tokens, Access tokens, and Userinfo tokens.

This behavior is controlled via the `CEDARLING_ID_TOKEN_TRUST_MODE` [bootstrap property](#related-bootstrap-properties).

#### `strict` Mode

If `CEDARLING_ID_TOKEN_TRUST_MODE` is set to `strict`, Cedarling will enforce the following:

1. The `id_token`'s `aud` (Audience) must match the `access_token`'s `client_id`;
2. If a `userinfo_token` is also provided:
  - Its `sub` (Subject) must match that of the `id_token`
  - Its `aud` must also match the `access_token`'s `client_id`.

These additional checks add another layer of identity assurance across tokens.

## JWT Revocation (W.I.P)

Cedarling optionally supports JWT revocation checks by validating the status bit of a "Status Token" JWT, as proposed in the [OAuth Status Lists](https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/) draft.

This feature is toggled with the `CEDARLING_JWT_STATUS_VALIDATION` property.

> ℹ️ **Use Case**
>
> Enforcing token revocation can help mitigate account takeover risks by allowing for near-instant invalidation of compromised tokens.


## JWT Validation Flow Diagram

JWTs (JSON Web Tokens) contain authorization information that is used by the Cedarling to construct the Principal entities. In order to verify the authenticity of this information, the Cedarling can verify the integrity of the JWT by validating its signature and status(active, expired, or revoked). It does so by fetching the public keyset and the list of active tokens from the issuer of the JWT.

![](../assets/lock-cedarling-diagram-4.jpg)

## Json Web Token Validation

**Note:** Make sure you have specified at least one [Trusted Issuer](./cedarling-policy-store.md#trusted-issuer-schema) in your [Policy Store](./cedarling-policy-store.md).

###  JWT Signature Validation

To enable this feature, set the `CEDARLING_JWT_VALIDATION` bootstrap property to `True`. For development and testing purposes, you can set this property to `False` and submit an unsigned JWT, such as one generated from [jwt.io](https://jwt.io). On initialization, the Cedarling will fetch the latest public 
keys from the issuers specified in the Policy Store and cache them. The cedarling uses the JWT `iss` claim to select the appropriate keys for validation.

### JWT Content Validation

If timestamps are provided in the context, the Cedaring always verifies: 

- `exp` (expiration)
- `nbf` (not before)

If the Cedarling property `CEDARLING_ID_TOKEN_TRUST_MODE` is `Strict`, the Cedarling will:

- Discard `id_token` if the `aud` claim does not contain the `client_id` of the access token
- Discard Userinfo tokens that are not associated with a `sub` claim from the `id_token`


### JWT Status Validation

The Cedarling can also check for JWT revocation if you have the `CEDARLING_JWT_STATUS_VALIDATION` property `Enabled`. The Cedarling checks the status bit of the Status Token JWT, as described in the [OAuth Status Lists](https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/) draft. Token status enforcement mitigates account takeover by enabling immediate revocation of all tokens issued to an attacker. 

## ID Token Trust Mode

The level of validation for the ID Token JWT can be set to either `None` or `Strict`.

### `None` Mode

Setting the validation level to `None` will not check for the conditions outlined in [Strict Mode](#strict-mode).

### `Strict` Mode

Strict mode requires:

1. The `id_token`'s `aud` contains the `access_token`'s `client_id` (the `aud` field is an array that must contain the `client_id`);
2. if a Userinfo token is present, the `sub` matches the `id_token`, and that the `aud` matches the access token's `client_id`.

## Local JWKS

A local JWKS can be used by setting the `CEDARLING_LOCAL_JWKS` bootstrap property to a path to a local JSON file. When providing a local Json Web Key Store (JWKS), the file must follow the following schema:

```json
{
    "trusted_issuer_id": [ ... ]
    "another_trusted_issuer_id": [ ... ]
}
```

* Where keys are `Trusted Issuer IDs` assigned to each key store
* and the values contains the JSON Web Keys as defined in [RFC 7517](https://datatracker.ietf.org/doc/html/rfc7517).
* The `trusted_issuers_id` is used to tag a JWKS with a unique identifier and enables using multiple key stores.

## Related Bootstrap Properties

The following properties control JWT behavior in Cedarling. These must be set before startup and cannot be changed at runtime.

| name | description | allowed values | default |
| --- | --- | --- | --- |
| `CEDARLING_LOCAL_JWKS` | A JSON Web Key Set for local testing | `null` or JWKS JSON string | `null` |
| `CEDARLING_POLICY_STORE_LOCAL` | A JSON string string containing the full policy store. | `null` or JSON object string | `null` |
| `CEDARLING_JWT_SIG_VALIDATION` | Toggles the JWT signature validation | `enabled`, `disbled` | `disabled` |
| `CEDARLING_JWT_STATUS_VALIDATION` | Enables token status validation (revocation support). See [JWT revocation section](#jwt-revocation-wip). | `enabled`, `disabled` | `disabled` |
| `CEDARLING_ID_TOKEN_TRUST_MODE` | Controls ID token trust checks. See [Id Token Trust Mode]. | `strict`, `none` | `strict` |

See the full list of supported properties in the [bootstrap properties reference](./cedarling-properties.md).
