---
tags:
  - administration
  - configuration
  - cli
  - interactive
---

# JWT Management

!!! Important
    The interactive mode of the CLI will be deprecated upon the full release of the Configuration TUI in the coming months.
    
> Prerequisite: Know how to use the Janssen CLI in [interactive mode](im-index.md)

This operation is used to get the JSON Web Key Set (JWKS) from OP host. The JWKS is a set of keys containing the public 
keys that should be used to verify any JSON Web Token (JWT) issued by the authorization server.
From the Main Menu, select option 12, It returns some options as stated below:

```text
Configuration – JWK - JSON Web Key (JWK)
----------------------------------------
1 Gets a list of JSON Web Key (JWK) used by a server
2 Puts/replaces JWKS
3 Patch JWKS
```
You can `view` the list of JSON Web Key, `add/replace` and `patch` using Janssen CLI.

## Get list of JSON Web Key

Select option 1 from JSON Web Key Menu and it will return a list of key with details information as below:
  
```text
Gets list of JSON Web Key (JWK) used by server
Gets list of JSON Web Key (JWK) used by server. JWK is a JSON data structure that represents a set of public keys as a JSON object [RFC4627].
---------------------------------------------------------------------------------------------------------------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/jwks.readonly

{
  "keys": [
    {
      "kid": "a1d120af-d4c1-45aa-8cff-034e00f13d2b_sig_rs256",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAKefzbtkilZu5nn6G1WHSbJZu/PIdKpR9U5QA58DXN6GMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzODU5WhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxmm58zzhORBJkyxcjyfFUrRO06V4PwDZT/ObroQOQDuN8KbOzqkGdZX6BkZiFPNHuWdnUp0/2Fxf2LM1z5nhyCG4Wy92rUqHL6ispNtPfWOe3mWwQlFJk/Z/87gqJZ00ss3vnSk+05j4AsgvnPoKZJtgJPAEjZ8+bBSNExpqWdHBFcqJJsLhyjE5o7hQFQplMevQLyVvrzxsY8YwZuoTZA+bUo7//vsrHUe/PyZP0+0FHRbFzwo+ArxrdFcFlEhTqjKijo7pyh8gmZkgvXG8D1Zi1Fmstnf9yiF36ZBlN+RSr+JHxPAvwU2O/aMmFhvZNJ9aOzP0dienSZo72xSiRwIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAGB6JFBWpIAux87xE1GL1iY+LrcxC7T6ITRb+mwhtsA0bOTx9CISNLhuFUIcOBrB+2LQD7asVvbo7I2zJ9enIR0QJbO4Z3niSCVULWeBhPACh5a+HgkpZ7mlFLJyD1hpw+pEfobasvoJLzvyuVpL/EXCgMYoi0qmrkwZfYXoajjZAhsT6Y5mTBd25xYGcatglQZutaVOgneQxZb2vjAH4h3H14EHdKPh3viXbpyXe6MP+DqX1kIqFHX3rYbhvLXdALHkRsqlcoHMW7jQuQyyfXNbwddg6H/IR0VV5yliOsoHP8BxHS9vIGHroGpZarpCwkxgsRKL+Uib1+wBN1GLu6o="
      ],
      "n": "xmm58zzhORBJkyxcjyfFUrRO06V4PwDZT_ObroQOQDuN8KbOzqkGdZX6BkZiFPNHuWdnUp0_2Fxf2LM1z5nhyCG4Wy92rUqHL6ispNtPfWOe3mWwQlFJk_Z_87gqJZ00ss3vnSk-05j4AsgvnPoKZJtgJPAEjZ8-bBSNExpqWdHBFcqJJsLhyjE5o7hQFQplMevQLyVvrzxsY8YwZuoTZA-bUo7__vsrHUe_PyZP0-0FHRbFzwo-ArxrdFcFlEhTqjKijo7pyh8gmZkgvXG8D1Zi1Fmstnf9yiF36ZBlN-RSr-JHxPAvwU2O_aMmFhvZNJ9aOzP0dienSZo72xSiRw",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "5841b726-4a62-4a91-9b14-2c4e774b8187_sig_rs384",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS384",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAK29kWeoIZxzuN9D5Bi+TJOSkxSMyK+9O6sFHH9UG6KTMA0GCSqGSIb3DQEBDAUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAwWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtegG/5p4hXBV8BhPE7bUYgCXYnwFY9J9yVNjMI306qnN1sRrTvqH88SCLg2/sY2gWI+Y8lmqXYsLbsmCoCXMUAHU6ujqrwWZsiubucyb6wmE2yWdkSgIcT1jpepnfvm4oyKnhZVqn6hOuDx+/vBNk/RJfPibBrhJp/+uiZFc86at3JIgqXB5RqV9ryXGSXpL7tj5cST2HFU+2WzoutHRze7T3XLcA0bIiiQUfHzssxElfSbrUZRY36mpoaqm2WDMEhBEwu2B1L2Jwx76LIn7dWszwaIHkqLMy7PSl3Hit0MdO7SD5bqHnMHHmSjj+9XmYBg5oErfOKJOWAevLlksgQIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQEMBQADggEBACSX/4j+sd5TGsM1e3ISJHxjDlWhvsurPQhadaDu49NdCP/9hrwo7Th48q8Q8o99DnDBOIV0AE7VORYC4xRWHXlGJV84YAQRhHi1rL8L5YWheNeR0/ibanLhaTMb4Ecw8CRJWplslKmt78bn/J1xl4cWilDTVeB+LAYrpmDJNXSx/3QHtIc2PoIKn3dE8cHhHvQ+zHmd52TxGdBR08+TqZDcwZT9XvjrOwyUkk5LIXp8Di9oqPtcDM2vqrgZna40cZAtXHzY1x6PKlwRoMSEZ+olYjjy2OlqsotORc+fbQIkLkUUnhyHTAiobZT1N55LjYkhwjXV+Ps1Qm0Q2px9uMs="
      ],
      "n": "tegG_5p4hXBV8BhPE7bUYgCXYnwFY9J9yVNjMI306qnN1sRrTvqH88SCLg2_sY2gWI-Y8lmqXYsLbsmCoCXMUAHU6ujqrwWZsiubucyb6wmE2yWdkSgIcT1jpepnfvm4oyKnhZVqn6hOuDx-_vBNk_RJfPibBrhJp_-uiZFc86at3JIgqXB5RqV9ryXGSXpL7tj5cST2HFU-2WzoutHRze7T3XLcA0bIiiQUfHzssxElfSbrUZRY36mpoaqm2WDMEhBEwu2B1L2Jwx76LIn7dWszwaIHkqLMy7PSl3Hit0MdO7SD5bqHnMHHmSjj-9XmYBg5oErfOKJOWAevLlksgQ",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    ...
    ...
    ...
    {
      "kid": "e6e8ccc4-708b-4a83-bbd2-7a9e0181734f_enc_rsa-oaep",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA-OAEP",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAIfkfNwuxlcdhdiAKvWrX+LbYKvZwRC9aEn9tOqCZLunMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAyWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAw6p2QLwHKwPA+W9oTjTAkYn1iRQVXdPsNIu18Lao11Fbp0krMKSsnVcBIuO8zjsERf8b/awTN9zJQpKO3LqHHcGIjZJdAfH42CPgyUMjn6laF8iO0S+kI8RCocRLoPP2PVbqPjYD6kvK0mlSSLu+t9bU7mgEsYF5y8r05hX1ROdLUTFuHMa2g4cuD0HEEJMzewK1TzPikNiThsQv0yzwkwGZrBldWeB1E8BGWha2jwVom/Noo6vimtN8Le1XeYq5PvRVaS4AtLup4K0SaVetL0mAiCWKUTudWNDCRWB/Z4lJCJGOCCfk6bPp0TsjOcDjGkPzP05G9FFWndOpQ49UcwIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAJ6zGYZqI4rwBJri7v3XSLvKrUgU19xLq6aik6h3DMylzHFEydnMdgyYU23GWP/rcvM1K4whhiopUcoj/FHQ0uaQV67zb6/NvCbIxiGjQs08ZcYnMtZ0zwm9hj7YeafsVQVI3qo1VdJfPWYHEW4IUfaqIlWdsj/CW1HeKWOrw0+WC1JYwD5Ka13bwYtC3jgt8yHwn3XoOhbINzFsVdRA5pfJKCvZN5IteHhpkmeOkvOlRFaPrqlGM2rukCzo2aBakC8F8SwaQje6prm2wSRJp/qjJKxKO8fMklcBT/FMD2zdYeHb4+YFRo8/CzjRNPEmMSI4LHdFkmjGDrLQYjrxOsY="
      ],
      "n": "w6p2QLwHKwPA-W9oTjTAkYn1iRQVXdPsNIu18Lao11Fbp0krMKSsnVcBIuO8zjsERf8b_awTN9zJQpKO3LqHHcGIjZJdAfH42CPgyUMjn6laF8iO0S-kI8RCocRLoPP2PVbqPjYD6kvK0mlSSLu-t9bU7mgEsYF5y8r05hX1ROdLUTFuHMa2g4cuD0HEEJMzewK1TzPikNiThsQv0yzwkwGZrBldWeB1E8BGWha2jwVom_Noo6vimtN8Le1XeYq5PvRVaS4AtLup4K0SaVetL0mAiCWKUTudWNDCRWB_Z4lJCJGOCCfk6bPp0TsjOcDjGkPzP05G9FFWndOpQ49Ucw",
      "e": "AQAB",
      "x": null,
      "y": null
    }
  ]
}
```

