---
tags:
  - administration
  - configuration
  - cli
  - commandline
---

# JSON Web Key (JWK)

> Prerequisite: Know how to use the Janssen CLI in [command-line mode](cli-index.md)

This operation is used to get the JSON Web Key Set (JWKS) from OP host. The JWKS is a set of keys containing the public keys that should be used to verify any JSON Web Token (JWT) issued by the authorization server.

There are few operations we can do using `jans-cli` commands. To get list of operations id run below command:

```
/opt/jans/jans-cli/config-cli.py --info ConfigurationJwkJsonWebKeyJwk
```

It returns operations id with details information.

```
Operation ID: get-config-jwks
  Description: Gets list of JSON Web Key (JWK) used by server. JWK is a JSON data structure that represents a set of public keys as a JSON object [RFC4627].
Operation ID: put-config-jwks
  Description: Puts/replaces JSON Web Keys (JWKS).
  Schema: /components/schemas/WebKeysConfiguration
Operation ID: patch-config-jwks
  Description: Patch JSON Web Keys (JWKS).
  Schema: Array of /components/schemas/PatchRequest
Operation ID: post-config-jwks-key
  Description: Adds a new key to JSON Web Keys (JWKS).
  Schema: /components/schemas/JsonWebKey
Operation ID: put-config-jwk-kid
  Description: Get a JSON Web Key based on kid
  url-suffix: kid
Operation ID: patch-config-jwk-kid
  Description: Patch a specific JSON Web Key based on kid
  url-suffix: kid
  Schema: Array of /components/schemas/PatchRequest
Operation ID: delete-config-jwk-kid
  Description: Delete a JSON Web Key based on kid
  url-suffix: kid

To get sample schema type /opt/jans/jans-cli/config-cli.py --schema <schma>, for example /opt/jans/jans-cli/config-cli.py --schema /components/schemas/PatchRequest


```
Let's explore each of these operations.

## Get Configurations list of JWKs

We can get list of all configurations of the jwk configuration within a single command like this:

```
/opt/jans/jans-cli/config-cli.py --operation-id get-config-jwks
```

It will return all the jwk configuration information as below:


