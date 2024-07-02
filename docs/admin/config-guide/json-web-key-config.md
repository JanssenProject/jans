---
tags:
  - administration
  - configuration
  - JWK
  - json web keys
---

# JSON Web Key (JWK)

The Janssen Server provides multiple configuration tools to perform these
tasks.

=== "Use Command-line"

    Use the command line to perform actions from the terminal. Learn how to 
    use Jans CLI [here](./config-tools/jans-cli/README.md) or jump straight to 
    the [Using Command Line](#using-command-line)


=== "Use Text-based UI"

    Use a fully functional text-based user interface from the terminal. 
    Learn how to use Jans Text-based UI (TUI) 
    [here](./config-tools/jans-tui/README.md) or jump straight to the
    [Using Text-based UI](#using-text-based-ui)

=== "Use REST API"

    Use REST API for programmatic access or invoke via tools like CURL or 
    Postman. Learn how to use Janssen Server Config API 
    [here](./config-tools/config-api/README.md) or Jump straight to the
    [Using Configuration REST API](#using-configuration-rest-api)



##  Using Command Line


In the Janssen Server, you can deploy and customize the JSON Web key using the
command Line. To get the details of Janssen command line operations relevant to
JSON Web Key, you can check the operations under `ConfigurationJwkJsonWebKeyJwk` task using the
command below:


```bash title="Command"
/opt/jans/jans-cli/config-cli.py --info ConfigurationJwkJsonWebKeyJwk
```

```text title="Sample Output"
Operation ID: get-jwk-by-kid
  Description: Get a JSON Web Key based on kid
  Parameters:
  kid: The unique identifier for the key [string]
Operation ID: delete-config-jwk-kid
  Description: Delete a JSON Web Key based on kid
  Parameters:
  kid: The unique identifier for the key [string]
Operation ID: patch-config-jwk-kid
  Description: Patch a specific JSON Web Key based on kid
  Parameters:
  kid: The unique identifier for the key [string]
  Schema: Array of JsonPatch
Operation ID: get-config-jwks
  Description: Gets list of JSON Web Key (JWK) used by server
Operation ID: put-config-jwks
  Description: Replaces JSON Web Keys
  Schema: WebKeysConfiguration
Operation ID: patch-config-jwks
  Description: Patches JSON Web Keys
  Schema: Array of JsonPatch
Operation ID: post-config-jwks-key
  Description: Configuration – JWK - JSON Web Key (JWK)
  Schema: JSONWebKey

To get sample schema type /opt/jans/jans-cli/config-cli.py --schema <schema>, for example /opt/jans/jans-cli/config-cli.py --schema JSONWebKey
```
Let's explore each of these operations.

### Get Configurations list of JWKs

We can get list of all configurations of the jwk configuration within a single command like this:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id get-config-jwks
```
It will return all the jwk configuration information as below:

```json title="Sample Output"
{
  "keys": [
    {
      "descr": "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-256",
      "kty": "RSA",
      "e": "AQAB",
      "use": "sig",
      "key_ops_type": [
        "connect"
      ],
      "kid": "connect_e47fd367-51d6-4d17-811d-adb1f1a0b723_sig_rs256",
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAInuAAbxL2O7H+/V0lm3bbEdCmdPdgJh+OqljtWpwi7wMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjQwNjIxMDg0ODE5WhcNMjQwNjIzMDg0ODI4WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1RCY0ZfJ/SHZ+jojCyStBjIh4upkLXuMgZLi6b7k0fQ8/oNmCBEsOKMPUubHFEHrDHZLbXj7w5gEdMPZOiLaBP8Pv0JD8IbOUtoSXEawE33LRldKiof296nlBJFsX00ipiLq3ANXuTDXtP4+pd+lvIufv1nBXpqqrN4MOsSsKuvKvxRPCg6JusHVU5hsiqbwh9y3X7sPFwqw4LJFa0U3Z4RoX7vCsS/axPPSyUi9x0zsF4S7ZGHclBReC6IipOnGyGeSEQdpuchhoZs382md+wejIf4hVtusRbEHz+wwZFhnrfh/nvHCvrWCcxBgeEntAin+ig1RlR8N4x9Ox9K01wIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBACkrKROjAIf6n1PKpXFTRQVov32EFcwhi1YSao/MZHURV2ruYXjh/S6HuvHWWofV8R6muLnD938GytS2mRjr+X7DOZj/bsDT7amd810SDvFUCh6IoPt46FeXFZMV4XyL4DQKoNxOEGGDVnD41NVC6k5GLzPwcVBwX11+b7wRfy/KoPP9aoSXjyWnNbhwClFQ9oTJYkNtaNeh2kJZ2j1UIqO51vyhUjpSM9awqV2u+ouxDKCT4h9xRcDwkOUlVXtBwn+dfJHnG6riLzT59MiPtWeo037hESUxIvJxLQP6jV97eEi/CMhSb1y6YJPFjnTBmCpzeHRp5+DNu65KPaGntB8="
      ],
      "name": "Connect RS256 Sign Key",
      "exp": 1719132508905,
      "alg": "RS256",
      "n": "1RCY0ZfJ_SHZ-jojCyStBjIh4upkLXuMgZLi6b7k0fQ8_oNmCBEsOKMPUubHFEHrDHZLbXj7w5gEdMPZOiLaBP8Pv0JD8IbOUtoSXEawE33LRldKiof296nlBJFsX00ipiLq3ANXuTDXtP4-pd-lvIufv1nBXpqqrN4MOsSsKuvKvxRPCg6JusHVU5hsiqbwh9y3X7sPFwqw4LJFa0U3Z4RoX7vCsS_axPPSyUi9x0zsF4S7ZGHclBReC6IipOnGyGeSEQdpuchhoZs382md-wejIf4hVtusRbEHz-wwZFhnrfh_nvHCvrWCcxBgeEntAin-ig1RlR8N4x9Ox9K01w"
    },
    {
      "descr": "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-384",
      "kty": "RSA",
      "e": "AQAB",
      "use": "sig",
      "key_ops_type": [
        "connect"
      ],
      "kid": "connect_fed19dd0-7139-4ed8-ad43-43322b7eeaea_sig_rs384",
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAKLBm5p06uGv/lXT4tKLgjsS5kosfHb1rgO50fdLAFRoMA0GCSqGSIb3DQEBDAUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjQwNjIxMDg0ODE5WhcNMjQwNjIzMDg0ODI4WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkIt6G/MDIyfp1uTERxFYoY1nGuq3FhZG7xdPYi7eHoyM+PAbq+8rKeIRcJWJi37TazbqqLViGbejaD6rRxOihzoMPo1LtBuqGGw8m6fI1UJlvnt1NHd+du3Q6065WufL+nXn2Osmg962TF+gSvExgxr+HFeAgjP/kyG99dzSv4mUFbikegK8Dql1K36fZ427vDQ7mGRrR1vBsbMPqW5d9huXhl+iy11AOtYNNcfRDu47Hzae5Srzp32si36+Da/dEwntuMXnK7BwxDp/BGoOuWLPFoVctH6PDoIYzXUiTbj+XiQ8zFgPydt+x/2ZEGyq61Ewebjkpj9b0g+yHk/2aQIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQEMBQADggEBAHnfO3jItuXdSr5Js3kj/7kI1OElwcU4fco53ThSilbtq6FBOk5GzonaZOHFQayzjo3Qp5qe/uShSknFQ+sjEmhGTVPiQFssNTXxEnEw1WrTvEc/09I9oTNg9jitppn1z4/QT/wP0X3erRIjBQvFe4ov1wL/atjs5Mg8KRHqGttze5xN1pY3r0hrUiIxYiA7F5DUigGD4hYGGXKXymr0SgEoMZQx9Duxn8wxtX5l3fcC0FA53vn/4ZDd/ikhQRqTyz/C7ffHsVcOxnlWd3pm37+W9swGhkb9EaKRg2gJOdA5+Vw62tJA4Gp8WOIeMrRiXvtn2AVks5nyG7oiWt0ldYk="
      ],
      "name": "Connect RS384 Sign Key",
      "exp": 1719132508905,
      "alg": "RS384",
      "n": "kIt6G_MDIyfp1uTERxFYoY1nGuq3FhZG7xdPYi7eHoyM-PAbq-8rKeIRcJWJi37TazbqqLViGbejaD6rRxOihzoMPo1LtBuqGGw8m6fI1UJlvnt1NHd-du3Q6065WufL-nXn2Osmg962TF-gSvExgxr-HFeAgjP_kyG99dzSv4mUFbikegK8Dql1K36fZ427vDQ7mGRrR1vBsbMPqW5d9huXhl-iy11AOtYNNcfRDu47Hzae5Srzp32si36-Da_dEwntuMXnK7BwxDp_BGoOuWLPFoVctH6PDoIYzXUiTbj-XiQ8zFgPydt-x_2ZEGyq61Ewebjkpj9b0g-yHk_2aQ"
    },
    {
      "descr": "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-512",
      "kty": "RSA",
      "e": "AQAB",
      "use": "sig",
      "key_ops_type": [
        "connect"
      ],
      "kid": "connect_b14a8daa-bd0c-4e41-b067-eb96f2f7d09a_sig_rs512",
      "x5c": [
        "MIIDCTCCAfGgAwIBAgIgNR1DmshFLTSQ8VtMyIYdar+VTZS0tQvEbZQ4jaytwxMwDQYJKoZIhvcNAQENBQAwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yNDA2MjEwODQ4MjBaFw0yNDA2MjMwODQ4MjhaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCLbDRFT+RQEenWyAN06/q1bliMP5aR40Brr/HuLTKt5OM8c3vsICjo8n8h3OZZBCDaxvvKchVOAVkURuft1NqyYxY6tXOebEW76WbYYUTlJge6faRDY0vj7Qz2uO0E0kVynhL3dQM/9EMADJPiWb0kRp1MocccrBqw7zex7b8KXRPJWRfVbgsZWY4hbKAl3fjTGKz4xSwaGn6GBzaaogWtGdlwBPz0ThSNW47AvEnc+b321d/JPHHQU/n1I0+G5Sg5AVQwlbuddQxF1j2Ggwqyk92axTPWlaJ8kBgb+CNIoQLypX+cBI1muYh2hGEiVcdTTCvTE+SDSk7V7uxkJXjZAgMBAAGjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG9w0BAQ0FAAOCAQEAQVx8z37viPVBAdWftHtH2FQbJGbOyCVJ+OYj8EhBo41IA6fhNGq5gi0U9MISOGUPsm6FHMfxK759muvyWb8m0ntZX6oh3LudkZTj8mUAERuR59LvWPTduuy32LFjHo5COYexKL9skRc4M+bO3RsajXeAAHBOsi3BVCXoqyPtgYcN5l1VuF2ABeUcFp+5B1tAM8y1CFPPJFn2ls1Go5FWR236/sfGLE2OIbb26RLnZ1sn7Luh2yooDk7jxZiC4Fjy3d6feWnAw0BA+wc723P/xcO00kja6ldnkvSBeNLuATGli9nww0HCr9n8a7oYPuIFjQPKESO8MuR7QOEt8iLmTA=="
      ],
      "name": "Connect RS512 Sign Key",
      "exp": 1719132508905,
      "alg": "RS512",
      "n": "i2w0RU_kUBHp1sgDdOv6tW5YjD-WkeNAa6_x7i0yreTjPHN77CAo6PJ_IdzmWQQg2sb7ynIVTgFZFEbn7dTasmMWOrVznmxFu-lm2GFE5SYHun2kQ2NL4-0M9rjtBNJFcp4S93UDP_RDAAyT4lm9JEadTKHHHKwasO83se2_Cl0TyVkX1W4LGVmOIWygJd340xis-MUsGhp-hgc2mqIFrRnZcAT89E4UjVuOwLxJ3Pm99tXfyTxx0FP59SNPhuUoOQFUMJW7nXUMRdY9hoMKspPdmsUz1pWifJAYG_gjSKEC8qV_nASNZrmIdoRhIlXHU0wr0xPkg0pO1e7sZCV42Q"
    },
    {
      "descr": "Signature Key: ECDSA using P-256 (secp256r1) and SHA-256",
      "kty": "EC",
      "use": "sig",
      "key_ops_type": [
        "connect"
      ],
      "crv": "P-256",
      "kid": "connect_04458eea-e477-40ea-b20d-9b43a024ff86_sig_es256",
      "x5c": [
        "MIIBfTCCASOgAwIBAgIgNV5pu8XN50ogpNIXX17bPc587m76jr1//wgZUbxZO+wwCgYIKoZIzj0EAwIwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yNDA2MjEwODQ4MjBaFw0yNDA2MjMwODQ4MjhaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAARxjUn0DDMRPA5PI2n41NEigto3HZ9jyX9v8TK5kASfSXRL3PCnLxv3fwfuLdK7RZV80Qr2+1+4ApTdkVBGuxmboycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwCgYIKoZIzj0EAwIDSAAwRQIgVydKFOLW2erlzfA0bzYdgoEQz88KyorBwkBCdSrdJf4CIQCfhMaAdGEdeyFDcAef+oCM5YjjGY343sa+VMMZ+xEuLg=="
      ],
      "name": "Connect ES256 Sign Key",
      "x": "cY1J9AwzETwOTyNp-NTRIoLaNx2fY8l_b_EyuZAEn0k",
      "y": "dEvc8KcvG_d_B-4t0rtFlXzRCvb7X7gClN2RUEa7GZs",
      "exp": 1719132508905,
      "alg": "ES256"
    },
    {
      "descr": "Signature Key: ECDSA using secp256k1 and SHA-256",
      "kty": "EC",
      "use": "sig",
      "key_ops_type": [
        "connect"
      ],
      "crv": "P-256K",
      "kid": "connect_64506150-5ac7-43f3-85b9-91d66dc8d64d_sig_es256k",
      "x5c": [
        "MIIBezCCASGgAwIBAgIhAJTUFmnWINo3/y2zHjbP4Y4so4b+KvKFQimYOpCkQFd4MAoGCCqGSM49BAMCMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjQwNjIxMDg0ODIwWhcNMjQwNjIzMDg0ODI4WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEva9+dxrBuqAQZbUoH1JWP9e9Grsu4+QrFdZnIZhOFM+5iKP89N47usfwwN4wd8mWLTBSsEQ1S4OZX+f75m0JhKMnMCUwIwYDVR0lBBwwGgYIKwYBBQUHAwEGCCsGAQUFBwMCBgRVHSUAMAoGCCqGSM49BAMCA0gAMEUCIFCN2XingYoViNPd5MIo056Ft/z1nlDqIHARTtxOmLEiAiEAok795B5g+fNOawGUG8aAoL7/3XmSGTXmlZlOjx0Ulnk="
      ],
      "name": "Connect ES256K Sign Key",
      "x": "va9-dxrBuqAQZbUoH1JWP9e9Grsu4-QrFdZnIZhOFM8",
      "y": "uYij_PTeO7rH8MDeMHfJli0wUrBENUuDmV_n--ZtCYQ",
      "exp": 1719132508905,
      "alg": "ES256K"
    },
    {
      "descr": "Signature Key: ECDSA using P-384 (secp384r1) and SHA-384",
      "kty": "EC",
      "use": "sig",
      "key_ops_type": [
        "connect"
      ],
      "crv": "P-384",
      "kid": "connect_dd24642c-d981-44cc-88ad-d4964d55196b_sig_es384",
      "x5c": [
        "MIIBuTCCAUCgAwIBAgIgGiJxrwtpIUQOqTeeuFSsTSZQ/OnuMXZpRzMMh0VkrTowCgYIKoZIzj0EAwMwJDEiMCAGA1UEAwwZSmFucyBBdXRoIENBIENlcnRpZmljYXRlczAeFw0yNDA2MjEwODQ4MjBaFw0yNDA2MjMwODQ4MjhaMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAASgEXufyTMaIu+pBihzdMIp1YhtPu4T65yy2P12hfAlajYBdUy6136+8WTzzF3ektSqJ6MsTLQI4wMmO+XhOqBEcLT5e9Egp3FsixGTy/c/Jtpq0Hr38nYkDdGWQgwQhjejJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDAwNnADBkAjBLG10rRee/EUjusZy6ZccvlP1n8jZDhDB6eUdDCf9vd2z03GhHj1McS4FFClbEP+ICMGi6Xty6cdFXBl/G6enkPW8jox5fhW5lJ2Kqw00rcSmNXFQaXQQgeQUMhXBEEhafJQ=="
      ],
      "name": "Connect ES384 Sign Key",
      "x": "oBF7n8kzGiLvqQYoc3TCKdWIbT7uE-ucstj9doXwJWo2AXVMutd-vvFk88xd3pLU",
      "y": "qiejLEy0COMDJjvl4TqgRHC0-XvRIKdxbIsRk8v3PybaatB69_J2JA3RlkIMEIY3",
      "exp": 1719132508905,
      "alg": "ES384"
    },
    {
      "descr": "Signature Key: ECDSA using P-521 (secp521r1) and SHA-512",
      "kty": "EC",
      "use": "sig",
      "key_ops_type": [
        "connect"
      ],
      "crv": "P-521",
      "kid": "connect_22b8535a-e8a6-4f5e-b21d-c1c79c63b09b_sig_es512",
      "x5c": [
        "MIICBTCCAWegAwIBAgIhAIJraHUmmv3uuzpaYybev98vK02Pym3VDxqI4tc8EdZLMAoGCCqGSM49BAMEMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjQwNjIxMDg0ODIxWhcNMjQwNjIzMDg0ODI4WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQBZsJTE4x+wqEgNmz4W7A15qSz/KnIEa2qZvs8Xy3CB/uXSguHO6+OhULneopb5WZC+5wzsqqj7ED2AIr+XLSZ3sIBbDrJRCMwbnrnLP9D6MPkgHdJy4tYwMAfo0Bn8mEc+AOfJkQ/jRApQDrtU9CRUYBMdiiiCEVZtmtDziL3RAMza6qjJzAlMCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADAKBggqhkjOPQQDBAOBiwAwgYcCQSD27PoA+OnE1Y55NzmnuRKL6yJyT2ZuSCxpKTsnbV/2bAxAGKzeDII6PwD9ClQcd5sW2JjmQyqZ6VTdTnfGSX++AkIBDeehHj8NjNgnaFF3vtidTquoXUNDpGb9+8YsxfdgMo4qr5ADXEdqh7oMdtaiS84ODF+IEAAkX2fuIOkGLiKJuwQ="
      ],
      "name": "Connect ES512 Sign Key",
      "x": "AWbCUxOMfsKhIDZs-FuwNeaks_ypyBGtqmb7PF8twgf7l0oLhzuvjoVC53qKW-VmQvucM7Kqo-xA9gCK_ly0md7C",
      "y": "AWw6yUQjMG565yz_Q-jD5IB3ScuLWMDAH6NAZ_JhHPgDnyZEP40QKUA67VPQkVGATHYooghFWbZrQ84i90QDM2uq",
      "exp": 1719132508905,
      "alg": "ES512"
    },
    ...
    ...
    ...
  ]
```

### Adds new JSON Web key (JWK)

In case we need to add new key, we can use this operation id. 
To add a new key, we need to follow the schema definition. If we look at the description, 
we can see a schema definition available.

```text 
Operation ID: post-config-jwks-key
  Description: Configuration – JWK - JSON Web Key (JWK)
  Schema: JSONWebKey
```

So, let's get the schema file and update it with keys data:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema JSONWebKey > /tmp/jwk.json
```
For your information, you can obtain the format of the `JSONWebKey`
schema by running the aforementioned command without a file.
```text title="Schema Format"

name           string
descr          string
kid            string
kty            string
               enum: ['EC', 'RSA', 'OKP', 'oct']
use            string
               enum: ['sig', 'enc']
alg            string
               enum: ['RS256', 'RS384', 'RS512', 'ES256', 'ES256K', 'ES384', 'ES512', 'PS256', 'PS384', 'PS512', 'EdDSA', 'RSA1_5', 'RSA-OAEP', 'RSA-OAEP-256', 'ECDH-ES', 'ECDH-ES+A128KW', 'ECDH-ES+A192KW', 'ECDH-ES+A256KW', 'A128KW', 'A192KW', 'A256KW', 'A128GCMKW', 'A192GCMKW', 'A256GCMKW', 'PBES2-HS256+A128KW', 'PBES2-HS384+A192KW', 'PBES2-HS512+A256KW', 'dir']
exp            integer
               format: int64
crv            string
               enum: ['P-256', 'P-256K', 'P-384', 'P-521', 'Ed25519', 'Ed448']
x5c            array of string
n              string
e              string
x              string
y              string
key_ops_type   array of string
               enum: ["KeyOps{value='connect'} CONNECT", "KeyOps{value='ssa'} SSA", "KeyOps{value='all'} ALL"]
```

you can also use the following command for `JSONWebKey` schema example.
```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema-sample JSONWebKey
```

```text title="Schema Example"
{
  "name": "string",
  "descr": "string",
  "kid": "string",
  "kty": "oct",
  "use": "sig",
  "alg": "dir",
  "exp": 94,
  "crv": "Ed25519",
  "x5c": [
    "string"
  ],
  "n": "string",
  "e": "string",
  "x": "string",
  "y": "string",
  "key_ops_type": [
    "KeyOps{value='ssa'} SSA"
  ]
}
```


Let's update the json file; In our case, I have added sample data for testing purpose only.

```bash title="Input"
"name": "Connect RSA-OAEP Encryption Key 2",
"descr": "Encryption Key 2: Elliptic Curve Diffie-Hellman Ephemeral Static key agreement using Concat KDF",
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

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id post-config-jwks-key --data /tmp/jwk.json
```
You can check with `get-config-jwks` operation id for new JSON Web key

## Update / Replace JSON Web Key (JWK)

To `update / replace` any JWK configuration, let get the schema first.

```text
  Operation ID: put-config-jwks
  Description: Replaces JSON Web Keys
  Schema: WebKeysConfiguration
```

To get the schema file:

```bash title="Command"
 /opt/jans/jans-cli/config-cli.py --schema WebKeysConfiguration \
 > /tmp/path-jwk.json
```

For your information, you can obtain the format of the `WebKeysConfiguration` schema 
by running the aforementioned command without a file.

```text title="Schema Format"
keys   array
       description: 
```
you can also use the following command for `WebKeysConfiguration` schema example.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --schema-sample WebKeysConfiguration
```
```json title="Schema Example"
{
  "keys": null
}
``` 

It's a json file containing `key-value` pair. Each of these properties in the key is defined by 
the JWK specification [RFC 7517](https://datatracker.ietf.org/doc/html/rfc7517), and for algorithm-specific properties, in [RFC 7518](https://datatracker.ietf.org/doc/html/rfc7518).

#### Properties

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


```json title="Input Key"
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
  }, 
  {
     "descr": "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-256",
     "kty": "RSA",
     "e": "AQAB",
     "use": "sig",
     "key_ops_type": [
       "connect"
       ],
     "kid": "connect_e47fd367-51d6-4d17-811d-adb1f1a0b723_sig_rs256",
     "x5c": [
         "MIIDCjCCAfKgAwIBAgIhAInuAAbxL2O7H+/V0lm3bbEdCmdPdgJh+OqljtWpwi7wMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjQwNjIxMDg0ODE5WhcNMjQwNjIzMDg0ODI4WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1RCY0ZfJ/SHZ+jojCyStBjIh4upkLXuMgZLi6b7k0fQ8/oNmCBEsOKMPUubHFEHrDHZLbXj7w5gEdMPZOiLaBP8Pv0JD8IbOUtoSXEawE33LRldKiof296nlBJFsX00ipiLq3ANXuTDXtP4+pd+lvIufv1nBXpqqrN4MOsSsKuvKvxRPCg6JusHVU5hsiqbwh9y3X7sPFwqw4LJFa0U3Z4RoX7vCsS/axPPSyUi9x0zsF4S7ZGHclBReC6IipOnGyGeSEQdpuchhoZs382md+wejIf4hVtusRbEHz+wwZFhnrfh/nvHCvrWCcxBgeEntAin+ig1RlR8N4x9Ox9K01wIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBACkrKROjAIf6n1PKpXFTRQVov32EFcwhi1YSao/MZHURV2ruYXjh/S6HuvHWWofV8R6muLnD938GytS2mRjr+X7DOZj/bsDT7amd810SDvFUCh6IoPt46FeXFZMV4XyL4DQKoNxOEGGDVnD41NVC6k5GLzPwcVBwX11+b7wRfy/KoPP9aoSXjyWnNbhwClFQ9oTJYkNtaNeh2kJZ2j1UIqO51vyhUjpSM9awqV2u+ouxDKCT4h9xRcDwkOUlVXtBwn+dfJHnG6riLzT59MiPtWeo037hESUxIvJxLQP6jV97eEi/CMhSb1y6YJPFjnTBmCpzeHRp5+DNu65KPaGntB8="
         ],
     "name": "Connect RS256 Sign Key",
     "exp": 1719132508905,
     "alg": "RS256",
     "n": "1RCY0ZfJ_SHZ-jojCyStBjIh4upkLXuMgZLi6b7k0fQ8_oNmCBEsOKMPUubHFEHrDHZLbXj7w5gEdMPZOiLaBP8Pv0JD8IbOUtoSXEawE33LRldKiof296nlBJFsX00ipiLq3ANXuTDXtP4-pd-lvIufv1nBXpqqrN4MOsSsKuvKvxRPCg6JusHVU5hsiqbwh9y3X7sPFwqw4LJFa0U3Z4RoX7vCsS_axPPSyUi9x0zsF4S7ZGHclBReC6IipOnGyGeSEQdpuchhoZs382md-wejIf4hVtusRbEHz-wwZFhnrfh_nvHCvrWCcxBgeEntAin-ig1RlR8N4x9Ox9K01w"
     }
     
     
   ]
}
```

Please remember if `kid` already matched then this will be replaced otherwise a new 
key configuration will be created in the Janssen server.

Now let's put the updated data into the Janssen server.

```bash title="Command"
 /opt/jans/jans-cli/config-cli.py --operation-id put-config-jwks \
 --data /tmp/path-jwk.json
