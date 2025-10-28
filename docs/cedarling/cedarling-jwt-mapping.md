---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - JWT
---

# Cedarling JWT Mapping

JWT mapping in Cedarling transforms JWT tokens into Cedar entities that can be used in policy evaluation. The mapping process differs between standard authorization methods (`authorize`, `authorize_unsigned`) and multi-issuer authorization (`authorize_multi_issuer`).

## Standard JWT Mapping (authorize)

In the standard `authorize` method, Cedarling creates User, Workload, and Role entities from JWT tokens:

### User Entity
Created from id_token and/or userinfo_token:
- Entity ID comes from `sub` claim (or configured `user_id` field)
- Claims are mapped to entity attributes according to schema
- Role entities are created from role claims

### Workload Entity  
Created from access_token:
- Entity ID comes from `aud` or `client_id` claim (or configured `workload_id`)
- OAuth scopes and other claims mapped to attributes
- Represents the application/service acting on behalf of the user

### Token Entities
Each token (access, id, userinfo) also becomes its own entity:
- Contains token metadata (jti, iss, exp, etc.)
- Referenced in context for fine-grained policy evaluation
- Claims mapped according to token metadata configuration

See the [policy store documentation](./cedarling-policy-store.md#token-metadata-schema) for details on configuring token metadata and claim mappings.

## Multi-Issuer JWT Mapping (authorize_multi_issuer)

The `authorize_multi_issuer` method uses a different approach focused on individual token entities without creating User/Workload principals.

### Token Input Format

Each token is explicitly typed using the `TokenInput` structure:

```json
{
  "mapping": "Jans::Access_Token",
  "payload": "eyJhbGciOiJIUzI1NiIs..."
}
```

- **mapping**: The Cedar entity type this token should become (e.g., "Jans::Access_Token", "Acme::DolphinToken")
- **payload**: The JWT token string

### Entity Creation Process

1. **Token Validation**
   - Verify JWT signature using issuer's public keys
   - Validate expiration, not-before, and other time-based claims
   - Check token status (revocation) if configured
   - Only process tokens from trusted issuers

2. **Dynamic Entity Creation**
   - Create Cedar entity with type specified in `mapping` field
   - Extract standard token metadata:
     - `token_type`: The mapping string (e.g., "Jans::Access_Token")
     - `jti`: Token ID from JWT (configurable via token metadata)
     - `issuer`: JWT `iss` claim
     - `exp`: Token expiration timestamp
     - `validated_at`: Timestamp when validation occurred

3. **Claim Storage as Tags**
   - All JWT claims stored as entity tags
   - **Default behavior**: All claims stored as `Set<String>`
   - Provides consistent interface regardless of claim cardinality
   - Schema-aware typing available if Cedar schema defines the entity

### Token Collection Assembly

Validated token entities are organized into a `tokens` collection using a naming algorithm:

**Naming Pattern**: `{issuer_name}_{token_type}`

**Issuer Name Resolution**:
1. Look up token's issuer in trusted issuer metadata
2. Use the `name` field from trusted issuer configuration
3. If no `name` field, extract hostname from JWT `iss` claim
4. Convert to lowercase, replace dots/special chars with underscores

**Token Type Resolution**:
1. Extract from `mapping` field (e.g., "Jans::Access_Token")
2. Split by namespace separator ("::")
3. Take last component and convert to lowercase
4. Preserve underscores in token type names

**Examples**:

| JWT Issuer | Trusted Issuer Name | Token Mapping | Result Key |
|------------|---------------------|---------------|------------|
| `https://idp.acme.com/auth` | `"Acme"` | `Jans::Access_Token` | `acme_access_token` |
| `https://accounts.google.com` | `"Google"` | `Jans::Id_Token` | `google_id_token` |
| `https://idp.dolphin.sea/auth` | `"Dolphin"` | `Acme::DolphinToken` | `dolphin_acme_dolphin_token` |
| `https://login.microsoftonline.com/tenant` | `"Microsoft"` | `Jans::Access_Token` | `microsoft_access_token` |

### Claim Access in Policies

All JWT claims are accessible via Cedar tag operations:

```cedar
// Check if claim exists
context.tokens.acme_access_token.hasTag("scope")

// Get claim value (always returns a Set)
context.tokens.acme_access_token.getTag("scope")

// Check if Set contains value
context.tokens.acme_access_token.getTag("scope").contains("read:profile")
```

**Common Patterns**:

```cedar
// Single-valued claim (sub, email, etc.)
context.tokens.acme_id_token.hasTag("sub") &&
context.tokens.acme_id_token.getTag("sub").contains("user123")

// Multi-valued claim (scope, aud, roles)
context.tokens.acme_access_token.hasTag("scope") &&
context.tokens.acme_access_token.getTag("scope").contains("admin")

// Numeric claim (stored as string in default mode)
context.tokens.acme_access_token.hasTag("age") &&
context.tokens.acme_access_token.getTag("age").contains("25")

// Custom claim from custom token type
context.tokens.dolphin_acme_dolphin_token.hasTag("waiver") &&
context.tokens.dolphin_acme_dolphin_token.getTag("waiver").contains("signed")
```

### Schema-Aware Type Mapping

When a Cedar schema is defined for token entities, claims can use enhanced typing:

```cedar
entity Access_Token = {
  "token_type": String,
  "jti": String,
  "issuer": String,
  "exp": Long,
  "validated_at": Long,
  "iat": Long,
  "nbf": Long,
  "scope": Set<String>,
  "aud": Set<String>,
  "client_id": String,
  "custom_numeric_claim": Long,
  "custom_boolean_claim": Boolean
} tags String;
```

With this schema:
- Numeric claims parsed as `Long`
- Boolean claims parsed as `Boolean`
- Timestamp claims parsed as `Long`
- Multi-valued claims parsed as `Set<String>`
- Unknown claims still stored in tags as `Set<String>`

### Custom Token Types

Multi-issuer authorization supports arbitrary token mappings:

```json
{
  "mapping": "Healthcare::ConsentToken",
  "payload": "eyJhbGc..."
}
```

This creates an entity of type `Healthcare::ConsentToken` with:
- Standard token metadata fields
- All JWT claims as tags
- Accessible as `context.tokens.healthcare_consent_token` (naming depends on issuer)

**Policy Example**:
```cedar
permit(
  principal,
  action == Healthcare::Action::"AccessRecord",
  resource in Healthcare::MedicalRecord
) when {
  context has tokens.hipaa_consent_token &&
  context.tokens.hipaa_consent_token.hasTag("patient_consent") &&
  context.tokens.hipaa_consent_token.getTag("patient_consent").contains("granted") &&
  context.tokens.hipaa_consent_token.hasTag("consent_expires") &&
  context.tokens.hipaa_consent_token.getTag("consent_expires").contains("2025-12-31")
};
```

### Token Validation Constraints

**One Token Per Type Per Issuer**:
- Each issuer can only provide one token of each type
- Multiple `Jans::Access_Token` from same issuer → **Error** (non-deterministic)
- `Jans::Access_Token` from Acme + `Jans::Id_Token` from Acme → **OK**
- `Jans::Access_Token` from Acme + `Jans::Access_Token` from Google → **OK**

**Failed Token Handling**:
- Invalid tokens are ignored (logged but don't block processing)
- Authorization continues with remaining valid tokens
- At least one valid token required

**Trusted Issuer Requirement**:
- Tokens must come from issuers in trusted issuer configuration
- Unknown issuers are rejected
- Issuer validation uses JWT `iss` claim

### Migration Path

Applications can migrate from standard authorization to multi-issuer:

**Phase 1**: Start schema-less
- Use default `Set<String>` for all claims
- Rapid development and testing
- Flexible policy authoring

**Phase 2**: Add schema
- Define Cedar schema for token entities
- Enable proper type casting
- Enhanced type safety for production

**Phase 3**: Refine policies
- Leverage typed claims for better validation
- Add cross-token validation rules
- Implement fine-grained multi-issuer policies

## Comparison: Standard vs Multi-Issuer Mapping

| Aspect | Standard (authorize) | Multi-Issuer (authorize_multi_issuer) |
|--------|---------------------|----------------------------------------|
| Entities Created | User, Workload, Role, Tokens | Token entities only |
| Principal | User/Workload required | No principal (principal-less) |
| Token Types | Fixed (access, id, userinfo) | Flexible via mapping field |
| Issuer Support | Single issuer expected | Multiple issuers native |
| Claim Access | Via principal attributes | Via token tags |
| Context Structure | `context.user`, `context.workload` | `context.tokens.{name}` |
| Use Case | Standard RBAC/ABAC | Federation, multi-org, API gateways |