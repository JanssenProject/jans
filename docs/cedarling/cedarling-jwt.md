---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - JWT
---

# Cedarling JWT Flow

![](../assets/lock-cedarling-diagram-4.jpg)

# Json Web Token Validation

**Note:** To enable Json Web Token (JWT) Validation in Cedarling, it is required to populate the [`trusted_issuers`](./cedarling-policy-store.md#trusted-issuer-schema) field in the [Policy Store](./cedarling-policy-store.md).

### Enabling JWT Signature Validation

Cedarling can validate JWT signatures for enhanced security. To enable this feature, set the `CEDARLING_JWT_VALIDATION` bootstrap property to `True`. For development and testing purposes, you can set this property to `False` and submit an unsigned JWT, such as one generated from [JWT.io](https://jwt.io).

### Public Key Management

When token validation is enabled, Cedarling downloads the public keys of the Trusted IDPs specified in the policy store during initialization. Cedarling uses the JWT `iss` claim to select the appropriate keys for validation.

### JWT Revocation

In enterprise deployments, Cedarling can also check for JWT revocation. This is achieved by following the mechanism described in the [OAuth Status Lists](https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/) draft. Token status enforcement helps mitigate risks associated with account takeover by enabling immediate revocation of all tokens issued to an attacker. Additionally, domains may choose to use Token Status to implement single-use transaction tokens.

### Summary of JWT Validation Mechanisms

Depending on your bootstrap properties, Cedarling may validate JWTs through the following methods:

- Validate signatures from Trusted Issuers
- Check JWT status for revocation
- Discard `id_token` if the `aud` claim does not match the `client_id` of the access token
- Discard Userinfo tokens that are not associated with a `sub` claim from the `id_token`
- Verify `exp` (expiration) and `nbf` (not before) claims of access tokens and id_tokens, if timestamps are provided in the context

![](./assets/lock-cedarling-diagram-4.jpg)