```
Getting access token for scope https://jans.io/oauth/config/jwks.readonly
{
  "keys": [
    {
      "kid": "8627c7e1-0702-4103-9c28-28ff6a818da9_sig_rs256",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "crv": "",
      "exp": 1622245655163,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAIi82XlGMTJRsn5djImvoD2wSLY1hkhPPUv2qaGjvC3IMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwNTI2MjM0NzI1WhcNMjEwNTI4MjM0NzM1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqpU+2D0eQTszq3iq7qIHm3ryYGIDd4+3WUwMyCJoV0dYDDKZwFxaZD9auW/9wsqQZnwJcpYuDt+sAYVfei7+1nLKZhuo1eRLQxpHzDTmAoQcuWAGDg5f0sL24qaO9n1zSNAcEt8pyRJVZ1VcCEePryNSoustDZX36Eh/1pAOVjHzlVXSWCMtlS5uG6VcWoCzbrQU/z8ittfWqSpk/hM6z9KO95JofuFm5JG3U6qMFiZdG5qp3dY2zQ8clpqtV4yqaMTD6mv3IiH3TGxo1PGS2UHCRWh6TLVp15ElLlGiaaC6LlYxIdxMgV2AV80718ROKQT8OieVY5Q2T+198lPUWwIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAGGUO67E3UjQaaxGI2pIrau0A5qWF0SAMay2FI+o1xstud8+imkANjoqIQgWVpaZCR6I7q9rDHbaSHQy6uPHRBGVtV9izC+vr6+ohVjyFO/1K97FHQJxaR62X+qswFHot6RGFjQbMDEWQs/BMOfuojp7rvkRYjazdxqX2Obbp0cmHuaJV0iiZ71+k2VpoJrpdP/RI+3OdCWvV+fydNDrBXAi7JYNaqhx9wVoqVEeMAOCXbcjEe6YiYcU2WHPCD7DaeOzPgJimzc5ujyu30RFMVl5RYOMYgETt4g+fVKDuRcstQJaR5zowBUGaiXCjOfhfkWa2fFd9v4hg5Y7YCmOMtI="
      ],
      "n": "qpU-2D0eQTszq3iq7qIHm3ryYGIDd4-3WUwMyCJoV0dYDDKZwFxaZD9auW_9wsqQZnwJcpYuDt-sAYVfei7-1nLKZhuo1eRLQxpHzDTmAoQcuWAGDg5f0sL24qaO9n1zSNAcEt8pyRJVZ1VcCEePryNSoustDZX36Eh_1pAOVjHzlVXSWCMtlS5uG6VcWoCzbrQU_z8ittfWqSpk_hM6z9KO95JofuFm5JG3U6qMFiZdG5qp3dY2zQ8clpqtV4yqaMTD6mv3IiH3TGxo1PGS2UHCRWh6TLVp15ElLlGiaaC6LlYxIdxMgV2AV80718ROKQT8OieVY5Q2T-198lPUWw",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "77e3dec8-8a3e-479b-bcfc-aa508e5d9825_sig_rs384",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS384",
      "crv": "",
      "exp": 1622245655163,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAPMrnxSwxnvlx7up5juXI8j52fQQwbmdLtcooIjv7pC3MA0GCSqGSIb3DQEBDAUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwNTI2MjM0NzI2WhcNMjEwNTI4MjM0NzM1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq415zmHRArQ9zD5YN44nG52yJI7FakkBUN/aYWNJSCk5Pq/0qc7tm3M6Jhqrz8XuOMfatqzOxG65i4s/kOwGmET+Mq+lJ416A9toUfGdZRHmint/spGOf5nUeWjXRxMOooVtafq0Ao2/WZnsBB4jfAI9F211pJv2MNcxJ1ZwLpNSoaJSrEgxQh3laIVBozEcikozSOVUj8CMfF+WJRo049nv+IazTwTg1ZS96WcAztIemqgjAoE80i9Y4DYNp1R97A9MtFcsLlEPjYyiGmt5SomRa1oo6gymMs3AHYBdYPY/U4iXMsNDBsG+8cbTz44fFTkWHeWSuTnP9z8aj32HFQIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQEMBQADggEBAKdot2Upc7jH4XfgqXFDCkD0ILB2Zu/HE1PG0UGetJiR0hHwQ64CebpBqa8ExCHxiCamGjck6mZOVQHphJ0bgG1svMBG4M/x//RZcFld9L+mlPVEeTDJY5K0cb1csbuziACF1MrZB4pvVIVZgywdGFm+EqJ0aHDr5ByP5foBwqR3e7nGbuu6xWJx1hbXOrnjOFUqlwtnq0Qy4ZNNKgAbVW7n1We44ceWhWb4UenM5Ee2+xy1y9hkETdyLfgSjRh75pAv/+BhLM98x8ozns5bZXMTMZXt49Pbr29x2axBObGwKpv7Suqqndaw09iwSDtzyNUMVnCU4C1ikrozC8KHGvE="
      ],
      "n": "q415zmHRArQ9zD5YN44nG52yJI7FakkBUN_aYWNJSCk5Pq_0qc7tm3M6Jhqrz8XuOMfatqzOxG65i4s_kOwGmET-Mq-lJ416A9toUfGdZRHmint_spGOf5nUeWjXRxMOooVtafq0Ao2_WZnsBB4jfAI9F211pJv2MNcxJ1ZwLpNSoaJSrEgxQh3laIVBozEcikozSOVUj8CMfF-WJRo049nv-IazTwTg1ZS96WcAztIemqgjAoE80i9Y4DYNp1R97A9MtFcsLlEPjYyiGmt5SomRa1oo6gymMs3AHYBdYPY_U4iXMsNDBsG-8cbTz44fFTkWHeWSuTnP9z8aj32HFQ",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "cb0b2d29-0d2f-411f-9d47-5885cffad6e2_sig_rs512",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS512",
      "crv": "",
      "exp": 1622245655163,
      "x5c": [
        "MIIDCTCCAfGgAwIBAgIgW10M3Wl7/TEDNK9DarGKNAFT+4E3HQfyJcVmh0e3s8gwDQYJKoZIhvcNAQENBQAwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTA1MjYyMzQ3MjdaFw0yMTA1MjgyMzQ3MzVaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDrEdrcsQGHM1T+7ZcpU46DbbxGTA2qM4T6FSFb2UEPyeD/zKnCcdOgM/598tSzsKZJrIR3ZQn1igzcJkm7gKqkn7Qbg3Thk+MZtX28S68YJSDonjdce1NkIn3fx3TsImBh2Le9iOJfkLRnwCFm1EnplahNZYmV6PyHZ9IChpC6XCoBa6+U6O/owL5iAS34XrRHKxJaECTJObI2Gydg1doEP8jSnU3EZChSwmRDCSE3IH7oiTIIISdhMhD5ZBWGZlPl73Mj8PzYUdWOU1ZqCDngCfBm+SGvXBJcSheJr4la9mnl0AtCnE3zwEGWLKBxhouDQvXYa6ELq8r3VbDBb+4VAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQ0FAAOCAQEAx8ykyU8CAE6IS+li9pRx/rStBNgvAyY8TiQDlpkBSPwA64KuKWZmSjmMqjZLtqCEIY5X9+GPAeEhFtBQYJVcxYAAWV7JnMrq6RuWmom1gHN8igOYEyBSwPWb9TS1kWrPYg/hD4p9kOlNAbbdEuid/NPWOZJ4uGA1+ViEY9D29dsYvPpPLBxf7XFVjXAkljefi3yREQzEpJ7/cerJW9WyYCeS0/Ahr8Yhn2txBwrniMwyMJPLb+KvLWfqPol0zsyHBwWIa2nygTHJ8mYftHGO26dxIef2kQAu5uUX7po4MXFUBkuUnrkqB2CfvjMq5cU5AATgtPDdmJjKMQ9ZjgGhNQ=="
      ],
      "n": "6xHa3LEBhzNU_u2XKVOOg228RkwNqjOE-hUhW9lBD8ng_8ypwnHToDP-ffLUs7CmSayEd2UJ9YoM3CZJu4CqpJ-0G4N04ZPjGbV9vEuvGCUg6J43XHtTZCJ938d07CJgYdi3vYjiX5C0Z8AhZtRJ6ZWoTWWJlej8h2fSAoaQulwqAWuvlOjv6MC-YgEt-F60RysSWhAkyTmyNhsnYNXaBD_I0p1NxGQoUsJkQwkhNyB-6IkyCCEnYTIQ-WQVhmZT5e9zI_D82FHVjlNWagg54AnwZvkhr1wSXEoXia-JWvZp5dALQpxN88BBliygcYaLg0L12GuhC6vK91WwwW_uFQ",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    ...
    ...
    ...
  ]
```

