# JSON Web Key (JWK)

This operation is used to get the JSON Web Key Set (JWKS) from OP host. The JWKS is a set of keys containing the public keys that should be used to verify any JSON Web Token (JWT) issued by the authorization server.

There are few operations we can do using `jans-cli` commands. To get list of operations id run below command:

```
/opt/jans/jans-cli/config-cli.py --info ConfigurationJWKJSONWebKeyJWK
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

## Update / Replace JSON Web Key (JWK)

