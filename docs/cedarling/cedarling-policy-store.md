---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - policy store
---

# Cedarling Policy Store

By convention, the filename is `cedarling_store.json`. It contains all the data the 
Cedarling needs to evaluate policies and verify JWT tokens:

1. Cedar Schema - Base64 encoded human format
2. Cedar Policies - Base64 encoded human format
3. Trusted Issuers - See below syntax

The JSON schema looks like this:

```
{
    "some_unique_identifier": {
        "name": "...",
        "description": "...",
        "policies": {},
        "identity_source": {},
        "schema": "..."
    }
}
```

## Trusted Issuer Schema

- **name** : (String, no spaces) The name of the trusted issuer.
- **description** : (String) A brief description of the issuer, providing context for administrators.
- **openid_configuration_endpoint** : (String) The HTTPS URL for the OpenID Connect configuration endpoint (usually found under /.well-known/openid-configuration).
- **access_tokens** : (Object with claims) 
- **trusted**: (True | False) Indicates whether the issuer's access token are trusted.
- **id_tokens** : (Object with claims) 
- **trusted**: (True | False) Indicates whether the issuer's id_token are trusted.
- **principal_identifier**: the token claim used to identify the User entity (in SAML jargon it's the "NameID format"). This claim is optional--it may be present in the Userinfo token. Defaults to sub.
- **role_mapping**: A list of the User's roles
- **userinfo_tokens** :
- **trusted**: (True | False) Indicates whether the issuer's userinfo_tokens are trusted.
- **principal_identifier**: the token claim used to identify the User entity (in SAML jargon it's the "NameID format"). This claim is optional--it may be present in the Userinfo token. Defaults to sub.
- **role_mapping**: A list of the User's roles
- **tx_tokens** : (Object with claims)
- **trusted**: (True | False)

```
[
{"name": "Google", 
 "Description": "Consumer IDP", 
 "openid_configuration_endpoint": "https://accounts.google.com/.well-known/openid-configuration",
 "access_tokens": {"trusted": True}, 
 "id_tokens": {"trusted":True, "principal_identifier": "email"},
 "userinfo_tokens": {"trusted": True, "role_mapping": "role"},  
 "tx_tokens": {"trusted": True}
},
{IDP-2},
{IDP-3}...
]
```

## Policy and Schema Authoring

You can hand create your Cedar policies and schema in 
[Visual Studio](https://marketplace.visualstudio.com/items?itemName=cedar-policy.vscode-cedar). 
Make sure you run the cedar command line tool to validate both your schema and policies. 

The easiest way to author your policy store is to use the Policy Designer in 
[Agama Lab](https://cloud.gluu.org/agama-lab). This tool helps you define the policies, schema and
trusted IDPs and to publish a policy store to a Github repository.
