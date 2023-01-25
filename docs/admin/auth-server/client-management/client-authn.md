---
tags:
  - administration
  - client
---

# Client Authentication

Janssen Server supports authentication for confidential clients at the token endpoint. Confidential clients
have to specify a preferred method of authentication during the client registration process.

## Supported Authentication Methods

List of supported authentication methods for a Janssen Server host is listed in the response of Janssen Server's 
well-known [configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`token_endpoint_auth_methods_supported` claim in the response specifies the list of all the supported methods.

## Authentication Methods

### client_secret_basic


## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).