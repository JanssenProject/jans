---
tags:
- administration
- auth-server
- session-status-list
- endpoint
---

# Session Status List

## Overview

Janssen Server provides session status list endpoint to enable the client to query session status.
Though not being part of any industry standard/specification, Janssen Server provides this endpoint to allow greater 
visibility of sessions on OP.

URL to access session status list endpoint on Janssen Server is listed in the response of Janssen Server's well-known
endpoint given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`session_status_list_endpoint` claim in the response specifies the URL for session status list endpoint. By default, endpoint looks like below:

```
https://janssen.server.host/jans-auth/restv1/session_status_list
```

More information about request and response of the revocation endpoint can be found in
the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml#/Session_Management/get-session-status-list).

## Usage

There is new concept of Session JWT. It can be requested at Authorization Endpoint by providing additional parameter `session_jwt=true`.

Sample Session JWT request

```curl
https://janssen.server.host/jans-auth/restv1/authorize?response_type=code&client_id=990593dd-ba2f-45e8-a284-947965631336&scope=openid+profile+address+email+phone+user_name+revoke_session&redirect_uri=https%3A%2F%2Fyuriyz-internal-jaguar.gluu.info%2Fjans-auth-rp%2Fhome.htm&state=db87d011-8953-45a3-87fd-9f91f723a09f&nonce=928d1314-577e-446f-9a33-f2cb76a4647e&prompt=&ui_locales=&claims_locales=&acr_values=&request_session_id=false&session_jwt=true
```

After successful authentication and authorization AS returns back also Session JWT.

**Encoded Session JWT example**
```java
eyJraWQiOiJjb25uZWN0X2FkYjg2ODg5LWRjOGYtNDJkMC04ZjdiLTVjYTc1YjhiZDgwY19zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiI5OTA1OTNkZC1iYTJmLTQ1ZTgtYTI4NC05NDc5NjU2MzEzMzYiLCJuYmYiOjE3NDcwNDk1NjEsInN0YXR1c19saXN0Ijp7ImlkeCI6MjgyNCwidXJpIjoiaHR0cHM6Ly95dXJpeXotaW50ZXJuYWwtamFndWFyLmdsdXUuaW5mby9qYW5zLWF1dGgvcmVzdHYxL3Nlc3Npb25fc3RhdHVzX2xpc3QifSwiaXNzIjoiaHR0cHM6Ly95dXJpeXotaW50ZXJuYWwtamFndWFyLmdsdXUuaW5mbyIsImV4cCI6MTc0NzEzNTk2MSwiaWF0IjoxNzQ3MDQ5NTYxLCJqdGkiOiJjYzk1MmYyNS1kODc2LTQ1NWUtYmNmZC00ZDcwNTAzNjYwZjUiLCJzaWQiOiJjY2RkZjQ0MC1iMDAzLTRkNzItODFhMS0yMTQzM2E5ZTBjZmMifQ.QZYEkqu-nFFkklRN60RDH7_bCwDyS_2d4LN9p3WpSpWBUiepea3h0m_o6wMw8qACE_qDr4b1S9QLVYEnMi_AN7fj_k3HGIGnksV8WqKkCgvz4DIozMYlXpaPs4BchZq4whgJsIfIphEskYisJ_7GjnoMDDPkhRCJHyGidNk_hay3ESHf9Eu2MnPkXUu3apNTpIOlHEUbfrqRxTMoyvg6Y2pIb7rYTYxWVJdVeJefZHt8HMyJ2WmUZAFEfRii4n6cQ2LbdwVsrQ64nZZSHOvEZIQnvL03_XdYiypJWgeiua3kTHWa72uBRJOgiavrZnPuKmzJjHZh6A0JyxB8wbHcaQ
```

**Decoded payload of Session JWT**
```json
{
  "aud": "990593dd-ba2f-45e8-a284-947965631336",
  "nbf": 1747049561,
  "status_list": {
    "idx": 2824,
    "uri": "https://janssen.server.host/jans-auth/restv1/session_status_list"
  },
  "iss": "https://janssen.server.host",
  "exp": 1747135961,
  "iat": 1747049561,
  "jti": "cc952f25-d876-455e-bcfd-4d70503660f5",
  "sid": "ccddf440-b003-4d72-81a1-21433a9e0cfc"
}
```

**Claims**
- idx - index in session status list
- exp - expiration date of the session

When RP receives Session JWT it can query Session Status List to get session status at any time.

**Sample session status list request**
```java
POST /jans-auth/restv1/session_status_list
Accept: application/statuslist+json
```

**Sample session status list response**
```json
{
  "sub": "https://janssen.server.host/jans-auth/restv1/session_status_list",
  "nbf": 1747049562,
  "status_list": {
    "bits": 2,
    "lst": "eNpjZRipgBEABeMABw"
  },
  "iss": "https://janssen.server.host",
  "exp": 1747050162,
  "iat": 1747049562,
  "ttl": 600
}
```

**lst** contains encoded status list. Status list has encoded statuses based on [Token Status List spec](https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-02.html).

**Important**
Session JWT specifies `exp` which means that OP Session must be considered expired when this time is reached independently from Session Status List response.
If Session Status List has `INVALID` (`1` - at `idx` index for 2 bits) then RP must consider OP Session as invalid. 


## Disabling The Endpoint Using Feature Flag

`Session status list` endpoint can be enabled or disable using [SESSION_STATUS_LIST feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#session_status_list).
Use [Janssen Text-based UI(TUI)](../../config-guide/config-tools/jans-tui/README.md) or [Janssen command-line interface](../../config-guide/config-tools/jans-cli/README.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or
disable `SESSION_STATUS_LIST` flag as required.

![](../../../assets/image-tui-enable-components.png)

## References

- [Token Status List spec](https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-02.html)

