---
tags:
  - administration
  - shibboleth
  - config-api
  - rest-api
---

# Shibboleth IDP Config API

The Janssen Config API provides REST endpoints for managing Shibboleth IDP configuration and trusted Service Providers.

## Authentication

All endpoints require OAuth 2.0 authentication with appropriate scopes.

### Required Scopes

| Scope | Description |
|-------|-------------|
| `https://jans.io/oauth/config/shibboleth.readonly` | Read IDP configuration |
| `https://jans.io/oauth/config/shibboleth.write` | Modify IDP configuration |

### Example Token Request

```bash
ACCESS_TOKEN=$(curl -s -X POST \
  "https://auth.example.com/jans-auth/restv1/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=your-client-id" \
  -d "client_secret=your-client-secret" \
  -d "scope=https://jans.io/oauth/config/shibboleth.readonly https://jans.io/oauth/config/shibboleth.write" \
  | jq -r '.access_token')
```

## API Endpoints

### Get IDP Configuration

Retrieve the current Shibboleth IDP configuration.

**Request:**
```http
GET /jans-config-api/shibboleth/config
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "entityId": "https://idp.example.com/idp/shibboleth",
  "scope": "example.com",
  "enabled": true,
  "metadataUrl": "https://idp.example.com/idp/shibboleth",
  "signingKeyAlias": "idp-signing",
  "encryptionKeyAlias": "idp-encryption",
  "jansAuthEnabled": true,
  "jansAuthClientId": "shibboleth-client",
  "jansAuthScopes": "openid,profile,email"
}
```

**cURL Example:**
```bash
curl -X GET \
  "https://api.example.com/jans-config-api/shibboleth/config" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Accept: application/json"
```

### Update IDP Configuration

Update the Shibboleth IDP configuration.

**Request:**
```http
PUT /jans-config-api/shibboleth/config
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "entityId": "https://idp.example.com/idp/shibboleth",
  "scope": "example.com",
  "enabled": true,
  "signingKeyAlias": "idp-signing",
  "encryptionKeyAlias": "idp-encryption",
  "jansAuthEnabled": true,
  "jansAuthClientId": "shibboleth-client-updated",
  "jansAuthScopes": "openid,profile,email,address"
}
```

**Response:**
```json
{
  "entityId": "https://idp.example.com/idp/shibboleth",
  "scope": "example.com",
  "enabled": true,
  "metadataUrl": "https://idp.example.com/idp/shibboleth",
  "signingKeyAlias": "idp-signing",
  "encryptionKeyAlias": "idp-encryption",
  "jansAuthEnabled": true,
  "jansAuthClientId": "shibboleth-client-updated",
  "jansAuthScopes": "openid,profile,email,address"
}
```

### List Trusted Service Providers

Get all configured trusted Service Providers.

**Request:**
```http
GET /jans-config-api/shibboleth/trust
Authorization: Bearer {access_token}
```

**Response:**
```json
[
  {
    "entityId": "https://sp1.example.org",
    "name": "Example SP 1",
    "description": "First example service provider",
    "enabled": true,
    "metadataUrl": "https://sp1.example.org/metadata",
    "releasedAttributes": ["uid", "mail", "displayName"]
  },
  {
    "entityId": "https://sp2.example.org",
    "name": "Example SP 2",
    "description": "Second example service provider",
    "enabled": true,
    "metadataUrl": "https://sp2.example.org/metadata",
    "releasedAttributes": ["uid", "mail"]
  }
]
```

### Get Trusted Service Provider

Get a specific trusted Service Provider by entity ID.

**Request:**
```http
GET /jans-config-api/shibboleth/trust/{entityId}
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "entityId": "https://sp1.example.org",
  "name": "Example SP 1",
  "description": "First example service provider",
  "enabled": true,
  "metadataUrl": "https://sp1.example.org/metadata",
  "releasedAttributes": ["uid", "mail", "displayName"],
  "assertionLifetime": 300,
  "signAssertions": true,
  "encryptAssertions": false
}
```

### Add Trusted Service Provider

Add a new trusted Service Provider.

**Request:**
```http
POST /jans-config-api/shibboleth/trust
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "entityId": "https://new-sp.example.org",
  "name": "New Service Provider",
  "description": "A new service provider",
  "enabled": true,
  "metadataUrl": "https://new-sp.example.org/metadata",
  "releasedAttributes": ["uid", "mail", "displayName", "eduPersonPrincipalName"],
  "assertionLifetime": 300,
  "signAssertions": true,
  "encryptAssertions": true
}
```

**Response (201 Created):**
```json
{
  "entityId": "https://new-sp.example.org",
  "name": "New Service Provider",
  "description": "A new service provider",
  "enabled": true,
  "metadataUrl": "https://new-sp.example.org/metadata",
  "releasedAttributes": ["uid", "mail", "displayName", "eduPersonPrincipalName"],
  "assertionLifetime": 300,
  "signAssertions": true,
  "encryptAssertions": true
}
```

### Update Trusted Service Provider

Update an existing trusted Service Provider.

**Request:**
```http
PUT /jans-config-api/shibboleth/trust/{entityId}
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "name": "Updated Service Provider",
  "description": "Updated description",
  "enabled": true,
  "metadataUrl": "https://new-sp.example.org/metadata",
  "releasedAttributes": ["uid", "mail"],
  "assertionLifetime": 600,
  "signAssertions": true,
  "encryptAssertions": false
}
```

### Delete Trusted Service Provider

Remove a trusted Service Provider.

**Request:**
```http
DELETE /jans-config-api/shibboleth/trust/{entityId}
Authorization: Bearer {access_token}
```

**Response:** `204 No Content`

## Error Responses

### 401 Unauthorized

```json
{
  "error": "unauthorized",
  "error_description": "Invalid or expired access token"
}
```

### 404 Not Found

```json
{
  "error": "not_found",
  "error_description": "Service provider with entity ID not found"
}
```

### 409 Conflict

```json
{
  "error": "conflict",
  "error_description": "Service Provider with this entity ID already exists"
}
```

## OpenAPI Specification

The complete OpenAPI specification is available at:

```
https://api.example.com/jans-config-api/openapi.json
```

Filter for Shibboleth endpoints:
```bash
curl -s "https://api.example.com/jans-config-api/openapi.json" | \
  jq '.paths | with_entries(select(.key | contains("shibboleth")))'
```
