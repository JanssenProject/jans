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

The Policy Store provides:

1. **Cedar Schema**: The Cedar schema encoded in Base64.
2. **Cedar Policies**: The Cedar policies encoded in Base64.
3. **Trusted Issuers**: Details about the trusted issuers (see [below](#trusted-issuers-schema) for syntax).
4. **Default Entities**: Optional static entities that are loaded at startup and available for all policy evaluations (see [below](#default-entities)).

For a comprehensive JSON schema defining the structure of the policy store, see: [policy_store_schema.json](https://raw.githubusercontent.com/JanssenProject/jans/refs/heads/main/jans-cedarling/schema/policy_store_schema.json). You test the validity of your policy store with this schema at [https://www.jsonschemavalidator.net/].

**Note:** The `cedarling_store.json` file is only needed if the bootstrap properties: `CEDARLING_LOCK`; `CEDARLING_POLICY_STORE_URI`; and `CEDARLING_POLICY_STORE_ID` are not set to a local location. If you're fetching the policies remotely, you don't need a `cedarling_store.json` file.

## JSON Schema

The JSON Schema accepted by Cedarling is defined as follows:

```json
{
  "cedar_version": "v4.0.0",
  "policy_stores": {
      "some_unique_string_id": {
          "name": "DoveCRM Policy Store",
          "description": "DoveCRM policies, schema, and trusted JWT issuers.",
          "policies": { ... },
          "schema": { ... },
          "trusted_issuers": { ... },
          "default_entities": { ... }
      }
  }
}
```

- **cedar_version** : (*String*) The version of [Cedar policy](https://docs.cedarpolicy.com/). The protocols of this version will be followed when processing Cedar schema and policies.
- **policies** : (*Object*) Base64 encoded object containing one or more policy IDs as keys, with their corresponding objects as values. See: [policies schema](#cedar-policies-schema).
- **schema** : (*String* | *Object*) Base64 encoded JSON Object. See [schema](#schema) below.
- **trusted_issuers** : (*Object of {unique_id => IdentitySource}(#trusted-issuer-schema)*) List of metadata for Identity Sources.
- **default_entities** : (*Object*) Optional map of entity IDs to encoded/default entity payloads. See [Default Entities](#default-entities).

### `schema`

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

## Default Entities

Default entities allow you to preload static entity records that are available to every authorization request. Cedarling loads these at startup alongside policies and schema.

Format:

- Each entry is a simple key-value pair where the key is the entity ID and the value is a Base64-encoded JSON object representing the entity payload.

```json
"default_entities": {
  "1694c954f8d9": "eyJlbnRpdHlfaWQiOiIxNjk0Yzk1NGY4ZDkiLCJvIjoiQWNtZSBEb2xwaGlucyBEaXZpc2lvbiIsIm9yZ19pZCI6IjEwMDEyOSIsImRvbWFpbiI6ImFjbWUtZG9scGhpbi5zZWEiLCJyZWdpb25zIjpbIkF0bGFudGljIiwiUGFjaWZpYyIsIkluZGlhbiJdfQ==",
  "74d109b20248": "eyJlbnRpdHlfaWQiOiI3NGQxMDliMjAyNDgiLCJkZXNjcmlwdGlvbiI6IjIwMjUgUHJpY2UgTGlzdCIsInByb2R1Y3RzIjp7IjE1MDIwIjo5Ljk1LCIxNTA1MCI6MTQuOTV9LCJzZXJ2aWNlcyI6eyI1MTAwMSI6OTkuMCwiNTEwMjAiOjI5OS4wfX0="
}
```

Example of the decoded payloads for two default entities:

```json
{
  "1694c954f8d9": {
    "entity_id": "1694c954f8d9",
    "o": "Acme Dolphins Division",
    "org_id": "100129",
    "domain": "acme-dolphin.sea",
    "regions": ["Atlantic", "Pacific", "Indian"]
  },
  "74d109b20248": {
    "entity_id": "74d109b20248",
    "description": "2025 Price List",
    "products": {"15020": 9.95, "15050": 14.95},
    "services": {"51001": 99.0, "51020": 299.0}
  }
}
```

Notes:

- Cedar policies can reference these entities directly by type and ID, just like any other entity present during evaluation.
- A common use case is defining an organization entity and writing policies that compare runtime attributes (e.g., `resource.org_id`) to attributes on the default entity.

### Entity Conflict Resolution

When request entities have the same UID as default entities, Cedarling automatically resolves conflicts by giving **request entities precedence** over default entities. This ensures that runtime data can override static configurations while maintaining consistency.

Example: If a resource entity with UID `"org1"` is passed in an authorization request, and a default entity with the same UID exists, the resource entity's attributes will be used instead of the default entity's attributes.

## Cedar Policies Schema

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

- **unique_policy_id**: (*String*) A unique policy ID used to for tracking and auditing purposes.
- **name** : (*String*) A name for the policy
- **description** : (*String*) A brief description of cedar policy
- **creation_date** : (*String*) Policy creating date in `YYYY-MM-DDTHH:MM:SS.ssssss`
- **policy_content** : (*String* | *Object*) The Cedar Policy. See [policy_content](#policy_content) below.

### `policy_content`

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

## Trusted Issuers Schema

This record contains the information needed to validate tokens from this issuer:

```json
"trusted_issuers": {
  "trusted_issuer_id" : {
    "name": "name_of_the_trusted_issuer",
    "description": "description for the trusted issuer",
    "openid_configuration_endpoint": "https://<trusted-issuer-hostname>/.well-known/openid-configuration",
    "token_metadata": {
      "access_tokens": {
        "trusted": true,
        "token_id": "jti",
        ...
      },
      "id_tokens": { ... },
      "userinfo_tokens": { ... },
      "tx_tokens": { ... },
      ...
    }
  },
  "another_issuer_id": {
    ...
  }
  ...
}
```

- **name** : (*String*) The name of the trusted issuer.
- **description** : (*String*) A brief description of the trusted issuer, providing context for administrators.
- **openid_configuration_endpoint** : (*String*) The HTTPS URL for the OpenID Connect configuration endpoint (usually found at `/.well-known/openid-configuration`).
- **trusted_issuer_id** : (*Object*, *optional*) Metadata related to a particular issuer. You can add as many trusted issuers you want. Furthermore, the name this object is what will be used as the entity ID of the [Trusted Issuer](./cedarling-entities.md#trusted-issuer) that Cedarling automatically creates at startup.
- **token_metadata** : (*Object*, *optional*) Tokens metadata in a map of *token name* -> *token metadata*. See  [Token Metadata Schema](#token-metadata-schema).

### Token Metadata Schema

The Token Entity Metadata Schema defines how tokens are mapped, parsed, and transformed within Cedarling. It allows you to specify how to extract user IDs, roles, and other claims from a token using customizable parsers.

```json
{
  "trusted": true,
  "entity_type_name": "Jans::Access_token",
  "token_id": "jti",
  "workload_id": "aud | client_id",
  "user_id": "sub | uid | email",
  "principal_mapping": ["Jans::Workload"],
  "role_mapping": "role | group | memberOf",
  "required_claims": ["iss", "exp", "some_custom_claim", ...],
  "claim_mapping": {
    "mapping_target": {
      "parser": "<type of parser ('regex' or 'json')>",
      "type": "<type identifier (e.g., 'Acme::Email')>",
      "...": "Additional configurations specific to the parser"
    },
  },
}
```

- `"trusted"` (bool, Default: true): Allows toggling configuration without deleting the object.
- `"entity_type_name"` (string, required): The type name of the Cedar Entity that will be created from the token; for example: "Jans::Access_token".
- `"principal_mapping"` (array[string], Default: []): Describes where references of the created token entity should be included.
- `"token_id"` (string, Default: "jti"): The JWT claim that will be used as the ID for the Token Entity.
- `"user_id"` (string, Default: "sub"): The JWT claim that will be used as the ID for the User Entity.
- `"role_mapping"` (string, Default: "role"): The JWT claim that will be used as the ID for any Role Entities. For more info, see: [role mapping](#role-mapping).
- `"workload_id"` (string, Default: "aud"): The JWT claim that will be used as the ID for the Workload Entity.
- `"required_claims"` (array[string], Default: []): A list of claims that must be present within the JWT to be considered valid. Additionally, if a required claim is a registered claim name under RFC 7519 Section 4.1, the claim will also be validated.
- `"claim_mapping"` (object, Default: {}): Applies a transformation on a JWT's claim to types defined in the Cedar schema before creating the Token Entity's attribute. This enables creating a Cedar Type that has multiple attributes from a single JWT claim. For more info, see [claim mapping](#claim-mapping).

#### Role mapping

- **role_mapping**: (String OR Array of String, *Optional*) Indicates which field in the token should be used for role-based access control. If not needed, set to an empty string (`""`).

You can include a `role_mapping` in each token, all of them will be executed by Cedarling.
If none `role_mapping` defined the `Cedarling` will try to find role in `userinfo` token in field `role`.

#### Claim mapping

- **claim_mapping:** Defines how to extract and transform specific claims from the token. Each claim can have its own parser (`regex` or `json`) and type (`Acme::email_address`, `Acme::Url`, etc.).

In regex attribute mapping like `"UID": {"attr": "uid", "type":"String"},`, `type` field can contain possible variants:

- `String` - to string without transformation,
- `Number` -  parse string to float64 (JSON number) if error returns default value
- `Boolean` - if string NOT empty map to true else false

Note, use of regex **named capture groups** which is more readable by referring to parts of a regex match by descriptive names rather than numbers. For example, `(?P<name>...)` defines a named capture group where name is the identifier, and ... is the regex pattern for what you want to capture.

When you use `(?x)` modifier in regexp, ensure that you escaped character `#` => `\#`.

example of mapping `email_address` and `Url`:

```json
...
  "claim_mapping": {
    "email": {
      "parser": "regex",
      "type": "Test::email_address",
      "regex_expression": "^(?P<UID>[^@]+)@(?P<DOMAIN>.+)$",
      "UID": {
        "attr": "uid",
        "type": "String"
      },
      "DOMAIN": {
        "attr": "domain",
        "type": "String"
      }
    },
    "profile": {
      "parser": "regex",
      "type": "Test::Url",
      "regex_expression": "(?x) ^(?P<SCHEME>[a-zA-Z][a-zA-Z0-9+.-]*):\\/\\/(?P<HOST>[^\\/:\\#?]+)(?::(?<PORT>\\d+))?(?P<PATH>\\/[^?\\#]*)?(?:\\?(?P<QUERY>[^\\#]*))?(?:(?P<FRAGMENT>.*))?",
      "SCHEME": {
        "attr": "scheme",
        "type": "String"
      },
      "HOST": {
        "attr": "host",
        "type": "String"
      },
      "PORT": {
        "attr": "port",
        "type": "String"
      },
      "PATH": {
        "attr": "path",
        "type": "String"
      },
      "QUERY": {
        "attr": "query",
        "type": "String"
      },
      "FRAGMENT": {
        "attr": "fragment",
        "type": "String"
      }
    }
  }
...
```

## Example Policy store

Here is a non-normative example of a `cedarling_store.json` file:

```json
{
    "cedar_version": "v4.0.0",
    "policies": {
        "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
            "description": "simple policy example",
            "creation_date": "2024-09-20T17:22:39.996050",
            "policy_content": "cedar_policy_encoded_in_base64"
        }
    },
    "schema": "schema_encoded_in_base64",
    "trusted_issuers": {
        "08c6c18a654f492adcf3fe069d729b4d9e6bf82605cb" : {
            "name": "Google",
            "description": "Consumer IDP",
            "openid_configuration_endpoint": "https://accounts.google.com/.well-known/openid-configuration",
            "token_metadata": {
              "access_token": {
                "trusted": true,
                "entity_type_name": "Jans::Access_token",
                "token_id": "jti"
              },
              "id_token": {
                "trusted": true,
                "entity_type_name": "Jans::Id_token",
                "token_id": "jti",
                "role_mapping": "role"
              },
              "userinfo_token": {
                "trusted": true,
                "entity_type_name": "Jans::Userinfo_token",
                "token_id": "jti",
                "role_mapping": "role"
              }
            }
        }
    },
    "default_entities": {
        "1694c954f8d9": "eyJlbnRpdHlfaWQiOiIxNjk0Yzk1NGY4ZDkiLCJvIjoiQWNtZSBEb2xwaGlucyBEaXZpc2lvbiIsIm9yZ19pZCI6IjEwMDEyOSIsImRvbWFpbiI6ImFjbWUtZG9scGhpbi5zZWEiLCJyZWdpb25zIjpbIkF0bGFudGljIiwiUGFjaWZpYyIsIkluZGlhbiJdfQ==",
        "74d109b20248": "eyJlbnRpdHlfaWQiOiI3NGQxMDliMjAyNDgiLCJkZXNjcmlwdGlvbiI6IjIwMjUgUHJpY2UgTGlzdCIsInByb2R1Y3RzIjp7IjE1MDIwIjo5Ljk1LCIxNTA1MCI6MTQuOTV9LCJzZXJ2aWNlcyI6eyI1MTAwMSI6OTkuMCwiNTEwMjAiOjI5OS4wfX0="
    }
}
```

## Multi-Issuer Token Entities

When using the `authorize_multi_issuer` method, Cedarling creates token entities dynamically without requiring predefined User/Workload principals. This section describes how these token entities are structured and made available in policies.

### Token Entity Structure

Each validated token in a multi-issuer authorization request becomes a Cedar entity with the following structure:

```cedar
entity Token = {
  "token_type": String,        // e.g., "Jans::Access_Token", "Acme::DolphinToken"
  "jti": String,               // Unique token identifier
  "issuer": String,            // JWT issuer claim
  "exp": Long,                 // Token expiration timestamp
  "validated_at": Long         // Timestamp when token was validated
} tags String;
```

**Important**: All JWT claims are stored as **entity tags** on the Token entity. By default, claims are stored as **Sets of Strings** to provide a consistent interface regardless of whether a claim has zero, one, or multiple values.

### Accessing Token Claims in Policies

JWT claims are accessed using Cedar's tag syntax:

```cedar
// Check if token has a claim
context.tokens.acme_access_token.hasTag("scope")

// Get claim value (returns a Set)
context.tokens.acme_access_token.getTag("scope")

// Check if Set contains specific value
context.tokens.acme_access_token.getTag("scope").contains("read:profile")
```

**Examples of claim access**:

```cedar
// Single-valued claim (stored as single-element Set)
context.tokens.acme_access_token.hasTag("sub") &&
context.tokens.acme_access_token.getTag("sub").contains("user123")

// Multi-valued claim (stored as Set)
context.tokens.acme_access_token.hasTag("scope") &&
context.tokens.acme_access_token.getTag("scope").contains("read:profile")

// Audience claim (typically multi-valued)
context.tokens.acme_access_token.hasTag("aud") &&
context.tokens.acme_access_token.getTag("aud").contains("my-client-id")

// Custom claim
context.tokens.dolphin_acme_dolphin_token.hasTag("waiver") &&
context.tokens.dolphin_acme_dolphin_token.getTag("waiver").contains("signed")
```

### Token Collection in Context

Validated tokens are organized into a `tokens` collection in the Cedar context using predictable naming:

```cedar
entity Tokens = {
  // Format: {issuer_name}_{token_type}
  "acme_access_token": Token,           // Access token from Acme
  "acme_id_token": Token,               // ID token from Acme
  "google_id_token": Token,             // ID token from Google
  "dolphin_acme_dolphin_token": Token,  // Custom token type from Dolphin
  "total_token_count": Long,            // Number of validated tokens
};
```

The naming follows this pattern:
- **Issuer name**: From trusted issuer metadata `name` field, or hostname from JWT `iss` claim
- **Token type**: Extracted from the `mapping` field (e.g., "Jans::Access_Token" → "access_token")
- Both converted to lowercase with underscores replacing special characters

### Schema Support

#### Without Schema (Default)
When no Cedar schema is defined for Token entities:
- All claims are stored as `Set<String>`
- Provides consistent interface for all claim types
- Simpler for rapid development

#### With Schema (Enhanced Type Safety)
When a Cedar schema defines the Token entity:
- Claims can use specific types (DateTime, Long, Boolean, Record)
- Enables proper type casting during entity creation
- Better for production deployments

Example schema for multi-issuer tokens:

```cedar
namespace Jans {
  entity Access_Token = {
    "token_type": String,
    "jti": String,
    "issuer": String,
    "exp": Long,
    "validated_at": Long,
    "iat": Long,
    "scope": Set<String>,
    "aud": Set<String>,
    "client_id": String
  } tags String;
  
  entity Id_Token = {
    "token_type": String,
    "jti": String,
    "issuer": String,
    "exp": Long,
    "validated_at": Long,
    "iat": Long,
    "sub": String,
    "email": String,
    "email_verified": Boolean
  } tags String;
}

namespace Acme {
  entity DolphinToken = {
    "token_type": String,
    "jti": String,
    "issuer": String,
    "exp": Long,
    "validated_at": Long,
    "waiver": String,
    "location": String,
    "clearance_level": Long
  } tags String;
}
```

### Policy Examples Using Token Entities

**Simple token validation**:
```cedar
permit(
  principal,
  action == Jans::Action::"Read",
  resource in Jans::Document
) when {
  context has tokens.acme_access_token &&
  context.tokens.acme_access_token.hasTag("scope") &&
  context.tokens.acme_access_token.getTag("scope").contains("read:documents")
};
```

**Multi-issuer validation**:
```cedar
permit(
  principal,
  action == Trade::Action::"Vote",
  resource in Trade::Election
) when {
  // Require token from trade association
  context has tokens.trade_association_access_token &&
  context.tokens.trade_association_access_token.hasTag("member_status") &&
  context.tokens.trade_association_access_token.getTag("member_status").contains("Corporate Member") &&
  // AND token from employer
  context has tokens.company_access_token &&
  context.tokens.company_access_token.hasTag("employee_id")
};
```

**Custom token type**:
```cedar
permit(
  principal,
  action == Acme::Action::"SwimWithDolphin",
  resource == Acme::Aquarium::"Miami"
) when {
  context has tokens.dolphin_acme_dolphin_token &&
  context.tokens.dolphin_acme_dolphin_token.hasTag("waiver") &&
  context.tokens.dolphin_acme_dolphin_token.getTag("waiver").contains("signed") &&
  context.tokens.dolphin_acme_dolphin_token.hasTag("clearance_level") &&
  context.tokens.dolphin_acme_dolphin_token.getTag("clearance_level").contains(5)
};
```

### Trusted Issuer Configuration for Multi-Issuer

The `name` field in trusted issuer configuration is critical for multi-issuer authorization:

```json
"trusted_issuers": {
  "acme_issuer_id": {
    "name": "Acme",  // Used in token collection naming: "acme_access_token"
    "description": "Acme Corporation IDP",
    "openid_configuration_endpoint": "https://idp.acme.com/.well-known/openid-configuration",
    "token_metadata": {
      "access_token": {
        "entity_type_name": "Jans::Access_Token",
        "token_id": "jti"
      }
    }
  },
  "dolphin_issuer_id": {
    "name": "Dolphin",  // Used in token collection naming: "dolphin_acme_dolphin_token"
    "description": "Dolphin Service Provider",
    "openid_configuration_endpoint": "https://idp.dolphin.sea/.well-known/openid-configuration",
    "token_metadata": {
      "dolphin_token": {
        "entity_type_name": "Acme::DolphinToken",
        "token_id": "jti"
      }
    }
  }
}
```

The `name` field ensures predictable and secure token entity naming in the policy evaluation context.

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
  entity id_token = {"aud": Set<String>,"iss": String, "sub": String};
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
  - Each `Role` is parent for `User`. So to check role in policy use operator `in` to check hierarchy.

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