## Adds new JSON Web key (JWK)

In case we need to add new key, we can use this operation id. To add a new key, we need to follow the schema definition. If we look at the description, we can see a schema definition available.

```
Operation ID: post-config-jwks-key
  Description: Adds a new key to JSON Web Keys (JWKS).
  Schema: /components/schemas/JsonWebKey
```

So, let's get the schema file and update it with keys data:

```
/opt/jans/jans-cli/config-cli.py --schema /components/schemas/JsonWebKey > /tmp/jwk.json
```

```
{
  "kid": "string",
  "kty": "string",
  "use": "string",
  "alg": "string",
  "crv": null,
  "exp": "integer",
  "x5c": [],
  "n": null,
  "e": null,
  "x": null,
  "y": null
}
```

Let's update the json file; In our case, I have added sample data for testing purpose only.

```
"kid": "dd550214-7969-41b9-b919-2a0cfa36047b_enc_rsa1_5",
"kty": "RSA",
"use": "enc",
"alg": "RSA-OAEP",
"crv": "",
"exp": 1622245655163,
"x5c": [
  "MIIDCjCCAfKgAwIBAgIhANYLiviUTmgOsf9Bf+6N/pr6H4Mis5ku1VXNj7VW/CMbMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwNTI2MjM0NzI5WhcNMjEwNTI4MjM0NzM1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArlD19ib3J2bKYr2iap1d/gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx/CfHSgdEmACCXMiG7sQt80DPM67dlbtv/pEnWrHk4fwwst83OF+HXTSi4Sd9QWhDtBvaUu8Rp8ir+x2D0RK8YNGs0prA+qGR8O/h6Y+ascz4VNbbDlbJ+w7DJYeWU1HVp/5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y/9i8kf2pmznpu5QEDimj1yxEB+G5WEYuHD/+qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk/KKB7/KT0rEOn7T2rXW9QIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAKrtlIPhvDBxBfcqS9Xy39QqE1WOPiNQooa/FVVOsCROdRZrHhFcP27HpxO9e6genQSJ6nBRaJ4ykEf0oM535Ker5jZcDWzCwPIyt+5Kc6qeacZI5FxEHRldYkSd4lF1OTzQNvGLOPKnNWnYnXwj48ZxO50lJUsRFspVbP79E6llVNOPexrZ2GOzWghyY1E74f4uGr6fzcXQk2aFaIfLusoJlvbROPTnDu68Jt+IW4WZcO4F0tl0JIcuaqSmLS6McJW0Mpmu4wqEPV6E45zRAuX0kJUkKDMzM/lYW1MZ8QaSTt/pCmlknX1+KTgb6Sf9zZJEya8AyKML/NCpc4sfn8g="
],
"n": "rlD19ib3J2bKYr2iap1d_gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx_CfHSgdEmACCXMiG7sQt80DPM67dlbtv_pEnWrHk4fwwst83OF-HXTSi4Sd9QWhDtBvaUu8Rp8ir-x2D0RK8YNGs0prA-qGR8O_h6Y-ascz4VNbbDlbJ-w7DJYeWU1HVp_5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y_9i8kf2pmznpu5QEDimj1yxEB-G5WEYuHD_-qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk_KKB7_KT0rEOn7T2rXW9Q",
"e": "AQAB",
"x": null,
"y": null
```

