---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - multi-issuer
  - federation
---

# Multi-Issuer Authorization Guide

This guide provides a comprehensive overview of Cedarling's multi-issuer authorization feature, including concepts, implementation patterns, and real-world use cases.

## Overview

Multi-issuer authorization (`authorize_multi_issuer`) enables applications to make authorization decisions based on multiple JWT tokens from different identity providers in a single request. Unlike traditional authorization that creates User and Workload principals, multi-issuer authorization evaluates policies based purely on token entities themselves.

### Key Benefits

- **Federation Support**: Native support for tokens from multiple identity providers
- **Capability-Based Authorization**: Make decisions based on capabilities asserted by different issuers
- **Zero Trust Architecture**: Each token represents verification from a different trust boundary
- **Flexible Token Types**: Support for custom token types beyond standard OAuth/OIDC tokens
- **API Gateway Ready**: Ideal for API gateways validating tokens from various upstream services

## How It Works

### 1. Request Structure

A multi-issuer authorization request consists of:

```json
{
  "tokens": [
    {
      "mapping": "Jans::Access_Token",
      "payload": "eyJhbGciOiJIUzI1NiIs..."
    },
    {
      "mapping": "Jans::Id_Token",
      "payload": "eyJhbGciOiJFZERTQSIs..."
    },
    {
      "mapping": "Acme::DolphinToken",
      "payload": "ey1b6cfMef21084633a7..."
    }
  ],
  "action": "Jans::Action::\"Read\"",
  "resource": {
    "cedar_entity_mapping": {
      "entity_type": "Jans::Document",
      "id": "doc-123"
    },
    "owner": "alice@example.com",
    "classification": "confidential"
  },
  "context": {
    "ip_address": "54.9.21.201",
    "time": 1730000000
  }
}
```

### 2. Token Processing Pipeline

```text
┌─────────────────────────────────────────────────────────┐
│ 1. Token Input                                          │
│    Array of tokens with explicit type mappings         │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 2. Token Validation                                     │
│    - Signature verification                             │
│    - Time-based validation (exp, nbf, iat)             │
│    - Status validation (revocation check)               │
│    - Trusted issuer verification                        │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 3. Entity Creation                                      │
│    - Create Cedar entity for each valid token          │
│    - Store token metadata (type, jti, issuer, exp)     │
│    - Store JWT claims as entity tags (Set<String>)     │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 4. Token Collection Assembly                            │
│    - Organize tokens with predictable naming           │
│    - Pattern: {issuer_name}_{token_type}               │
│    - Example: acme_access_token, google_id_token       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 5. Policy Evaluation                                    │
│    - Evaluate Cedar policies without principal         │
│    - Policies reference context.tokens.{name}          │
│    - Return authorization decision                      │
└─────────────────────────────────────────────────────────┘
```

### 3. Token Entity Structure

Each validated token becomes a Cedar entity:

```cedar
entity Token = {
  "token_type": String,        // e.g., "Jans::Access_Token"
  "jti": String,               // Token ID
  "iss"?: Jans::TrustedIssuer,  // JWT iss claim
  "exp": Long,                 // Expiration timestamp
  "validated_at": Long         // Validation timestamp
} tags Set<String>;
```

**All JWT claims are stored as tags** and accessed using Cedar's tag operations:

```cedar
context.tokens.acme_access_token.hasTag("scope")
context.tokens.acme_access_token.getTag("scope").contains("read:profile")
```

### 4. Token Collection Naming

Tokens are organized in the context using a deterministic naming algorithm:

**Pattern**: `{issuer_name}_{token_type}`

**Issuer Name Resolution**:

1. Look up issuer in trusted issuer metadata
2. Use the `name` field from configuration
3. If no `name` field, extract hostname from JWT `iss` claim
4. Convert to lowercase, replace special characters with underscores

**Token Type Resolution**:

1. Extract from `mapping` field (e.g., "Jans::Access_Token")
2. Split by namespace separator ("::"), and use the last segment.
3. Convert to lowercase, preserve underscores

**Examples**:

