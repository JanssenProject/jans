---
tags:
  - administration
  - Cedar
  - Cedarling
  - Principal
  - Mappings
---

# Cedarling Principal Mappings

Cedarling automatically creates the following entities:

- [Trusted Issuer](#trusted-issuer-entity)
- [Workload](#workload-entity)
- [User](#user-entity)
- [Role](#role-entity)
- [JWT Entities](#jwt-entities)

The entity type names of the Workload and User entities can be customized via the `CEDARLING_MAPPING_USER` and `CEDARLING_MAPPING_WORKLOAD` [properties](./cedarling-properties.md) respectively.

>  ***Notes***
> - All entity creation and attribute population logic is configurable via the [Token Metadata Schema (TEMS)](./cedarling-policy-store.md#token-metadata-schema) and [Cedarling bootstrap properties](./cedarling-properties.md).
> - Attribute presence depends on token contents and policy store configuration.
> - Role inheritance simplifies **user-role mapping** for RBAC policy enforcement.

## Trusted Issuer

Cedarling creates a Trusted Issuer entity at startup for each trusted issuer defined in the [policy store](./cedarling-policy-store.md#trusted-issuers-schema).

- *Default Type Name:* `Jans::TrustedIssuer`
- *Entity ID:*  Set using the name of the trusted issuer object in the policy store.

## Workload Entity

Cedarling creates a **Workload** entity for each request when the `CEDARLING_WORKLOAD_AUTHZ`  [bootstrap property](./cedarling-properties.md) is set to `enabled`.

- *Default Type Name:* `Jans::Workload`
- *Entity ID:* Determined by the `workload_id` attribute from the [Token Entity Metadata Schema (TEMS)](./cedarling-policy-store.md#token-metadata-schema).
- If `workload_id` is **not set**, Cedarling will fall back to the following claims (in order):

  1. `aud` from the `access_token`
  2. `client_id` from the `access_token`

### Example Workload Schema

```cedarschema/
entity Workload = {
  iss: TrustedIssuer,
  client_id?: String,
  aud?: String,
  name?: String,
  rp_id?: String,
  spiffe_id?: String,
  access_token?: Access_token,
};
```

#### Attribute Mappings

| Attribute | Source |
| --- | --- |
| `iss` | Reference to the Trusted Issuer entity created at startup |
| `client_id` | the `"lient_id` claim from the JWT |
| `aud` | the `aud` claim from the JWT |
| `name` | the `name` claim from the JWT |
| `spiffe_id` | the `spiffe_id` claim from the JWT |
| `rp_id` | Derived from either the `aud` or the `client_id` claim from the JWT |

## User Entity

Cedarling creates a **User** entity for each request when the `CEDARLING_USER_AUTHZ`  [bootstrap property](./cedarling-properties.md) is set to `enabled`.

- *Default Type Name:* `Jans::User`
- *Entity ID:* Determined by the `user_id` attribute from the [TEMS](./cedarling-policy-store.md#token-metadata-schema).
- If `user_id` is **not set**, Cedarling will fall back to the following claims (in order):

  1. `sub` from the `userinfo_token`
  2. `sub` from the `id_token`

### Default User Schema

```cedarschema
entity User in [Role] = {
  sub?: String,
  email?: email_address,
  phone_number?: String,
  role?: Set<String>,
  username?: String,
  id_token?: Id_token,
  userinfo_token?: Userinfo_token,
};
```

#### **Attribute Mappings**

User attributes are derived from the combined claims in the `id_token` and the `userinfo_token` if available.

**Standard Claims**

- `sub`
- `role`
- `email`
- `phone_number`
- `username`
- `birthdate`
- `country`

## Role Entity

Cedarling automatically attempts to create **Role** entities for each request.

- **Default Type Name:** `Jans::Role`
- **Entity ID:** Determined by the `role_mapping` attribute from the [TEMS](./cedarling-policy-store.md#token-metadata-schema).
- If `role_mapping` is **not set**, Cedarling will try to create Role entities based on the following claims (in order):

  1. `role` from the `userinfo_token`
  2. `role` from the `id_token`

### RBAC Support

Since Role entities are automatically assigned as parents of User entities, you can easily define RBAC policies like:

```cedarschema
permit (
   principal == Jans::Role::"Admin",
   action in [Jans::Action::"Compare",Jans::Action::"Execute"],
)
```

## JWT Entities

Cedarling creates **JWT entities** for each token defined in the [trusted issuers schema](./cedarling-properties.md#trusted-issuers-schema). 


- *Type Name:* Determined by the `entity_type_name` attribute from the [TEMS](./cedarling-policy-store.md#token-metadata-schema).
- *Entity ID:* Determined by the `token_id` attribute from the [TEMS](./cedarling-policy-store.md#token-metadata-schema).

### Attribute Mappings

Each **claim** in the JWT is automatically added to the JWT entity's attributes.
