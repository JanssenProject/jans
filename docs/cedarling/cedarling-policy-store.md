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
3. **Identity Source**: Details about the trusted issuers (see [below](#identity-source-schema) for syntax).

**Note:** The `cedarling_store.json` file is only needed if the bootstrap properties: `CEDARLING_LOCK`; `CEDARLING_POLICY_STORE_URI`; and `CEDARLING_POLICY_STORE_ID` are not set to a local location. If you're fetching the policies remotely, you don't need a `cedarling_store.json` file.

### JSON Schema

The JSON Schema accepted by cedarling is defined as follows:

```json
{
  "cedar_version": "v4.0.0",
  "policy_stores": {
      "some_unique_string_id": {
          "name": "TestPolicy",
          "description": "Once upon a time there was a Policy, that lived in a Store.",
          "policies": { ... },
          "schema": { ... },
          "trusted_issuers": { ... }
      }
  }
}
```

- **cedar_version** : (*String*) The version of [Cedar policy](https://docs.cedarpolicy.com/). The protocols of this version will be followed when processing Cedar schema and policies.
- **policies** : (*Object*) Object containing one or more policy IDs as keys, with their corresponding objects as values. See: [policies schema](#cedar-policies-schema).
- **schema** : (*String*) The JSON Cedar Schema encoded in Base64.
- **trusted_issuers** : (*Object of {unique_id => IdentitySource}(#trusted-issuer-schema)*) List of metadata for Identity Sources.

## Cedar Policies Schema

The `policies` field describes the Cedar policies that will be used in Cedarling. Currently, Cedarling only supports having one policy store -- however the policy still requires a `unique_policy_id`.

```json
  "policies": {
    "unique_policy_id": {
      "name": "Policy for Unique Id",
      "description": "simple policy example",
      "creation_date": "2024-09-20T17:22:39.996050",
      "policy_content": { ... },
    },
  }
```

- **unique_policy_id**: (*String*) A uniqe policy ID used to for tracking and auditing purposes.
- **name** : (*String*) A name for the policy
- **description** : (*String*) A brief description of cedar policy
- **creation_date** :  (*String*) Policy creating date in `YYYY-MM-DDTHH:MM:SS.ssssss`
- **policy_content** : (*String*) The Cedar Policy Encoded in Base64.

### Example of `policies`

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
      "policy_content": "oiAwjAlk1kJ2oiklD..."
    },
    "2fo1kl928Afa0sc9123scma0123891asklajsh1233ad": {
      "cedar_version": "v2.7.4",
      "name": "Policy-the-fourth",
      "description": "another policy example",
      "creation_date": "2024-09-20T18:22:39.192051",
      "policy_content": "kJW1bWl0KA0g3CAxa..."
    },
    ...
  }
```

## Trusted Issuers Schema

This record contains the information needed to validate tokens from this issuer:

```json
"trusted_issuers": {
  "some_unique_id" : {
    "name": "name_of_the_trusted_issuer",
    "description": "description for the trusted issuer",
    "openid_configuration_endpoint": "https://<trusted-issuer-hostname>/.well-known/openid-configuration",
    "access_tokens": { 
      "trusted": true,
      "principlal_identifier": "jti",
      ...
    },
    "id_tokens": { ... },
    "userinfo_tokens": { ... },
    "tx_tokens": { ... },
  },
  ...
}
```

- **name** : (*String*) The name of the trusted issuer.
- **description** : (*String*) A brief description of the trusted issuer, providing context for administrators.
- **openid_configuration_endpoint** : (*String*) The HTTPS URL for the OpenID Connect configuration endpoint (usually found at `/.well-known/openid-configuration`).
- **identity_source** : (*Object*, *optional*) Metadata related to the tokens issued by this issuer.
- **`access_tokens`, `id_tokens`, `userinfo_tokens`, `tx_tokens`** : (*Object*, *optional*) Metadata related to the tokens issued by this issuer. See schema [below](#token-entity-metadata-schema). **NOTE**: The `access_tokens` field contains the additional fields: `trusted` and `principal_identifier` in addition to the fields from it's `TokenEntityMetadata`.


### Token Entity Metadata Schema

The Token Entity Metadata Schema defines how tokens are mapped, parsed, and transformed within Cedarling. It allows you to specify how to extract user IDs, roles, and other claims from a token using customizable parsers.

```json
{
  "token_type": {
    "user_id": "<field name in token (e.g., 'email', 'sub', 'uid', etc.) or '' if not used>",
    "role_mapping": "<field for role assignment (e.g., 'role', 'memberOf', etc.) or '' if not used>",
    "claim_mapping": {
      "mapping_target": {
        "parser": "<type of parser ('regex' or 'json')>",
        "type": "<type identifier (e.g., 'Acme::Email')>",
        "...": "Additional configurations specific to the parser"
      },
    },
  }
}
```

**Fields**

- **token_type:** The type of token being processed, such as `access_tokens`, `id_tokens`, `userinfo_tokens`, and `tx_tokens`.
- **user_id (_Optional_):** The field in the token used to identify the user. If not needed, set to an empty string (`""`).
- **role_mapping (_Optional_):** Indicates which field in the token should be used for role-based access control. If not needed, set to an empty string (`""`).
- **claim_mapping:** Defines how to extract and transform specific claims from the token. Each claim can have its own parser (`regex` or `json`) and type (`Acme::Email`, `Acme::URI`, etc.).

#### Token Entity Metadata Schema Example

Below is a non-normative example of a **Token Entity Metadata** for `id_tokens`.

```json
"id_tokens": {
  "user_id": "sub",
  "role_mapping": "",
  "claim_mapping": {
    "email": {
      "parser": "regex",
      "type": "Acme::Email",
      "regex_expression": "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
    },
    "profile": {
      "parser": "regex", 
      "type": "Acme::URI", 
      "regex_expression": "(?x) ^(?P<SCHEME>[a-zA-Z][a-zA-Z0-9+.-]*):\/\/ (?P<HOST>[^\/:#?]+) (?::(?P<PORT>\d+))?  (?P<PATH>\/[^?#]*)? (?:\?(?P<QUERY>[^#]*))?  (?:#(?P<FRAGMENT>.*))?$",
      "SCHEME": {"attr": "scheme", "type":"string"}, 
      "HOST": {"attr": "host", "type":"string"}, 
      "PORT": {"attr": "port", "type":"string"}, 
      "PATH": {"attr": "path", "type":"string"}, 
      "QUERY": {"attr": "query", "type":"string"}, 
      "FRAGMENT": {"attr": "fragment", "type":"string"}
    },
    "dolphin": {
      "parser": "json",
      "type": "Acme::Dolphin"
    }
  },
}
```

**Explanation for the example:**

- `id_tokens.user_id`: Extracts the user ID from the `"sub"` field of the token.
- `id_tokens.role_mapping`: Not used, so set to an empty string (`""`).
- `claim_mapping.email`: Uses a regex parser to extract the `UID` and `DOMAIN` components from an email address.
- `claim_mapping.profile`: Uses a regex parser to extract various components of a URI (e.g., `scheme`, `host`, `path`).
- `claim_mapping.dolphin`: Uses a JSON parser to interpret the claim as a custom type (`Acme::Dolphin`).

## Example Policy store

Here is a non-normative example of a `cedarling_store.json` file:

```json
{
  "cedar_version": "v4.0.0",
  "policy_stores": {
    "b6391dbcd7..." : {
        "name": "somecompany::store",
        "description": "",
        "policies": {
            "fbd921a51b8b78b3b8af5f93e94fbdc57f3e2238b29f": {
                "description": "Admin",
                "creation_date": "2024-11-07T07:49:11.813002",
                "policy_content": "QGlkKCJBZG..."
            },

            "1a2dd16865cf220ea9807608c6648a457bdf4057c4a4": {
                "description": "Member",
                "creation_date": "2024-11-07T07:50:05.520757",
                "policy_content": "QGlkKCJNZW..."
            }
        },
        "identity_source": {
            "630d232db3...": {
                "name": "test",
                "description": "test",
                "openid_configuration_endpoint": "https://test-casa.gluu.info/.well-known/openid-configuration",
                "access_tokens": {
                  "user_id": "",
                  "role_mapping": "",
                  "claim_mapping": {},
                },
                "id_tokens": {
                  "user_id": "sub",
                  "role_mapping": "",
                  "claim_mapping": {
                    "email": {
                      "parser": "regex",
                      "type": "Acme::Email",
                      "regex_expression": "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
                    },
                    "profile": {
                      "parser": "regex", 
                      "type": "Acme::URI", 
                      "regex_expression": "(?x) ^(?P<SCHEME>[a-zA-Z][a-zA-Z0-9+.-]*):\/\/ (?P<HOST>[^\/:#?]+) (?::(?P<PORT>\d+))?  (?P<PATH>\/[^?#]*)? (?:\?(?P<QUERY>[^#]*))?  (?:#(?P<FRAGMENT>.*))?$",
                      "SCHEME": {"attr": "scheme", "type":"string"}, 
                      "HOST": {"attr": "host", "type":"string"}, 
                      "PORT": {"attr": "port", "type":"string"}, 
                      "PATH": {"attr": "path", "type":"string"}, 
                      "QUERY": {"attr": "query", "type":"string"}, 
                      "FRAGMENT": {"attr": "fragment", "type":"string"}
                    },
                    "dolphin": {
                      "parser": "json",
                      "type": "Acme::Dolphin"
                    }
                  },
                },
                "userinfo_tokens": {},
                "tx_tokens": {}
            }
        },
        "schema": "ewogICAgIn..."
    }
  },
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

### Example YAML policy store:

Here is a non-normative example of a YAML policy store:

```yaml
cedar_version: v4.0.0
name: PolicyStoreOk
description: A test policy store where everything is fine.
policies:
  840da5d85403f35ea76519ed1a18a33989f855bf1cf8:
    description: simple policy example for principal workload
    creation_date: '2024-09-20T17:22:39.996050'
    policy_content: |-
      permit(
          principal is Jans::Workload,
          action in [Jans::Action::"Update"],
          resource is Jans::Issue
      )when{
          principal.org_id == resource.org_id
      };
  444da5d85403f35ea76519ed1a18a33989f855bf1cf8:
    description: simple policy example for principal user
    creation_date: '2024-09-20T17:22:39.996050'
    policy_content: |-
      permit(
          principal is Jans::User,
          action in [Jans::Action::"Update"],
          resource is Jans::Issue
      )when{
          principal.country == resource.country
      };
schema: |-
  namespace Jans {
    type Url = {"host": String, "path": String, "protocol": String};
    entity Access_token = {"aud": String, "exp": Long, "iat": Long, "iss": TrustedIssuer, "jti": String};
    entity Issue = {"country": String, "org_id": String};
    entity Role;
    entity TrustedIssuer = {"issuer_entity_id": Url};
    entity User in [Role] = {"country": String, "email": String, "sub": String, "username": String};
    entity Workload = {"client_id": String, "iss": TrustedIssuer, "name": String, "org_id": String};
    entity id_token = {"acr": String, "amr": String, "aud": String, "exp": Long, "iat": Long, "iss": TrustedIssuer, "jti": String, "sub": String};
    action "Update" appliesTo {

      principal: [Workload, User, Role],
      resource: [Issue],
      context: {}
    };
  }
trusted_issuers:
  IDP1:
    name: 'Google'
    description: 'Consumer IDP'
    openid_configuration_endpoint: 'https://accounts.google.com/.well-known/openid-configuration'
    access_tokens:
        trusted: true
        principal_identifier: jti
    id_tokens:
        user_id: 'sub'
        role_mapping: 'role'
        claim_mapping: {}
```
