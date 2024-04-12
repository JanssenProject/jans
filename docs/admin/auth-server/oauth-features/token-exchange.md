---
tags:
  - administration
  - auth-server
  - oauth
  - token
  - toke-exchange
---

# OAuth 2.0 Token Exchange (RFC 8693)

Token Exchange  is an extension to OAuth 2.0 for implementing use cases when one token needs to be exchanged for another token.
The exchange occurs at the Token Endpoint of an authorization server, with a special `grant_type=urn:ietf:params:oauth:grant-type:token-exchange`.

The client makes a token exchange request to the Token Endpoint with an extension grant type using the HTTP POST method.
HTTP request entity-body should be `application/x-www-form-urlencoded` with a character encoding of UTF-8.

`subject_token_type` parameter must have `urn:ietf:params:oauth:token-type:id_token` value. It means that
`subject_token` must be valid `id_token` issued by this AS. 

"Token Exchange" interception script allows to customize logic including 
`subject_token_type`, `subject_token` parameters validation and  response of token exchange.    

**Sample request**

```curl
POST /as/token.oauth2 HTTP/1.1
Host: as.example.com
Authorization: Basic cnMwODpsb25nLXNlY3VyZS1yYW5kb20tc2VjcmV0
Content-Type: application/x-www-form-urlencoded

grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Atoken-exchange
 &resource=https%3A%2F%2Fbackend.example.com%2Fapi
 &subject_token=accVkjcJyb4BWCxGsndESCJQbdFMogUC5PbRDqceLTC
 &subject_token_type=urn%3Aietf%3Aparams%3Aoauth%3Atoken-type%3Aaccess_token
```

**Sample response**
```curl
HTTP/1.1 200 OK
 Content-Type: application/json
 Cache-Control: no-cache, no-store

 {
  "access_token":"eyJhbGciOiJFUzI1NiIsImtpZCI6IjllciJ9.eyJhdWQiOiJo
    dHRwczovL2JhY2tlbmQuZXhhbXBsZS5jb20iLCJpc3MiOiJodHRwczovL2FzLmV
    4YW1wbGUuY29tIiwiZXhwIjoxNDQxOTE3NTkzLCJpYXQiOjE0NDE5MTc1MzMsIn
    N1YiI6ImJkY0BleGFtcGxlLmNvbSIsInNjb3BlIjoiYXBpIn0.40y3ZgQedw6rx
    f59WlwHDD9jryFOr0_Wh3CGozQBihNBhnXEQgU85AI9x3KmsPottVMLPIWvmDCM
    y5-kdXjwhw",
  "issued_token_type":
      "urn:ietf:params:oauth:token-type:access_token",
  "token_type":"Bearer",
  "expires_in":60
 }
```

AS supports following token types in response:

- urn:ietf:params:oauth:token-type:access_token
- urn:ietf:params:oauth:token-type:refresh_token
- urn:ietf:params:oauth:token-type:id_token

## References

- [RFC 8693 OAuth 2.0 Token Exchange](https://www.rfc-editor.org/rfc/rfc8693)