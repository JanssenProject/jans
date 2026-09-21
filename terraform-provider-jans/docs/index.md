---
page_title: "Provider: Janssen"
description: |-
  The Janssen provider is used to manage resources in a Janssen instance.
---

# Janssen Provider

The Janssen provider is used to manage resources in a Janssen instance. This
includes all configurations, users, groups, OIDC clients, and more.

## Provider Configuration

To use the provider, you need to provide the URL of the Janssen instance, as
well as valid credentials that have access to the Janssen instance.

```terraform
terraform {
  required_version = ">= 0.12.0"
  required_providers {
    janssen = {
      source = "JanssenProject/jans"
      version = "1.4.0"
    }
  }
}

provider "jans" {
  url           = "https://test-instnace.jans.io"
  client_id     = "1800.3d29d884-e56b-47ac-83ab-b37942b83a89"
  client_secret = "Ma3egYQ5dkqS"
}
```

Make sure that the client you authenticate with has the full list of scopes
attached to it, or else you might not be able to manage all resources.

## User Management Endpoints

Config API protects its user management endpoints with an extra role check on
top of the OAuth2 scope check, controlled by the
`userRolePermissionValidationEnabled` attribute. That check expects a human
behind the request and reads their role from the token. This provider
authenticates with the `client_credentials` grant, so its token carries no
user and cannot satisfy it.

Without the exemption below, `jans_custom_user` fails on every operation:

```
Error: get request failed: bad request: Header attribute `User-inum` missing
```

A `401 Unauthorized` carrying `Header attribute 'User-inum' does not correspond
to User token` has the same cause.

Add the provider's own client ID to the allow list to resolve it:

```terraform
resource "jans_api_app_configuration" "global" {
  user_role_permission_excluded_clients = [
    "1800.3d29d884-e56b-47ac-83ab-b37942b83a89",
  ]
}
```

The check is registered only for the user management plugin, so
`jans_api_app_configuration` itself is not affected and the allow list can be
applied with Terraform before managing any user. Apply it once, then add the
`jans_custom_user` resources.

Other resources are unaffected — only `jans_custom_user` uses these endpoints.

The client ID is read from the token introspection response, so a client cannot
exempt itself, and a token issued for a user is checked in full even when its
client is listed. An exempt client is authorized by its OAuth scopes alone:
grant it only the user scopes it needs, and do not reuse it for anything else.

## Instance Configuration

Every instance of Janssen comes with a set of configurations, which are valid
for the whole instance. These resources cannot be created or destroyed, as they
are always present in a Janssen instance. Instead, they can be imported and 
updated. The creation of such a resource will result in an error. Deletion on
the other hand will result in the resource being removed from the state file.

The following resources are considered instance configurations:

- jans_api_app_configuration
- jans_app_configuration
- jans_cache_configuration
- jans_default_authentication_method
- jans_fido2_configuration
- jans_logging_configuration
- jans_organization
- jans_scim_app_configuration
- jans_smtp_configuration

Terraform cannot create them, so each one has to be imported before it can be
managed:

```bash
terraform import jans_api_app_configuration.global global
terraform import jans_app_configuration.global global
terraform import jans_cache_configuration.global global
terraform import jans_default_authentication_method.global global
terraform import jans_fido2_configuration.global global
terraform import jans_logging_configuration.global global
terraform import jans_organization.global global
terraform import jans_scim_app_configuration.global global
terraform import jans_smtp_configuration.global global
```

Note that the resource identifier can be any other valid identifier, instead of `global`.

Only the attributes declared in the configuration are sent on update; every other
attribute keeps its current server value. A resource that declares a single
attribute therefore leaves the rest of the configuration alone.

The following resources can also not be created from within Terraform, but can be imported,
updated, or deleted (unlike with the instance configurations, deletion will result in the
actual resource being deleted):

- jans_fido_device
- jans_fido2_device
