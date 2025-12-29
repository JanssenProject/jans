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

## Policy Store Formats

Cedarling supports two policy store formats:

### 1. Legacy Single-File Format (JSON/YAML)

The original format stores all policies and schema in a single JSON or YAML file with Base64-encoded content. This is documented in detail in the sections below.

### 2. New Directory-Based Format

The new directory-based format uses human-readable Cedar files organized in a structured directory:

```text
policy-store/
├── metadata.json           # Required: Store identification and versioning
├── manifest.json           # Optional: File checksums for integrity validation
├── schema.cedarschema      # Required: Cedar schema in human-readable format
├── policies/               # Required: Directory containing .cedar policy files
│   ├── allow-read.cedar
│   └── deny-guest.cedar
├── templates/              # Optional: Directory containing .cedar template files
├── entities/               # Optional: Directory containing .json entity files
└── trusted-issuers/        # Optional: Directory containing .json issuer configs
```

#### metadata.json

Contains policy store identification and versioning:

```json
{
  "cedar_version": "4.4.0",
  "policy_store": {
    "id": "abc123def456",
    "name": "My Application Policies",
    "description": "Optional description",
    "version": "1.0.0",
    "created_date": "2024-01-01T00:00:00Z",
    "updated_date": "2024-01-02T00:00:00Z"
  }
}
```

#### manifest.json (Optional)

Provides integrity validation with file checksums:

```json
{
  "policy_store_id": "abc123def456",
  "generated_date": "2024-01-01T12:00:00Z",
  "files": {
    "metadata.json": {
      "size": 245,
      "checksum": "sha256:abc123..."
    },
    "schema.cedarschema": {
      "size": 1024,
      "checksum": "sha256:def456..."
    }
  }
}
```

When a manifest is present, Cedarling validates:

- File checksums match (SHA-256)
- File sizes match
- Policy store ID matches between manifest and metadata

#### Policy Files

Policies are stored as human-readable `.cedar` files in the `policies/` directory:

```cedar
@id("allow-read")
permit(
    principal,
    action == MyApp::Action::"read",
    resource
);
```

Each policy file must have an `@id` annotation that uniquely identifies the policy.

#### Cedar Archive (.cjar) Format

The directory structure can be packaged as a `.cjar` file (ZIP archive) for distribution:

```bash
# Create a .cjar archive from a policy store directory
cd policy-store && zip -r ../policy-store.cjar .
```
**Note:** In WASM environments, only URL-based and inline string sources are available. Use `CjarUrl` or `init_from_archive_bytes()` for custom fetch scenarios.

**Advanced: Loading from Bytes**

For scenarios requiring custom fetch logic (e.g., auth headers), archive bytes can be loaded directly:

- **WASM**: Use `init_from_archive_bytes(config, bytes)` function
- **Rust**: Use `PolicyStoreSource::ArchiveBytes(Vec<u8>)` or `load_policy_store_archive_bytes()` function

```javascript
// WASM example with custom fetch
const response = await fetch(url, { headers: { Authorization: "..." } });
const bytes = new Uint8Array(await response.arrayBuffer());
const cedarling = await init_from_archive_bytes(config, bytes);
```

## Legacy Single-File Format (JSON)

The following sections document the legacy single-file JSON format.

### JSON Schema

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

