---
tags:
  - administration
  - auth-server
  - token
---

## `Userinfo` JWT

An OpenID Connect client, after obtaining an access token, can present it
at the Userinfo endpoint to obtain the Userinfo JWT token. The Userinfo response
is is described in [OpenID Core 5.4.3](https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponseValidation), and directs the developer to:

* Verify that the OP that responded was the intended OP through a TLS server certificate check

* If the Client has provided a `userinfo_encrypted_response_alg` parameter
during Registration, decrypt the UserInfo Response using the keys specified
during Registration.

* If the response was signed, the Client SHOULD validate the signature.

Below is an example of the Userinfo claims for the default Jans Admin user:

```
{
  "sub": "e25c4146-ce9d-465e-9b59-b9d959bdfe3a",
  "email": "admin@issuer.tld",
  "given_name": "Admin",
  "family_name": "User",
  "name": "Default Admin User",
  "middle_name": "Admin",
  "nickname": "Admin",
  "email_verified": true,
  "inum": "e25c4146-ce9d-465e-9b59-b9d959bdfe3a",
  "jansAdminUIRole": ["api-admin"]
}

```

## Selective disclosure

Domains can limit the claims released to a client from the Userinfo endpoint
by associating only the OpenID scopes required by that client. You can also
define new scopes, and associate any user claims with them. Note, clients still
must request the scopes they need for an access token. For example, a client
may be authorize

## Requesting individual claims

If you want to use the `claims` parameter, you will have to first enable this feature in the Auth Server properties: set `claimsParameterSupported=True`.
This is not a recommended configuration, because the claims parameter bypasses
the privacy protection of the OpenID scopes construct.

## Dynamic Scopes / Interception Script

If you need to call an API to render scopes or scope values on the fly,
you should see the [Dynamic Scopes](../developer/scripts/dynamic-scope.md) interception script.

## Userinfo formatter

There is a configuration property `userInfoConfiguration` which has a default
value of `{'dateFormatterPattern': {'birthdate':'yyyy-MM-dd'}}`.

## Language support

The default value for the configuration parameter `claimsLocalesSupported` is `['en']`. Currently, only the name and description supports localization. You
will also need to make a proper request and provide the associated values for
the claim in the database.
