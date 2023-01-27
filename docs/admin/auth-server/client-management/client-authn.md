---
tags:
  - administration
  - client
---

# Client Authentication

Janssen Server supports authentication for confidential clients at the token endpoint. Confidential clients
have to specify a preferred method of authentication during the client registration process. Client authentication
is defined by [OAuth](https://datatracker.ietf.org/doc/html/rfc6749#section-2.3) and OpenID Connect
[Client Metadata](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) in the section 
`token_endpoint_auth_method` and [client authentication section](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication)

## Supported Authentication Methods

List of supported authentication methods for a Janssen Server host is listed in the response of Janssen Server's 
well-known [configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`token_endpoint_auth_methods_supported` claim in the response specifies the list of all the supported methods.

## Authentication Methods

Authentication methods can be broadly categorised in two categories:

1. Shared key based
2. Private key based

While shared key based authentication is simpler to implement, it is less secure than private key based authentication
mechanisms. This is primarily because client secret is transferred between client and authorization server at some point
during the authentication process. 

Characteristics table below shows side-by-side comparison of various supported authentication methods.

| Method                        | Secret Not Sent in Clear |      Signed      | Only client has secret | Token Expiry     |
|-------------------------------|:------------------------:|:----------------:|:----------------------:|------------------|
| `client_secret_basic`         |     :material-close:     | :material-close: |    :material-close:    | :material-close: |
| `client_secret_post`          |     :material-close:     | :material-close: |    :material-close:    | :material-close: |
| `client_secret_jwt`           |     :material-check:     | :material-check: |    :material-close:    | :material-check: |
| `private_key_jwt`             |     :material-check:     | :material-check: |    :material-check:    | :material-check: |
| `tls_client_auth`             |                          |                  |                        |                  |
| `self_signed_tls_client_auth` |                          |                  |                        |                  |
| `none`                        |                          |                  |                        |                  |

### client_secret_basic

Default authentication method for Janssen Server. Authenticates clients using method described in 
[client authentication](https://datatracker.ietf.org/doc/html/rfc6749#section-2.3.1) section of OAuth framework. 

## client_secret_post

Authenticates clients using method described in 
[client authentication](https://datatracker.ietf.org/doc/html/rfc6749#section-2.3.1) section of OAuth framework by 
adding client credentials in request body.

## client_secret_jwt

Like client_secret_basic(#client_secret_basic) and client_secret_post (#client_secret_post) methods, this method is also
based on a secret that client receives from Janssen Server. But instead of sending
secret back to authorization server everytime, the client creates a JWT using an HMAC SHA algorithm where the shared
secret is the key. This method is more secure than the client_secret_basic(#client_secret_basic) and client_secret_post
(#client_secret_post) due to following reasons:

- Secret which is shared once will never be transmitted again
- JWT can have expiration time, beyond which the same JWT can not be used. This reduces the time window for replay of 
the same token in case it is compromised.

This method is further described in OpenId Connect specification, [section 9](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication).

### Configuration

Janssen Server clients should specify the preferred algorithm for use of this method during client configuration.

Algorithms supported by Janssen Server are listed in the response of Janssen Server's well-known
[configuration endpoint](../endpoints/configuration.md). From the response, the claim 
`token_endpoint_auth_signing_alg_values_supported` list the supported algorithms.

To specify preferred algorithm for a client, when using [Janssen Text-based UI(TUI)](../../config-guide/tui.md) to configure the properties,
navigate via `Auth Server` -> Get or add clients -> `encryption/signing` -> TODO: which exact properties.

## private_key_jwt

`private_key_jwt` is private key based method where secret is not shared between client and authorization server. This method is 
further described in OpenId Connect specification, [section 9](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication).

Janssen server implements signing and encryption mechanism following the guidelines in [section 10] of OpenId Connect
specification. Clients sign and encrypt JWT as per its security requirements. 

### Configuration

Janssen Server clients can specify signing and encryption keys in using client configuration. Clients can either specify
JWKS as value or as reference URI. 

To specify JWKS or reference URI, when using [Janssen Text-based UI(TUI)](../../config-guide/tui.md) to configure the properties,
navigate via `Auth Server` -> Get or add clients -> `encryption/signing` -> set value for `Client JWKS URI` or 
`Client JWKS`.

## tls_client_auth

TODO: add more details

## self_signed_tls_client_auth

TODO: add more details

## none

TODO: add more details

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).