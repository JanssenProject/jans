---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - JWT
---

# Cedarling JWT Flow

![](../assets/lock-cedarling-diagram-4.jpg)

### Trusted Issuer Schema

* **`name`** : String, no spaces
* **`description`** : String
* **`openid_configuration_endpoint`** : String with `https` url of `.well-known` for domain.
* **`access_tokens`** : Object with claims:
  * `trusted`: `True | False` 
* **`id_tokens`** : Object with claims: 
  * `trusted`: `True | False`
  * `principal_identifier`: the token claim used to identify the User entity (in SAML jargon it's
  the "NameID format"). This claim is optional--it may be present in the Userinfo token. Defaults to `sub`. 
  * `role_mapping`: A list of the User's roles
* **`userinfo_tokens`** :
  * `trusted`: `True | False`
  * `principal_identifier`: the token claim used to identify the User entity (in SAML jargon it's
  the "NameID format"). This claim is optional--it may be present in the Userinfo token. Defaults to `sub`. 
  * `role_mapping`: A list of the User's roles
* **`tx_tokens`** :
  * `trusted`: `True | False`

Non-normative example:
```
[
{"name": "IDP-1", 
 "description": "Acme IDP", 
 "openid_configuration_endpoint": "https://acme.com/.well-known/openid-configuration",
 "access_tokens": {"trusted": True}, 
 "id_tokens": {"trusted": True, "principal_identifier": "email"},
 "userinfo_tokens": {"trusted": True},
 "tx_tokens": {"trusted": True}
},
{IDP-2},
{IDP-3}...
]
```

### JWT Validation

Optionally, the Cedarling can validate the signatures of the JWTs for developers. To enable this, 
set the `CEDARLING_JWT_VALIDATION` bootstrap property to `True`. For testing, developers can set 
this property to `False` and submit an unsigned JWT, for example a JWT generated on 
[JWT.io](https://jwt.io). 

If token validation is enabled, on initiatilization the Cedarling downloads the public keys of 
the Trusted IDPs specified in the Cedarling policy store. The Cedarling uses the JWT `iss` 
claim to determine the right keys for validation.

In an enterprise deployment, the Cedarling can also check for JWT revocation. The Cedarling
checks the status following a mechanism described in the 
[OAuth Status Lists](https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/)
draft. Enforcing the status of tokens helps limit the damage of account takeover--i.e. to immediately 
recursively revoke all the tokens issued to an attacker. Domains may want to use Token Status also to 
implement single-transaction tokens.

Here is a summary of the ways the Cedarling may validate a JWT, depending on your bootstrap properties:

* Validate signature from Trusted Issuer 
* Check JWT status
* Discard id_token if `aud` does not match access_token `client_id` 
* Discard Userinfo token not associated with a `sub` from the id_token
* Check access token and id_token `exp` and `nbf` claims if time sent in Context


![](./assets/lock-cedarling-diagram-4.jpg)