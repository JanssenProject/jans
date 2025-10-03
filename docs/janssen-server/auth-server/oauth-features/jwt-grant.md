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

**User identification in JWT Grant**

By default there is no user in assertion and thus no user in JWT Grant.
It means that User Info Endpoint can't be called with access token obtains with JWT Grant.

However it's possible to associate user with JWT Grant for this two things has to be done:
1. explicitly allow it by setting `jwtGrantAllowUserByUidInAssertion` AS configuration property to `true`
2. add `uid` claim to `assertion` payload. 

If `jwtGrantAllowUserByUidInAssertion` is `true` and `uid` claim value points to valid user AS will associate JWT Grant with this user. 

**Sample request to Token Endpoint**

```
POST /token HTTP/1.1
Host: authz.example.net
Content-Type: application/x-www-form-urlencoded

grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer
&assertion=eyJhbGciOiJFUzI1NiIsImtpZCI6IjE2In0.
eyJpc3Mi[...omitted for brevity...].J9l-ZhwP[...omitted for brevity...]
```

**Full sample request and response to Token Endpoint**

```
POST /jans-auth/restv1/token HTTP/1.1
Host: authz.example.net
Content-Type: application/x-www-form-urlencoded
Authorization: Basic MmExY2UzZjMtOTM0ZS00ZGJkLThkNDEtNzdjOTU5N2IxOTM5OjNmYzI1ZGFjLTRjMjgtNDAzZS04MTA0LWY4ZGQwYmZkMDFlYg==

grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&redirect_uri=https%3A%2F%2Fauthz.example.net%2Fjans-auth-rp%2Fhome.htm&scope=openid+profile+address+email+phone+user_name&assertion=eyJraWQiOiI4Mzk0ODg0ZS02ZGMwLTQxYWUtOWU3YS0yOWY1NDEzMGY2NTRfc2lnX3JzMjU2IiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIyYTFjZTNmMy05MzRlLTRkYmQtOGQ0MS03N2M5NTk3YjE5MzkiLCJhdWQiOiJodHRwczovL3l1cml5ei1sZWdpYmxlLWJ1bm55LmdsdXUuaW5mbyIsImlzcyI6IjJhMWNlM2YzLTkzNGUtNGRiZC04ZDQxLTc3Yzk1OTdiMTkzOSIsImV4cCI6MTc1NzU5MzEwMCwiaWF0IjoxNzU3NTkyODAwLCJqdGkiOiIyZmYxY2M1YS1mYWRjLTQ1MDgtOTkxMi1hZmE1Y2MyNjY1ODEifQ.t0bQW6olusFFcV3WVM08pTdqSH-tk_BuTSx6XROfSn72nboCAGcPxv0ow1FWYcwX05polAbFzcGLFRdSaZvunJ-OiQrHENlJxZUJNuou58bbg4mj08o9UnVMrXaEnIW19QQJC17G3b5R-y-_nKtC3P-hV1AJPxAChr0YQErAXZDoapmANKos6IbmoSQ03J2AmvzFvnpm8vFUKhpnyGyXbMPWViqojWfqMRaDgDDRyVQl1qzK4sBSTGj0CbTHN-xptNHgOBkl3fnBGYZMixw4kz50mjGHnP2r0MOfxrLvEwpuwy1FNxTSxxHjV_8d_h_HLLnqGyO4U7QaFVyC6j_2Bg


HTTP/1.1 200
Cache-Control: no-store
Connection: Keep-Alive
Content-Length: 179
Content-Type: application/json
Pragma: no-cache

{"access_token":"f7da6d49-08b4-4027-97c9-182a4ce6d6a5","issued_token_type":"urn:ietf:params:oauth:token-type:access_token","scope":"openid","token_type":"Bearer","expires_in":300}
```
