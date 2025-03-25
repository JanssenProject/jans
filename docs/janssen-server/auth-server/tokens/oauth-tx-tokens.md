---
tags:
  - administration
  - auth-server
  - oauth
  - access-token
  - tx-token
  
---

# OAuth Transaction Tokens

### Background

Transaction Tokens (Txn-Tokens) enable workloads in a trusted domain
to ensure that user identity and authorization context of an external
programmatic request, such as an API invocation, are preserved and
available to all workloads that are invoked as part of processing
such a request.  Txn-Tokens also enable workloads within the trusted
domain to optionally immutably assert to downstream workloads that
they were invoked in the call chain of the request.

Txn-Tokens are short-lived, signed JWTs that assert the
identity of a user or a workload and assert an authorization context.
The authorization context provides information expected to remain
constant during the execution of a call as it passes through multiple
workloads.

Transaction Tokens [spec](https://drafts.oauth.net/oauth-transaction-tokens/draft-ietf-oauth-transaction-tokens.html)


### Transaction Token JWT

**Sample header**

```json
{
   "typ": "N_A",
   "alg": "RS256",
   "kid": "identifier-to-key"
}
```

**Sample payload**

- **iss** - a URN [RFC8141] that uniquely
      identifies the workload or the Txn-Token Service that created the
      Txn-Token.      
- **iat** - the time at which the Txn-Token was created.
- **aud** - a URN [RFC8141] that uniquely
      identifies the audience of the Txn-Token.  This MUST identify the
      trust domain in which the Txn-Token is used.
- **exp** - the time at which the Txn-Token expires.
- **txn** - the unique transaction identifier as
      defined in Section 2.2 of [RFC8417].  When used in the transaction
      token, it identifies the entire call chain.
- **purp** - a string defining the purpose or intent of this transaction      
- **sub** - the unique identifier of the user
      or workload on whose behalf the call chain is being executed.  The
      format of this claim MAY be a Subject Identifier.
- **azd** - a JSON object that contains values that remain constant in the call chain.
- **rctx** - a JSON object that describes the environmental context of the requested transaction.

```json
{
  "aud": [
    "d60f21b7-b6dd-4140-b228-e6be099bc3ce",
    "http://trusted.com"
  ],
  "rctx": {
    "req_ip": "69.151.72.123"
  },
  "purp": "tx_token",
  "sub": "3245675432",
  "iss": "https://yuriyz-adjusted-coyote.gluu.info",
  "azd": {
    "client_id": "d60f21b7-b6dd-4140-b228-e6be099bc3ce"
  },
  "txn": "080c8f19-d9ee-4a26-852d-32e4fdc6ff6c",
  "exp": 1705054542,
  "iat": 1705054362
}

```

### Transaction Token Obtain/Replace

Transaction Tokens can be obtained at Token Endpoint

**Sample request**

```text
POST /jans-auth/restv1/token HTTP/1.1
Host: yuriyz-adjusted-coyote.gluu.info
Content-Type: application/x-www-form-urlencoded

grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Atoken-exchange&audience=http%3A%2F%2Ftrusted.com&subject_token=5fb696ac-638a-4dbf-81cd-27daeb61caf9&subject_token_type=urn%3Aietf%3Aparams%3Aoauth%3Atoken-type%3Aaccess_token&requested_token_type=urn%3Aietf%3Aparams%3Aoauth%3Atoken-type%3Atxn_token&rctx=%7B%22req_ip%22%3A%2269.151.72.123%22%7D
```

**Sample response**
```json
HTTP/1.1 200
Cache-Control: no-store
Connection: Keep-Alive
Content-Length: 980
Content-Type: application/json
Date: Fri, 12 Jan 2024 10:12:40 GMT
Keep-Alive: timeout=5, max=100
Pragma: no-cache
Set-Cookie: X-Correlation-Id=a348acce-d9c1-424a-af83-e4d75f7f3368; Secure; HttpOnly;HttpOnly
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Xss-Protection: 1; mode=block

{"access_token":"eyJraWQiOiJjb25uZWN0XzBlZGMxOTIyLTk1MjAtNDFkNi1iZGMyLTk3ZjdmYWMwMzRkMl9zaWdfcnMyNTYiLCJ0eXAiOiJqd3QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOlsiZDYwZjIxYjctYjZkZC00MTQwLWIyMjgtZTZiZTA5OWJjM2NlIiwiaHR0cDovL3RydXN0ZWQuY29tIl0sInJlcV9jdHgiOnsicmVxX2lwIjoiNjkuMTUxLjcyLjEyMyJ9LCJzdWJfaWQiOiIiLCJpc3MiOiJodHRwczovL3l1cml5ei1hZGp1c3RlZC1jb3lvdGUuZ2x1dS5pbmZvIiwiYXpkIjp7ImNsaWVudF9pZCI6ImQ2MGYyMWI3LWI2ZGQtNDE0MC1iMjI4LWU2YmUwOTliYzNjZSJ9LCJ0eG4iOiIwODBjOGYxOS1kOWVlLTRhMjYtODUyZC0zMmU0ZmRjNmZmNmMiLCJleHAiOjE3MDUwNTQ1NDIsImlhdCI6MTcwNTA1NDM2Mn0.fpPFZpzxitbLw71RgO3O8uSOHJARp16H2THO4YAimJKWiczSE8-DvUAqulEW2nCNN3PRdojXWCe4ipxPSr_0ugLSFWhFKdpLmQqec_udhcV-UWiuGPLfq0XeKte60ESSvj5jgpaBNaaGS2vmFeSLdGrAx1CY2EH06OYrttOrgFFGqMhLJJ1Cpacqa0vmXnHi9gbrS-FIf2_4nNkQKMitQ-m-ec-0J02RjgkEL9zrzFwYNAoE1HEIZNFBhh7GqBejH0cXnR2tBOOz66z83SLqMTAZ-WyaMxmITHGLGLmHZOGyHdiIYME1rLXalrK58XHesMFtB-gae10Ey6w1OIgiAg","issued_token_type":"urn:ietf:params:oauth:token-type:txn_token","token_type":"N_A"}
```

Decoded transaction token JWT
```json
{
  "aud": [
    "d60f21b7-b6dd-4140-b228-e6be099bc3ce",
    "http://trusted.com"
  ],
  "rctx": {
    "req_ip": "69.151.72.123"
  },
  "purp": "tx_token",
  "sub_id": "",
  "iss": "https://yuriyz-adjusted-coyote.gluu.info",
  "azd": {
    "client_id": "d60f21b7-b6dd-4140-b228-e6be099bc3ce"
  },
  "txn": "080c8f19-d9ee-4a26-852d-32e4fdc6ff6c",
  "exp": 1705054542,
  "iat": 1705054362
}
```

Replacement looks exactly the same with one different that `subject_token` must have previously obtained
transaction token (not regular `access_token`).

- View full obtain execution log [here](../../../assets/log/tx-token-request-run-log.txt)
- View full replace execution log [here](../../../assets/log/tx-token-replace-run-log.txt)

### References

- Transaction Tokens [spec](https://drafts.oauth.net/oauth-transaction-tokens/draft-ietf-oauth-transaction-tokens.html)