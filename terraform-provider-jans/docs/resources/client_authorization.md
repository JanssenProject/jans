---
# generated by https://github.com/hashicorp/terraform-plugin-docs
page_title: "jans_client_authorization Resource - terraform-provider-jans"
subcategory: ""
description: |-
  Resource for managing client authorizations in Janssen server
---

# jans_client_authorization (Resource)

Resource for managing client authorizations in Janssen server

## Example Usage

```terraform
resource "jans_client_authorization" "example" {
  inum        = "1800.abcd1234-5678-90ef-ghij-klmnopqrstuv"
  dn          = "inum=1800.abcd1234-5678-90ef-ghij-klmnopqrstuv,ou=clients,o=jans"
  client_id   = "example-client-id"
  scopes      = ["openid", "profile", "email"]
  attributes  = {
    displayName = "Example Client Authorization"
    description = "Example client authorization for testing"
  }
}
```

<!-- schema generated by tfplugindocs -->
## Schema

### Required

- `client_id` (String) Client identifier
- `user_id` (String) User identifier

### Optional

- `deletable` (Boolean) Whether this authorization can be deleted
- `expiration_date` (String) Expiration date in RFC3339 format
- `grant_types` (List of String) Grant types for the client authorization
- `redirect_uris` (List of String) Redirect URIs for the client authorization
- `scopes` (List of String) Authorized scopes

### Read-Only

- `creation_date` (String) Creation date in RFC3339 format
- `dn` (String) Distinguished name
- `id` (String) ID of the client authorization
- `inum` (String) Unique identifier
