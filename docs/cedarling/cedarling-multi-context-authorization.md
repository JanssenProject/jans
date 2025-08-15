---
tags:
  - administration
  - authorization / authz
  - Cedar
  - Cedarling
  - multi-context
---

# Multi-Context Authorization

Cedarling supports **Multi-Context Authorization**, which enables processing multiple token bundles from different issuers/authorities in a single authorization request. This feature addresses real-world scenarios where a workload or user needs to present multiple transaction tokens from different sources, and can handle a mix of signed (JWT) and unsigned (pre-built principal) requests within the same multi-context call.

## Overview

The multi-context authorization feature allows Cedarling to process multiple token bundles from different issuers/authorities in a single authorization request. This is particularly useful for complex scenarios where authorization decisions depend on multiple sources of identity and context. The system now supports:

- **Mixed signed/unsigned requests**: Some contexts can use JWT tokens while others use pre-built principal entities
- **Dynamic token type support**: Custom token types beyond standard OAuth2/OIDC tokens
- **Flexible context processing**: Each context can be processed independently with different validation rules

## Use Cases

### Government Services

Multiple agencies (DMV, IRS, State Police) each provide their own tokens for vehicle registration or tax processing.

### Healthcare

Doctor credentials, patient consent, insurance verification all need to be validated together.

### Financial Services

Bank identity, credit bureau data, fraud detection tokens from different financial institutions.

### IoT/Smart Cities

Device authentication, location services, environmental sensors providing different types of authorization data.

### Mixed Authentication Scenarios

Some contexts use JWT tokens while others use pre-built principals, allowing for flexible authentication strategies.

## API Design

### Request Structure

```rust
pub struct MultiContextTokenBundle {
    pub tokens: Option<HashMap<String, String>>,  // Token type -> JWT string (for signed requests)
    pub principals: Option<Vec<EntityData>>,      // Pre-built principal entities (for unsigned requests)
    pub context_id: Option<String>,               // Optional context identifier
}

pub struct MultiContextRequest {
    pub token_bundles: Vec<MultiContextTokenBundle>,
    pub action: String,
    pub resource: EntityData,
    pub context: Value,
}
```

### Response Structure

```rust
pub struct MultiContextAuthorizeResult {
    pub context_results: HashMap<String, AuthorizeResult>,
    pub overall_decision: bool,
    pub request_id: String,
}
```

### Method Signature

```rust
pub async fn authorize_multi_context(
    &self,
    request: MultiContextRequest,
) -> Result<MultiContextAuthorizeResult, AuthorizeError>
```

## Implementation Details

### Token Processing Logic

The implementation processes each token bundle as a separate authorization context:

1. **Individual Context Processing**: Each `MultiContextTokenBundle` is processed as a separate authorization request
2. **Mixed Request Support**: Each bundle can contain either `tokens` (signed) or `principals` (unsigned), but not both
3. **Overall Decision Logic**: If any context denies, the overall decision is `DENY` (fail-safe approach)
4. **Context Identification**: Uses `context_id` if provided, otherwise uses index as string
5. **Result Aggregation**: Combines individual results into a comprehensive response

### Mixed Signed/Unsigned Support

The multi-context authorization now supports mixing signed and unsigned requests within the same call:

#### Signed Requests (JWT Tokens)

```rust
MultiContextTokenBundle {
    tokens: Some(HashMap::from([
        ("access_token".to_string(), "jwt_string".to_string()),
        ("id_token".to_string(), "jwt_string".to_string()),
    ])),
    principals: None,
    context_id: Some("signed_context".to_string()),
}
```

#### Unsigned Requests (Pre-built Principals)

```rust
MultiContextTokenBundle {
    tokens: None,
    principals: Some(vec![
        EntityData {
            id: "user123".to_string(),
            entity_type: "Jans::User".to_string(),
            attributes: HashMap::from([
                ("sub".to_string(), serde_json::Value::String("user123".to_string())),
                ("country".to_string(), serde_json::Value::String("US".to_string())),
            ]),
        }
    ]),
    context_id: Some("unsigned_context".to_string()),
}
```

#### Mixed Multi-Context Request

