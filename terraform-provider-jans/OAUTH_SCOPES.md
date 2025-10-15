# OAuth Scopes Reference - Terraform Provider for Janssen

## Overview

This document lists all OAuth scopes required for the Terraform Provider for Janssen resources and data sources. Configure your OAuth client with these scopes to enable full functionality.

## New Resources & Data Sources (2025-10-13)

### Session Management

**Data Source: `jans_sessions`**
- Scope: `https://jans.io/oauth/jans-auth-server/session.readonly`
- Purpose: List all active user sessions

**Resource: `jans_session_revocation`**
- Scopes: 
  - `revoke_session`
  - `https://jans.io/oauth/jans-auth-server/session.delete`
- Purpose: Revoke all sessions for a user
- **Note**: BOTH scopes must be granted simultaneously

### Token Management

**Data Source: `jans_tokens`**
- Scope: `https://jans.io/oauth/config/token.readonly`
- Purpose: Search and retrieve token information

**Resource: `jans_token_revocation`**
- Scope: `https://jans.io/oauth/config/token.delete`
- Purpose: Revoke specific tokens

### SSA Management

**Resource: `jans_ssa_revocation`**
- Scope: `https://jans.io/oauth/config/ssa.delete`
- Purpose: Revoke Software Statement Assertions

### Statistics

**Data Source: `jans_statistics`**
- Scope: `https://jans.io/oauth/config/stats.readonly`
- Purpose: Retrieve server statistics and metrics

## Existing Resources & Data Sources

### Application Configuration

**Resource: `jans_app_configuration`**
- Read: `https://jans.io/oauth/jans-auth-server/config/properties.readonly`
- Write: `https://jans.io/oauth/jans-auth-server/config/properties.write`

**Resource: `jans_api_app_configuration`**
- Read: `https://jans.io/oauth/config/api/properties.readonly`
- Write: `https://jans.io/oauth/config/api/properties.write`

**Resource: `jans_scim_app_configuration`**
- Read: `https://jans.io/oauth/config/scim/properties.readonly`
- Write: `https://jans.io/oauth/config/scim/properties.write`

### OIDC Clients

**Resource: `jans_oidc_client`**
**Data Source: `jans_oidc_client`**
- Read: `https://jans.io/oauth/config/openid/clients.readonly`
- Write: `https://jans.io/oauth/config/openid/clients.write`
- Delete: `https://jans.io/oauth/config/openid/clients.delete`

### Scopes

**Resource: `jans_scope`**
- Read: `https://jans.io/oauth/config/scopes.readonly`
- Write: `https://jans.io/oauth/config/scopes.write`
- Delete: `https://jans.io/oauth/config/scopes.delete`

### Attributes

**Resource: `jans_attribute`**
- Read: `https://jans.io/oauth/config/attributes.readonly`
- Write: `https://jans.io/oauth/config/attributes.write`
- Delete: `https://jans.io/oauth/config/attributes.delete`

### Scripts

**Resource: `jans_script`**
**Data Source: `jans_custom_script_types`**
- Read: `https://jans.io/oauth/config/scripts.readonly`
- Write: `https://jans.io/oauth/config/scripts.write`
- Delete: `https://jans.io/oauth/config/scripts.delete`

### SCIM Resources

**Resource: `jans_user`**
**Data Source: `jans_user`**
- Read: `https://jans.io/scim/users.read`
- Write: `https://jans.io/scim/users.write`

**Resource: `jans_group`**
- Read: `https://jans.io/scim/groups.read`
- Write: `https://jans.io/scim/groups.write`

**Resource: `jans_custom_user`**
- Read: `https://jans.io/scim/users.read`
- Write: `https://jans.io/scim/users.write`

### FIDO2

**Resource: `jans_fido2_configuration`**
**Data Source: `jans_fido2_configuration`**
- Read: `https://jans.io/oauth/config/fido2.readonly`
- Write: `https://jans.io/oauth/config/fido2.write`

### Caching

**Resource: `jans_cache_configuration`**
- Read: `https://jans.io/oauth/config/cache.readonly`
- Write: `https://jans.io/oauth/config/cache.write`

### Database