## Creating or Replacing - JWK

You can add a new JWK or replace an old JWK with new value through this Interactive Mode option.
When it will ask `Add JwonWebKey?` just press `y` to confirm. Fill each property with a value, keep empty to skip.

- **__kid__**: Unique Key Identifier [String].
- **__kty__**: Cryptographic algorithm name used with the key [String].
- **__use__**: Key usage, [enc, sig,..]
- **__alg__**: The cryptographic algorithm name that is going to be used.
- **__exp__**: time validation

```text
Gets list of JSON Web Key (JWK) used by server
Gets list of JSON Web Key (JWK) used by server. JWK is a JSON data structure that represents a set of public keys as a JSON object [RFC4627].
---------------------------------------------------------------------------------------------------------------------------------------------
Please wait while retreiving data ...

Getting access token for scope https://jans.io/oauth/config/jwks.readonly

«JsonWebKey. »
Add JsonWebKey? y

   «The unique identifier for the key. Type: string»
   kid: aabb

   «The family of cryptographic algorithms used with the key. Type: string»
   kty: RSA

   «How the key was meant to be used; sig represents the signature. Type: string»
   use: enc

   «The specific cryptographic algorithm used with the key. Type: string»
   alg: RSA-OAEP

   «The crv member identifies the cryptographic curve used with the key. Values defined by this specification are P-256, P-384 and P-521. Additional crv values MAY be used, provided they are understood by implementations using that Elliptic Curve key. The crv value is case sensitive. Type: string»
   crv: 

   «Contains the token expiration timestamp. Type: integer»
   exp: 

   «The x.509 certificate chain. The first entry in the array is the certificate to use for token verification; the other certificates can be used to verify this first certificate. Type: array of string separated by _,»
   x5c: 

   «The modulus for the RSA public key. Type: string»
   n: 

   «The exponent for the RSA public key. Type: string»
   e: 

   «The x member contains the x coordinate for the elliptic curve point. It is represented as the base64url encoding of the coordinate's big endian representation. Type: string»
   x: 

   «The y member contains the y coordinate for the elliptic curve point. It is represented as the base64url encoding of the coordinate's big endian representation. Type: string»
   y: 

Add another JsonWebKey? n
Obtained Data:

{
  "keys": [
    {
      "kid": "aabb",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA-OAEP",
      "crv": null,
      "exp": null,
      "x5c": [],
      "n": null,
      "e": null,
      "x": null,
      "y": null
    }
  ]
}

Continue? y
Getting access token for scope https://jans.io/oauth/config/jwks.write
Please wait while posting data ...

{
  "keys": [
    {
      "kid": "aabb",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA-OAEP",
      "crv": "",
      "exp": null,
      "x5c": null,
      "n": null,
      "e": null,
      "x": null,
      "y": null
    }
  ]
}
```

## Update an Existing JWK Configuration


Just choose this option and fill the value for `op`, `path`, and `value` to patch JSON Web Key.