| JWT Issuer                     | Trusted Issuer Name | Token Mapping        | Result                       |
| ------------------------------ | ------------------- | -------------------- | ---------------------------- |
| `https://idp.acme.com/auth`    | `"Acme"`            | `Jans::Access_Token` | `acme_access_token`          |
| `https://accounts.google.com`  | `"Google"`          | `Jans::Id_Token`     | `google_id_token`            |
| `https://idp.dolphin.sea/auth` | `"Dolphin"`         | `Acme::DolphinToken` | `dolphin_acme_dolphin_token` |

## Use Cases

### Use Case 1: Federation Scenario

**Scenario**: A collaborative platform accepts tokens from multiple corporate identity providers.

**Requirements**:

- Users can authenticate with their corporate IDP
- Authorization requires valid token from user's organization
- Different organizations have different permission structures

**Implementation**:

```python
# User presents tokens from their corporate IDP and the platform IDP
tokens = [
    TokenInput(
        mapping="Jans::Access_Token",
        payload="<acme_corp_token>"  # From Acme Corp IDP
    ),
    TokenInput(
        mapping="Platform::Access_Token",
        payload="<platform_token>"  # From platform IDP
    )
]

request = AuthorizeMultiIssuerRequest(
    tokens=tokens,
    action='Platform::Action::"ShareDocument"',
    resource=document,
    context={"ip_address": "192.168.1.100"}
)

result = cedarling.authorize_multi_issuer(request)
```

**Policy**:

```cedar
// Allow sharing if user has valid tokens from both IDPs
permit(
  principal,
  action == Platform::Action::"ShareDocument",
  resource in Platform::Document
) when {
  // Verify corporate IDP token with employee status
  context has tokens.acme_corp_access_token &&
  context.tokens.acme_corp_access_token.hasTag("employee_status") &&
  context.tokens.acme_corp_access_token.getTag("employee_status").contains("active") &&

  // Verify platform token with sharing scope
  context has tokens.platform_access_token &&
  context.tokens.platform_access_token.hasTag("scope") &&
  context.tokens.platform_access_token.getTag("scope").contains("share:documents")
};
```

### Use Case 2: API Gateway with Multiple Upstream Services

**Scenario**: An API gateway needs to validate tokens from various upstream microservices.

**Requirements**:

- Each microservice issues its own JWT tokens
- Gateway validates all tokens before forwarding requests
- Authorization based on combination of service capabilities

**Implementation**:

```javascript
// Gateway receives tokens from multiple services
let tokens = [
  {
    mapping: "AuthService::Access_Token",
    payload: authServiceToken,
  },
  {
    mapping: "PaymentService::Access_Token",
    payload: paymentServiceToken,
  },
  {
    mapping: "UserService::Access_Token",
    payload: userServiceToken,
  },
];

let request = {
  tokens: tokens,
  action: 'Gateway::Action::"ProcessPayment"',
  resource: {
    cedar_entity_mapping: {
      entity_type: "Gateway::Transaction",
      id: "txn-12345",
    },
    amount: 1000.0,
    currency: "USD",
  },
  context: {
    ip_address: request.ip,
    user_agent: request.headers["user-agent"],
  },
};

let result = await cedarling.authorize_multi_issuer(request);
```

**Policy**:

```cedar
// Require tokens from all three services for payment processing
permit(
  principal,
  action == Gateway::Action::"ProcessPayment",
  resource in Gateway::Transaction
) when {
  // Auth service token with authenticated user
  context has tokens.auth_service_access_token &&
  context.tokens.auth_service_access_token.hasTag("authenticated") &&

  // Payment service token with sufficient balance
  context has tokens.payment_service_access_token &&
  context.tokens.payment_service_access_token.hasTag("balance_verified") &&

  // User service token with kyc_verified status
  context has tokens.user_service_access_token &&
  context.tokens.user_service_access_token.hasTag("kyc_verified") &&
  context.tokens.user_service_access_token.getTag("kyc_verified").contains("true")
};
```

### Use Case 3: Multi-Organization Voting System

**Scenario**: A trade association requires tokens from both the association and member organizations for voting.

**Requirements**:

- User must have valid membership token from trade association
- User must have valid employee token from their organization
- Organization must be a corporate member
- User must be designated as voting representative

**Implementation**:

