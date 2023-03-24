---
tags:
  - administration
  - planning
  - certificates
  - keys
  - cryptography
  - JCE
  - HSM
  - FIPS
---

# JSON Signing and Encryption

Janssen uses keys for signing and encryption, primarily concerning
JSON documents. There are six IETF RFCs that provide considerable detail:

* [RFC 7515 JSON Web Signing (JWE)](https://www.rfc-editor.org/rfc/rfc7515)
* [RFC 7516 JSON Web Encryption (JWE)](https://www.rfc-editor.org/rfc/rfc7516)
* [RFC 7517 JSON Web Key (JWK)](https://www.rfc-editor.org/rfc/rfc7517)
* [RFC 7518 JSON Web Algorithms (JWA)](https://www.rfc-editor.org/rfc/rfc7518)
* [RFC 7519 JSON Web Signing (JWT)](https://www.rfc-editor.org/rfc/rfc7519)
* [RFC 7520 Examples of Protecting Content Using JSON Object Signing and Encryption (JOSE)](https://www.rfc-editor.org/rfc/rfc7520)

### Auth Server Supported Signing

| JWT Type     | Algorithms    |
| ------------- |:-------------:|
| DPOP     | RS256, RS384, RS512, ES256, ES384, ES512, PS256, PS384, PS512 |
| Authorization  | HS256, HS384, HS512, RS256, RS384, RS512, ES256, ES384, ES512, ES512, PS256, PS384, PS512      |
| Request Object  | HS256, HS384, HS512, RS256, RS384, RS512, ES256, ES384, ES512, PS256, PS384, PS512      |
| Userinfo  | HS256, HS384, HS512, RS256, RS384, RS512, ES256, ES384, ES512, PS256, PS384, PS512       |
| Token Endpoint Auth | HS256, HS384, HS512, RS256, RS384, RS512, ES256, ES384, ES512, PS256, PS384, PS512   |
| ID Token  | HS256, HS384, HS512, RS256, RS384, RS512, ES256, ES384, ES512, PS256, PS384, PS512 |

### Auth Server Supported Encryption

| Encryption Methods   | Algorithms    |
| ------------- |:-------------:|
| Authorization |  A128CBC+HS256, A256CBC+HS512, A128GCM, A256GCM |
| id_token  | A128CBC+HS256, A256CBC+HS512, A128GCM, A256GCM |
| Request object  | A128CBC+HS256, A256CBC+HS512, A128GCM, A256GCM |
| Userinfo | A128CBC+HS256, A256CBC+HS512, A128GCM, A256GCM |

| Encryption Algorithms   | Algorithms    |
| ------------- |:-------------:|
| Userinfo |RSA1_5, RSA-OAEP, A128KW, A256KW|
| id_token |RSA1_5, RSA-OAEP, A128KW, A256KW|
| authorization | RSA1_5, RSA-OAEP, A128KW, A256KW |
| request object | RSA1_5, RSA-OAEP, A128KW, A256KW |

## Java Cryptographic Engine (JCE)

Janssen projects ships and tests with the
[Bouncy Castle Crypto API's](https://www.bouncycastle.org/) JCE. However, you
may substitute your own JCE as long as it has implementations for the algorithms
used by Auth Server.

## Using a Hardware Security Module (HSM)

Janssen Auth Server can utilize an external REST API,
[Jans Eleven](https://github.com/JanssenProject/jans/tree/main/jans-eleven),
which leverages an HSM that exposes a `PKCS #11` interface.

## Key Rotation

OpenID Connect clients must support the rotation of both
[signing](https://openid.net/specs/openid-connect-core-1_0.html#RotateSigKeys) and
[encryption](https://openid.net/specs/openid-connect-core-1_0.html#RotateEncKeys)
keys. The best practice is to rotate often--the default configuration in
a VM installation of Auth Server is **every two days**. The reason for such a
frequent rotation is to make sure developer account for rotation at the time
they create applications--lest they forget and their software breaks a year
later when rotation happens.

In a single VM deployment, key rotation is controlled by Auth Server. But in a
clustered deployment, key rotation has to happen centrally. Janssen includes a
key rotation service for cloud deployments.

# Certificates

X.509 is used extensively for web server TLS. But it is also used for Mutual
Transport Layer Security (MTLS), either initiated by a software client, or
presented by a person (i.e. a personal certificate). MTLS is generally
implemented in the web tier.

# FIPS

To support FIPS 140-2 conformance, you must use a FIPS approved JCE, and
preferably, use an operating system that has FIPS Enforcement, like
[RHEL 8](https://access.redhat.com/documentation/en-us/red_hat_enterprise_linux/8/html/security_hardening/assembly_installing-a-rhel-8-system-with-fips-mode-enabled_security-hardening).