- **cedar_version** : (_String_) The version of [Cedar policy](https://docs.cedarpolicy.com/). The protocols of this version will be followed when processing Cedar schema and policies.
- **policies** : (_Object_) Base64 encoded object containing one or more policy IDs as keys, with their corresponding objects as values. See: [policies schema](#cedar-policies-schema).
- **schema** : (_String_ | _Object_) Base64 encoded JSON Object. See [schema](#schema) below.
- **trusted_issuers** : (_Object of {unique_id => IdentitySource}(#trusted-issuer-schema)_) List of metadata for Identity Sources.
- **default_entities** : (_Object_) Optional map of entity IDs to encoded/default entity payloads. See [Default Entities](#default-entities).

### `schema`

Either _String_ or _Object_, where _Object_ is preferred.

Where _Object_ - An object with `encoding`, `content_type` and `body` keys. For example:

```json
"schema": {
    "encoding": "none", // can be one of "none" or "base64"
    "content_type": "cedar", // can be one of "cedar" or "cedar-json"
    "body": "namespace Jans {\ntype Url = {"host": String, "path": String, "protocol": String};..."
}
```

Where _String_ - The schema in cedar-json format, encoded as Base64. For example:

```json
"schema": "cGVybWl0KAogICAgc..."
```

## Default Entities

Default entities allow you to preload static entity records that are available to every authorization request. Cedarling loads these at startup alongside policies and schema.

Format:

- Each entry is a simple key-value pair where the key is the entity ID and the value is a Base64-encoded JSON object representing the entity payload.
- Default entities support two formats:
  - **Cedar format**: `{"uid": {"type": "...", "id": "..."}, "attrs": {...}, "parents": [...]}`
  - **Legacy format**: `{"entity_type": "...", "entity_id": "...", ...attributes...}` (for backward compatibility)

```json
"default_entities": {
  "1694c954f8d9": "eyJ1aWQiOnsidHlwZSI6IkphbnM6Ok9yZ2FuaXphdGlvbiIsImlkIjoiMTY5NGM5NTRmOGQ5In0sImF0dHJzIjp7Im8iOiJBY21lIERvbHBoaW5zIERpdmlzaW9uIiwib3JnX2lkIjoiMTAwMTI5IiwiZG9tYWluIjoiYWNtZS1kb2xwaGluLnNlYSIsInJlZ2lvbnMiOlsiQXRsYW50aWMiLCJQYWNpZmljIiwiSW5kaWFuIl19LCJwYXJlbnRzIjpbXX0=",
  "74d109b20248": "eyJ1aWQiOnsidHlwZSI6IkphbnM6OlByaWNlTGlzdCIsImlkIjoiNzRkMTA5YjIwMjQ4In0sImF0dHJzIjp7ImRlc2NyaXB0aW9uIjoiMjAyNSBQcmljZSBMaXN0IiwicHJvZHVjdHMiOnsiMTUwMjAiOjkuOTUsIjE1MDUwIjoxNC45NX0sInNlcnZpY2VzIjp7IjUxMDAxIjo5OS4wLCI1MTAyMCI6Mjk5LjB9fSwicGFyZW50cyI6W119"
}
```

Example of the decoded payloads for two default entities:

```json
{
  "1694c954f8d9": {
    "uid": {
      "type": "Jans::Organization",
      "id": "1694c954f8d9"
    },
    "attrs": {
      "o": "Acme Dolphins Division",
      "org_id": "100129",
      "domain": "acme-dolphin.sea",
      "regions": ["Atlantic", "Pacific", "Indian"]
    },
    "parents": []
  },
  "74d109b20248": {
    "uid": {
      "type": "Jans::PriceList",
      "id": "74d109b20248"
    },
    "attrs": {
      "description": "2025 Price List",
      "products": { "15020": 9.95, "15050": 14.95 },
      "services": { "51001": 99.0, "51020": 299.0 }
    },
    "parents": []
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

- **unique_policy_id**: (_String_) A unique policy ID used to for tracking and auditing purposes.
- **name** : (_String_) A name for the policy
- **description** : (_String_) A brief description of cedar policy
- **creation_date** : (_String_) Policy creating date in `YYYY-MM-DDTHH:MM:SS.ssssss`
- **policy_content** : (_String_ | _Object_) The Cedar Policy. See [policy_content](#policy_content) below.

### `policy_content`

Either _String_ or _Object_, where _Object_ is preferred.

Where _Object_ - An object with `encoding`, `content_type` and `body` keys. For example:

```json
"policy_content": {
    "encoding": "none", // can be one of "none" or "base64"
    "content_type": "cedar", // ONLY "cedar" for now due to limitations in cedar-policy crate
    "body": "permit(\n    principal is Jans::User,\n    action in [Jans::Action::\"Update\"],\n    resource is Jans::Issue\n)when{\n    principal.country == resource.country\n};"
}
```

Where _String_ - The policy in cedar format, encoded as Base64. For example:

```json
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

- **name** : (_String_) The name of the trusted issuer.
- **description** : (_String_) A brief description of the trusted issuer, providing context for administrators.
- **openid_configuration_endpoint** : (_String_) The HTTPS URL for the OpenID Connect configuration endpoint (usually found at `/.well-known/openid-configuration`).
- **trusted_issuer_id** : (_Object_, _optional_) Metadata related to a particular issuer. You can add as many trusted issuers you want. Furthermore, the name this object is what will be used as the entity ID of the [Trusted Issuer](./cedarling-entities.md#trusted-issuer) that Cedarling automatically creates at startup.
- **token_metadata** : (_Object_, _optional_) Tokens metadata in a map of _token name_ -> _token metadata_. See [Token Metadata Schema](#token-metadata-schema).

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

- **role_mapping**: (String OR Array of String, _Optional_) Indicates which field in the token should be used for role-based access control. If not needed, set to an empty string (`""`).

You can include a `role_mapping` in each token, all of them will be executed by Cedarling.
If none `role_mapping` defined the `Cedarling` will try to find role in `userinfo` token in field `role`.

#### Claim mapping

- **claim_mapping:** Defines how to extract and transform specific claims from the token. Each claim can have its own parser (`regex` or `json`) and type (`Acme::email_address`, `Acme::Url`, etc.).

In regex attribute mapping like `"UID": {"attr": "uid", "type":"String"},`, `type` field can contain possible variants:

- `String` - to string without transformation,
- `Number` - parse string to float64 (JSON number) if error returns default value
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
    "08c6c18a654f492adcf3fe069d729b4d9e6bf82605cb": {
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
    "1694c954f8d9": "eyJ1aWQiOnsidHlwZSI6IkphbnM6Ok9yZ2FuaXphdGlvbiIsImlkIjoiMTY5NGM5NTRmOGQ5In0sImF0dHJzIjp7Im8iOiJBY21lIERvbHBoaW5zIERpdmlzaW9uIiwib3JnX2lkIjoiMTAwMTI5IiwiZG9tYWluIjoiYWNtZS1kb2xwaGluLnNlYSIsInJlZ2lvbnMiOlsiQXRsYW50aWMiLCJQYWNpZmljIiwiSW5kaWFuIl19LCJwYXJlbnRzIjpbXX0=",
    "74d109b20248": "eyJ1aWQiOnsidHlwZSI6IkphbnM6OlByaWNlTGlzdCIsImlkIjoiNzRkMTA5YjIwMjQ4In0sImF0dHJzIjp7ImRlc2NyaXB0aW9uIjoiMjAyNSBQcmljZSBMaXN0IiwicHJvZHVjdHMiOnsiMTUwMjAiOjkuOTUsIjE1MDUwIjoxNC45NX0sInNlcnZpY2VzIjp7IjUxMDAxIjo5OS4wLCI1MTAyMCI6Mjk5LjB9fSwicGFyZW50cyI6W119"
  }
}
```

## Multi-Issuer Token Entities

When using the `authorize_multi_issuer` method, Cedarling creates token entities dynamically without requiring predefined User/Workload principals. This section describes how these token entities are structured and made available in policies.

> **⚠️ SCHEMA REQUIREMENT**: Multi-issuer authorization requires specific Cedar schema modifications. Your schema **must** include the multi-issuer token structure or authorization will fail with schema validation errors. See [Schema Requirements](#schema-requirements-for-multi-issuer) below.

### Token Entity Structure

Each validated token in a multi-issuer authorization request becomes a Cedar entity with the following structure:

**Required Attributes** (stored as entity attributes):

- `token_type` (String): The entity type name (e.g., "Jans::Access_Token", "Acme::DolphinToken")
- `jti` (String): Unique token identifier from JWT
- `iss?` (TrustedIssuer): JWT issuer claim value (derived from iss to TrustedIssuer); optional when the token has not been verified or the issuer could not be determined.
- `exp` (Long): Token expiration timestamp
- `validated_at` (Long): Timestamp when Cedarling validated the token

**Optional Attributes** (JWT claims stored as entity tags):

- All other JWT claims are stored as **entity tags** with `Set<String>` type by default
- Examples: `scope`, `aud`, `sub`, `client_id`, custom claims

**Complete Example**:

```cedar
namespace Jans{
  entity Access_token = {
    "token_type": String,        // Required: Entity type name
    "jti": String,               // Required: Token ID
    "iss": Jans::TrustedIssuer,  // Required: JWT issuer
    "exp": Long,                 // Required: Expiration
    "validated_at": Long,        // Required: Validation timestamp
    // Optional JWT claims (must be optional)
    "aud"?: String,
    "iat"?: Long,
    "scope"?: Set<String>,
    // ... other claims
  } tags Set<String>;            // Required: For dynamic claim storage

  entity TrustedIssuer = {
    issuer_entity_id: Url
  };
}
```

**Important Notes**:

- All token entity attributes (except the five required ones) **must be optional** (`?`) to support tokens with varying claim structures
- JWT claims not defined as attributes are stored as **entity tags** (Set<String> by default)
- The `tags Set<String>` declaration is required for dynamic JWT claim access

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

### Schema Requirements for Multi-Issuer

#### 1. Token Entity Schema

All token entities **must** include these five required attributes and declare tags:

```cedar
namespace Jans {
  entity Access_token = {
    // *** REQUIRED ATTRIBUTES FOR MULTI-ISSUER ***
    token_type?: String,        // Entity type (e.g., "Jans::Access_Token")
    jti?: String,               // JWT ID - unique token identifier
    iss?: Jans::TrustedIssuer,  // Issuer entity (for standard authz)
    exp?: Long,                 // Token expiration timestamp
    validated_at?: Long,        // Validation timestamp

    // Optional JWT claims - ALL MUST BE OPTIONAL (?)
    aud?: String,               // Audience
    iat?: Long,                 // Issued at
    scope?: Set<String>,        // OAuth scopes
    client_id?: String,         // Client ID
    sub?: String,               // Subject
    // Add other claims as needed
  } tags Set<String>;           // *** REQUIRED: For dynamic claims ***

  entity id_token = {
    // *** REQUIRED ATTRIBUTES FOR MULTI-ISSUER ***
    token_type?: String,
    jti?: String,
    iss?: Jans::TrustedIssuer,
    exp?: Long,
    validated_at?: Long,

    // Optional JWT claims - ALL MUST BE OPTIONAL (?)
    aud?: Set<String>,
    iat?: Long,
    sub?: String,
    email?: email_address,
    name?: String,
    phone_number?: String,
    role?: Set<String>,
    acr?: String,
    amr?: Set<String>,
    azp?: String,
    birthdate?: String,
    // Add other claims as needed
  } tags Set<String>;           // *** REQUIRED: For dynamic claims ***

  entity Userinfo_token = {
    // *** REQUIRED ATTRIBUTES FOR MULTI-ISSUER ***
    token_type?: String,
    jti?: String,
    iss?: Jans::TrustedIssuer,
    exp?: Long,
    validated_at?: Long,

    // Optional JWT claims - ALL MUST BE OPTIONAL (?)
    aud?: String,
    iat?: Long,
    sub?: String,
    email?: email_address,
    name?: String,
    birthdate?: String,
    phone_number?: String,
    role?: Set<String>,
    // Add other claims as needed
  } tags Set<String>;           // *** REQUIRED: For dynamic claims ***

  entity TrustedIssuer = {
    issuer_entity_id: Url
  };
}
```

#### 2. Custom Token Types

For custom token types, follow the same pattern:

```cedar
namespace Acme {
  entity DolphinToken = {
    // *** REQUIRED ATTRIBUTES FOR MULTI-ISSUER ***
    token_type?: String,
    jti?: String,
    iss?: Acme::TrustedIssuer,
    exp?: Long,
    validated_at?: Long,

    // Custom token-specific attributes (all optional)
    waiver?: String,
    location?: String,
    clearance_level?: Long,
  } tags Set<String>;           // *** REQUIRED: For dynamic claims ***

  entity TrustedIssuer = {
    issuer_entity_id: Url
  };
}
```

#### 3. Context Type Schema

The Context type **must** include a `tokens` field:

```cedar
type Context = {
  // Standard context fields
  network?: String,
  network_type?: String,
  user_agent?: String,
  operating_system?: String,
  device_health?: Set<String>,
  current_time?: Long,
  geolocation?: Set<String>,
  fraud_indicators?: Set<String>,

  // *** REQUIRED FOR MULTI-ISSUER ***
  tokens?: TokensContext,
};

type TokensContext = {
  total_token_count: Long,      // Required field
  // Individual token fields added dynamically:
  // Pattern: {issuer_name}_{token_type}
  // Example: acme_access_token, google_id_token
};
```

#### Key Requirements

1. **Required Attributes**: Add `token_type`, `jti`, `issuer`, `exp`, `validated_at` to all token entities
2. **Optional Modifier**: All attributes must be optional (`?`) to support varying token structures
3. **Tags Declaration**: All token entities must declare `tags Set<String>` for dynamic JWT claims
4. **Context Update**: Add `tokens?: TokensContext` field to your Context type
5. **Consistency**: Use the same attribute names across all token entity types

#### Why These Changes Are Required

- **Schema Validation**: Cedar validates all entities against the schema; missing required attributes cause authorization failures
- **Dynamic Claims**: JWT claims vary by issuer and token type; tags allow flexible claim storage
- **Context Structure**: Multi-issuer authorization places token entities in `context.tokens.*` which requires schema support
- **Compatibility**: Optional attributes ensure schema works with both standard and multi-issuer authorization

#### Schema Update Checklist

Before using multi-issuer authorization, verify your schema has:

- Added five required attributes to all token entities (token_type, jti, issuer, exp, validated_at)
- Made all token entity attributes optional (`?`)
- Added `tags Set<String>` declaration to all token entities
- Added `tokens?: TokensContext` field to Context type
- Defined `TokensContext` type with `total_token_count: Long`

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
    "name": "Acme",  // Used in token collection naming: "acme_access_token" and depict namespace for `TrustedIssuer` cedar type
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
    "name": "Dolphin",  // Used in token collection naming: "dolphin_acme_dolphin_token" and depict namespace for `TrustedIssuer` cedar type
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

**Note**: The `name` field ensures predictable and secure token entity naming in the policy evaluation context.
And is used as **namespace** for `TrustedIssuer` cedar type.

## Policy and Schema Authoring

You can manually create your Cedar policies and schema in
[Visual Studio](https://marketplace.visualstudio.com/items?itemName=cedar-policy.vscode-cedar).
Make sure you run the cedar command line tool to validate both your schema and policies.

The easiest way to author your policy store is to use the Policy Designer in
[Agama Lab](https://cloud.gluu.org/agama-lab). This tool helps you define the policies, schema and
trusted IDPs and to publish a policy store to a Github repository.

### Minimum supported `cedar-policy schema`

Here is example of a minimum supported `cedar-policy schema`:

```cedar-policy_schema
namespace Jans {
  entity id_token = {"aud": Set<String>,"iss": String, "sub": String, iss?: Jans::TrustedIssuer};
  entity Role;
  entity User in [Role] = {};
  entity Access_token = {"aud": String,"iss": String, "jti": String, "client_id": String, iss?: Jans::TrustedIssuer};
  entity Workload = {};
  entity TrustedIssuer = {
    issuer_entity_id: Url
  };

  entity Issue = {};
  action "Update" appliesTo {
    principal: [Workload, User, Role],
    resource: [Issue],
    context: {}
  };
}
```

**Note**: The principal you use may vary depending on the authorization method.

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