**Resource: `jans_ldap_database_configuration`**
- Read: `https://jans.io/oauth/config/database/ldap.readonly`
- Write: `https://jans.io/oauth/config/database/ldap.write`

**Data Source: `jans_persistence_config`**
- Read: `https://jans.io/oauth/config/database/persistence.readonly`

### Logging

**Resource: `jans_logging_configuration`**
- Read: `https://jans.io/oauth/config/logging.readonly`
- Write: `https://jans.io/oauth/config/logging.write`

### SMTP

**Resource: `jans_smtp_configuration`**
- Read: `https://jans.io/oauth/config/smtp.readonly`
- Write: `https://jans.io/oauth/config/smtp.write`

### Admin UI

**Resource: `jans_admin_role`**
**Resource: `jans_admin_permission`**
**Resource: `jans_admin_role_permission_mapping`**
- Read: `https://jans.io/oauth/jans-auth-server/config/adminui/permission.readonly`
- Write: `https://jans.io/oauth/jans-auth-server/config/adminui/permission.write`

### Agama

**Resource: `jans_agama_deployment`**
- Read: `https://jans.io/oauth/config/agama.readonly`
- Write: `https://jans.io/oauth/config/agama.write`
- Delete: `https://jans.io/oauth/config/agama.delete`

### UMA

**Resource: `jans_uma_resource`**
- Read: `https://jans.io/oauth/config/uma/resources.readonly`
- Write: `https://jans.io/oauth/config/uma/resources.write`

### Other

**Resource: `jans_default_authentication_method`**
- Read: `https://jans.io/oauth/config/acrs.readonly`
- Write: `https://jans.io/oauth/config/acrs.write`

**Resource: `jans_organization`**
- Read: `https://jans.io/oauth/config/organization.readonly`
- Write: `https://jans.io/oauth/config/organization.write`

**Resource: `jans_json_web_key`**
- Read: `https://jans.io/oauth/config/jwks.readonly`
- Write: `https://jans.io/oauth/config/jwks.write`

**Resource: `jans_client_authorization`**
- Read: `https://jans.io/oauth/config/openid/clients.readonly`
- Write: `https://jans.io/oauth/config/openid/clients.write`

**Resource: `jans_message`**
- Read: `https://jans.io/oauth/config/message.readonly`
- Write: `https://jans.io/oauth/config/message.write`

### Data Sources

**Data Source: `jans_health_status`**
- Scope: `https://jans.io/oauth/config/health.readonly`

**Data Source: `jans_server_stats`**
- Scope: `https://jans.io/oauth/config/stats.readonly`

**Data Source: `jans_audit_logs`**
- Scope: `https://jans.io/oauth/config/audit.readonly`

**Data Source: `jans_plugins`**
- Scope: `https://jans.io/oauth/config/plugins.readonly`

**Data Source: `jans_schema`**
- Scope: `https://jans.io/scim/schemas.read`

**Data Source: `jans_service_provider_config`**
- Scope: `https://jans.io/scim/service-provider-config.read`

## OAuth Client Configuration

### Minimal Configuration (New Resources Only)

For testing only the new session/token/SSA/statistics features:

```json
{
  "scopes": [
    "https://jans.io/oauth/jans-auth-server/session.readonly",
    "revoke_session",
    "https://jans.io/oauth/jans-auth-server/session.delete",
    "https://jans.io/oauth/config/token.readonly",
    "https://jans.io/oauth/config/token.delete",
    "https://jans.io/oauth/config/ssa.delete",
    "https://jans.io/oauth/config/stats.readonly"
  ]
}
```

### Full Configuration (All Resources)

For complete provider functionality, configure all scopes:

```json
{
  "scopes": [
    "openid",
    "profile",
    "email",
    "https://jans.io/oauth/jans-auth-server/config/properties.readonly",
    "https://jans.io/oauth/jans-auth-server/config/properties.write",
    "https://jans.io/oauth/jans-auth-server/session.readonly",
    "revoke_session",
    "https://jans.io/oauth/jans-auth-server/session.delete",
    "https://jans.io/oauth/config/token.readonly",
    "https://jans.io/oauth/config/token.delete",
    "https://jans.io/oauth/config/ssa.delete",
    "https://jans.io/oauth/config/stats.readonly",
    "https://jans.io/oauth/config/openid/clients.readonly",
    "https://jans.io/oauth/config/openid/clients.write",
    "https://jans.io/oauth/config/openid/clients.delete",
    "https://jans.io/oauth/config/scopes.readonly",
    "https://jans.io/oauth/config/scopes.write",
    "https://jans.io/oauth/config/scopes.delete",
    "https://jans.io/oauth/config/attributes.readonly",
    "https://jans.io/oauth/config/attributes.write",
    "https://jans.io/oauth/config/attributes.delete",
    "https://jans.io/oauth/config/scripts.readonly",
    "https://jans.io/oauth/config/scripts.write",
    "https://jans.io/oauth/config/scripts.delete",
    "https://jans.io/scim/users.read",
    "https://jans.io/scim/users.write",
    "https://jans.io/scim/groups.read",
    "https://jans.io/scim/groups.write",
    "https://jans.io/oauth/config/fido2.readonly",
    "https://jans.io/oauth/config/fido2.write",
    "https://jans.io/oauth/config/cache.readonly",
    "https://jans.io/oauth/config/cache.write",
    "https://jans.io/oauth/config/database/ldap.readonly",
    "https://jans.io/oauth/config/database/ldap.write",
    "https://jans.io/oauth/config/database/persistence.readonly",
    "https://jans.io/oauth/config/logging.readonly",
    "https://jans.io/oauth/config/logging.write",
    "https://jans.io/oauth/config/smtp.readonly",
    "https://jans.io/oauth/config/smtp.write",
    "https://jans.io/oauth/jans-auth-server/config/adminui/permission.readonly",
    "https://jans.io/oauth/jans-auth-server/config/adminui/permission.write",
    "https://jans.io/oauth/config/agama.readonly",
    "https://jans.io/oauth/config/agama.write",
    "https://jans.io/oauth/config/agama.delete",
    "https://jans.io/oauth/config/uma/resources.readonly",
    "https://jans.io/oauth/config/uma/resources.write",
    "https://jans.io/oauth/config/acrs.readonly",
    "https://jans.io/oauth/config/acrs.write",
    "https://jans.io/oauth/config/organization.readonly",
    "https://jans.io/oauth/config/organization.write",
    "https://jans.io/oauth/config/jwks.readonly",
    "https://jans.io/oauth/config/jwks.write",
    "https://jans.io/oauth/config/message.readonly",
    "https://jans.io/oauth/config/message.write",
    "https://jans.io/oauth/config/health.readonly",
    "https://jans.io/oauth/config/audit.readonly",
    "https://jans.io/oauth/config/plugins.readonly",
    "https://jans.io/scim/schemas.read",
    "https://jans.io/scim/service-provider-config.read",
    "https://jans.io/oauth/config/api/properties.readonly",
    "https://jans.io/oauth/config/api/properties.write",
    "https://jans.io/oauth/config/scim/properties.readonly",
    "https://jans.io/oauth/config/scim/properties.write"
  ]
}
```

## Troubleshooting

### 401 Unauthorized Errors

If you get 401 errors, check:
1. OAuth client has required scopes configured
2. Scopes are correctly spelled (case-sensitive)
3. Token request includes the scope
4. Token response actually grants the scope

### Insufficient Scopes Error

Error message example:
```
oAuth authorization error Insufficient scopes! , Required scope: [https://jans.io/oauth/jans-auth-server/session.delete, revoke_session], however token scopes: [revoke_session]
```

Solution: Ensure BOTH scopes are configured on the OAuth client.

### Scope Not Granted Error

If token request succeeds but scope isn't granted:
1. Check OAuth client configuration in Janssen Admin UI
2. Verify scope exists in the Janssen server
3. Check OAuth client's allowed scopes list

## References

- [Janssen OAuth Scopes Documentation](https://docs.jans.io/head/janssen-server/auth-server/scopes/)
- [Config API Scopes](https://docs.jans.io/head/admin/config-guide/config-api/)
- [SCIM API Scopes](https://docs.jans.io/head/admin/config-guide/scim-config/)
