---
page_title: "Provider: Janssen"
description: |-
  The Janssen provider is used to manage resources in a Janssen instance.
---

# Janssen Provider

The Janssen provider is used to manage resources in a Janssen instance. This
includes all configurations, users, groups, OIDC clients, and more.

## Provider Configuration

To use the provider, you need to provide the URL of the Jansen instance, as 
well as valid credentials that have access to the Janssen instance.

```terraform
provider "jans" {
  url           = "https://test-instnace.jans.io"
  client_id     = "1800.3d29d884-e56b-47ac-83ab-b37942b83a89"
  client_secret = "Ma3egYQ5dkqS"
}
```

Make sure that the client you authenticate with has the full list of scopes
attached to it, or else you might not be able to manage all resources.

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

It is recommended to import all of those resources before managing anything else:

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

The following resources can also not be created from within Terraform, but can be imported,
updated, or deleted (unlike with the instance configurations, deletion will result in the
actual resource being deleted):

- jans_fido_device
- jans_fido2_device
