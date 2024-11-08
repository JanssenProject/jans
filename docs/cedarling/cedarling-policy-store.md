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
2. **Policies**: The Cedar policies encoded in Base64.
3. **Identity Source**: Details about the trusted issuers (see [below](#identity-source-schema) for syntax).

You could either define your own schema or generate one using the Agama Lab Policy Designer. See: [Policy and Schema Authoring](#policy-and-schema-authoring).

**Note:** The `cedarling_store.json` file is only needed if the bootstrap properties: `CEDARLING_LOCK`; `CEDARLING_POLICY_STORE_URI`; and `CEDARLING_POLICY_STORE_ID` are not set to a local location. If you're fetching the policies remotely, you don't need a `cedarling_store.json` file.

## Cedarling Store Schema

The JSON Schema accepted by cedarling is defined as follows:

```json
{
  "cedar_version": "v4.0.0",
  "name": "Jans::Store",
  "description": "Once upon a time there was a Policy, that lived in a Store.",
  "policies": { ... },
  "schema": { ... },
  "identity_source": { ... },
}
```

### Fields

- **cedar_version** : (*String*) The version of [Cedar policy](https://docs.cedarpolicy.com/). The protocols of this version will be followed when processing Cedar schema and policies.
- **name** : (*String*) The name of the policy store.
- **description** : (*String*) A brief description of policy store.
- **policies** : (*Object*) Object containing one or more policy IDs as keys, with their corresponding objects as values. See: [policies schema](#cedar-policies-schema) and [#generating]
- **schema** : (*String* | *Object*) The Cedar Schema. See [schema](#schema) below.
- **identity_source** : (*Object of {unique_id => IdentitySource}(#trusted-issuer-schema)*) List of metadata for Identity Sources.

#### `schema`

Either *String* or *Object*, where *Object* is preferred.

Where *Object* - An object with `encoding`, `content_type` and `body` keys. For example:

``` json
"schema": {
    "encoding": "none", // can be one of "none" or "base64"
    "content_type": "cedar", // can be one of "cedar" or "cedar-json"
    "body": "namespace Jans {\ntype Url = {"host": String, "path": String, "protocol": String};..."
}
```
  Where *String* - The schema in cedar-json format, encoded as Base64. For example:
``` json
"schema": "cGVybWl0KAogICAgc..."
```

Each `policy_store_id` includes:

The `policies` field describes the Cedar policies that will be used in Cedarling. Multiple policies can be defined, with each policy requiring a `unique_policy_id`.

```json
  "policies": {
    "unique_policy_id": {
      "cedar_version" : "v4.0.0",
      "name": "Policy for Unique Id",
      "description": "simple policy example",
      "creation_date": "2024-09-20T17:22:39.996050",
      "policy_content": { ... },
    },
    ...
  }
```

- **unique_policy_id**: (*String*) A uniqe policy ID used to for tracking and auditing purposes.
- **name** : (*String*) A name for the policy
- **description** : (*String*) A brief description of cedar policy
- **creation_date** :  (*String*) Policy creating date in `YYYY-MM-DDTHH:MM:SS.ssssss`
- **policy_content** : (*String* | *Object*) The Cedar Policy. See [policy_content](#policy_content) below.

##### `policy_content` Schema

Either *String* or *Object*, where *Object* is preferred.

Where *Object* - An object with `encoding`, `content_type` and `body` keys. For example:

``` json
"policy_content": {
    "encoding": "none", // can be one of "none" or "base64"
    "content_type": "cedar", // ONLY "cedar" for now due to limitations in cedar-policy crate
    "body": "permit(\n    principal is Jans::User,\n    action in [Jans::Action::\"Update\"],\n    resource is Jans::Issue\n)when{\n    principal.country == resource.country\n};"
}
```

  Where *String* - The policy in cedar format, encoded as Base64. For example:

``` json
"policy_content": "cGVybWl0KAogICAgc..."
```

##### Example of `policies`

Here is a non-normative example of the `policies` field:

```json
  "policies": {
    "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
      "cedar_version": "v2.7.4",
      "name": "Policy-the-first",
      "description": "simple policy example for principal workload",
      "creation_date": "2024-09-20T17:22:39.996050",
      "policy_content": "cGVybWl0KAogICAgc..."
    },
    "0fo1kl928Afa0sc9123scma0123891asklajsh1233ab": {
      "cedar_version": "v2.7.4",
      "name": "Policy-the-second",
      "description": "another policy example",
      "creation_date": "2024-09-20T18:22:39.192051",
      "policy_content": "kJW1bWl0KA0g3CAxa..."
    },
    "1fo1kl928Afa0sc9123scma0123891asklajsh1233ac": {
      "cedar_version": "v2.7.4",
      "name": "Policy-the-third",
      "description": "another policy example",
      "creation_date": "2024-09-20T18:22:39.192051",
      "policy_content": {
        "encoding": "none",
        "content_type" : "cedar",
        "body": "permit(...) where {...}"
      }
    },
    "2fo1kl928Afa0sc9123scma0123891asklajsh1233ad": {
      "cedar_version": "v2.7.4",
      "name": "Policy-the-fourth",
      "description": "another policy example",
      "creation_date": "2024-09-20T18:22:39.192051",
      "policy_content": {
        "encoding": "base64",
        "content_type" : "cedar",
        "body": "kJW1bWl0KA0g3CAxa..."
      }
    },
    ...
  }
```

#### Identity Source Schema

This record contains the information needed to validate tokens from this issuer:

```json
  "identity_source": {
    "some_unique_id" : {
      "name": "name_of_the_trusted_issuer",
      "description": "description for the trusted issuer",
      "openid_configuration_endpoint": "https://<trusted-issuer-hostname>/.well-known/openid-configuration",
      "access_tokens": { ... },
      "id_tokens": { ... },
      "userinfo_tokens": { ... },
      "tx_tokens": { ... },
    }
    ...
  }
```

- **some_unique_id** : (*String*) A unique ID assigned to the Identity Source.
- **name** : (*String*) The name of the trusted issuer.
- **description** : (*String*) A brief description of the trusted issuer, providing context for administrators.
- **openid_configuration_endpoint** : (*String*) The HTTPS URL for the OpenID Connect configuration endpoint (usually found at `/.well-known/openid-configuration`).
- **`access_tokens`, `id_tokens`, `userinfo_tokens`, `tx_tokens`** : (*Object*) Metadata for each toke type. See: [Token Metadata Schema](#token-metadata-schema).

##### Token Metadata Schema

```json
{
  "trusted": true|false
  "principal_identifier": "some_user123",
  "role_mapping": "role",
}
```

- **trusted** : (Boolean) The type of token
- **principal_id** : (String) The claim used to create the Cedar entity associated with this token.
- **role_mapping** : (String, *optional*) The claim used to create a role for the token. The default value of `role_mapping` is `role`. The claim can be string or array of string.

##### Token Entity Metadata Schema

Each token entity defines metadata and mappings needed to transform a tokenΓÇÖs claims into a Cedar role.

```json
{
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

###### Fields

- **type** : (*String*) Type of token (e.g., `id_token`, `userinfo`, `access`, `transaction`).
- **user_id** : (*String*) Claim used as a unique identifier for the user (e.g., `email`, `sub`, `uid`).
- **role_mapping** : (*String*) Mapping used to identify the user's role (e.g., `role`, `memberOf`).
- **claim_mapping** : (*Object*) Definitions for parsing individual claims.
- **claim_name**: (*Object*) Metadata for mapping the claim.
- **parser** : (*String*) The parser to use (e.g., `regex`, `json`).
- **type** : (*String*) Data type (e.g., `Acme::Email`).

###### Non-Normative Example of Token Entity

This example demonstrates how to parse claims in various formats using regex and json parsers.

```json
{
  "user_id": "aud",
  "role_mapping": "role",
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
        "user_id": "",
        "role_mapping": "",
        "claim_mapping": {}
      },
      "id_tokens": {
        "user_id": "sub",
        "role_mapping": "role",
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
      },
      "userinfo_tokens": {
        "user_id": "",
        "role_mapping": "",
        "claim_mapping": {}
      },
      "tx_tokens": {
        "user_id": "",
        "role_mapping": "",
        "claim_mapping": {}
      },
    }
  },
  "schema": "ewogICAgIm..."
}
```

## Policy and Schema Authoring

You can hand create your Cedar policies and schema in
[Visual Studio](https://marketplace.visualstudio.com/items?itemName=cedar-policy.vscode-cedar).
Make sure you run the cedar command line tool to validate both your schema and policies.

The easiest way to author your policy store is to use the Policy Designer in
[Agama Lab](https://cloud.gluu.org/agama-lab). This tool helps you define the policies, schema and
trusted IDPs and to publish a policy store to a Github repository.

### Minimum supported `cedar-policy schema`

Here is example of a minimum supported `cedar-policy schema`:

```cedar-policy_schema
namespace Jans {
entity id_token = {"aud": String,"iss": String, "sub": String};
entity Role;
entity User in [Role] = {};
entity Access_token = {"aud": String,"iss": String, "jti": String, "client_id": String};
entity Workload = {};

entity Issue = {};
action "Update" appliesTo {
  principal: [Workload, User, Role],
  resource: [Issue],
  context: {}
};
}
```

You can extend all of this entites and add your own.

Mandatory entities is: `id_token`, `Role`, `User`, `Access_token`, `Workload`.
`Issue` entity and `Update` action are optinal. Is created for example, you can create others for your needs.

`Context` and `Resource` entities you can pass during authorization request and next entites creating based on the JWT tokens:

- `id_token` - entity based on the `id` JWT token fields.
  - ID for entity based in `jti` field.

- `Role` - define role of user.
  - Mapping defined in `Token Metadata Schema`.
  - Claim in JWT usually is string or array of string.
  - If many roles present, the `cedarling` will try each to find first permit case.

- `User` - entity based on the `id`and `userinfo` JWT token fields.
  - If `id`and `userinfo` JWT token fields has different `sub` value, `userinfo` JWT token will be ignored.
  - ID for entity based in `sub` field. (will be changed in future)

- `Access_token` - entity based on the `access` JWT token fields.
  - ID for entity based in `jti` field.

- `Workload` - entity based on the `access` JWT token fields.
  - ID for entity based in `client_id` field.

## Note on test fixtures

You will notice that test fixtures in the cedarling code base are quite often in yaml rather than in json.

yaml is intended for **cedarling internal use only**.

The rationale is that yaml has excellent support for embedded, indented, multiline string values. That is far easier to read than base64 encoded json strings, and is beneficial for debugging and validation that test cases are correct.