```

Please remember, This operation replaces all JWKs having in
the Janssen server with new ones. So, In this case,
if you want to keep olds JWKs, you have to put them as well in the schema file.

```json title="Output Sample"
{
  "keys": [
    {
      "kty": "RSA",
      "e": "AQAB",
      "use": "enc",
      "key_ops_type": [],
      "kid": "dd550214-7969-41b9-b919-2a0cfa36047b_enc_rsa1_5",
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhANYLiviUTmgOsf9Bf+6N/pr6H4Mis5ku1VXNj7VW/CMbMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwNTI2MjM0NzI5WhcNMjEwNTI4MjM0NzM1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArlD19ib3J2bKYr2iap1d/gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx/CfHSgdEmACCXMiG7sQt80DPM67dlbtv/pEnWrHk4fwwst83OF+HXTSi4Sd9QWhDtBvaUu8Rp8ir+x2D0RK8YNGs0prA+qGR8O/h6Y+ascz4VNbbDlbJ+w7DJYeWU1HVp/5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y/9i8kf2pmznpu5QEDimj1yxEB+G5WEYuHD/+qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk/KKB7/KT0rEOn7T2rXW9QIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAKrtlIPhvDBxBfcqS9Xy39QqE1WOPiNQooa/FVVOsCROdRZrHhFcP27HpxO9e6genQSJ6nBRaJ4ykEf0oM535Ker5jZcDWzCwPIyt+5Kc6qeacZI5FxEHRldYkSd4lF1OTzQNvGLOPKnNWnYnXwj48ZxO50lJUsRFspVbP79E6llVNOPexrZ2GOzWghyY1E74f4uGr6fzcXQk2aFaIfLusoJlvbROPTnDu68Jt+IW4WZcO4F0tl0JIcuaqSmLS6McJW0Mpmu4wqEPV6E45zRAuX0kJUkKDMzM/lYW1MZ8QaSTt/pCmlknX1+KTgb6Sf9zZJEya8AyKML/NCpc4sfn8g="
      ],
      "exp": 1622245655163,
      "alg": "RSA-OAEP",
      "n": "rlD19ib3J2bKYr2iap1d_gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx_CfHSgdEmACCXMiG7sQt80DPM67dlbtv_pEnWrHk4fwwst83OF-HXTSi4Sd9QWhDtBvaUu8Rp8ir-x2D0RK8YNGs0prA-qGR8O_h6Y-ascz4VNbbDlbJ-w7DJYeWU1HVp_5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y_9i8kf2pmznpu5QEDimj1yxEB-G5WEYuHD_-qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk_KKB7_KT0rEOn7T2rXW9Q"
    },
    {
      "descr": "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-256",
      "kty": "RSA",
      "e": "AQAB",
      "use": "sig",
      "key_ops_type": [
        "connect"
      ],
      "kid": "connect_e47fd367-51d6-4d17-811d-adb1f1a0b723_sig_rs256",
      "x5c": [
        "MIIDCjCCAfKgAwIBAgIhAInuAAbxL2O7H+/V0lm3bbEdCmdPdgJh+OqljtWpwi7wMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjQwNjIxMDg0ODE5WhcNMjQwNjIzMDg0ODI4WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1RCY0ZfJ/SHZ+jojCyStBjIh4upkLXuMgZLi6b7k0fQ8/oNmCBEsOKMPUubHFEHrDHZLbXj7w5gEdMPZOiLaBP8Pv0JD8IbOUtoSXEawE33LRldKiof296nlBJFsX00ipiLq3ANXuTDXtP4+pd+lvIufv1nBXpqqrN4MOsSsKuvKvxRPCg6JusHVU5hsiqbwh9y3X7sPFwqw4LJFa0U3Z4RoX7vCsS/axPPSyUi9x0zsF4S7ZGHclBReC6IipOnGyGeSEQdpuchhoZs382md+wejIf4hVtusRbEHz+wwZFhnrfh/nvHCvrWCcxBgeEntAin+ig1RlR8N4x9Ox9K01wIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBACkrKROjAIf6n1PKpXFTRQVov32EFcwhi1YSao/MZHURV2ruYXjh/S6HuvHWWofV8R6muLnD938GytS2mRjr+X7DOZj/bsDT7amd810SDvFUCh6IoPt46FeXFZMV4XyL4DQKoNxOEGGDVnD41NVC6k5GLzPwcVBwX11+b7wRfy/KoPP9aoSXjyWnNbhwClFQ9oTJYkNtaNeh2kJZ2j1UIqO51vyhUjpSM9awqV2u+ouxDKCT4h9xRcDwkOUlVXtBwn+dfJHnG6riLzT59MiPtWeo037hESUxIvJxLQP6jV97eEi/CMhSb1y6YJPFjnTBmCpzeHRp5+DNu65KPaGntB8="
      ],
      "name": "Connect RS256 Sign Key",
      "exp": 1719132508905,
      "alg": "RS256",
      "n": "1RCY0ZfJ_SHZ-jojCyStBjIh4upkLXuMgZLi6b7k0fQ8_oNmCBEsOKMPUubHFEHrDHZLbXj7w5gEdMPZOiLaBP8Pv0JD8IbOUtoSXEawE33LRldKiof296nlBJFsX00ipiLq3ANXuTDXtP4-pd-lvIufv1nBXpqqrN4MOsSsKuvKvxRPCg6JusHVU5hsiqbwh9y3X7sPFwqw4LJFa0U3Z4RoX7vCsS_axPPSyUi9x0zsF4S7ZGHclBReC6IipOnGyGeSEQdpuchhoZs382md-wejIf4hVtusRbEHz-wwZFhnrfh_nvHCvrWCcxBgeEntAin-ig1RlR8N4x9Ox9K01w"
    }
  ]
}
```
Now use the [Get Configurations list of JWKs](#get-configurations-list-of-jwks) to confirm the upgrade.

### Get a JSON Web Key Based on kid

With this operation-id, We can get any specific jwk matched with `kid`.
If we know the `kid`, we can simply use the below command:

```text
Operation ID: get-jwk-by-kid
Description: Get a JSON Web Key based on kid
Parameters:
kid: The unique identifier for the key [string]
```

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id get-jwk-by-kid \
--url-suffix kid:dd550214-7969-41b9-b919-2a0cfa36047b_enc_rsa1_5

```
It returns the details as below:

