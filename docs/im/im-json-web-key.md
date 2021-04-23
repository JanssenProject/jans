# JWT Management

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

- **__`Get list of JSON Web Key`__**

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
    {
      "kid": "71d17b7d-045b-4095-ad00-5025fa6829ec_sig_rs512",
      "kty": "RSA",
      "use": "sig",
      "alg": "RS512",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDCTCCAfGgAwIBAgIgBcLFz+d7BzpRRt6y8Q7tx+JHp/Mz+W7wYrJ79B879AwwDQYJKoZIhvcNAQENBQAwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MDBaFw0yMTAxMTcyMjM5MDlaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC5fSJXTly7NcBc0hUWE4+n0n9qS0dshbO4iHWEGrIcE3sWAYlz1Y1UItlS3LdkIkP92yV7d2c1Y9DEpVaanH21yIoHDjUkNtgYOUv6QMVzOb0o3P6NDZQBUTW1dXIc6XF2gfM3HVd7blcq1yqrENQKpZcqAFd6acyPvuIJq9o8w7MfEJHrpOlBWYeMarXGgzIdNd4S+1jGBpbsbROm3jUFVftjunL0sub1+tViBEk+cspleaOtA2r5oc8pC5BbkIQ4CreocOHtIBSQOXhp4iS30xiEemKcM0Me9n5A9DzzB2EHfo48qR0zjsKdV2XRVGYehwxwQTXJcmFLWJb1phQDAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQ0FAAOCAQEAnqYSP4iuwe2fZjPw3yYBk9ZR64qeQvihJZKwgQFwJ5yOo+bu8gErDisVmMUVxeR/g6RTuBBbJGwECtz2Fcpj7euapPoGGxkHEnZMAwkro0UPWztSi798tIhZoeRrZI0A9Lhk885qI2ZSE9hcD5oaDU1uRcnBFwbsoZovmLnwm+9hNJyb6WNpLO3fOnIQGAIx1P6CGShKCK/U4Q3kXrhCg5H7ceSVbHWPwMrKmBSp4dytSXimo8VCFe2a2sInKWcGC1nhBHD0vUlrmJrPo31xYkoSDFkBIszGXLrSUAj5rFFY+qurWv2izECkdMHtv8TqRC9+wFLppFgbbr/R8EZP8A=="
      ],
      "n": "uX0iV05cuzXAXNIVFhOPp9J_aktHbIWzuIh1hBqyHBN7FgGJc9WNVCLZUty3ZCJD_dsle3dnNWPQxKVWmpx9tciKBw41JDbYGDlL-kDFczm9KNz-jQ2UAVE1tXVyHOlxdoHzNx1Xe25XKtcqqxDUCqWXKgBXemnMj77iCavaPMOzHxCR66TpQVmHjGq1xoMyHTXeEvtYxgaW7G0Tpt41BVX7Y7py9LLm9frVYgRJPnLKZXmjrQNq-aHPKQuQW5CEOAq3qHDh7SAUkDl4aeIkt9MYhHpinDNDHvZ-QPQ88wdhB36OPKkdM47CnVdl0VRmHocMcEE1yXJhS1iW9aYUAw",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "941699d5-7abf-4d7b-a34d-680778dbf202_sig_es256",
      "kty": "EC",
      "use": "sig",
      "alg": "ES256",
      "crv": "P-256",
      "exp": 1610923149000,
      "x5c": [
        "MIIBfjCCASSgAwIBAgIhAJEw743rfFLOZzVxJ5Y0/syaX3M2pgKQRHSiikfcKlu+MAoGCCqGSM49BAMCMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAwWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEjIQfizmxui3ZMItmyfI+PAXt/lu5DAVx9U7XNLWlJV1SslGyJO5EpQgw3KaMbT8Z7CXUEM15YBX7Q3ipRwmaYaMnMCUwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMAoGCCqGSM49BAMCA0gAMEUCICFR1HkIWNaKvmnC3K2yKzqT3mV04li35Y3wSa2jNE0jAiEAgPSZmkjWhAybbeFVhwYw2tRoUr33aKxH75kIy8MdYGE="
      ],
      "n": null,
      "e": null,
      "x": "AIyEH4s5sbot2TCLZsnyPjwF7f5buQwFcfVO1zS1pSVd",
      "y": "UrJRsiTuRKUIMNymjG0_Gewl1BDNeWAV-0N4qUcJmmE"
    },
    {
      "kid": "1e422bb9-09d8-429b-965b-85ec88e29059_sig_es384",
      "kty": "EC",
      "use": "sig",
      "alg": "ES384",
      "crv": "P-384",
      "exp": 1610923149000,
      "x5c": [
        "MIIBuzCCAUGgAwIBAgIhAKhVZ9S8jBZxo7bFKUTGPlxAmXE7+NRa15UhkwD3DjrRMAoGCCqGSM49BAMDMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAwWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEsVnnhnMVqJlgdJL1nAMdkY3TYsfe7+jCvG8/HV9fFcLWGYj8LGRFPqjr0tuwAW0Y96przj2GvNOyA90nddd8X5KHiln8x9OZ0ZhCAjlH8KJfn3SqaXfDF+N6A/LAN7uBoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwCgYIKoZIzj0EAwMDaAAwZQIwXO/CxFQRYyEucM2ELFG/duih44ghTzxhzOP+3RbJQHLLT0Z34iZlRPePVzXC52AaAjEAiM6wI9pOMxl8rdNdHEuCiL1SETlHs6K3mr6An6iWm63E0jL6szZX1zOZtVk/y386"
      ],
      "n": null,
      "e": null,
      "x": "ALFZ54ZzFaiZYHSS9ZwDHZGN02LH3u_owrxvPx1fXxXC1hmI_CxkRT6o69LbsAFtGA",
      "y": "APeqa849hrzTsgPdJ3XXfF-Sh4pZ_MfTmdGYQgI5R_CiX590qml3wxfjegPywDe7gQ"
    },
    {
      "kid": "52f64d60-b50b-4b07-95b2-582f87a2cb37_sig_es512",
      "kty": "EC",
      "use": "sig",
      "alg": "ES512",
      "crv": "P-521",
      "exp": 1610923149000,
      "x5c": [
        "MIICBTCCAWagAwIBAgIgEtgLr/raWVlRStF9d1djdZucnWYKKhRiZAH0KiElSnwwCgYIKoZIzj0EAwQwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yMTAxMTUyMjM5MDBaFw0yMTAxMTcyMjM5MDlaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwgZswEAYHKoZIzj0CAQYFK4EEACMDgYYABACvjTXQc/bAWetmIpkGg4z8b/81zz3y4ycZjiKHb9BOQfq4503ZfJ4KBj22UzaUPzjWV1se9UVv6QL+IIAkHVJbzQCP3vMaURBUUvplobpQ2791d0LFC98T6qfzGlMgBcTTMRF5QsEuFyc3PkbNpUIFLJUaLCRauNIExRuxOWjp2Zu9KaMnMCUwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMAoGCCqGSM49BAMEA4GMADCBiAJCAZmADtCphAEt7UO7rfCC1fNVwHJiUz7msGCFP7pLZObijl/z/uy0dIV+YV5TcwE//aMzXj5QGfJ4tkdibp+iskNhAkIBPO7ZoCVHKiRvyxAa3RTXZHK8Sv0UixFmeS2nICyUVBLKgz36qsLkA6uXxLxuvXUhabJwJsyrAKjyp/i0X897KE0="
      ],
      "n": null,
      "e": null,
      "x": "AK-NNdBz9sBZ62YimQaDjPxv_zXPPfLjJxmOIodv0E5B-rjnTdl8ngoGPbZTNpQ_ONZXWx71RW_pAv4ggCQdUlvN",
      "y": "AI_e8xpREFRS-mWhulDbv3V3QsUL3xPqp_MaUyAFxNMxEXlCwS4XJzc-Rs2lQgUslRosJFq40gTFG7E5aOnZm70p"
    },
    {
      "kid": "13394d6c-e7cc-4699-8b96-6423984b94d7_sig_ps256",
      "kty": "RSA",
      "use": "sig",
      "alg": "PS256",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDcTCCAiWgAwIBAgIgMp9yALa/gAL+jgCC/gs20Zlyx+WMmmIewokfcGn3xgYwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgEFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgEFAKIDAgEgMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAxWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkcvMiWwaCFNJYpq0Ioev9nlrt40XNASE5NjuhZHGg4MHlBOAaMOsnmLqkEGhdWybbAaXkOoeoPFVBkMkuo+BpmugNBJjiGtlGbOkguRQ7+P3Ifjq45EcaBoEEr9h7MfbkYHpGSxQS1JnmylT7ORfbAaqsh3LbS+82Q/eP4/AF4WjoJeTPdkGXrqaGmqrHKtNLTN/U8/hFO52nnnbjljGbsPnTGU5279r0L1+VN5hUEstAyNlDcy5MVDesswq4AK3U7vRxDmguAiW1sw0Xzv9jR6xecdwJM0Q54i7Lwjk7IlFxWm7akE78Q9frlWaQLA8kR16yYnxoI4O+cum618c8wIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgEFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgEFAKIDAgEgA4IBAQAZtk2ljb/Z/gT0uzdYRUYNR6JuZXOpajoMALSSjmPSWF9IVMgQZ5dmZWg6PYmJJ/mBKSGw6ju6nbHXeVE8gPlcTZ3YOdaRS7hX9zDAF7VtiAblbDRNSt3F61euMl3HBNMcfUXhBlYt+RJwaN/x1WQ1YvOaB3xt7rihJHYhTaK+WZAQl1ptdXN7m9IXCHMWEqO4A3+m3a9z3bNoSd7W221He+G0OUcNhMXyLxSJp2D6RO80AIAOCahhSy3DQ33Uk6PPiQu3AZlKk9Ppi6e17B+yUU9PVt9U2GgqAFa8ll2AjT3lG/kyQZLSmM0WU44rcsncsCdWWXGTmChxD4DUCV0u"
      ],
      "n": "kcvMiWwaCFNJYpq0Ioev9nlrt40XNASE5NjuhZHGg4MHlBOAaMOsnmLqkEGhdWybbAaXkOoeoPFVBkMkuo-BpmugNBJjiGtlGbOkguRQ7-P3Ifjq45EcaBoEEr9h7MfbkYHpGSxQS1JnmylT7ORfbAaqsh3LbS-82Q_eP4_AF4WjoJeTPdkGXrqaGmqrHKtNLTN_U8_hFO52nnnbjljGbsPnTGU5279r0L1-VN5hUEstAyNlDcy5MVDesswq4AK3U7vRxDmguAiW1sw0Xzv9jR6xecdwJM0Q54i7Lwjk7IlFxWm7akE78Q9frlWaQLA8kR16yYnxoI4O-cum618c8w",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "66294948-40f2-48df-8ff6-d342d56f8102_sig_ps384",
      "kty": "RSA",
      "use": "sig",
      "alg": "PS384",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDcTCCAiWgAwIBAgIgOpAUrzN/UPGm9uLadWCgpQIaDb7wp6I44MAYu3pAsuAwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgIFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgIFAKIDAgEwMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAxWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAy9KnfzsvwhO23Pef6TfyIOkAqnDGNwrzzeBYuqiql3/Zu9uhtbDD9Q1/XClqOvbi+ouUXJHZ9Rsxpvald1z9+8XAZYIpQ0Rrqr11KBeNEwyrn/fK/lfgOaVuVSG1oe7o5zVZXMlYUiRk+koVkrm2Jj9NG7FDBW28pChps3Wk05KBsopgndKjIna3WYgT2zr0KHDwPe4EbQSL2NyS87TF77jD6yW24jqEMuAa/FvvFIDiEP60JQG9aNO19w0yMkmj8c8+cQtwO/E5huRg83bfGHMPMSaDgxoXoDCt+UKIpkVC+SgE1+SBZLMuLpsj+wTBXhz0MJm6LYFLJxalz9+eawIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwQQYJKoZIhvcNAQEKMDSgDzANBglghkgBZQMEAgIFAKEcMBoGCSqGSIb3DQEBCDANBglghkgBZQMEAgIFAKIDAgEwA4IBAQBGhHD5FKSqJ4GJYPEi48AhUlrmc4xBsKr7FXNGzYpXkgzwlhP0XkifGguT4W/4JD9ra4GdPjVweC6XM525Uu1NDKvBDMLUYaQj5rDmXe0hHSL8cJ60yKOipbbyOXycvHot3Kebqy7rECCKDjEbejaHnYYjdVNFehz7CvweAB0cn34QKAPfBjfqDHW8d2B46Ow7jhGL18ep01j9WeEeLW5K0nbdgRRLKA2T7RV6jXrsnI5w7eOxfn1WXj7PGmFeyLZQaEUtK7pX+cU10wBVk3Jtt/9HaQTxE9aVETpddwWLjakAzoS0gXRqlQLh1786zIBcHGKlpkYKbTEXIBokBgdN"
      ],
      "n": "y9KnfzsvwhO23Pef6TfyIOkAqnDGNwrzzeBYuqiql3_Zu9uhtbDD9Q1_XClqOvbi-ouUXJHZ9Rsxpvald1z9-8XAZYIpQ0Rrqr11KBeNEwyrn_fK_lfgOaVuVSG1oe7o5zVZXMlYUiRk-koVkrm2Jj9NG7FDBW28pChps3Wk05KBsopgndKjIna3WYgT2zr0KHDwPe4EbQSL2NyS87TF77jD6yW24jqEMuAa_FvvFIDiEP60JQG9aNO19w0yMkmj8c8-cQtwO_E5huRg83bfGHMPMSaDgxoXoDCt-UKIpkVC-SgE1-SBZLMuLpsj-wTBXhz0MJm6LYFLJxalz9-eaw",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "c9239f5e-9168-43dc-a931-e3fe26ffec51_sig_ps512",
      "kty": "RSA",
      "use": "sig",
      "alg": "PS512",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDcjCCAiagAwIBAgIhALyTZpvJJ7wbNwXRjx4qAOkF8+PE0CAKLiNmluz8qBsSMEEGCSqGSIb3DQEBCjA0oA8wDQYJYIZIAWUDBAIDBQChHDAaBgkqhkiG9w0BAQgwDQYJYIZIAWUDBAIDBQCiAwIBQDAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMB4XDTIxMDExNTIyMzkwMVoXDTIxMDExNzIyMzkwOVowJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAOV/yvZh9YgWhjWUX/uhFSxXscTiidKrmxRoHJprK3daNgNj04jkWC53aZYt4TvYI/gTOUwmt+j8IzVmxpDZeKi7BiEb5C0iv1iDndEDZfVScLBQnXMPvaObtnFc5FXPe/8rMsRdtPigIcZW7lB987+jlgi62lojFSQKR3C+q7P6jQ0LSy/v9bCwE/X+dVfgQ0iiigFQfLISb/o49jyMZ8tiW+skHCpfE5WrlYq4dilfZIExcF5fgNKXb9DaGRT/GUtF7at+tDD186UiToWcez2V1i5hh97WGVq43CUA4PWfXHidJN4Orzn8rP/tx7CAsl3fVFImKStIKtuhzm0ig4sCAwEAAaMnMCUwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMEEGCSqGSIb3DQEBCjA0oA8wDQYJYIZIAWUDBAIDBQChHDAaBgkqhkiG9w0BAQgwDQYJYIZIAWUDBAIDBQCiAwIBQAOCAQEAzm9YrqtVIUwnXZ2Uhl9J7FCC73nF8OC31/SQMBzfxsyo6Hx9ksxuBfvvmnCb761luVgeFOELlttfsV9uj4T3TuNzZ8uOCrxCgFOeG1CTJ3V4tl5XDSo3cLeYwIlj8hpeHKwCc6Ecjh4BLfQvzIGQWTVFLfiA1SHUsidnLFNUSogD7OXBDHq+GzErcQafCqeozRF1JfrUEt9NHdGk4+w859qz7jAP3hjkBtCMYWFhMUnR55PHBUfZ0sLIH10nDG+eeRCaVDJLbrUb6lLMPQH7sWDU7f3EYa2lk3Z25M1p7Oah6R5VaKcO/AQ4bEk9ptQWd6HLyYLMVtk4dRNbr7IX+Q=="
      ],
      "n": "5X_K9mH1iBaGNZRf-6EVLFexxOKJ0qubFGgcmmsrd1o2A2PTiORYLndpli3hO9gj-BM5TCa36PwjNWbGkNl4qLsGIRvkLSK_WIOd0QNl9VJwsFCdcw-9o5u2cVzkVc97_ysyxF20-KAhxlbuUH3zv6OWCLraWiMVJApHcL6rs_qNDQtLL-_1sLAT9f51V-BDSKKKAVB8shJv-jj2PIxny2Jb6yQcKl8TlauVirh2KV9kgTFwXl-A0pdv0NoZFP8ZS0Xtq360MPXzpSJOhZx7PZXWLmGH3tYZWrjcJQDg9Z9ceJ0k3g6vOfys_-3HsICyXd9UUiYpK0gq26HObSKDiw",
      "e": "AQAB",
      "x": null,
      "y": null
    },
    {
      "kid": "90d4965b-868d-4290-91a6-8c5d49459f88_enc_rsa1_5",
      "kty": "RSA",
      "use": "enc",
      "alg": "RSA1_5",
      "crv": "",
      "exp": 1610923149000,
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAIksD5qI5INemoZFFfxQ+DT6CwQT/3vPF+hSmWtKOqUpMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwMTE1MjIzOTAyWhcNMjEwMTE3MjIzOTA5WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApB+uqGCDL32xtRSrdADTTakbLKx3DelyBLIfTbEsTv2u0GjRawv/AocnWiwHd3N6b/SkmXXnOpvsFSlFNTbZJho49kjdcdZ7zmXE7LkcXsa3Z1F1bHweXddqqWuxVUXt80so5eAQseOW33KPGMbORHhIkt4IRDC0bAcEKUgk5ibGA9wuolcsA4tZ9/zuKhZpQzXy+3Z2ezfX+veRLXB7N7bh3JZeaJMKEaMriOE73iTHk6xr4qcEK2wdVJhQjxCHoEm0++66ATivSGcJqAexoS6hoR/LibpSMP5+Tw5QHHvLFc1b0CDa0yhFzPMpB+CYJJnDOpPdLz1i2/oAOeh1RQIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAHbVma4EZAsw7ltfqlwpJK6lGPCNdPInzYlrWof4vay4oCkKXrhE4AvAayxlszoom+Ap1RKf+8oUzVN0Ilz1IF0OvKle+PL1S92kb90uM6NxlQ/fRSveAg7At/J+N9Xu2OJ1v0xUkDvmCTmX6bnnfSafWWSU0Z/na2w84owRD29dRsFn2Ge5EmuhIjNzDYfZCMhfVQQvJh1olvHXr59+ustbJGM8pmeZ0Uh15IidzpTR3kiXbcOVbHuJCJ8vjjLiUq2MzNFXWRxTDinw920yVCxLT1GhMy0lq2IHsZFGMwrsDT50aUv9+7/dz4RQlpLuVs+p2q8ERGCfxPLxaO8MG38="
      ],
      "n": "pB-uqGCDL32xtRSrdADTTakbLKx3DelyBLIfTbEsTv2u0GjRawv_AocnWiwHd3N6b_SkmXXnOpvsFSlFNTbZJho49kjdcdZ7zmXE7LkcXsa3Z1F1bHweXddqqWuxVUXt80so5eAQseOW33KPGMbORHhIkt4IRDC0bAcEKUgk5ibGA9wuolcsA4tZ9_zuKhZpQzXy-3Z2ezfX-veRLXB7N7bh3JZeaJMKEaMriOE73iTHk6xr4qcEK2wdVJhQjxCHoEm0--66ATivSGcJqAexoS6hoR_LibpSMP5-Tw5QHHvLFc1b0CDa0yhFzPMpB-CYJJnDOpPdLz1i2_oAOeh1RQ",
      "e": "AQAB",
      "x": null,
      "y": null
    },
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

- **`puts/replace - JWK`**

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

- **`Patch JWK`**

Just chose this option and fill the value for `op`, `path`, and `value` to patch JSON Web Key.