```rust
let request = MultiContextRequest {
    token_bundles: vec![
        // Signed context
        MultiContextTokenBundle {
            tokens: Some(HashMap::from([
                ("access_token".to_string(), access_token.clone()),
                ("id_token".to_string(), id_token.clone()),
            ])),
            principals: None,
            context_id: Some("signed_context".to_string()),
        },
        // Unsigned context
        MultiContextTokenBundle {
            tokens: None,
            principals: Some(vec![
                EntityData {
                    id: "user123".to_string(),
                    entity_type: "Jans::User".to_string(),
                    attributes: HashMap::from([
                        ("sub".to_string(), serde_json::Value::String("user123".to_string())),
                        ("country".to_string(), serde_json::Value::String("US".to_string())),
                    ]),
                }
            ]),
            context_id: Some("unsigned_context".to_string()),
        }
    ],
    action: "Jans::Action::\"Update\"".to_string(),
    resource: resource_data,
    context: context_data,
};
```

## Dynamic Token Type Support

The multi-context authorization feature supports **dynamic token types** beyond the standard ones. This allows for extensible token mapping without hardcoding.

### Standard Token Types

- `id_token`: Identity tokens (OIDC standard)
- `access_token`: Access tokens (OAuth2 standard)
- `userinfo_token`: User info tokens (OIDC standard)
- `tx_token`: Transaction tokens (custom)

### Custom Token Types

The system supports any custom token type defined in your policy store configuration:

- `dolphin_token`: Custom marine research tokens
- `healthcare_token`: Custom healthcare tokens
- `emissions_token`: Custom environmental tokens
- Any custom token type you define

### Token Type Configuration

Custom token types are mapped to Cedar entities based on the policy store configuration:

```yaml
# Policy store configuration
trusted_issuers:
  marine_research_issuer:
    name: "Marine Research Authority"
    description: "Marine research token issuer"
    openid_configuration_endpoint: "https://marine.auth.org/.well-known/openid-configuration"
    token_metadata:
      dolphin_token:
        entity_type_name: "Marine::DolphinToken"
        principal_mapping:
          - "Marine::Researcher"
        user_id: "sub"
        workload_id: "pod_id"
        claim_mapping:
          blowhole_size:
            parser: "regex"
            regex_expression: "^(?P<SIZE>\\d+)$"
            SIZE:
              attr: "size"
              type: "Number"
          pod_name:
            attr: "pod_name"
            type: "String"
```

### Dynamic Token Processing

The system handles unknown token types gracefully:

1. **Policy Store Lookup**: First checks if the token type is defined in the policy store's `token_metadata`
2. **Entity Type Mapping**: Maps the token to the specified `entity_type_name`
3. **Fallback Behavior**: If not found in policy store, the token is skipped (graceful degradation)
4. **Extensible Design**: New token types can be added without code changes

## Migration Guide

### From Single-Context to Multi-Context

1. **Wrap existing tokens**: Convert single token map to `MultiContextTokenBundle`
2. **Add context IDs**: Optionally add meaningful context identifiers
3. **Update client code**: Use `authorize_multi_context` instead of `authorize`
4. **Handle new response format**: Process `context_results` and `overall_decision`

### Example Migration

**Before (Single Context)**:

```rust
let request = Request {
    tokens: Some(HashMap::from([
        ("access_token".to_string(), "jwt_string".to_string()),
    ])),
    action: "Update".to_string(),
    resource: resource_data,
    context: context_data,
};
let result = cedarling.authorize(request).await?;
```

**After (Multi Context)**:

```rust
let request = MultiContextRequest {
    token_bundles: vec![
        MultiContextTokenBundle {
            tokens: Some(HashMap::from([
                ("access_token".to_string(), "jwt_string".to_string()),
            ])),
            principals: None,
            context_id: Some("primary_context".to_string()),
        }
    ],
    action: "Update".to_string(),
    resource: resource_data,
    context: context_data,
};
let result = cedarling.authorize_multi_context(request).await?;
```

### From Unsigned to Mixed Multi-Context

**Before (Unsigned Only)**:

```rust
let request = RequestUnsigned {
    principals: vec![principal_data],
    action: "Update".to_string(),
    resource: resource_data,
    context: context_data,
};
let result = cedarling.authorize_unsigned(request).await?;
```

**After (Mixed Multi-Context)**:

```rust
let request = MultiContextRequest {
    token_bundles: vec![
        MultiContextTokenBundle {
            tokens: None,
            principals: Some(vec![principal_data]),
            context_id: Some("unsigned_context".to_string()),
        }
    ],
    action: "Update".to_string(),
    resource: resource_data,
    context: context_data,
};
let result = cedarling.authorize_multi_context(request).await?;
```

## Related Documentation

- [Cedarling Authorization](./cedarling-authz.md) - General authorization concepts
- [Cedarling Policy Store](./cedarling-policy-store.md) - Policy store configuration
- [Cedarling JWT Validation](./cedarling-jwt-validation.md) - JWT validation details
- [Cedarling Entities](./cedarling-entities.md) - Entity data structures
