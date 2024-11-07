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

The Cedarling Policy Store uses a JSON file named `cedarling_store.json` to store all necessary data for evaluating policies and verifying JWT tokens. The structure includes the following key components:

1. **Cedar Schema**: The Cedar schema encoded in Base64.
2. **Cedar Policies**: The Cedar policies encoded in Base64.

3. **Trusted Issuers**: Details about the trusted issuers (see [below](#trusted-issuer-schema) for syntax).

**Note:** The `cedarling_store.json` file is only needed if the bootstrap properties: `CEDARLING_LOCK`; `CEDARLING_POLICY_STORE_URI`; and `CEDARLING_POLICY_STORE_ID` are not set to a local location. If you're fetching the policies remotely, you don't need a `cedarling_store.json` file.

## Cedarling Store Schema

```json
{
  "cedar_version": "v4.0.0",
  "policy_stores": { 
    "policy_store_id": { 
      "name": "...",
      "description": "...",
      "policies": { ... },
      "trusted_issuers": { ... },

      "schema": "..."
    },
    ...
  },
}
```

### Fields

- **cedar_version** : (*String*) The version of [Cedar policy](https://docs.cedarpolicy.com/). The protocols of this version will be followed when processing Cedar schema and policies.
- **policy_stores** : (*Object*) Object containing one or more policy IDs as keys, with their corresponding objects as the Policy Store. You can generate this using the [Agama Lab Policy Designer](https://cloud.gluu.org/agama-lab/dashboard/policy_store).
- **policy_store_id**: (*String*) The unique ID generated when creating the policy. See: [Policy Store Schema](#policy-store-schema).

Each `policy_store_id` includes:

- **name** : (*String*) The name of the policy store.
- **description** : (*String*) A brief description of the policy store.
- **policies** : (*Object*) The policies to be used for evaluation, defined in JSON format.
- **trusted_issuers** : (*Object*) Specifies the trusted issuers for JWT tokens (see [Trusted Issuers Schema](#trusted-issuers-schema) below).
- **schema** : (*String*) The Cedar schema, encoded in Base64 format.



### Policy Store Schema

Each `policy_store_id` defines the policies, trusted issuers, and Cedar schema relevant to a particular policy store instance.

Example of a `policy_store_id` entry:

```json
"policy_store_id": {
  "name": "Jans::store",
  "description": "policy description",

  "policies": { ... },
  "trusted_issuers": { ... },
  "schema": "ewogICAgInNvbWVjb21wYW55OjpzdG9...",

}
```

### Trusted Issuers Schema

Trusted issuers contain the details required to validate tokens from specific issuers.

```json
"issuer_id" : {
   "name": "Google", 

   "description": "Consumer IDP", 
   "openid_configuration_endpoint": "https://accounts.google.com/.well-known/openid-configuration",
   "access_tokens": { ... },
   "id_tokens": { ... },
   "userinfo_tokens": { ... },
   "tx_tokens": { ... }
}
```

### Token Entity Schema

Each token entity defines metadata and mappings needed to transform a tokenΓÇÖs claims into a Cedar role.

```json
{
  "type": "id_token | userinfo | access | transaction",
  "user_id": "email | sub | uid | ...",

  "role_mapping": "role | memberOf | ...",
  "claim_mapping": {  
    "claim_name": {
      "parser": "regex",
      "type": "Acme::Email",
      ...
    },
  }
}
```

#### Fields

- **type** : (*String*) Type of token (e.g., `id_token`, `userinfo`, `access`, `transaction`).
- **user_id** : (*String*) Claim used as a unique identifier for the user (e.g., `email`, `sub`, `uid`).
- **role_mapping** : (*String*) Mapping used to identify the user's role (e.g., `role`, `memberOf`).
- **claim_mapping** : (*Object*) Definitions for parsing individual claims.
- **claim_name**: (*Object*) Metadata for mapping the claim.
- **parser** : (*String*) The parser to use (e.g., `regex`, `json`).
- **type** : (*String*) Data type (e.g., `Acme::Email`).

#### Non-Normative Example of Token Entity

This example demonstrates how to parse claims in various formats using regex and json parsers.

```json
{
  "type": "id_token | userinfo | access | transaction",
  "user_id": "email | sub | uid | ...",
  "role_mapping": "role | memberOf",

  "claim_mapping": {  
    "email": {
      "parser": "regex",
      "type": "Acme::Email",
      "regex_expression": "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
      "UID": {"attr": "uid", "type": "string" },
      "DOMAIN": {"attr": "domain", "type": "string" }
    },

    "profile": {
      "parser": "regex",
      "type": "Acme::Email",
      "regex_expression": "(?x) ^(?P<SCHEME>[a-zA-Z][a-zA-Z0-9+.-]*):\/\/ (?P<HOST>[^\/:#?]+) (?::(?P<PORT>\d+))?  (?P<PATH>\/[^?#]*)? (?:\?(?P<QUERY>[^#]*))?  (?:#(?P<FRAGMENT>.*))?$",
         "SCHEME": { "attr": "scheme", "type": "string" }, 
         "HOST": { "attr": "host", "type": "string" }, 
         "PORT": { "attr": "port", "type": "string" }, 
         "PATH": { "attr": "path", "type": "string" }, 
         "QUERY": { "attr": "query", "type": "string" }, 
         "FRAGMENT": { "attr": "fragment", "type": "string" }
    },
    "dolphin": {
      "parser": "json", 
      "type": "Acme::Dolphin"
    },
  }
}
```

## Example Policy store

Below is a non-normative example of a `cedarling_store.json` file:

```json
{
  "cedar_version": "v4.0.0",
  "policy_stores": {
    "840da5d854...": {
      "name": "Jans::store",
      "description": "a simple policy example",
      "policies": { 
          "b6391dbcd7...": {

            "name": "somecompany::store",
            "description": "",
            "policies": {
                "fbd921a51b...": {
                    "description": "Admin",
                    "creation_date": "2024-11-07T07:49:11.813002",
                    "policy_content": "QGlkKCJBZG..."
                },

                "1a2dd16865...": {
                    "description": "Member",
                    "creation_date": "2024-11-07T07:50:05.520757",
                    "policy_content": "QGlkKCJNZW..."
                }
            },
          }

      },
      "trusted_issuers": {
        "id": {
          "name": "Google", 
            "description": "Consumer IDP", 

            "openid_configuration_endpoint": "https://accounts.google.com/.well-known/openid-configuration",
            "access_tokens": {
              "trusted": true,
              "principal_identifier": "jti",
              "role_mapping": ""
            },
            "id_tokens": {
              "trusted": true,
              "principal_identifier": "jti",
              "role_mapping": ""
            },
            "userinfo_tokens": {
              "trusted": true,
              "principal_identifier": "jti",

              "role_mapping": ""
            },
            "tx_tokens": {
              "trusted": true,
              "principal_identifier": "jti",
              "role_mapping": ""
            },
        }
      },
      "schema": "ewogICAgIm..."
    }
  }
}
```
