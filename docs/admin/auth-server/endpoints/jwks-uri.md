---
tags:
- administration
- auth-server
- jwks
- json-web-key-set
- endpoint
---

# Overview

Janssen Server supports `/jwks` metadata endpoint and publishes its JSON Web Key Set (JWKS) at this endpoint. This 
endpoint publishes signing keys as well as encryption keys used by Janssen Server. RP can use these keys to validate
signatures from Janssen Server, and also to perform encryption and decryption.  This is not a secure endpoint. 

URL to access jwks endpoint on Janssen Server is listed in the response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`jwks_uri` claim in the response specifies the URL for jwks endpoint. By default, the jwks endpoint looks like below:

```
https://janssen.server.host/jans-auth/restv1/jwks
```

This endpoint is always enabled and can not be disabled using feature flags.

## Configuration Properties

End session endpoint can be further configured using Janssen Server configuration properties listed below. When using
[Janssen Text-based UI(TUI)](../../config-guide/tui.md) to configure the properties,
navigate via `Auth Server`->`Properties`.

- [jwksUri](../../reference/json/properties/janssenauthserver-properties.md#jwksuri)
- [jwksAlgorithmsSupported](../../reference/json/properties/janssenauthserver-properties.md#jwksalgorithmssupported)
- [mtlsJwksUri](../../reference/json/properties/janssenauthserver-properties.md#mtlsjwksuri)

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).