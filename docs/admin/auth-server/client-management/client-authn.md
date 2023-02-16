---
tags:
  - administration
  - client
  - authentication
---

# Client Authentication

Janssen Server supports authentication at the token endpoint for confidential clients. Confidential clients
have to specify a preferred method of authentication during the client registration process. Client authentication
is defined by [OAuth](https://datatracker.ietf.org/doc/html/rfc6749#section-2.3) and OpenID Connect 
[client authentication section](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication) and in
[Metadata section](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata) property 
`token_endpoint_auth_method`

## Supported Authentication Methods

List of supported authentication methods are listed in the response of Janssen Server's 
well-known [configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

The `token_endpoint_auth_methods_supported` claim in the response specifies the list of all the supported methods.

## Authentication Methods

Authentication methods can be broadly categorised in two categories:

1. Shared key based
2. Private( or asymmetric) key based

While shared key based authentication is simpler to implement, it is less secure than a private key based authentication.
This is primarily because when using shared key based authentication, the client secret is transmitted between the client
and the authorization server. This makes the authentication vulnerable to theft attacks. There are more 
reasons to prefer private key over shared secret as listed in this [section](#security-features-of-private-key-authentication)

Characteristics table below shows side-by-side comparison of various supported authentication methods.

| Method                        | Secret Not Sent in Clear |      Signed      | Only client has secret | Expiry           | Token Binding    |
|-------------------------------|:------------------------:|:----------------:|:----------------------:|------------------|------------------|
| `client_secret_basic`         |     :material-close:     | :material-close: |    :material-close:    | :material-close: | :material-close: |
| `client_secret_post`          |     :material-close:     | :material-close: |    :material-close:    | :material-close: | :material-close: |
| `client_secret_jwt`           |     :material-close:     | :material-check: |    :material-close:    | :material-check: | :material-close: |
| `private_key_jwt`             |     :material-check:     | :material-check: |    :material-check:    | :material-check: | :material-close: |
| `tls_client_auth`             |     :material-check:     | :material-check: |    :material-check:    | :material-check: | :material-check: |
| `self_signed_tls_client_auth` |     :material-check:     | :material-check: |    :material-check:    | :material-check: | :material-check: |

## client_secret_basic

Default authentication method for Janssen Server. It authenticates clients using method described in 
[client authentication](https://datatracker.ietf.org/doc/html/rfc6749#section-2.3.1) section of OAuth framework. 

## client_secret_post

`client_secret_post` method authenticates clients using method described in 
[client authentication](https://datatracker.ietf.org/doc/html/rfc6749#section-2.3.1) section of OAuth framework by 
adding client credentials in request body.

## client_secret_jwt

Like `client_secret_basic` and `client_secret_post` methods, this method is also
based on a shared secret that client receives from Janssen Server. But instead of sending
secret back to authorization server everytime, the client creates a JWT using an HMAC SHA algorithm where the shared
secret is used as the key. This method is more secure than the `client_secret_basic` and `client_secret_post`
 due to following reasons:

- Secret which is shared once will never be transmitted again
- JWT can have expiration time, beyond which the same JWT can not be used. This reduces the time window for replay of 
the same token in case it is compromised.

This method is further described in OpenId Connect specification, [section 9](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication).

### Client Configuration For Using client_secret_jwt

Janssen Server clients should specify the preferred algorithm for use with this method during client configuration.

Algorithms supported by Janssen Server are listed in the response of Janssen Server's well-known
[configuration endpoint](../endpoints/configuration.md). From the response, the claim 
`token_endpoint_auth_signing_alg_values_supported` lists the supported algorithms.

To specify preferred algorithm for a client, using [Janssen Text-based UI(TUI)](../../config-guide/tui.md),
navigate via `Auth Server` -> Get or add clients -> `encryption/signing` -> TODO: which exact properties.

## private_key_jwt

`private_key_jwt` is private key based method where secret is not shared between client and authorization server. 
Instead, the client generates an JSON Web Token(JWT) which is shared with the Janssen Server. Upon receiving JWT singned
by client's private key, the Janssen Server can validate this JWT using public keys supplied by client at the time of 
registration. 

The client can supply public keys in form of [JSON Web Key Set(JWKS)](https://www.rfc-editor.org/rfc/rfc7517#section-5) or 
provide a URI where the client publishes its JWKS. Providing JWKS URI is preferred method as it enable easy key rotations
without client having to update JWKS manualy. Check `jwks` and `jwks_uri` elements in [OIDC client metadata section](https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata)
for more details.

This authentication method is further described in OpenId Connect specification, 
[section 9](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication).

Janssen server implements signing and encryption mechanism following the guidelines in [section 10](https://openid.net/specs/openid-connect-core-1_0.html#SigEnc) 
of OpenId Connect specification. Clients should choose the algorithm to sign and encrypt JWT as per their security 
requirements. 

### Security Features Of Private Key Authentication

This method of authentication is more secure than the methods relying on shared key due to following features.

- **Secure Storage**: Hardware Security Module(HSM) or Trusted Platform Module(TPM) can be used to securely store private key 
  and sign using it at client side. These modules make it impossible to access private key from outside.
- **Smaller footprint**: Unlike shared secret, where client secret resides on authorization server as well as client, 
  the private key only exists on client. This reduced footprint reduces potential risks.
- **Limited Time Validity**: JWT can be set to expire after a certain duration. Similarly, client certificates can be 
  made to expire. This makes it harder to execute replay attacks using a compromised token or certificate.


### Implementation Steps

Below are the high-level steps involved in using `private_key_jwt` authentication method:

- Create JWK private key
- Derive JWK public key for the private key. Public key JWK should be provided to Janssen Server at the time of registration
- Create JWT header and payload as described in [section 9 of specification](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication)
- Sign JWT with a signing algorithm

Client code that implements above steps should leverage any of the secure and trusted library that implements these functions. A non-exhaustive list of such libraries
can be found at [jwt.io](https://jwt.io/libraries). Psudocode implementation for Java based client using `nimbus-jose-jwt` library can be found [here](https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-rsa-signature)

### Client Configuration For Using private_key_jwt

Janssen Server clients can specify signing and encryption keys using client configuration. Clients can either specify
JWKS as value or as reference URI. 

To specify JWKS values or reference URI, using [Janssen Text-based UI(TUI)](../../config-guide/tui.md),
navigate via `Auth Server` -> Get or add clients -> `encryption/signing` -> set value for `Client JWKS URI` or 
`Client JWKS`.

## tls_client_auth

During TLS handshake, the client authenticates with Janssen Server using X.509 certificate that is issued by a 
Certificate Authority(CA). This is mutual TLS based authentication method.
Benefit of using mutual TLS based authentication is that the authorization server binds the token with the certificate.
This provides enhanced security where it is possible to check that the token belongs to the intended client.

This authentication mechanism is defined in [mTLS specification for OAuth2](https://www.rfc-editor.org/rfc/rfc8705#name-mutual-tls-for-oauth-client)

## self_signed_tls_client_auth

Client authenticates with Janssen Server using self signed X.509 certificate during TLS handshake. Use of self-signed
certificate removes the dependency of public key infrastructure(PKIX). This is mutual TLS based authentication method.
Benefit of using mutual TLS based authentication is that the authorization server binds the token with the certificate.
This provides enhanced security where it is possible to check that the token belongs to the intended client. 

This authentication mechanism is defined in [mTLS specification for OAuth2](https://www.rfc-editor.org/rfc/rfc8705#name-self-signed-certificate-mut)

## none

The Client does not authenticate itself at the Token Endpoint, either because it uses only the Implicit Flow (and so 
does not use the Token Endpoint) or because it is a Public Client with no Client Secret or other authentication 
mechanism.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
