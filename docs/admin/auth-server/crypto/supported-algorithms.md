---
tags:
  - administration
  - auth-server
  - cryptography
---

# Supported Cryptographic Algorithms


## Overview
Out of the box, Janssen supports several encryption algorithms in salted and unsalted, to fit a variety of business needs. If other algorithms are necessary, Janssen supports them via interception scripts.

When the connection between a device (such as a client) and a server is established, the client and the server 
exchange data in order to determine the algorithms to use in the  transport layer.
To know more browse [here](https://www.cryptosys.net/pki/manpki/pki_SupportedAlgorithms.html)

The following are the type of supported algorithms 

* Signing 
* Encryption
* Hashing

### Signing algorithms
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



### Encryption algorithms

In cryptography, encryption is the process of encoding information. This process converts the original 
representation of the information, known as plaintext, into an alternative form known as ciphertext. 
Ideally, only authorized parties can decipher a ciphertext back to plaintext and access the original information.

Let's see some encryption key algorithms.

| Name | Kty | Use | Alg | 
|--------|-------|------|---------|
|RSA1_5 Encryption key|RSA |enc|RSA1_5|
|RSA-OAEP Encryption Key|RSA|enc|RSA-OAEP|
|ECDH-ES Encryption Key|CE|enc|ECDH-ES|
|RSA-OAEP Encryption Key|RSA|enc|RSA-OAEP|


### Hashing algorithms
In cryptography, hashing is a process that allows to take data of any size and apply a mathematical process to it that 
creates an output that’s a unique string of characters and numbers of the same length. Thus, no matter what size or length 
of the input data, we always get a hash output of the same length.

Let's see some Hashing algorithms 

|Name|Hash length|
|--------|-------|
|MD5|16|
|SHA1|20|
|SHA224|28|
|SHA256|32|
|SHA384|48|
|SHA512|64|


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

## Elliptic Curve Key Formats

* PKCS#8 encrypted private key EncryptedPrivateKeyInfo using "id-ecPublicKey" (default for private keys)
* PKCS#8 unencrypted PrivateKeyInfo
* ECPrivateKey from [[RFC5915]](https://www.cryptosys.net/pki/manpki/pki_Bibliography.html#RFC5915)
* SubjectPublicKeyInfo from [[RFC5480]](https://www.cryptosys.net/pki/manpki/pki_Bibliography.html#RFC5480) for public keys (default)

These elliptic curve key values can be passed as (a) a binary DER-encoded ASN.1 file, (b) a text file in PEM format, 
(c) a string containing the key in [PEM format](https://www.cryptosys.net/pki/manpki/pki_Keystorage.html).


## Jans server JSON objects of public key

Gets list of JSON Web Key (JWK) used by server. JWK is a JSON data structure that represents a set of public keys as a JSON object
``` commandLine
/opt/jans/jans-cli/config-cli.py --operation-id get-config-jwks > supportedAlgorithm.json
```
``` commandLine
{
  "keys": [
    {
      "name": "id_token RS256 Sign Key",
      "descr": "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-256",
      "kid": "958548a6-875f-4f9b-9b33-d04b375400a4_sig_rs256",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "crv": null,
      "exp": 1669923019275,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAPn+YNECOyB01jKX9GY/IX32tTm4IHg/n6qX3Cs0+rxGMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIxMTI5MTkzMDEwWhcNMjIxMjAxMTkzMDE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAz8zYwiW4hYYbdcBN4TrSpqRBCIH6VFm/a/B50arNPVArxXpIXz6J8Uooujeqm4y1b8oNnkfsozUJ5FHK4eeODXI7JNa9qbuzJrAS6tapvqlktZEGYyEG91O7+m+K06HvTqIB86xI5bwYoLW+4uqEN3mIKSOlKdoH+qeNN+DVXtfkQEtmsY33rxtOS0WJ5cFh9h/JaPj2SDWzw8P5RCbXsnPeR9joYXhZOYEE5Uy2XhJbY942Gk3mJwwfWuSmrWkSarc2vle1Q2wS0aAF05kha18/6gJJaw3Nx/8fiuZKnIxfCyyJnlHANtLROCQ33a4fq0Nl0V6hplr9mmjHzLVWEQIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAIEPNhDXzhO5mBeOtR/B5KCEpll7Bq58MHjNEf0oL6/L3Kpobj4XyowSOA8CKutl3auKNBiC8Mxhav6wK4245EC3GLxsNT2I8gXf5jonSYAKIr2XCRFfxSqp4eoNSP8I92jm9sLjAJf528VM6Ca6TYQAZ/EM9b2dA6wYgRM3fOGxoPGfUUokquHib5UVXcZE73tKLKQTvOMXIDxoXcUVfmeq4AuAgn0Zs4isC7Sg5IAixpdtTDUoHECYQZQsXed7d5jcMFinQCrS3PCEw+Gke8/D66FNpmHtD4HFHrLsqle3TawWFI8QMw5HdsIE8PCvG7qtvJq6uEGOJfWQyD7JzQg="
      ],
      "n": "z8zYwiW4hYYbdcBN4TrSpqRBCIH6VFm_a_B50arNPVArxXpIXz6J8Uooujeqm4y1b8oNnkfsozUJ5FHK4eeODXI7JNa9qbuzJrAS6tapvqlktZEGYyEG91O7-m-K06HvTqIB86xI5bwYoLW-4uqEN3mIKSOlKdoH-qeNN-DVXtfkQEtmsY33rxtOS0WJ5cFh9h_JaPj2SDWzw8P5RCbXsnPeR9joYXhZOYEE5Uy2XhJbY942Gk3mJwwfWuSmrWkSarc2vle1Q2wS0aAF05kha18_6gJJaw3Nx_8fiuZKnIxfCyyJnlHANtLROCQ33a4fq0Nl0V6hplr9mmjHzLVWEQ",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "name": "id_token RS384 Sign Key",
      "descr": "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-384",
      "kid": "8d2a13ff-b1c6-4143-a829-403787e11710_sig_rs384",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS384",
      "crv": null,
      "exp": 1669923019275,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAO3JRkP46i4nBlo0KPK+Wr5kToNYkGdLiXBO/LElKWQTMA0GCSqGSIb3DQEBDAUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIxMTI5MTkzMDEwWhcNMjIxMjAxMTkzMDE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA9Rc6XZ4ZTu3stpL1E8mu9fBaE7/fUoT4rC5kEieQI1Hu2Lyry+uYryZtRT4EUr6ZcikQEg1A3eumoceWeNVgcky8C2+nmPY5SmLxspTMJP+RdvT1ouFxSfHgtNCixgGeAQJFnzWBOgmZlUCL/YeX6bXjzdl4t17JlYkobmIfIHXzuRtfjiQT5PPiMkRL5Csm5lcGdDrViqMXcaSq1MDu5mgqGCnN9T+CtuTwLXdwl/Ln2QLKIjcGbda7ly+0CuoIm8MigFtG7+qKiLh7RY6x47cPbQyd9P+kn4IHZv2/y0r/QDovIwbzekv9pF7NiXsmjS5c6bSilM8dOu5IgOMddQIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQEMBQADggEBAKUmn49l8l/XIhKzE8i/KSeKwxSiKTPBRMZJHmsWMsBhnqOclm1VWsOmjfwm7t3LwgiMLy52hOiApHZ7StR/nKyi3GyBo9o646m/6luxLMxj+MrfKj5huIM2KpQRcD26S6gMS+G3M0CvkfjH9P95ELBEOsLJrrQM5dyYdZOLuOdTvkXAK5fs9G2klJm1PLo9IKm17W2va7dS2Hf0/7Pfjt1HguZhV9H9Ark2BC0RsNiB3SPo7TK7lJOnekVCcLI/cR7TtEf/7vqHV4JYlr3WRMg7O0/xWgtdub0ju4KJ6RGHUbVxDrkVC8t5EcgSXnuiYbze2S8/2NEaEz5HeAShOQ8="
      ],
      "n": "9Rc6XZ4ZTu3stpL1E8mu9fBaE7_fUoT4rC5kEieQI1Hu2Lyry-uYryZtRT4EUr6ZcikQEg1A3eumoceWeNVgcky8C2-nmPY5SmLxspTMJP-RdvT1ouFxSfHgtNCixgGeAQJFnzWBOgmZlUCL_YeX6bXjzdl4t17JlYkobmIfIHXzuRtfjiQT5PPiMkRL5Csm5lcGdDrViqMXcaSq1MDu5mgqGCnN9T-CtuTwLXdwl_Ln2QLKIjcGbda7ly-0CuoIm8MigFtG7-qKiLh7RY6x47cPbQyd9P-kn4IHZv2_y0r_QDovIwbzekv9pF7NiXsmjS5c6bSilM8dOu5IgOMddQ",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "name": "id_token RS512 Sign Key",
      "descr": "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-512",
      "kid": "99e5d37d-388d-49a1-9d85-4e85bbaf432a_sig_rs512",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS512",
      "crv": null,
      "exp": 1669923019275,
      "x5c": [
        "MIIDCTCCAfGgAwIBAgIgRcgzEHHu/tmWcYlXH4KmRdrDbVlTvoRCWTxf0I+T2XgwDQYJKoZIhvcNAQENBQAwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMjExMjkxOTMwMTFaFw0yMjEyMDExOTMwMTlaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC1xsjxzul26TVDUUg202lIIu9+kjZeYdZmpj1c4dZ0Cx9Au5Veez8wlnCLjf5LB4f0E4mSEKLqITMG9p4t2moHDlglHOgN+R2ZiefpVg/B3fcd1ehoqIZz7VHoB5jWIoeaWT1NZDJuz30wtYZHQgFyYuDjKEaE8c7Nn1Wvsx8QqKmaxjh1aZiC32Z3EopldJH82lOmcL1MP9mBVYHV26vqhR3VlpqB2dgiOJyzty7AWd3rxzF+KIsNaRKDyKTFbSf5W4Pkc8uvLzaS467fgkz0JyDMMHFJ9kJZtTeN3D9DNURRTmsgO1eyx31oKyv3J8voRwUi5IkyRXKcuzo9Ic2FAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQ0FAAOCAQEAocAqyBL2qYoHYb/sr40cj2FxXAl3i9AEGgKBvQ2lbSmOEY+1kdA5LCuMow7ulV7h22hv10Uh3lFU0X9P3AKq9hbgOvsqo3Z20F6sSWICrzWjbWBOCx2nlWTCG7u4R8WExUB8JfkHcuwaUhbqPC8SpXmEZIAsnTovLzSFgW6ht5jf/hiKmJ7Cj8Yw76j8l1ZtK9oMeXMW/WFleE+8xwS7szzTh0/oCm52QTSArgEoQVOl4ilgQXc2BQBswi+jx+6C54e3VQ5Cd7ewecpFoNMC5WmE5VCNo8GlNfPYrqX+mwSN2p+gqOYUu17MhzgOXI3aNgtSoSAGhcKJEVy6AsHLhg=="
      ],
      "n": "tcbI8c7pduk1Q1FINtNpSCLvfpI2XmHWZqY9XOHWdAsfQLuVXns_MJZwi43-SweH9BOJkhCi6iEzBvaeLdpqBw5YJRzoDfkdmYnn6VYPwd33HdXoaKiGc-1R6AeY1iKHmlk9TWQybs99MLWGR0IBcmLg4yhGhPHOzZ9Vr7MfEKipmsY4dWmYgt9mdxKKZXSR_NpTpnC9TD_ZgVWB1dur6oUd1ZaagdnYIjics7cuwFnd68cxfiiLDWkSg8ikxW0n-VuD5HPLry82kuOu34JM9CcgzDBxSfZCWbU3jdw_QzVEUU5rIDtXssd9aCsr9yfL6EcFIuSJMkVynLs6PSHNhQ",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "name": "id_token ES256 Sign Key",
      "descr": "Signature Key: ECDSA using P-256 (secp256r1) and SHA-256",
      "kid": "ef2a1589-932e-4243-8126-fe41c89b8df7_sig_es256",
      "kty": "EC",
      "use": "sig",
      "alg": "ES256",
      "crv": "P-256",
      "exp": 1669923019275,
      "x5c": [
        "MIIBfTCCASOgAwIBAgIgDOTzBpgaNYJpSUlTyKnFgDDCz0HIaLM9MaOjnPgd6/cwCgYIKoZIzj0EAwIwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMjExMjkxOTMwMTFaFw0yMjEyMDExOTMwMTlaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQ3je21+5/BZtl3SOCzA0euef8rK2fXKEyqEuOeo8x2ECke/43hxsemzHKGuovzZP2a5N0o9QCtB5yaeW5c97C6oycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwCgYIKoZIzj0EAwIDSAAwRQIhAKMA8bWKNr8LETQB9/1UtRsPcBu07/t0y7u1EUGBrGgAAiB5kWiPYotTJS5jLd6ZmXJd7XI9IF+HFIDzrhQeeLKfPA=="
      ],
      "n": null,
      "e": null,
      "x": "N43ttfufwWbZd0jgswNHrnn_Kytn1yhMqhLjnqPMdhA",
      "y": "KR7_jeHGx6bMcoa6i_Nk_Zrk3Sj1AK0HnJp5blz3sLo"
    },
    {
      "name": "id_token ES256K Sign Key",
      "descr": "Signature Key: ECDSA using secp256k1 and SHA-256",
      "kid": "3799a40f-81da-4966-996b-8dcce95256f6_sig_es256k",
      "kty": "EC",
      "use": "sig",
      "alg": "ES256K",
      "crv": "P-256K",
      "exp": 1669923019275,
      "x5c": [
        "MIIBezCCASGgAwIBAgIhANBVEnPekATxSIJgphdEWe541VCS4NwvfXkwEPpLOyrVMAoGCCqGSM49BAMCMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIxMTI5MTkzMDExWhcNMjIxMjAxMTkzMDE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEVNbuPs/kQW4g3354EqXbDLPeOJSWz+oyROPviWgrVgIvMsN8a1EdQ8vLktrJ2MdlLYD77DgS4zzfYT7WrjoC96MnMCUwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMAoGCCqGSM49BAMCA0gAMEUCIE9aDSGW/a7UdxLG6lEyqesYnzvsnWfYcStkyqGgFLDnAiEA34rwsziP/rENHL8dWVFXUNQDuBM4jxarcFP1PJ7a/Sw="
      ],
      "n": null,
      "e": null,
      "x": "VNbuPs_kQW4g3354EqXbDLPeOJSWz-oyROPviWgrVgI",
      "y": "LzLDfGtRHUPLy5LaydjHZS2A--w4EuM832E-1q46Avc"
    },
    {
      "name": "id_token ES384 Sign Key",
      "descr": "Signature Key: ECDSA using P-384 (secp384r1) and SHA-384",
      "kid": "dc084eec-5d26-47ff-a8d4-f5e619550dff_sig_es384",
      "kty": "EC",
      "use": "sig",
      "alg": "ES384",
      "crv": "P-384",
      "exp": 1669923019275,
      "x5c": [
        "MIIBuzCCAUGgAwIBAgIhAJGoFwWg50Ou3AhPStdszcQxNy4SWz3yrxtXssiQohshMAoGCCqGSM49BAMDMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIxMTI5MTkzMDEyWhcNMjIxMjAxMTkzMDE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEmAkQZXFh+1fl1IiysPDCKRDZFsaOba7e8KUTROsIww/+9/Lo5Tn0GyTu/W8Y7IGJuGHvbf8sSpD2Tpl46IJw5y/efVLEymX22UxX+XN7W8BvdpiMFR8FGhRCKqXdd27MoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwCgYIKoZIzj0EAwMDaAAwZQIwUzAqAEMT+9JV9/5KjGbjaMlyecN/tIl2PQHJKJ5bSW+l4D0uxc6DPADKfkJ7lvoBAjEA9OWMoAOcNIEuqu5jJIX61+m5GHK9DoT3VxFRNC4kOQxF4jbbfvHi5JilNgXNyyIf"
      ],
      "n": null,
      "e": null,
      "x": "mAkQZXFh-1fl1IiysPDCKRDZFsaOba7e8KUTROsIww_-9_Lo5Tn0GyTu_W8Y7IGJ",
      "y": "uGHvbf8sSpD2Tpl46IJw5y_efVLEymX22UxX-XN7W8BvdpiMFR8FGhRCKqXdd27M"
    },
    {
      "name": "id_token ES512 Sign Key",
      "descr": "Signature Key: ECDSA using P-521 (secp521r1) and SHA-512",
      "kid": "85603eb0-18db-4cb3-b4de-33b3750a504f_sig_es512",
      "kty": "EC",
      "use": "sig",
      "alg": "ES512",
      "crv": "P-521",
      "exp": 1669923019275,
      "x5c": [
        "MIICBjCCAWegAwIBAgIhAJqdYSipGYaCcBhk+ls5P5z6VW5+hcbXyeXR6bzjaYN2MAoGCCqGSM49BAMEMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIxMTI5MTkzMDEyWhcNMjIxMjAxMTkzMDE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQA12m4TQmwlOHcDKSsyGZal8Wh1FQ4Gyu6Wmqjgu40SFDi4lCN3Tu3/PNRP2RVhJXvkAJgFeuqVs5XaH6OFV1eRT0ApLnpdRV14BxiREZLMvb2lgQCQhnLUe1wMk7Qe3xu9/o9rNgzVo+jKXKKbf3hPifEUH2FHtUdPanOjK/xnNNyvPWjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDBAOBjAAwgYgCQgCHVf+tYXzWWMdcTEXvaq9298ehCUmhO6FwGE8hPeOivbPao4ZN/V5FDZZMRCzFuBFU8jDHGCIY3LpQ9cz+9OnEQAJCAKAXra4wKYDsg5hDzL2j1bdrGH8CMry+OmSrFwK7qmCq6+9AM5dasutW/GR6omRAebpfbHLwBgA7x7aVmQECiAY8"
      ],
      "n": null,
      "e": null,
      "x": "12m4TQmwlOHcDKSsyGZal8Wh1FQ4Gyu6Wmqjgu40SFDi4lCN3Tu3_PNRP2RVhJXvkAJgFeuqVs5XaH6OFV1eRT0",
      "y": "pLnpdRV14BxiREZLMvb2lgQCQhnLUe1wMk7Qe3xu9_o9rNgzVo-jKXKKbf3hPifEUH2FHtUdPanOjK_xnNNyvPU"
    },
    {
      "name": "id_token PS256 Sign Key",
      "descr": "Signature Key: RSASSA-PSS using SHA-256 and MGF1 with SHA-256",
      "kid": "d55ce557-1ff9-46d5-8221-fa4ceec27961_sig_ps256",
      "kty": "RSA",
      "use": "sig",
      "alg": "PS256",
      "crv": null,
      "exp": 1669923019275,
      "x5c": [
        "MIIDcTCCAiWgAwIBAgIgIFv73G2PzlXSudw/JMVU4aF9ZP9OHA6cKwQqktnNFR8wQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgEFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgEFAKIDAgEgMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIxMTI5MTkzMDEyWhcNMjIxMjAxMTkzMDE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq7G5D42+vPeelJLhp1DT40cVQVEiOddW/3bTKJswWr4Y8+0AkyUX0J68pKpUKfrVu0VW1A/12XB48FwhzGj23977viCnvZ+XjpWb/ZC87M851VKIVQAnILM1DswnfW7meQcoo7it/YQxNQwjCXCvo/6dxB7br9PNLeKse6Vqd5MrJUX84A9C+u0JQZU9/bwHDs1FFZI54MbfLzcm+WUfafMAlRyt1w7NefQKDIQC5DzkEor18mY2BW2hHDHjwTs0dtOf4SUOSdDbjXowoGsl84iqPjUvLrgAeVxHVAJSjX8UwsXfooFR4EgDFfNcH9ZFt3BAw3K9jEUhSSgYUSVmXwIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgEFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgEFAKIDAgEgA4IBAQCJiMzApVSUT98JhaH7pGravFifI4o35Tff0lEmjg7UEmZKoQ/wK/x3cp2QGvdC63l92ATkUqqTS2LGglLekyjpkLjBwfv4e6m6Ac6GZnPOGe4KhMQDoouea4S+tws8wBTY+7JoV4ssAu7wwnWn8pHPds5KxlQKJuwKANSih2ElhTe9aHH+mNdRh9oNkopSgYXKyKk8TD4Pdleaf8uroWRfnq4Aleg64eaw7ek5cJUzo3bUfAYUuX+AyufBVkBwFwKkio9giIWGoGw4Xrikre8C7uA4tNf/7rrodyjpBvhJ1qxXzTqxcmo9bDEZresuy3ERdJvAZ4CTEp3h2wHW1I6A"
      ],
      "n": "q7G5D42-vPeelJLhp1DT40cVQVEiOddW_3bTKJswWr4Y8-0AkyUX0J68pKpUKfrVu0VW1A_12XB48FwhzGj23977viCnvZ-XjpWb_ZC87M851VKIVQAnILM1DswnfW7meQcoo7it_YQxNQwjCXCvo_6dxB7br9PNLeKse6Vqd5MrJUX84A9C-u0JQZU9_bwHDs1FFZI54MbfLzcm-WUfafMAlRyt1w7NefQKDIQC5DzkEor18mY2BW2hHDHjwTs0dtOf4SUOSdDbjXowoGsl84iqPjUvLrgAeVxHVAJSjX8UwsXfooFR4EgDFfNcH9ZFt3BAw3K9jEUhSSgYUSVmXw",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "name": "id_token PS384 Sign Key",
      "descr": "Signature Key: RSASSA-PSS using SHA-384 and MGF1 with SHA-384",
      "kid": "8e2a896b-5403-4294-a195-7d386fad0b28_sig_ps384",
      "kty": "RSA",
      "use": "sig",
      "alg": "PS384",
      "crv": null,
      "exp": 1669923019275,
      "x5c": [
        "MIIDcTCCAiWgAwIBAgIgbPw5oYv1WoSZ6f8bwWepF0sMTZpkpokJvcnFP5lULsgwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgIFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgIFAKIDAgEwMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIxMTI5MTkzMDEzWhcNMjIxMjAxMTkzMDE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvWtgp8cXGFnGf8B61m3oVCWi5rUfPwO64NzsyuOgVxnnbylSangfp0AvDyNms7ePUXDzrfWe3UvjLqyl/JBXj8SoQTbokeHChJTTbE507Gh4yCldxdkbj6R3VDeiSCNrh46iSKTsc4ZJqDjED0bhPENURQIv09ezgv5H+iUThZU50/ge3n+5qiKNy/hoRmb1Ye0iI9ugODouPLSJL9C2Vfr4q+SL3VgLfodgzHM61uFJJv4sBqFRkJj6VFYfMjKuvrnNbuVAahmIMEh9GZhXgPZ728h4Gs5hAxEjPFxu09Z8EFj1gkN30i0cRJrcGZfgYN8PLQb3MxRTU2vxE13SIQIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgIFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgIFAKIDAgEwA4IBAQAJ4kjsl+cSOiKOD+nVhuGfnh+jtdsJSFZGMRA2P/DUuBjL0giUU3mIBA/Lr42t7CaBzAWxrY8dTcVF9FVkT+hVrfZXynyZm21PzxSf9xEmHVT7NJrCz1nRkdPhenMHp5ec4878idBxOLniT3+WRAqKQJaovM5o1owY2fdJRHwVrUrqxe6ORXok+uYAcLGJ7Zr//vaAtneCi40pHi636gtvau0KB7lDUTcKlVsJpEZ88HX8ZvW9FntowEKvbeKngvkWP7bg4sUMtvTHX28qJW3cExRA21DwxoJENVDLxAO2OIvc7Z2CJuHvKalYlFk57R5DQFSnzUKj7U20bWC3bm1i"
      ],
      "n": "vWtgp8cXGFnGf8B61m3oVCWi5rUfPwO64NzsyuOgVxnnbylSangfp0AvDyNms7ePUXDzrfWe3UvjLqyl_JBXj8SoQTbokeHChJTTbE507Gh4yCldxdkbj6R3VDeiSCNrh46iSKTsc4ZJqDjED0bhPENURQIv09ezgv5H-iUThZU50_ge3n-5qiKNy_hoRmb1Ye0iI9ugODouPLSJL9C2Vfr4q-SL3VgLfodgzHM61uFJJv4sBqFRkJj6VFYfMjKuvrnNbuVAahmIMEh9GZhXgPZ728h4Gs5hAxEjPFxu09Z8EFj1gkN30i0cRJrcGZfgYN8PLQb3MxRTU2vxE13SIQ",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "name": "id_token PS512 Sign Key",
      "descr": "Signature Key: RSASSA-PSS using SHA-512 and MGF1 with SHA-512",
      "kid": "31ddf4ae-eabc-4577-ac6e-45cca908fb27_sig_ps512",
      "kty": "RSA",
      "use": "sig",
      "alg": "PS512",
      "crv": null,
      "exp": 1669923019275,
      "x5c": [
        "MIIDcTCCAiWgAwIBAgIgXoUcTvK0u9HS2gvxg9KBQJJzOLuZP6n/OoR+wQ5vHsQwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgMFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgMFAKIDAgFAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIxMTI5MTkzMDEzWhcNMjIxMjAxMTkzMDE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsTpbxGZC6Xa1Lnre4UYC8MDe12rTQvS0xLWJIEY2x0boDWBQFxgG4sR78pkxYI+uVQZ8l9y+h26AtC5nN+aWfNDDVnoBrG/zqE/G01l3xjA6+Dy9aNjKXAz+tyFzuLTmvXLAfq/KYxzcpEy/5Ugtd09npD0j7ok3je9tSZbjs0XE01+ZSfi8iFpoTC4SEHeXHn+hSOz+pQy6GFLfvd0Im5K7d8T+/MqoqUFh3l58CgLtTflEez+HYwyo9B/K8Oyka6es0JF0ygdX675UwWFggvPdIZv2h5vqcv0t38orarQauI0RJpFGgrM5pKdu9j4JbfpjbOmnMGNl3eyC2cs7KwIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgMFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgMFAKIDAgFAA4IBAQBOq7lxSgfvfTOEu1fG/n4rmNiLmZaa7ssXwXD+L3C963/g3iIMZYLWV33Gvq06QcWmBn2SHekCm9fszG8KgNxeRoPtHob1o1FTN0iBBTIRtjm8jMCJ7vfL4IS61I0uqKTkUmM7Rx93PDEH304ueN1Lff+N/ZmtY+4KwXM3wsk2n9ANcVqBWBY8xRy3nbXllS+QbAyqu39zqqPNW8XG1secgNK0U3GkrdZcPFBUl/u7AhcXP8XBIeVVdBjLS4ake/IyZX9qPz8R1Je64opPO7cGzfyLfodTsWEAA7jhq9pDc4JWCuwqCECW4t0h1wM+JmjO3ygOMiTdXqsi5OQDfg3L"
      ],
      "n": "sTpbxGZC6Xa1Lnre4UYC8MDe12rTQvS0xLWJIEY2x0boDWBQFxgG4sR78pkxYI-uVQZ8l9y-h26AtC5nN-aWfNDDVnoBrG_zqE_G01l3xjA6-Dy9aNjKXAz-tyFzuLTmvXLAfq_KYxzcpEy_5Ugtd09npD0j7ok3je9tSZbjs0XE01-ZSfi8iFpoTC4SEHeXHn-hSOz-pQy6GFLfvd0Im5K7d8T-_MqoqUFh3l58CgLtTflEez-HYwyo9B_K8Oyka6es0JF0ygdX675UwWFggvPdIZv2h5vqcv0t38orarQauI0RJpFGgrM5pKdu9j4JbfpjbOmnMGNl3eyC2cs7Kw",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "name": "id_token RSA1_5 Encryption Key",
      "descr": "Encryption Key: RSAES-PKCS1-v1_5",
      "kid": "19a6a2df-3961-45fe-aed7-4663333ee0aa_enc_rsa1_5",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA1_5",
      "crv": null,
      "exp": 1669923019275,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAOiFp/L8s6U7lZInMdih8e1sJBMV+NVjt7UWc68vD2NjMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjIxMTI5MTkzMDEzWhcNMjIxMjAxMTkzMDE5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6Pjykuirk8zDsSFtEQ39Mct99IqLFVbOGVzNJJL1FJdmIBPApjTZQzxm2gZ9tn1QhUQ0ZrrLE6pfZe7rt1MEWasyjpaj5XQAaISp9bVQA11xu+CBRGCwleVGrOIO6VWDszoZQ1wXaHp9/478ZPQ+okAHvUfIyKLNDiLZepE5Jb9JQTicyZd4GTDrx8NZzjacX0xmqA5rs8fdTB520/PAH5mBZqhX/IU/SMQati1hAYLg9pNJ0ruHBJzXFJyg2OZC9CmwCDnoAFy+CwsCyv/m5bo+NxD+11wknMpZi7nPC6sQ6gEiEBdOyEVvishrNaEG31IYqsffu7iGHdAf0OuSPQIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAF8xpKgKoCj6/QBDCqVVY/C+RsvLvtbjJpyf6HrSvi5nI/5l5QKNNHrusyQ+LOHOPN3kVnPh9L98GUJqeqBgrtfElu4Fdkz4ddMwIZosY41DUfsEBw26q6f+Q0hQgVF3dFiTAmLX5AAg9Hbz9iuH8RtxS0kMGFq+/1qT824iXpitnLH0rfsasxFAajkJjCZKTU8Z3cbYQTl+6wV2umun2FAbVyUEu3EjB+NggeVyexCtMjED5Ugg1PeZS3j2a9YVn+tzibQOsJuULBn38iW54ztfc5VMLFc/pVjpeAY60kBsw03My2k7hSRIbZne7G5xj+1+Jn3oaOuLKaJZkfeP+Gc="
      ],
      "n": "6Pjykuirk8zDsSFtEQ39Mct99IqLFVbOGVzNJJL1FJdmIBPApjTZQzxm2gZ9tn1QhUQ0ZrrLE6pfZe7rt1MEWasyjpaj5XQAaISp9bVQA11xu-CBRGCwleVGrOIO6VWDszoZQ1wXaHp9_478ZPQ-okAHvUfIyKLNDiLZepE5Jb9JQTicyZd4GTDrx8NZzjacX0xmqA5rs8fdTB520_PAH5mBZqhX_IU_SMQati1hAYLg9pNJ0ruHBJzXFJyg2OZC9CmwCDnoAFy-CwsCyv_m5bo-NxD-11wknMpZi7nPC6sQ6gEiEBdOyEVvishrNaEG31IYqsffu7iGHdAf0OuSPQ",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "name": "id_token RSA-OAEP Encryption Key",
      "descr": "Encryption Key: RSAES OAEP using default parameters",
      "kid": "fc5d3ddc-b352-4cc7-ba1f-d98eef61b6cd_enc_rsa-oaep",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA-OAEP",
      "crv": null,
      "exp": 1669923019275,
      "x5c": [
        "MIIDCTCCAfGgAwIBAgIgGaq0go/8TGs9ozO2hht9DScEJqdYX80ol5ohqfs/0BAwDQYJKoZIhvcNAQELBQAwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMjExMjkxOTMwMTRaFw0yMjEyMDExOTMwMTlaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDH1Jf11LZ/LwBYfokLgbzJHtS9GB5ezmv65+wWiNCYvjuu+FgU+B0ABG7csWzBEvh2wE8a+7Xh2sLiLcOWkVMdN5WnpWwbs+y7yeRBVx7LrJVN0U+morYo9n8DfCtuJhvU8d9KDggGWhUJ0pwhodA24WeLVImjCElnmeTJsM3pvHGlfkZf9pjTb++rTCpMA7a8mcdpvFKb9FpTDqUxl7/clpSEYOvCa0rFepovr5JFHLK6mmGL+lpaFXVTyVeGUS2ON9+Q1OHooao3v1TgLf6ydTsloayzGxdwjQOMKDAAYfyLEYtNA+FgMii/WK9X6/9btbMiMGx6C1jGdYdTBse3AgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQsFAAOCAQEAf/wj7JNuJGy3OoXI6VSsblcfTRNqm+1jLvKv3/eeBrGRLqBsrnttbPj7OOaRxrhJmeHOXC0SI8+h0Kcq+8w8J+Tr8B2gN9vad6ajwTdveNaaHTRtRAgPiIk6+VF0tnVchRav589gj0hGf2MeL73045VwDWhEs95NztU9rkH1em++AGi/smn5cVFD0DDvfBRsqBLTTjd7rSuOIfivt28vMEuUxwdDUWGcB6IUvC2ZLHUzogkVVDtwhCu06kX2J8BakdJ1COfgbpnWiqNHJDO5VJceFvEZ48EdR5NgHkRJXKnNZUUN/0BaYkJGziNXvfXaAQu6ZOGtQ6r/NByNQJJioA=="
      ],
      "n": "x9SX9dS2fy8AWH6JC4G8yR7UvRgeXs5r-ufsFojQmL47rvhYFPgdAARu3LFswRL4dsBPGvu14drC4i3DlpFTHTeVp6VsG7Psu8nkQVcey6yVTdFPpqK2KPZ_A3wrbiYb1PHfSg4IBloVCdKcIaHQNuFni1SJowhJZ5nkybDN6bxxpX5GX_aY02_vq0wqTAO2vJnHabxSm_RaUw6lMZe_3JaUhGDrwmtKxXqaL6-SRRyyupphi_paWhV1U8lXhlEtjjffkNTh6KGqN79U4C3-snU7JaGssxsXcI0DjCgwAGH8ixGLTQPhYDIov1ivV-v_W7WzIjBsegtYxnWHUwbHtw",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "name": "id_token ECDH-ES Encryption Key",
      "descr": "Encryption Key: Elliptic Curve Diffie-Hellman Ephemeral Static key agreement using Concat KDF",
      "kid": "61231ba4-3e04-4e95-9bbf-5ed44938e597_enc_ecdh-es",
      "kty": "EC",
      "use": "enc",
      "alg": "ECDH-ES",
      "crv": "P-256",
      "exp": 1669923019275,
      "x5c": [
        "MIIBfTCCASOgAwIBAgIgVzrXVFidlSku8vaENooKmJAC4dv0eJlQl8k5lkCrjiMwCgYIKoZIzj0EAwIwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMjExMjkxOTMwMTRaFw0yMjEyMDExOTMwMTlaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAASU5S4skA87bz9xEvOpNz+itLvjEk5PcwPJ9h8U9FPfh/fXRo1dSzawTQnYyNG+0fUI9fsIvPcjCoCaZuY111PZoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwCgYIKoZIzj0EAwIDSAAwRQIhAL5/Z0Kj07PLeEOHiyetruNKx+/n7QbnnFxppeYwqrk3AiAXqLB8PYK8kbhRcuw9JXTSbcLRrGvxBX1Iie8cKgrbtg=="
      ],
      "n": null,
      "e": null,
      "x": "lOUuLJAPO28_cRLzqTc_orS74xJOT3MDyfYfFPRT34c",
      "y": "99dGjV1LNrBNCdjI0b7R9Qj1-wi89yMKgJpm5jXXU9k"
    }
  ]
}
```


