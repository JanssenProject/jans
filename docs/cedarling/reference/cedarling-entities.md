---
tags:
  - administration
  - Cedar
  - Cedarling
  - Principal
  - Mappings
---

# Cedarling Entity Mappings

Cedarling creates Cedar entities automatically based on the authorization interface used. The two interfaces build different sets of entities:

| Entity Type | `authorize_unsigned` | `authorize_multi_issuer` |
| --- | --- | --- |
| [Trusted Issuer](#trusted-issuer) | Created at startup | Created at startup |
| [Principal (User, Workload, etc.)](#principal-entities) | Built from `EntityData` | Not created |
| [Role](#role-entity) | Built as parent of principals | Not created |
| [Token (JWT)](#token-entities) | Not created | Built from JWT claims |
| [Resource](#resource-entity) | Built from `EntityData` | Built from `EntityData` |

The entity type names are defined in the Cedar schema within the policy store.

> ***Notes***
>
> - Attribute presence depends on the data provided and the Cedar schema in the policy store.
> - Role inheritance simplifies **user-role mapping** for RBAC policy enforcement.

## Trusted Issuer

Cedarling creates a Trusted Issuer entity at startup for each trusted issuer defined in the [policy store](./cedarling-policy-store.md#trusted-issuers-schema).

- *Namespace:* Corresponds to the trusted issuer name in the policy store.
- *Type Name:* `TrustedIssuer`
- *Entity ID:* Set using the issuer URL (corresponds to `iss` in tokens).

## Principal Entities

Principals are created only in the `authorize_unsigned` interface. The caller provides one or more principals as `EntityData`, each specifying the Cedar entity type, ID, and attributes explicitly.

There is no fixed set of principal types -- any entity type defined in the Cedar schema can be used as a principal. Common examples include `User` and `Workload`, but these are just conventions defined in your schema.

When multiple principals are provided, Cedarling evaluates each one independently and combines results using [`CEDARLING_PRINCIPAL_BOOLEAN_OPERATION`](./cedarling-principal-boolean-operations.md).

### EntityData Structure

Each principal is passed as an `EntityData`:

```json
{
  "cedar_mapping": {
    "entity_type": "Jans::User",
    "id": "user_123"
  },
  "attributes": {
    "sub": "user_123",
    "email": "bob@email.com",
    "role": ["Admin", "Editor"]
  }
}
```

- *Type Name:* Defined in `cedar_mapping.entity_type`. Must match a type in the Cedar schema.
- *Entity ID:* Defined in `cedar_mapping.id`.
- *Entity Attributes:* Cedarling checks the Cedar schema and maps the provided attributes to the entity's schema shape.

### Example: User Principal

Given the following `authorize_unsigned` request with a User principal:

```json
{
  "principals": [
    {
      "cedar_mapping": {
        "entity_type": "MyApp::User",
        "id": "some_sub"
      },
      "attributes": {
        "sub": "some_sub",
        "email": {"domain": "email.com", "uid": "bob"},
        "role": ["Admin", "Editor"]
      }
    }
  ],
  "action": "MyApp::Action::\"Read\"",
  "resource": {
    "cedar_mapping": {
      "entity_type": "MyApp::Application",
      "id": "app_1"
    },
    "attributes": {
      "app_id": "app_1",
      "name": "MyApp",
      "url": {"host": "myapp.com", "path": "/", "protocol": "https"}
    }
  },
  "context": {}
}
```

and Cedar schema:

```cedarschema
// Jans namespace: shared infrastructure types used across namespaces.
namespace Jans {
  type Url = {
    host: String,
    path: String,
    protocol: String
  };
  type email_address = {
    domain: String,
    uid: String
  };
};

// MyApp namespace: your business entities.
namespace MyApp {
  entity Role;
  entity User in [Role] = {
    email?: Jans::email_address,
    phone_number?: String,
    role: Set<String>,
    sub: String,
    "username"?: String,
  };
};
```

Cedarling builds the following entities:

**User entity:**

```json
{
  "uid": {"type": "MyApp::User", "id": "some_sub"},
  "attrs": {
    "sub": "some_sub",
    "email": {"domain": "email.com", "uid": "bob"},
    "role": ["Admin", "Editor"]
  },
  "parents": [
    {"type": "MyApp::Role", "id": "Admin"},
    {"type": "MyApp::Role", "id": "Editor"}
  ]
}
```

**Role entities** (created automatically as parents):

```json
[
  {"uid": {"type": "MyApp::Role", "id": "Admin"}, "attrs": {}, "parents": []},
  {"uid": {"type": "MyApp::Role", "id": "Editor"}, "attrs": {}, "parents": []}
]
```

### Example: Workload Principal

```json
{
  "principals": [
    {
      "cedar_mapping": {
        "entity_type": "MyApp::Workload",
        "id": "my_client"
      },
      "attributes": {
        "client_id": "my_client",
        "name": "Backend Service"
      }
    }
  ],
  "action": "MyApp::Action::\"Read\"",
  "resource": { "..." : "..." },
  "context": {}
}
```

Cedarling builds:

```json
{
  "uid": {"type": "MyApp::Workload", "id": "my_client"},
  "attrs": {
    "client_id": "my_client",
    "name": "Backend Service"
  },
  "parents": []
}
```

## Role Entity

Cedarling automatically creates **Role** entities when building principals in the `authorize_unsigned` interface.

- **Type Name:** Configurable via `mapping_role` bootstrap property. Must match the Role entity type in your Cedar schema.
- **Entity ID:** Extracted from the attribute configured in `unsigned_role_id_src` (defaults to `role`). The value can be a single string or an array of strings, each becoming a separate Role entity.
- **Relationship:** Role entities are added as **parents** of the principal entity, enabling RBAC policies.

### RBAC Support

Since Role entities are automatically assigned as parents of principals, you can define RBAC policies like:

```cedar
permit (
   principal == MyApp::Role::"Admin",
   action in [MyApp::Action::"Compare", MyApp::Action::"Execute"],
   resource
);
```

## Token Entities

Token entities are created only in the `authorize_multi_issuer` interface. Cedarling builds one entity per JWT token provided in the request.

- *Type Name:* Determined by the `entity_type_name` attribute from the [TEMS](./cedarling-policy-store.md#token-metadata-schema), or derived from the token's mapping name.
- *Entity ID:* Extracted from the `jti` (JWT ID) claim by default, configurable via `token_id` in TEMS.
- *Attributes:* JWT claims are mapped to entity attributes based on the Cedar schema. Reserved claims (`iss`, `jti`, `exp`) are typed correctly (entity reference, string, long). Synthetic attributes `token_type` and `validated_at` are added automatically.
- *Tags:* Non-reserved JWT claims are also added as entity tags (`Set<String>`), enabling `hasTag`/`getTag` operations in policies.

### Example: Multi-Issuer Token Entity

Given the following `authorize_multi_issuer` request:

```json
{
  "tokens": [
    {
      "mapping": "Acme::Access_Token",
      "payload": "<signed JWT>"
    }
  ],
  "action": "Acme::Action::\"GetFood\"",
  "resource": {
    "cedar_mapping": {
      "entity_type": "Acme::Resource",
      "id": "approved_foods"
    },
    "attributes": {
      "name": "Approved Foods"
    }
  }
}
```

Where the JWT payload decodes to:

```json
{
  "iss": "https://idp.acme.com/auth",
  "sub": "user_123",
  "jti": "token_abc",
  "scope": ["read", "write"],
  "exp": 2000000000
}
```

Cedarling builds the following token entity:

```json
{
  "uid": {"type": "Acme::Access_Token", "id": "token_abc"},
  "attrs": {
    "token_type": "Acme::Access_Token",
    "jti": "token_abc",
    "iss": {"__entity": {"type": "Acme::TrustedIssuer", "id": "https://idp.acme.com/auth"}},
    "exp": 2000000000,
    "validated_at": 1710892800,
    "sub": "user_123",
    "scope": ["read", "write"]
  },
  "tags": {
    "sub": ["user_123"],
    "scope": ["read", "write"]
  },
  "parents": []
}
```

Token entities are accessible in policies via the context (e.g., `context.tokens.acme_access_token`).

## Resource Entity

Resource entities are built in both authorization interfaces from the `EntityData` provided in the request. The structure is identical to [principal entities](#entitydata-structure), but resources have no parent entities.

If no attributes are provided and a default entity with the same UID exists in the policy store, the default entity's attributes are used.

## Entity Merging and Conflict Resolution

Cedarling automatically merges entities from multiple sources during authorization requests. The merging process follows specific rules to ensure consistency and handle conflicts appropriately.

### Merging Order and Precedence

1. **Default Entities**: Loaded first from policy store configuration
2. **Request Entities**: Resource, issuers, roles, tokens, and principal entities
3. **Conflict Resolution**: Request entities override default entities when UID conflicts occur

### Example: Entity Override Scenario

When a resource entity has the same UID as a default entity:

```json
// Default entity in policy store
{
  "org1": {
    "uid": {
      "type": "Jans::Organization",
      "id": "org1"
    },
    "attrs": {
      "name": "Default Organization",
      "is_active": true
    },
    "parents": []
  }
}

// Request resource entity
{
  "uid": {
    "type": "Jans::Organization",
    "id": "org1"
  },
  "attrs": {
    "name": "Updated Organization",
    "is_active": false
  },
  "parents": []
}
```

Result: The request entity's attributes (`"Updated Organization"`, `is_active: false`) will be used, overriding the default entity's values.
