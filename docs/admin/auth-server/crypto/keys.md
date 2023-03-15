---
tags:
  - administration
  - auth-server
  - cryptography
---

# Overview
 
Janssen uses keys for signing and encryption, primarily concerning JSON documents. Janssen supports several signing and encryption algorithms in salted and unsalted, to fit a variety of business needs. If other algorithms are necessary, Janssen supports them via interception scripts.
   
# Keys

A key is a piece of information, usually a string of numbers or letters that are stored in a file, which, when processed through a cryptographic algorithm, can encode or decode cryptographic data. In Janssen we use keys for creating token like `id_token`,`access_token`.

* ***access_token :***
Access tokens are what the OAuth client uses to make requests to an API. The access token is meant to be read and validated by the API.

* ***id_token :***
An ID token is an artifact that proves that the user has been authenticated. It was introduced by [OpenID Connect (OIDC)](https://openid.net/connect/), an open standard for authentication used by Janssen.
It contains information about what happened when a user authenticated, and is intended to be read by the OAuth client. The ID token may also contain information about the user such as their name or email address, although that is not a requirement of an ID token.


## Jans server JSON objects of public key
Gets list of JSON Web Key (JWK) used by server. JWK is a JSON data structure that represents a set of public keys as a JSON object. Browse below endpoint to know more about public keys used by server.

```https://<your_server>/jans-auth/restv1/jwks```

***Let's see some example of JWT public keys used in janssen.***

### Example-1

The "kty" (Key Type) of this key is RSA. This key is "use" (Public Key Use) for "sig" (Signature) on data. The "alg" (Algorithm) intended with this key is "RS256".
```commandLine
{
      "name" : "Connect RS256 Sign Key"
      "descr" : "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-256",
      ...
}
```
### Example-2

The "kty" (Key Type) of this key is RSA. This key is "use" (Public Key Use) for "sig" (Signature) on data. The "alg" (Algorithm) intended with this key is "RS384".

```commandLine
{
      "name" : "Connect RS384 Sign Key",
      "descr" : "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-384",
      ...
}
```

### Example-3

The "kty" (Key Type) of this key is RSA. This key is "use" (Public Key Use) for signature ("sig") on data. The "alg" (Algorithm) intended with this key is "RS512".

```commandLine
{
      "name" : "Connect RS512 Sign Key",
      "descr" : "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-512",
      ...
}
```

### Example-4
The "kty" (Key Type) of this key is "EC" (Ellliptic Curve). This key is "use" (Public Key Use) for signature ("sig") on data. The "alg" (Algorithm) intended with this key is "ES256".

```commandLine
{
      "name" : "Connect ES256 Sign Key",
      "descr" : "Signature Key: ECDSA using P-256 (secp256r1) and SHA-256",
      ...
}
```
### Example-5
The "kty" (Key Type) of this key is "EC" (Ellliptic Curve). This key is "use" (Public Key Use) for signature ("sig") on data. The "alg" (Algorithm) intended with this key is "ES256K".

```commandLine
{
      "name" : "Connect ES256 Sign Key",
      "descr" : "Signature Key: ECDSA using P-256 (secp256r1) and SHA-256",
      ...
}
```
### Example-6
The "kty" (Key Type) of this key is "EC" (Ellliptic Curve). This key is "use" (Public Key Use) for signature ("sig") on data. The "alg" (Algorithm) intended with this key is "ES256K".
```commandLine
{
      "name" : "Connect ES384 Sign Key",
      "descr" : "Signature Key: ECDSA using P-384 (secp384r1) and SHA-384",
      ...
}
```
### Example-7
The "kty" (Key Type) of this key is "EC" (Ellliptic Curve). This key is "use" (Public Key Use) for signature ("sig") on data. The "alg" (Algorithm) intended with this key is "ES512".

```commandLine
{
      "name" : "Connect ES512 Sign Key",
      "descr" : "Signature Key: ECDSA using P-521 (secp521r1) and SHA-512",
      ...
}
```
### Example-8

The "kty" (Key Type) of this key is "RSA". This key is "use" (Public Key Use) for signature ("sig") on data. The "alg" (Algorithm) intended with this key is "PS256".

```commandLine
{
      "name" : "Connect PS256 Sign Key",
      "descr" : "Signature Key: RSASSA-PSS using SHA-256 and MGF1 with SHA-256",
      ...
}
```
### Example-9

The "kty" (Key Type) of this key is "RSA". This key is "use" (Public Key Use) for signature ("sig") on data. The "alg" (Algorithm) intended with this key is "PS384".

```commandLine
{
      "name" : "Connect PS384 Sign Key",
      "descr" : "Signature Key: RSASSA-PSS using SHA-384 and MGF1 with SHA-384",
      ...
}
```

### Example-10

The "kty" (Key Type) of this key is "RSA". This key is "use" (Public Key Use) for signature ("sig") on data. The "alg" (Algorithm) intended with this key is "PS512".

```commandLine
{
      "name" : "Connect PS512 Sign Key",
      "descr" : "Signature Key: RSASSA-PSS using SHA-512 and MGF1 with SHA-512",
      ...
}
```

### Example-11

The "kty" (Key Type) of this key is "RSA". This key is "use" (Public Key Use) for "enc" (encryption) on data. The "alg" (Algorithm) intended with this key is "RSA1_5".

``commandLine
{
      "name" : "Connect RSA1_5 Encryption Key",
      "descr" : "Encryption Key: RSAES-PKCS1-v1_5",
      ...
}
```

### Example-12

The "kty" (Key Type) of this key is "RSA". This key is "use" (Public Key Use) for "enc" (encryption) on data. The "alg" (Algorithm) intended with this key is "RSA-OAEP".

```commandLine
{
      "name" : "Connect RSA-OAEP Encryption Key",
      "descr" : "Encryption Key: RSAES OAEP using default parameters",
      ...
 }
```
   
# Crypto Supported

Depending on the transport through which the messages are sent, the integrity of the message might not be guaranteed and the 
originator of the message might not be authenticated. To mitigate these risks, ID Token, UserInfo Response, Request Object, 
and Client Authentication JWT values can utilize [JSON Web Signature (JWS) [JWS]](https://openid.net/specs/openid-connect-core-1_0.html#JWS) to sign their contents. To achieve message 
confidentiality, these values can also use [JSON Web Encryption (JWE) [JWE]](https://openid.net/specs/openid-connect-core-1_0.html#JWE) to encrypt their contents.


## Signing algorithms
Signing or Signature algorithm is a cryptographic mechanism used to verify the authenticity and 
integrity of digital data. We may consider it as a digital version of the ordinary handwritten signatures, but with higher 
levels of complexity and security.

Let's see some signing key algorithms 

| Name | Kty | Use | Alg | 
|--------|-------|------|---------|
|RS256 Sign Key|RSA |sig|RS256|
|RS384 Sign Key|RSA|sig|RS384|
|RS512 Sign Key|RSA|sig|RS512|
|ES256 Sign Key|EC|sig|ES256|
|ES256 Sign Key|EC|sig|ES256K|
|ES384 Sign Key|EC|sig|ES384|
|ES512 Sign Key|EC|sig|ES384|
|PS256 Sign Key|RSA|sig|PS256|
|PS384 sign Key|RSA|sig|PS384|
|PS512 sign Key|RSA|sig|PS512|

## Encryption algorithms

In cryptography, encryption is the process of encoding information. This process converts the original 
representation of the information, known as plaintext, into an alternative form known as ciphertext. 
Ideally, only authorized parties can decipher a ciphertext back to plaintext and access the original information.

Let's see some encryption keys.

| Name | Kty | Use | Alg | 
|--------|-------|------|---------|
|RSA1_5 Encryption key|RSA |enc|RSA1_5|
|RSA-OAEP Encryption Key|RSA|enc|RSA-OAEP|
|ECDH-ES Encryption Key|CE|enc|ECDH-ES|



## RSA Key Formats

* PKCS#8 encrypted private key "EncryptedPrivateKeyInfo" (default for private keys)
* PKCS#1 "RSAPublicKey" (default for public keys)
* PKCS#8 unencrypted "PrivateKeyInfo"
* PKCS#1 unencrypted "RSAPrivateKey" (OpenSSL private key file format)
* X.509 "SubjectPublicKeyInfo" (OpenSSL public key file format)

The above key values can be passed as (a) a binary DER-encoded ASN.1 file, (b) a text file in PEM format, (c) a 
string containing the key in [PEM format](https://www.cryptosys.net/pki/manpki/pki_pemstring.html).

Also supported are RSA private and public keys represented in XML format to XKMS 2.0 [XKMS] and JSON Web Key (JWK) format [[JWK]](https://www.cryptosys.net/pki/manpki/pki_References.html#JWK). 
For more details, see [Key Storage Format](https://www.cryptosys.net/pki/manpki/pki_Keystorage.html).
To know more about RSA keyformat browse [here](https://www.cryptosys.net/pki/rsakeyformats.html)


## Janssen Keystore Formats

In Janssen we use  keystore formats [PKCS12](https://www.rfc-editor.org/rfc/rfc7292). Which is Encrypted private key formats.
In Janssen currently using an extension of `p12`. We also have `pkcs12`, `pfx`.
We can store private keys, secret keys and certificates on this type. You can find `jans-auth-keys.p12` in `/etc/certs/`

To see the private key you can use bellow command
```CommandLine
keytool -list -v -keystore /etc/certs/jans-auth-keys.p12 -storetype pkcs12 -storepass <password>
```