```go
tokens := []cedarling_go.TokenInput{
    {
        Mapping: "TradeAssociation::MemberToken",
        Payload: memberToken,
    },
    {
        Mapping: "Jans::Access_Token",
        Payload: employeeToken,
    },
}

request := cedarling_go.AuthorizeMultiIssuerRequest{
    Tokens: tokens,
    Action: `TradeAssociation::Action::"Vote"`,
    Resource: cedarling_go.EntityData{
        CedarMapping: cedarling_go.CedarMapping{
            EntityType: "TradeAssociation::Election",
            ID:         "election-2025",
        },
        Payload: map[string]any{
            "election_type": "board",
            "year":          2025,
        },
    },
}

result, err := instance.AuthorizeMultiIssuer(request)
```

**Policy**:

```cedar
permit(
  principal,
  action == TradeAssociation::Action::"Vote",
  resource in TradeAssociation::Election
) when {
  // Require corporate membership token
  context has tokens.trade_association_member_token &&
  context.tokens.trade_association_member_token.hasTag("member_status") &&
  context.tokens.trade_association_member_token.getTag("member_status").contains("Corporate Member") &&

  // Require employee token with voting representative designation
  context has tokens.company_access_token &&
  context.tokens.company_access_token.hasTag("role") &&
  context.tokens.company_access_token.getTag("role").contains("voting_representative")
};
```

### Use Case 4: Healthcare HIPAA Compliance

**Scenario**: Healthcare system requires multiple consent tokens for accessing medical records.

**Requirements**:

- Patient consent token required
- Provider credentials token required
- Facility authorization token required
- Purpose of use must match all tokens

**Implementation**:

```python
tokens = [
    TokenInput(
        mapping="Healthcare::PatientConsent",
        payload=patient_consent_token
    ),
    TokenInput(
        mapping="Healthcare::ProviderCredentials",
        payload=provider_credentials_token
    ),
    TokenInput(
        mapping="Healthcare::FacilityAuth",
        payload=facility_token
    )
]

request = AuthorizeMultiIssuerRequest(
    tokens=tokens,
    action='Healthcare::Action::"AccessMedicalRecord"',
    resource=medical_record,
    context={
        "purpose_of_use": "TREATMENT",
        "emergency": False
    }
)
```

**Policy**:

```cedar
permit(
  principal,
  action == Healthcare::Action::"AccessMedicalRecord",
  resource in Healthcare::MedicalRecord
) when {
  // Patient consent token
  context has tokens.patient_consent &&
  context.tokens.patient_consent.hasTag("consent_status") &&
  context.tokens.patient_consent.getTag("consent_status").contains("granted") &&
  context.tokens.patient_consent.hasTag("expiry") &&
  context.tokens.patient_consent.getTag("expiry").contains("2025-12-31") &&

  // Provider credentials token
  context has tokens.provider_credentials &&
  context.tokens.provider_credentials.hasTag("license_status") &&
  context.tokens.provider_credentials.getTag("license_status").contains("active") &&
  context.tokens.provider_credentials.hasTag("specialty") &&

  // Facility authorization token
  context has tokens.facility_auth &&
  context.tokens.facility_auth.hasTag("facility_type") &&
  context.tokens.facility_auth.getTag("facility_type").contains("hospital") &&

  // Purpose of use alignment
  context has purpose_of_use &&
  context.purpose_of_use == "TREATMENT"
};
```

### Use Case 5: Zero Trust Network with Custom Token Types

**Scenario**: A zero-trust architecture uses custom tokens for device attestation, network verification, and user authentication.

**Requirements**:

- Device attestation token from hardware TPM
- Network security token from network controller
- User authentication token from IDP
- All three required for accessing sensitive resources

**Implementation**:

```javascript
let tokens = [
  {
    mapping: "Security::DeviceAttestation",
    payload: deviceAttestationToken, // From device TPM
  },
  {
    mapping: "Security::NetworkToken",
    payload: networkToken, // From network controller
  },
  {
    mapping: "Jans::Access_Token",
    payload: userAccessToken, // From IDP
  },
];

let result = await cedarling.authorize_multi_issuer({
  tokens: tokens,
  action: 'Security::Action::"AccessClassified"',
  resource: {
    cedar_entity_mapping: {
      entity_type: "Security::Document",
      id: "classified-123",
    },
    classification: "SECRET",
    compartment: "SPECIAL_ACCESS",
  },
  context: {
    location: "secure_facility",
    time: Date.now(),
  },
});
```