Now let's post this keys into the list using below command:

```
/opt/jans/jans-cli/config-cli.py --operation-id post-config-jwks-key --data /tmp/jwk.json
```


## Update / Replace JSON Web Key (JWK)

To `update / replace` any JWK configuration, let get the schema first.

```
Operation ID: put-config-jwks
  Description: Puts/replaces JSON Web Keys (JWKS).
  Schema: /components/schemas/WebKeysConfiguration
```

To get the schema file:

```
 /opt/jans/jans-cli/config-cli.py --schema /components/schemas/WebKeysConfiguration > /tmp/path-jwk.json
```

```
root@testjans:~# cat /tmp/path-jwk.json

{
  "keys": {
    "kid": null,
    "kty": null,
    "use": null,
    "alg": null,
    "crv": null,
    "exp": null,
    "x5c": [],
    "n": null,
    "e": null,
    "x": null,
    "y": null
  }
}
```

It's a json file containing `key-value` pair. Each of these properties in the key is defined by the JWK specification [RFC 7517](https://datatracker.ietf.org/doc/html/rfc7517), and for algorithm-specific properties, in [RFC 7518](https://datatracker.ietf.org/doc/html/rfc7518).


### Properties


|name|Description|
|:---:|:---|
|`kid`| It's a unique identifier for the key configuration.|
|`kty`| It's used to define the type of the specific cryptographic algorithms |
|`use`| This parameter identifies the intend use of the public key. `sig` for signature and `enc` for encryption|
|`alg`| The specific algorithm used with the key|
|`crv`|  |
|`exp`| The exponent for the RSA public key. |
|`x5c`| The `x5c` parameter contains a chain of one or more PKIX certificates [RFC5280](https://datatracker.ietf.org/doc/html/rfc5280) |
|`n`| The modulus for the [RSA public key](https://datatracker.ietf.org/doc/html/rfc7518#section-6.3.1.1). |
|`e`| The "e" (exponent) parameter contains the exponent value for the [RSA public key](https://datatracker.ietf.org/doc/html/rfc7518#section-6.3.1.2).  It is represented as a Base64urlUInt-encoded value. |
|`x`| The "x" (x coordinate) parameter contains the x coordinate for the [Elliptic Curve point](https://datatracker.ietf.org/doc/html/rfc7518#section-6.2.1.2). |
|`y`|  The "y" (y coordinate) parameter contains the y coordinate for the [Elliptic Curve point](https://datatracker.ietf.org/doc/html/rfc7518#section-6.2.1.3). |

If you want to explore more, please go through the reference link.

Let's update the json file to create a new key configuration.

```
{
   "keys":
   [{
      "kid": "dd550214-7969-41b9-b919-2a0cfa36047b_enc_rsa1_5",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA-OAEP",
      "crv": "",
      "exp": 1622245655163,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhANYLiviUTmgOsf9Bf+6N/pr6H4Mis5ku1VXNj7VW/CMbMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwNTI2MjM0NzI5WhcNMjEwNTI4MjM0NzM1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArlD19ib3J2bKYr2iap1d/gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx/CfHSgdEmACCXMiG7sQt80DPM67dlbtv/pEnWrHk4fwwst83OF+HXTSi4Sd9QWhDtBvaUu8Rp8ir+x2D0RK8YNGs0prA+qGR8O/h6Y+ascz4VNbbDlbJ+w7DJYeWU1HVp/5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y/9i8kf2pmznpu5QEDimj1yxEB+G5WEYuHD/+qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk/KKB7/KT0rEOn7T2rXW9QIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAKrtlIPhvDBxBfcqS9Xy39QqE1WOPiNQooa/FVVOsCROdRZrHhFcP27HpxO9e6genQSJ6nBRaJ4ykEf0oM535Ker5jZcDWzCwPIyt+5Kc6qeacZI5FxEHRldYkSd4lF1OTzQNvGLOPKnNWnYnXwj48ZxO50lJUsRFspVbP79E6llVNOPexrZ2GOzWghyY1E74f4uGr6fzcXQk2aFaIfLusoJlvbROPTnDu68Jt+IW4WZcO4F0tl0JIcuaqSmLS6McJW0Mpmu4wqEPV6E45zRAuX0kJUkKDMzM/lYW1MZ8QaSTt/pCmlknX1+KTgb6Sf9zZJEya8AyKML/NCpc4sfn8g="
      ],
      "n": "rlD19ib3J2bKYr2iap1d_gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx_CfHSgdEmACCXMiG7sQt80DPM67dlbtv_pEnWrHk4fwwst83OF-HXTSi4Sd9QWhDtBvaUu8Rp8ir-x2D0RK8YNGs0prA-qGR8O_h6Y-ascz4VNbbDlbJ-w7DJYeWU1HVp_5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y_9i8kf2pmznpu5QEDimj1yxEB-G5WEYuHD_-qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk_KKB7_KT0rEOn7T2rXW9Q",
      "e": "AQAB",
      "x": null,
      "y": null
  }]
}
```

Please remember if `kid` already matched then this will be replaced otherwise a new key configuration will be created in the Janssen server.

Now let's put the updated data into the Janssen server.

```
/opt/jans/jans-cli/config-cli.py --operation-id put-config-jwks --data /tmp/path-jwk.json
```

```
Getting access token for scope https://jans.io/oauth/config/jwks.write
Server Response:
{
  "keys": [
    {
      "kid": "dd550214-7969-41b9-b919-2a0cfa36047b_enc_rsa1_5",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA-OAEP",
      "crv": "",
      "exp": 1622245655163,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhANYLiviUTmgOsf9Bf+6N/pr6H4Mis5ku1VXNj7VW/CMbMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwNTI2MjM0NzI5WhcNMjEwNTI4MjM0NzM1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArlD19ib3J2bKYr2iap1d/gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx/CfHSgdEmACCXMiG7sQt80DPM67dlbtv/pEnWrHk4fwwst83OF+HXTSi4Sd9QWhDtBvaUu8Rp8ir+x2D0RK8YNGs0prA+qGR8O/h6Y+ascz4VNbbDlbJ+w7DJYeWU1HVp/5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y/9i8kf2pmznpu5QEDimj1yxEB+G5WEYuHD/+qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk/KKB7/KT0rEOn7T2rXW9QIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAKrtlIPhvDBxBfcqS9Xy39QqE1WOPiNQooa/FVVOsCROdRZrHhFcP27HpxO9e6genQSJ6nBRaJ4ykEf0oM535Ker5jZcDWzCwPIyt+5Kc6qeacZI5FxEHRldYkSd4lF1OTzQNvGLOPKnNWnYnXwj48ZxO50lJUsRFspVbP79E6llVNOPexrZ2GOzWghyY1E74f4uGr6fzcXQk2aFaIfLusoJlvbROPTnDu68Jt+IW4WZcO4F0tl0JIcuaqSmLS6McJW0Mpmu4wqEPV6E45zRAuX0kJUkKDMzM/lYW1MZ8QaSTt/pCmlknX1+KTgb6Sf9zZJEya8AyKML/NCpc4sfn8g="
      ],
      "n": "rlD19ib3J2bKYr2iap1d_gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx_CfHSgdEmACCXMiG7sQt80DPM67dlbtv_pEnWrHk4fwwst83OF-HXTSi4Sd9QWhDtBvaUu8Rp8ir-x2D0RK8YNGs0prA-qGR8O_h6Y-ascz4VNbbDlbJ-w7DJYeWU1HVp_5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y_9i8kf2pmznpu5QEDimj1yxEB-G5WEYuHD_-qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk_KKB7_KT0rEOn7T2rXW9Q",
      "e": "AQAB",
      "x": null,
      "y": null
    }
  ]
}
```

Please remember, This operation replaces all JWKs having in the Janssen server with new ones. So, In this case, if you want to keep olds JWKs, you have to put them as well in the schema file.

## Get a JSON Web Key Based on kid

We know that `get-config-jwks` operation-id returns all the json web keys available in the Janssen Server. With this operation-id, We can get any specific jwk matched with kid. If we know the `kid`, we can simply use the below command:

```bash
/opt/jans/jans-cli/config-cli.py --operation-id put-config-jwk-kid --url-suffix kid:new-key-test-id
```

It returns the details as below:

```json
Getting access token for scope https://jans.io/oauth/config/jwks.readonly
{
  "kid": "new-key-test-id",
  "kty": "RSA",
  "use": "enc",
  "alg": "RSA-OAEP",
  "crv": "",
  "exp": 1622245655163,
  "x5c": [
    "MIIDCjCCAfKgAwIBAgIhANYLiviUTmgOsf9Bf+6N/pr6H4Mis5ku1VXNj7VW/CMbMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwNTI2MjM0NzI5WhcNMjEwNTI4MjM0NzM1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArlD19ib3J2bKYr2iap1d/gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx/CfHSgdEmACCXMiG7sQt80DPM67dlbtv/pEnWrHk4fwwst83OF+HXTSi4Sd9QWhDtBvaUu8Rp8ir+x2D0RK8YNGs0prA+qGR8O/h6Y+ascz4VNbbDlbJ+w7DJYeWU1HVp/5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y/9i8kf2pmznpu5QEDimj1yxEB+G5WEYuHD/+qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk/KKB7/KT0rEOn7T2rXW9QIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAKrtlIPhvDBxBfcqS9Xy39QqE1WOPiNQooa/FVVOsCROdRZrHhFcP27HpxO9e6genQSJ6nBRaJ4ykEf0oM535Ker5jZcDWzCwPIyt+5Kc6qeacZI5FxEHRldYkSd4lF1OTzQNvGLOPKnNWnYnXwj48ZxO50lJUsRFspVbP79E6llVNOPexrZ2GOzWghyY1E74f4uGr6fzcXQk2aFaIfLusoJlvbROPTnDu68Jt+IW4WZcO4F0tl0JIcuaqSmLS6McJW0Mpmu4wqEPV6E45zRAuX0kJUkKDMzM/lYW1MZ8QaSTt/pCmlknX1+KTgb6Sf9zZJEya8AyKML/NCpc4sfn8g="
  ],
  "n": "rlD19ib3J2bKYr2iap1d_gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx_CfHSgdEmACCXMiG7sQt80DPM67dlbtv_pEnWrHk4fwwst83OF-HXTSi4Sd9QWhDtBvaUu8Rp8ir-x2D0RK8YNGs0prA-qGR8O_h6Y-ascz4VNbbDlbJ-w7DJYeWU1HVp_5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y_9i8kf2pmznpu5QEDimj1yxEB-G5WEYuHD_-qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk_KKB7_KT0rEOn7T2rXW9Q",
  "e": "AQAB",
  "x": null,
  "y": null
}
```

## Patch JSON Web Key (JWK) by kid

With this operation id, we can modify JSON Web Keys partially of its properties.

```bash
Operation ID: patch-config-jwks
  Description: Patch JSON Web Keys (JWKS).
  Schema: Array of /components/schemas/PatchRequest
```

In this case, We are going to a test data JWK that already added in jwk list of the Janssen server.

```json
{
      "kid": "new-key-test-id",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA-OAEP",
      "crv": "",
      "exp": 1622245655163,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhANYLiviUTmgOsf9Bf+6N/pr6H4Mis5ku1VXNj7VW/CMbMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwNTI2MjM0NzI5WhcNMjEwNTI4MjM0NzM1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArlD19ib3J2bKYr2iap1d/gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx/CfHSgdEmACCXMiG7sQt80DPM67dlbtv/pEnWrHk4fwwst83OF+HXTSi4Sd9QWhDtBvaUu8Rp8ir+x2D0RK8YNGs0prA+qGR8O/h6Y+ascz4VNbbDlbJ+w7DJYeWU1HVp/5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y/9i8kf2pmznpu5QEDimj1yxEB+G5WEYuHD/+qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk/KKB7/KT0rEOn7T2rXW9QIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAKrtlIPhvDBxBfcqS9Xy39QqE1WOPiNQooa/FVVOsCROdRZrHhFcP27HpxO9e6genQSJ6nBRaJ4ykEf0oM535Ker5jZcDWzCwPIyt+5Kc6qeacZI5FxEHRldYkSd4lF1OTzQNvGLOPKnNWnYnXwj48ZxO50lJUsRFspVbP79E6llVNOPexrZ2GOzWghyY1E74f4uGr6fzcXQk2aFaIfLusoJlvbROPTnDu68Jt+IW4WZcO4F0tl0JIcuaqSmLS6McJW0Mpmu4wqEPV6E45zRAuX0kJUkKDMzM/lYW1MZ8QaSTt/pCmlknX1+KTgb6Sf9zZJEya8AyKML/NCpc4sfn8g="
      ],
      "n": "rlD19ib3J2bKYr2iap1d_gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx_CfHSgdEmACCXMiG7sQt80DPM67dlbtv_pEnWrHk4fwwst83OF-HXTSi4Sd9QWhDtBvaUu8Rp8ir-x2D0RK8YNGs0prA-qGR8O_h6Y-ascz4VNbbDlbJ-w7DJYeWU1HVp_5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y_9i8kf2pmznpu5QEDimj1yxEB-G5WEYuHD_-qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk_KKB7_KT0rEOn7T2rXW9Q",
      "e": "AQAB",
      "x": null,
      "y": null
    }
```

We can see here `kid` is `new-key-test-id`. Before going to patch this key, let's define the schema first. In this example; We are going to change `use` from `enc` to `sig`. So our schema definition as below:

```json
[
{
  "op": "replace",
  "path": "use",
  "value": "sig"
}
]
```

Please, remember, you can do multiple operation within a single command because this schema definition support `array` of multiple operations.

Now let's do the operation with below command line.

```bash
/opt/jans/jans-cli/config-cli.py --operation-id patch-config-jwk-kid --url-suffix kid:new-key-test-id --data /tmp/schema.json
```

You need to change `kid` and `data` path according to your own.
Updated Json Web Key:

```json

Getting access token for scope https://jans.io/oauth/config/jwks.write
Server Response:
{
  "kid": "new-key-test-id",
  "kty": "RSA",
  "use": "sig",
  "alg": "RSA-OAEP",
  "crv": null,
  "exp": 1622245655163,
  "x5c": [
    "MIIDCjCCAfKgAwIBAgIhANYLiviUTmgOsf9Bf+6N/pr6H4Mis5ku1VXNj7VW/CMbMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwNTI2MjM0NzI5WhcNMjEwNTI4MjM0NzM1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArlD19ib3J2bKYr2iap1d/gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx/CfHSgdEmACCXMiG7sQt80DPM67dlbtv/pEnWrHk4fwwst83OF+HXTSi4Sd9QWhDtBvaUu8Rp8ir+x2D0RK8YNGs0prA+qGR8O/h6Y+ascz4VNbbDlbJ+w7DJYeWU1HVp/5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y/9i8kf2pmznpu5QEDimj1yxEB+G5WEYuHD/+qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk/KKB7/KT0rEOn7T2rXW9QIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAKrtlIPhvDBxBfcqS9Xy39QqE1WOPiNQooa/FVVOsCROdRZrHhFcP27HpxO9e6genQSJ6nBRaJ4ykEf0oM535Ker5jZcDWzCwPIyt+5Kc6qeacZI5FxEHRldYkSd4lF1OTzQNvGLOPKnNWnYnXwj48ZxO50lJUsRFspVbP79E6llVNOPexrZ2GOzWghyY1E74f4uGr6fzcXQk2aFaIfLusoJlvbROPTnDu68Jt+IW4WZcO4F0tl0JIcuaqSmLS6McJW0Mpmu4wqEPV6E45zRAuX0kJUkKDMzM/lYW1MZ8QaSTt/pCmlknX1+KTgb6Sf9zZJEya8AyKML/NCpc4sfn8g="
  ],
  "n": "rlD19ib3J2bKYr2iap1d_gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx_CfHSgdEmACCXMiG7sQt80DPM67dlbtv_pEnWrHk4fwwst83OF-HXTSi4Sd9QWhDtBvaUu8Rp8ir-x2D0RK8YNGs0prA-qGR8O_h6Y-ascz4VNbbDlbJ-w7DJYeWU1HVp_5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y_9i8kf2pmznpu5QEDimj1yxEB-G5WEYuHD_-qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk_KKB7_KT0rEOn7T2rXW9Q",
  "e": "AQAB",
  "x": null,
  "y": null
}

```

We see it has replaced `use` from `enc` to `sig`.

Please read about [patch method](cli-index.md#quick-patch-operations), You can get some idea how this patch method works to modify particular properties of any task.

## Delete Json Web Key using kid

It's pretty simple to delete json web key using its `kid`. The command line is:

```bash
/opt/jans/jans-cli/config-cli.py --operation-id delete-config-jwk-kid --url-suffix kid:new-key-test-id
```

It will delete the jwk if it matches with the given `kid`.
