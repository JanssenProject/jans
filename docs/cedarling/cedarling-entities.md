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
  3. `aud` from the `id_token` -- note that the Workload attributes that will be created from this will still be from the `access_token`
- *Entity Attributes*: Extracted from by the claims of the `access_token`. Cedarling will check the schema and use the JWT claims with the same names as the Workload attributes.

### Example Workload Entity Creation

With the following `access_token` claims:

```json
{
  "iss": "https://test.com/",
  "aud": "some_aud",
  "jti": "some_jti",
}
```

and Cedar schema:

```cedarschema
entity TrustedIssuer;
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

The following entity Workload Entity could be created:

```json
{
  "uid": {"type": "Workload", "id": "some_aud"},
  "attrs": {
    "iss": {"__entity": {"type": "TrustedIssuer", "id": "https://test.com/"}},
    "aud": "some_aud"
    "access_token": {"__entity": {"type": "Access_token", "id": "some_jti"}},
  },
  "parents": []
}
```


## User Entity

Cedarling creates a **User** entity for each request when the `CEDARLING_USER_AUTHZ`  [bootstrap property](./cedarling-properties.md) is set to `enabled`.

- *Default Type Name:* `Jans::User`
- *Entity ID:* Determined by the `user_id` attribute from the [TEMS](./cedarling-policy-store.md#token-metadata-schema).
- If `user_id` is **not set**, Cedarling will fall back to the following claims (in order):
  1. `sub` from the `userinfo_token`
  2. `sub` from the `id_token`
- *Entity Attributes*: Determined by the combined claims of the `id_token` and `userinfo_token`. Cedarling will check the schema and use the JWT claims with the same names as the User attributes.

### Example User Entity Creation

With the following `id_token` claims:

```json
{
  "iss": "https://test.com/",
  "sub": "some_sub",
  "email": "bob@email.com",
  "jti": "id_tkn_jti",
  "role": "role1"
}
```

and `userinfo_token` claims:

```json
{
  "iss": "https://test.com/",
  "sub": "some_sub",
  "name": "bob",
  "jti": "userinfo_tkn_jti",
  "role": ["role2", "role3"]
}
```

and Cedar schema:

```cedarschema
entity Role;
entity User in [Role] = {
  sub: String,
  email: String,
  name: String,
};
```

The following entity Workload Entity could be created:

```json
{
  "uid": {"type": "User", "id": "some_sub"},
  "attrs": {
    "sub": "some_sub",
    "email": "email@email.com",
    "name": "bob"
  },
  "parents": [
    {"type": "Role", "id": "role1"},
    {"type": "Role", "id": "role2"},
    {"type": "Role", "id": "role3"}
  ]
}
```

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