```json title="Sample Output"
{
  "kid": "dd550214-7969-41b9-b919-2a0cfa36047b_enc_rsa1_5",
  "kty": "RSA",
  "use": "enc",
  "alg": "RSA-OAEP",
  "exp": 1622245655163,
  "x5c": [
    "MIIDCjCCAfKgAwIBAgIhANYLiviUTmgOsf9Bf+6N/pr6H4Mis5ku1VXNj7VW/CMbMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwNTI2MjM0NzI5WhcNMjEwNTI4MjM0NzM1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArlD19ib3J2bKYr2iap1d/gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx/CfHSgdEmACCXMiG7sQt80DPM67dlbtv/pEnWrHk4fwwst83OF+HXTSi4Sd9QWhDtBvaUu8Rp8ir+x2D0RK8YNGs0prA+qGR8O/h6Y+ascz4VNbbDlbJ+w7DJYeWU1HVp/5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y/9i8kf2pmznpu5QEDimj1yxEB+G5WEYuHD/+qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk/KKB7/KT0rEOn7T2rXW9QIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAKrtlIPhvDBxBfcqS9Xy39QqE1WOPiNQooa/FVVOsCROdRZrHhFcP27HpxO9e6genQSJ6nBRaJ4ykEf0oM535Ker5jZcDWzCwPIyt+5Kc6qeacZI5FxEHRldYkSd4lF1OTzQNvGLOPKnNWnYnXwj48ZxO50lJUsRFspVbP79E6llVNOPexrZ2GOzWghyY1E74f4uGr6fzcXQk2aFaIfLusoJlvbROPTnDu68Jt+IW4WZcO4F0tl0JIcuaqSmLS6McJW0Mpmu4wqEPV6E45zRAuX0kJUkKDMzM/lYW1MZ8QaSTt/pCmlknX1+KTgb6Sf9zZJEya8AyKML/NCpc4sfn8g="
  ],
  "n": "rlD19ib3J2bKYr2iap1d_gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx_CfHSgdEmACCXMiG7sQt80DPM67dlbtv_pEnWrHk4fwwst83OF-HXTSi4Sd9QWhDtBvaUu8Rp8ir-x2D0RK8YNGs0prA-qGR8O_h6Y-ascz4VNbbDlbJ-w7DJYeWU1HVp_5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y_9i8kf2pmznpu5QEDimj1yxEB-G5WEYuHD_-qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk_KKB7_KT0rEOn7T2rXW9Q",
  "e": "AQAB"
}

```