**Policy**:

```cedar
permit(
  principal,
  action == Security::Action::"AccessClassified",
  resource in Security::Document
) when {
  // Device attestation with hardware-backed key
  context has tokens.device_attestation &&
  context.tokens.device_attestation.hasTag("tpm_verified") &&
  context.tokens.device_attestation.getTag("tpm_verified").contains("true") &&
  context.tokens.device_attestation.hasTag("encryption_level") &&
  context.tokens.device_attestation.getTag("encryption_level").contains("FIPS-140-2") &&

  // Network token from secure network
  context has tokens.network_token &&
  context.tokens.network_token.hasTag("network_type") &&
  context.tokens.network_token.getTag("network_type").contains("CLASSIFIED") &&
  context.tokens.network_token.hasTag("segment") &&
  context.tokens.network_token.getTag("segment").contains("HIGH_SIDE") &&

  // User access token with clearance
  context has tokens.user_access_token &&
  context.tokens.user_access_token.hasTag("clearance_level") &&
  context.tokens.user_access_token.getTag("clearance_level").contains("SECRET") &&

  // Location verification
  context has location &&
  context.location == "secure_facility"
};
```

## Configuration Guide

### Schema Requirements

**IMPORTANT**: Multi-issuer authorization requires specific Cedar schema modifications. Without these changes, authorization will fail with schema validation errors.

#### Required Schema Changes

Multi-issuer authorization creates token entities dynamically and places them in the Cedar context. Your schema must support:

**1. Token Entity Structure**

Token entities must have these required attributes:

```cedar
namespace Jans{
  entity Access_token = {
    token_type?: String,        // Required for multi-issuer
    jti?: String,               // Required for multi-issuer
    iss?: Jans::TrustedIssuer,  // Required for multi-issuer
    exp?: Long,                 // Required for multi-issuer
    validated_at?: Long,        // Required for multi-issuer
    // Other JWT claims as optional attributes
    aud?: String,
    iat?: Long,
    scope?: Set<String>,
    // ...
  } tags Set<String>;         // Required for dynamic JWT claims

  entity TrustedIssuer = {
    issuer_entity_id: Url
  };
}
```

**2. Context Structure**

The Context type must include a `tokens` field:

```cedar
type Context = {
  network?: String,
  // ... other context fields
  tokens?: TokensContext,   // Required for multi-issuer
};

type TokensContext = {
  total_token_count: Long,  // Required
  // Individual token fields added dynamically
};
```

**3. Making Attributes Optional**

All token entity attributes (except the core multi-issuer fields) must be optional (`?`) to prevent schema validation errors. This is because multi-issuer tokens may not have all the claims that standard authorization tokens have.

#### Why These Changes Are Needed

- **Dynamic token entities**: Multi-issuer authorization creates token entities on-the-fly without User/Workload principals
- **Tag-based claims**: JWT claims are stored as entity tags (`Set<String>` by default) for flexible access
- **Context structure**: Tokens are organized in `context.tokens.{issuer}_{token_type}` format
- **Schema validation**: Cedar validates entities against the schema; missing required fields cause errors

#### Updating Core Schema

If you're using the default `cedarling_core.cedarschema` from Agama Lab, it has been updated to support multi-issuer authorization. If you have a custom schema, make sure to apply these changes.

### Policy Store Configuration

Configure trusted issuers with the `name` field for predictable token naming:

```json
{
  "trusted_issuers": {
    "acme_corp_issuer": {
      "name": "AcmeCorp",
      "description": "Acme Corporation Identity Provider",
      "openid_configuration_endpoint": "https://idp.acme.com/.well-known/openid-configuration",
      "token_metadata": {
        "access_token": {
          "entity_type_name": "AcmeCorp::Access_Token",
          "token_id": "jti"
        }
      }
    },
    "google_issuer": {
      "name": "Google",
      "description": "Google Identity Provider",
      "openid_configuration_endpoint": "https://accounts.google.com/.well-known/openid-configuration",
      "token_metadata": {
        "id_token": {
          "entity_type_name": "Google::Id_Token",
          "token_id": "jti"
        }
      }
    },
    "custom_service_issuer": {
      "name": "CustomService",
      "description": "Custom Service Provider",
      "openid_configuration_endpoint": "https://service.example.com/.well-known/openid-configuration",
      "token_metadata": {
        "custom_token": {
          "entity_type_name": "CustomService::ServiceToken",
          "token_id": "jti"
        }
      }
    }
  }
}
```

