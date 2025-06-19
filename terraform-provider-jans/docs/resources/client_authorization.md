
---
page_title: "jans_client_authorization Resource - terraform-provider-jans"
subcategory: ""
description: |-
  Manages client authorizations in Janssen.
---

# jans_client_authorization (Resource)

This resource manages client authorizations in Janssen, representing the permissions granted to specific clients for specific users.

## Example Usage

```terraform
resource "jans_client_authorization" "example" {
  client_id        = "1234567890"
  user_id          = "user123"
  scopes          = ["openid", "profile", "email"]
  redirect_uris   = ["https://example.com/callback"]
  grant_types     = ["authorization_code", "refresh_token"]
  deletable       = true
  expiration_date = "2024-12-31T23:59:59Z"
}
```

## Schema

### Required

- `client_id` (String) Client identifier
- `user_id` (String) User identifier

### Optional

- `scopes` (List of String) Authorized scopes
- `redirect_uris` (List of String) Redirect URIs for the client authorization
- `grant_types` (List of String) Grant types for the client authorization
- `expiration_date` (String) Expiration date in RFC3339 format
- `deletable` (Boolean) Whether this authorization can be deleted

### Read-Only

- `id` (String) ID of the client authorization
- `inum` (String) Unique identifier
- `dn` (String) Distinguished name
- `creation_date` (String) Creation date in RFC3339 format
