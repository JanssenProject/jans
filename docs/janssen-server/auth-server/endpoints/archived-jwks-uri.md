---
tags:
- administration
- auth-server
- jwks
- json-web-key-set
- endpoint
---
# Archived JWKS URI
## Overview

Janssen Server supports `/jwks/archived/{kid}` metadata endpoint and publishes its Archived JSON Web Keys (JWKs) at this endpoint. This 
endpoint publishes expired signing keys as well as expired encryption keys used by Janssen Server. RP can use these keys to validate
signatures from Janssen Server, and also to perform encryption and decryption if keys are no longer present in `/jwks` endpoint. 
Like other metadata endpoints, this is not a secure endpoint.

URL to access archived jwks endpoint on Janssen Server is listed in the response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`archived_jwks_uri` claim in the response specifies the URL for archived jwks endpoint. By default, the archived jwks endpoint looks like below:

```
https://janssen.server.host/jans-auth/restv1/jwks/archived/{kid}
```

This endpoint is always enabled and can not be disabled using feature flags.

## Configuration Properties

Archived JWKs endpoint can be further configured using Janssen Server configuration properties listed below. When using
[Janssen Text-based UI(TUI)](../../config-guide/config-tools/jans-tui/README.md) to configure the properties,
navigate via `Auth Server`->`Properties`.

- [archivedJwksUri](../../reference/json/properties/janssenauthserver-properties.md#jwksuri)
- [archivedJwkLifetimeInSeconds](../../reference/json/properties/janssenauthserver-properties.md#archivedjwklifetimeinseconds)

If `archivedJwkLifetimeInSeconds` is not set then AS falls back to one year expiration. After archived jwk lifetime is passed, jwk is removed from archive.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).