### Cedar Schema for Multi-Issuer Tokens

#### Core Token Schema Structure

Each token type (Access_Token, Id_Token, custom tokens) must follow this structure:

```cedar
namespace Jans {
  // Core token entity structure compatible with both standard and multi-issuer authorization
  entity Access_token = {
    // Required multi-issuer attributes
    token_type?: String,        // Entity type name (e.g., "Jans::Access_Token")
    jti?: String,               // JWT ID - unique token identifier
    iss?: TrustedIssuer,        // Issuer entity reference (for standard authz)
    exp?: Long,                 // Token expiration timestamp
    validated_at?: Long,        // Timestamp when token was validated

    // Optional JWT claims (make all optional for compatibility)
    aud?: String,               // Audience
    iat?: Long,                 // Issued at
    scope?: Set<String>,        // OAuth scopes
    client_id?: String,         // Client identifier
    sub?: String,               // Subject
    // Add other JWT claims as needed
  } tags Set<String>;           // Tags store dynamic JWT claims

  entity id_token = {
    // Required multi-issuer attributes
    token_type?: String,
    jti?: String,
    iss?: TrustedIssuer,
    exp?: Long,
    validated_at?: Long,

    // Optional JWT claims
    aud?: Set<String>,
    iat?: Long,
    sub?: String,
    email?: email_address,
    name?: String,
    phone_number?: String,
    role?: Set<String>,
    acr?: String,
    amr?: Set<String>,
    // Add other JWT claims as needed
  } tags Set<String>;

  entity Userinfo_token = {
    // Required multi-issuer attributes
    token_type?: String,
    jti?: String,
    iss?: TrustedIssuer,
    exp?: Long,
    validated_at?: Long,

    // Optional JWT claims
    aud?: String,
    iat?: Long,
    sub?: String,
    email?: email_address,
    name?: String,
    birthdate?: String,
    phone_number?: String,
    role?: Set<String>,
    // Add other JWT claims as needed
  } tags Set<String>;

  entity TrustedIssuer = {
    issuer_entity_id: Url
  };
}

```

#### Custom Token Types

For custom token types, follow the same pattern:

```cedar
namespace Custom {
  entity ServiceToken = {
    // Required multi-issuer attributes
    token_type?: String,
    jti?: String,
    iss?: Custom::TrustedIssuer,
    exp?: Long,
    validated_at?: Long,

    // Custom token-specific attributes
    service_id?: String,
    permissions?: Set<String>,
    service_tier?: String,
  } tags Set<String>;

  entity TrustedIssuer = {
    issuer_entity_id: Url
  };
}
```

#### Complete Context Schema

Define the Context type to include the tokens field:

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

  // Multi-issuer tokens context (required)
  tokens?: TokensContext,
};

type TokensContext = {
  total_token_count: Long,
  // Individual token fields are added dynamically by Cedarling
  // Pattern: {issuer_name}_{token_type} (e.g., acme_access_token)
};
```

#### Key Schema Principles

1. **Optional Attributes**: All token attributes must be optional (`?`) to support both standard and multi-issuer authorization
2. **Tags Declaration**: All token entities must declare `tags Set<String>` for dynamic JWT claim storage
3. **Context Integration**: The Context type must include an optional `tokens` field
4. **Consistency**: Use the same attribute names across all token types (token_type, jti, issuer, exp, validated_at)

## Error Handling

### Token Validation Failures

```python
# Individual token validation failures are handled gracefully
tokens = [
    TokenInput(mapping="Jans::Access_Token", payload="valid_token"),
    TokenInput(mapping="Jans::Id_Token", payload="invalid_token"),  # Will be ignored
    TokenInput(mapping="Acme::CustomToken", payload="valid_custom_token")
]

