---
tags:
  - administration
  - auth-server
  - oauth
  - feature
  - grant
  - jwt-grant
---

# OAuth 2.0 JWT Grant 

The [  JSON Web Token (JWT) Profile
           for OAuth 2.0 Client Authentication and Authorization Grants spec](https://datatracker.ietf.org/doc/html/rfc7523) . 

JWT Grant is identified by `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer` at Token Endpoint.           
           
AS requires client authentication during JWT Grant usage. 

**Assertion validation**
1. Client's `jwks` or `jwks_uri` is used to get keys for `assertion` signature validation
2. `aud` claim of `assertion` must contain AS `issuer` or otherwise any `aud` claim value must starts from AS `issuer` (for example if Token Endpoint address is set in `aud` claim value)
3. `iss` must not be blank. If AS `trustedSsaIssuers` configuration property is set then AS will validate `iss` value against `trustedSsaIssuers` configuration property.
4. AS validates `exp` on expiration
5. AS validates `nbf` if it's present
6. `sub` value must be not empty and not blank 

**Sample of decoded assertion payload**

```json
{
  "iss":"https://jwt-idp.example.com",
  "sub":"mailto:mike@example.com",
  "aud":"https://jwt-as.example.net",
  "nbf":1300815780,
  "exp":1300819380,
  "http://claims.example.com/member":true
}
```

**Sample request to Token Endpoint**

```
POST /token HTTP/1.1
Host: authz.example.net
Content-Type: application/x-www-form-urlencoded

grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer
&assertion=eyJhbGciOiJFUzI1NiIsImtpZCI6IjE2In0.
eyJpc3Mi[...omitted for brevity...].J9l-ZhwP[...omitted for brevity...]
```
