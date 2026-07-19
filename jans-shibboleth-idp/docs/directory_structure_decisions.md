# Directory Structure Decisions

## Discussion date: 2026-07-18

### Context

Planning the directory structure for `jans-shibboleth-idp` to accommodate:
- An OpenAPI spec for the domain
- DTOs and mappers for the domain objects
- A future persistence module

### Final directory structure

```
jans-shibboleth-idp/
├── docs/                        ← design docs, architecture, test plans (human prose only)
├── trust-domain/                ← pure domain model (unchanged)
│
├── trust-dto/                   ← NEW: DTOs + mappers + OpenAPI spec
│   ├── pom.xml
│   └── src/main/
│       ├── java/io/jans/shibboleth/trust/dto/
│       │   ├── config/
│       │   │   ├── TrustRelationshipDto.java
│       │   │   ├── MetadataSourceDto.java
│       │   │   ├── ProfileConfigurationDto.java
│       │   │   └── ...
│       │   ├── activation/
│       │   │   ├── WorkItemDto.java
│       │   │   └── ...
│       │   ├── shared/
│       │   │   └── ApiErrorDto.java
│       │   └── mapper/
│       │       ├── config/
│       │       │   └── TrustRelationshipMapper.java
│       │       └── activation/
│       │           └── WorkItemMapper.java
│       └── resources/
│           └── openapi/
│               └── trust-dto.yaml
│
└── trust-persistence/           ← NEW: persistence adapters (eventual)
    ├── pom.xml
    └── src/main/java/io/jans/shibboleth/trust/persistence/...
```

### Maven artifact names

| Module | ArtifactId |
|---|---|
| `trust-domain` | `jans-shibboleth-trust-domain` |
| `trust-dto` | `jans-shibboleth-trust-dto` |
| `trust-persistence` | `jans-shibboleth-trust-persistence` |

### Dependency direction

- `trust-dto` → `trust-domain` (DTOs know about domain types for mapping)
- API layer → `trust-dto`
- `trust-persistence` → `trust-domain`

### Decisions made

1. **`trust-dto` over `trust-model`**: `trust-domain` is already the domain model. `trust-model` would create confusion.
2. **`trust-dto` over `trust-contracts`**: DTO has zero semantic overhead — everyone instantly knows what's in that directory.
3. **`trust-dto` over `trust-adapter`**: Too ambiguous; persistence would also be an adapter in hexagonal terms.
4. **`trust-dto` over `trust-dto-mappings`**: Too verbose; mappers are a subordinate concern that don't need equal billing.
5. **OpenAPI YAML lives inside `trust-dto`**: The spec is an executable contract that drives DTO generation and must be versioned alongside the DTOs. It does NOT live in `docs/` (which is for human-authored prose like this file).
6. **Sibling modules**: Separated from existing `trust-domain` to keep the domain module pure (zero framework/annotation dependencies).

### Persistence and DTO projections

**Recommended approach (start simple):** `trust-persistence` depends on `trust-dto`. Repository methods project directly into the DTO contract shape. No extra translation layer.

```java
// In trust-persistence
List<TrustRelationshipListItemDto> findActiveByNature(TrustNature nature);
```

**Fallback (if needed later):** Introduce separate persistence projection classes and a mapper layer when:
- Projections carry internal fields the API shouldn't expose
- Multiple persistence backends produce divergent raw shapes
- Persistence-specific annotations would pollute DTOs

Starting with the simple approach is fine — the mapper boundary can be introduced later without breaking the API. Starting with it is premature.

### Naming convention alignment

The naming follows existing Jans project conventions observed in sibling projects:
- `jans-auth-server/model` → artifact `jans-auth-model`
- `jans-auth-server/persistence-model` → artifact `jans-auth-persistence-model`
- `jans-core/model` → artifact `jans-core-model`