# Authorization continues with valid tokens
# Invalid tokens are logged but don't block processing
result = cedarling.authorize_multi_issuer(request)
```

However, if every token is invalid, Cedarling will raise an error. **It is important that users always handle errors gracefully.**

### Non-Deterministic Tokens

```python
# ERROR: Multiple tokens of same type from same issuer
tokens = [
    TokenInput(mapping="Jans::Access_Token", payload="token1"),  # From Jans::Access_Token
    TokenInput(mapping="Jans::Access_Token", payload="token2"),  # Also from Jans::Access_Token - ERROR!
]

# This non-deterministic.
# Which token should policies reference?
# Cedarling processes only the first item and writes log messages for all subsequent items that are skipped.
```

### Trusted Issuer Validation

```python
# Tokens from unknown issuers are rejected
tokens = [
    TokenInput(
        mapping="Jans::Access_Token",
        payload="token_from_unknown_issuer"  # ERROR if issuer not in trusted issuers
    )
]

# Only tokens from issuers configured in policy store are accepted
```

## Best Practices

### 1. Use Descriptive Issuer Names

Configure clear, predictable issuer names in your policy store:

```json
{
  "name": "AcmeCorp", // Good - clear and predictable
  "name": "Issuer1" // Bad - unclear what this represents
}
```

### 2. Start Schema-Less for Development

Begin without Cedar schemas for rapid development:

- All claims stored in tags as `Set<String>`  
- Flexible and forgiving during development  
- Add schemas later for production type safety  

### 3. Implement Comprehensive Logging

Monitor token validation and policy evaluation:

```python
result = cedarling.authorize_multi_issuer(request)

# Retrieve logs for debugging
logs = cedarling.get_logs_by_request_id(result.request_id)
for log in logs:
    print(f"Log: {log}")
```

### 4. Handle Failed Tokens Gracefully

Design policies to work with partial token sets:

```cedar
// Allow if EITHER token is present
permit(
  principal,
  action == Jans::Action::"Read",
  resource in Jans::Document
) when {
  (context has tokens.acme_access_token &&
   context.tokens.acme_access_token.hasTag("scope") &&
   context.tokens.acme_access_token.getTag("scope").contains("read")) ||
  (context has tokens.google_access_token &&
   context.tokens.google_access_token.hasTag("scope") &&
   context.tokens.google_access_token.getTag("scope").contains("read"))
};
```

### 5. Test with Multiple Issuer Combinations

Test policies with various token combinations:

```python
# Test with all tokens
all_tokens_result = cedarling.authorize_multi_issuer(all_tokens_request)

# Test with subset of tokens
partial_tokens_result = cedarling.authorize_multi_issuer(partial_tokens_request)

# Test with invalid tokens mixed in
mixed_tokens_result = cedarling.authorize_multi_issuer(mixed_tokens_request)
```

## Migration from Standard Authorization

Migrating from `authorize()` to `authorize_multi_issuer()`:

### Before (Standard Authorization)

```python
request = Request(
    tokens={
        "access_token": access_token,
        "id_token": id_token
    },
    action='Jans::Action::"Read"',
    resource=resource,
    context=context
)

result = cedarling.authorize(request)
if result.is_allowed():
    # Access granted
```

**Policy**:

```cedar
permit(
  principal is Jans::User,
  action == Jans::Action::"Read",
  resource in Jans::Document
) when {
  principal.email == resource.owner
};
```

### After (Multi-Issuer Authorization)

```python
request = AuthorizeMultiIssuerRequest(
    tokens=[
        TokenInput(mapping="Jans::Access_Token", payload=access_token),
        TokenInput(mapping="Jans::Id_Token", payload=id_token)
    ],
    action='Jans::Action::"Read"',
    resource=resource,
    context=context
)

result = cedarling.authorize_multi_issuer(request)
if result.decision:
    # Access granted
```

**Policy**:

```cedar
permit(
  principal,
  action == Jans::Action::"Read",
  resource in Jans::Document
) when {
  context has tokens.jans_id_token &&
  context.tokens.jans_id_token.hasTag("email") &&
  context.tokens.jans_id_token.getTag("email").contains(resource.owner)
};
```

## See Also

- [Cedarling Authorization](./cedarling-authz.md)
- [Cedarling Interfaces](./cedarling-interfaces.md)
- [JWT Mapping](./cedarling-jwt-mapping.md)
- [Policy Store Configuration](./cedarling-policy-store.md)
- [Python Examples](./python/usage.md)
- [Go Examples](./getting-started/go.md)
