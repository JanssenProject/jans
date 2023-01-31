---
tags:
  - administration
  - client
  - authentication
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
mechanisms. This is primarily because when using shared key based authentication methods, the client secret is 
transferred between client and authorization server at some point during the authentication process. There are more 
reasons to prefer private key over shared secret as listed in this [section](#using-private-key-for-authentication)

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

`private_key_jwt` is private key based method where secret is not shared between client and authorization server. This method is 
further described in OpenId Connect specification, [section 9](https://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication).

Janssen server implements signing and encryption mechanism following the guidelines in [section 10](https://openid.net/specs/openid-connect-core-1_0.html#SigEnc) 
of OpenId Connect specification. Clients should sign and encrypt JWT as per their security requirements. 

### How To Use

- Create a private key

    ```shell
    openssl genrsa -out private-key.pem 2048
    ```

- Extract public key from private key

    ```shell
    openssl rsa -in private-key.pem -pubout -outform PEM -out public-key.pem
    ```

    During the client registration or via client configuration settings, the public key generated above should be attached
    to the client on authorization server.

- Create header payload. For example,

    ```json
    {
    "alg": "RS256",
    "typ": "JWT"
    }
    ```

    To create JWT we need header in base64 encoded format. Use commands below and to convert header in base64 encoded string:
  
    ```shell
    echo -n 'header-json' | base64 | sed s/\+/-/ | sed -E s/=+$//
    ```

- Create your JWT payload. For example,

    ```json
    {
    "jti":"myJWTId001",
    "sub":"38174623762",
    "iss":"38174623762",
    "aud":"http://localhost:4000/api/auth/token/direct/24523138205",
    "exp":1536165540,
    "iat":1536132708
    }
    ```

    To create JWT we need the payload in base64 encoded format. Use commands below and to convert the payload in base64 
    encoded string:
  
    ```shell
    echo -n 'payload-json' | base64 | sed s/\+/-/ | sed -E s/=+$//
    ```

- Use the encoded header and payload to create the signature. The signature would be in form of an encoded string.

    ```shell
    echo -n "encoded-header.encoded-payload" | openssl dgst -sha256 -binary -sign jwtRSA256-private.pem  | openssl enc -base64 | tr -d '\n=' | tr -- '+/' '-_'
    ```

- Put together encoded strings for header, payload and signature in format below to completely form JWT.

    ```json
    encoded-header.encoded-payload.encoded-signature
    ```

- Use this JWT in request to the server. As mentioned earlier, the server must have public key registered as part of 
  the client registration process in order to verify the JWT in the request.

### Client Configuration For Using private_key_jwt

Janssen Server clients can specify signing and encryption keys using client configuration. Clients can either specify
JWKS as value or as reference URI. 

To specify JWKS values or reference URI, using [Janssen Text-based UI(TUI)](../../config-guide/tui.md),
navigate via `Auth Server` -> Get or add clients -> `encryption/signing` -> set value for `Client JWKS URI` or 
`Client JWKS`.

## tls_client_auth

This is mutual TLS based authentication method where client authenticates with Janssen Server using X.509 certificate. In this authentication method, the client uses X.509 certificate that is issued by a Certificate Authority(CA). Benefit of using mutual TLS based authentication is that the authorization server binds the token with the certificate. This provides enhanced security where it is possible to check that the token belongs to the intended client. 

This authentication mechanism is defined in [mTLS specification for OAuth2](https://www.rfc-editor.org/rfc/rfc8705#name-mutual-tls-for-oauth-client)

## self_signed_tls_client_auth

This is mutual TLS based authentication method where client authenticates with Janssen Server using X.509 certificate. Client uses self signed X.509 certificate, removing the dependency on for public key infrastructure(PKIX). Benefit of using mutual TLS based authentication is that the authorization server binds the token with the certificate. This provides enhanced security where it is possible to check that the token belongs to the intended client. 

This authentication mechanism is defined in [mTLS specification for OAuth2](https://www.rfc-editor.org/rfc/rfc8705#name-self-signed-certificate-mut)

## none

The Client does not authenticate itself at the Token Endpoint, either because it uses only the Implicit Flow (and so 
does not use the Token Endpoint) or because it is a Public Client with no Client Secret or other authentication 
mechanism.

## Using Private Key for Authentication

### Security Features Of Private Key Authentication

- **Secure Storage**: Hardware Security Module(HSM) or Trusted Platform Module(TPM) can be used to securely store private key 
  and sign using it. These modules make it impossible to access private key from outside.
- **Smaller footprint**: Unlike shared secret, where client secret resides on authorization server as well as client, 
  the private key only exists on client. This reduced footprint reduces potential risks.
- **Limited Time Validity**: JWT can be set to expire after a certain duration. Similarly, client certificates can be 
  made to expire. This makes it harder to execute replay attacks using a compromised token or certificate.


## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
