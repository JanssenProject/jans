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
    "policystore_id": "...",
    "policies": {...},
    "schema": "...",
    "trusted_issuers": [],
    "cedar_version": "v2.4.7"
}

```

- **policystore_id** : (String, no spaces) The unique identifier for the policy store.
- **policies** : (Json) Json object containing one or more policy_id as key and policy object as value
- **schema** : (String) Base64 encoded cedar schema
- **trusted_issuers** : (List) List of Trusted Issuer metadata
- **cedar_version** : (String) The version of [Cedar policy](https://docs.cedarpolicy.com/). The protocols of this version will be followed when processing Cedar schema and policies.

## Trusted Issuer Schema

This record contains the information needed to validate tokens from this issuer:

```
"trusted_issuers": [
    {
         "name": "name_of_the_trusted_issuer", 
         "description": "description_of_the_trusted_issuer", 
         "openid_configuration_endpoint": "https://<trusted-issuer-hostname>/.well-known/openid-configuration",
         "token_metadata": [token1_entity_schema, token2_entity_schema, ... ]
    }
]
```

- **name** : (String) Name of the trusted issuer
- **description** : (String) Short description of the trusted issuer
- **openid_configuration_endpoint** : The HTTPS URL for the OpenID Connect configuration endpoint (usually found under /.well-known/openid-configuration).
- **token_metadata** : (List) List of token metadata

## Token Entity Schema

```
{ "type": "access"
  "user_id": "..."},                   
  "role_mapping": "..."},          
}
```

- **type** : (String) The type of token whether `access`, `id_token`, `userinfo` or `transaction`.
- **user_id** : (String) For id_token or userinfo tokens, the `user_id` refers to the claim used to identify the person entity. For example, in an id_token, the "user_id" could be the sub (subject) or email claim.
- **role_mapping** : (String) The role_mapping refers to the token claim used to get role values. The default value of `role_mapping` is `role`.

### Sample Policy store

```
{
  "policystore_id": "8b805e22fdd39f3dd33a13d9fb446d8e6314153ca997",
  "name": "gluustore",
  "description": "gluu",
  "policies": {
    "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
      "description": "simple policy example",
      "creation_date": "2024-09-20T17:22:39.996050",
      "policy_content": "cGVybWl0KAogICAgc..."
    }
  },
  "trusted_issuers": [
    {
      "name": "Google",
      "Description": "Consumer IDP",
      "openid_configuration_endpoint": "https://accounts.google.com/.well-known/openid-configuration",
      "token_metadata": [
        {
          "type": "id_token,
          "user_id": "email",
          "role_mapping": "role"
        }
      ]
    }
  ],
  "schema": "ewogICAgIkphbnMiOiB...",
  "cedar_version": "v2.4.7"
}
```


## Policy and Schema Authoring

You can hand create your Cedar policies and schema in 
[Visual Studio](https://marketplace.visualstudio.com/items?itemName=cedar-policy.vscode-cedar). 
Make sure you run the cedar command line tool to validate both your schema and policies. 

The easiest way to author your policy store is to use the Policy Designer in 
[Agama Lab](https://cloud.gluu.org/agama-lab). This tool helps you define the policies, schema and
trusted IDPs and to publish a policy store to a Github repository.