### Patch JSON Web Key (JWK) by kid

With this operation id, we can modify JSON Web Keys partially of its properties.

```text
Operation ID: patch-config-jwks
  Description: Patches JSON Web Keys
  Schema: Array of JsonPatch
```

In this case, We are going to a test data JWK which we  [Get a JSON Web Key Based on kid](#get-a-json-web-key-based-on-kid)


The `patch-config-jwks` operation uses the [JSON Patch ](https://jsonpatch.com/#the-patch) schema to describe 
the configuration change. Refer [here](https://docs.jans.io/vreplace-janssen-version/admin/config-guide/config-tools/jans-cli/#patch-request-schema) to know more about schema.

We can see here `kid` is `new-key-test-id`. Before going to patch this key, 
let's define the schema first. In this example; We are going to change `use` from `enc` to `sig`. 
So our schema definition as below:

```json title="Input"
[
{
  "op": "replace",
  "path": "use",
  "value": "sig"
}
]
```

Now let's do the operation with below command line.

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id patch-config-jwk-kid \
--url-suffix kid:dd550214-7969-41b9-b919-2a0cfa36047b_enc_rsa1_5 --data /tmp/schema.json
```

You need to change `kid` and `data` path according to your own.
Updated Json Web Key:

```json title="Key"
{
  "kid": "dd550214-7969-41b9-b919-2a0cfa36047b_enc_rsa1_5",
  "kty": "RSA",
  "use": "sig",
  "alg": "RSA-OAEP",
  "exp": 1622245655163,
  "x5c": [
    "MIIDCjCCAfKgAwIBAgIhANYLiviUTmgOsf9Bf+6N/pr6H4Mis5ku1VXNj7VW/CMbMA0GCSqGSIb3DQEBCwUAMCQxIjAgBgNVBAMMGUphbnMgQXV0aCBDQSBDZXJ0aWZpY2F0ZXMwHhcNMjEwNTI2MjM0NzI5WhcNMjEwNTI4MjM0NzM1WjAkMSIwIAYDVQQDDBlKYW5zIEF1dGggQ0EgQ2VydGlmaWNhdGVzMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArlD19ib3J2bKYr2iap1d/gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx/CfHSgdEmACCXMiG7sQt80DPM67dlbtv/pEnWrHk4fwwst83OF+HXTSi4Sd9QWhDtBvaUu8Rp8ir+x2D0RK8YNGs0prA+qGR8O/h6Y+ascz4VNbbDlbJ+w7DJYeWU1HVp/5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y/9i8kf2pmznpu5QEDimj1yxEB+G5WEYuHD/+qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk/KKB7/KT0rEOn7T2rXW9QIDAQABoycwJTAjBgNVHSUEHDAaBggrBgEFBQcDAQYIKwYBBQUHAwIGBFUdJQAwDQYJKoZIhvcNAQELBQADggEBAKrtlIPhvDBxBfcqS9Xy39QqE1WOPiNQooa/FVVOsCROdRZrHhFcP27HpxO9e6genQSJ6nBRaJ4ykEf0oM535Ker5jZcDWzCwPIyt+5Kc6qeacZI5FxEHRldYkSd4lF1OTzQNvGLOPKnNWnYnXwj48ZxO50lJUsRFspVbP79E6llVNOPexrZ2GOzWghyY1E74f4uGr6fzcXQk2aFaIfLusoJlvbROPTnDu68Jt+IW4WZcO4F0tl0JIcuaqSmLS6McJW0Mpmu4wqEPV6E45zRAuX0kJUkKDMzM/lYW1MZ8QaSTt/pCmlknX1+KTgb6Sf9zZJEya8AyKML/NCpc4sfn8g="
  ],
  "n": "rlD19ib3J2bKYr2iap1d_gCmbXocMJTk5o7o3h9jJKXbh9pdf2gd3ZOE6wc5XwGx_CfHSgdEmACCXMiG7sQt80DPM67dlbtv_pEnWrHk4fwwst83OF-HXTSi4Sd9QWhDtBvaUu8Rp8ir-x2D0RK8YNGs0prA-qGR8O_h6Y-ascz4VNbbDlbJ-w7DJYeWU1HVp_5Lt8O5i4Q6I8KZEAytwvspF5y8m8DCrfYXF6Kz14vXgqr08hj0l0Aj4O3y_9i8kf2pmznpu5QEDimj1yxEB-G5WEYuHD_-qRTV85OXDIQJz6fgNM4kEimv7pmspcDfk_KKB7_KT0rEOn7T2rXW9Q",
  "e": "AQAB"
}

```

We see it has replaced `use` from `enc` to `sig`.

Please read about [patch method](config-tools/jans-cli/README.md#quick-patch-operations),
You can get some idea how this patch method works to modify particular properties of any task.

### Delete Json Web Key using kid

It's pretty simple to delete json web key using its `kid`. The command line is:

```bash title="Command"
/opt/jans/jans-cli/config-cli.py --operation-id delete-config-jwk-kid \
--url-suffix kid:dd550214-7969-41b9-b919-2a0cfa36047b_enc_rsa1_5
```

It will delete the jwk if it matches with the given `kid`.


##  Using Text-based UI

In Janssen, you can view the JSON web keys using
the [Text-Based UI](./config-tools/jans-tui/README.md) also.

You can start TUI using the command below:

```bash title="Command"
sudo /opt/jans/jans-cli/jans_cli_tui.py
```

### JSON Web Key (JWK) Screen

Navigate to `Auth Server` -> `Keys` to open the JSON Web Key screen as shown
in the image below.

* After clicking on `Get keys`,
You can view Key's list on this page. You cannot perform any operations in text-based UI.

* Only you can perform operations using the command line.

![image](../../assets/tui-json-web-key.png)


## Using Configuration REST API

Janssen Server Configuration REST API exposes relevant endpoints for managing
and configuring the Json Web key. Endpoint details are published in the [Swagger
document](./../reference/openapi.md).