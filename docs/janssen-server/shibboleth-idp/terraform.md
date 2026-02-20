---
tags:
  - administration
  - shibboleth
  - terraform
  - infrastructure-as-code
---

# Shibboleth IDP Terraform Provider

The Janssen Terraform Provider includes resources for managing Shibboleth IDP configuration as Infrastructure as Code.

## Provider Configuration

Configure the Janssen provider:

```hcl
terraform {
  required_providers {
    jans = {
      source  = "JanssenProject/jans"
      version = "~> 1.0"
    }
  }
}

provider "jans" {
  url           = "https://auth.example.com"
  client_id     = var.jans_client_id
  client_secret = var.jans_client_secret
}
```

## Resources

### jans_shibboleth_configuration

Manages the Shibboleth IDP configuration.

#### Example Usage

```hcl
resource "jans_shibboleth_configuration" "idp" {
  entity_id            = "https://idp.example.com/idp/shibboleth"
  scope                = "example.com"
  enabled              = true
  signing_key_alias    = "idp-signing"
  encryption_key_alias = "idp-encryption"
  
  jans_auth {
    enabled   = true
    client_id = "shibboleth-client"
    scopes    = ["openid", "profile", "email"]
  }
}
```

#### Argument Reference

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `entity_id` | string | Yes | IDP entity ID (SAML EntityDescriptor entityID) |
| `scope` | string | Yes | IDP scope for scoped attributes |
| `enabled` | bool | No | Whether IDP is enabled (default: true) |
| `signing_key_alias` | string | No | Alias for signing key (default: idp-signing) |
| `encryption_key_alias` | string | No | Alias for encryption key (default: idp-encryption) |
| `jans_auth` | block | No | Janssen Auth Server integration settings |

##### jans_auth Block

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `enabled` | bool | No | Enable Janssen authentication (default: true) |
| `client_id` | string | Yes | OAuth client ID |
| `client_secret` | string | No | OAuth client secret (sensitive) |
| `scopes` | list(string) | No | OAuth scopes (default: ["openid", "profile", "email"]) |
| `redirect_uri` | string | No | OAuth redirect URI |

#### Attributes Reference

| Attribute | Type | Description |
|-----------|------|-------------|
| `id` | string | Configuration ID |
| `metadata_url` | string | URL to IDP metadata |

### jans_shibboleth_trusted_sp

Manages trusted SAML Service Providers.

#### Example Usage

```hcl
resource "jans_shibboleth_trusted_sp" "example_sp" {
  entity_id   = "https://sp.example.org"
  name        = "Example Service Provider"
  description = "Production service provider for example.org"
  enabled     = true
  
  metadata_url = "https://sp.example.org/metadata"
  
  released_attributes = [
    "uid",
    "mail",
    "displayName",
    "eduPersonPrincipalName"
  ]
  
  assertion_lifetime = 300
  sign_assertions    = true
  encrypt_assertions = true
}
```

#### Argument Reference

| Argument | Type | Required | Description |
|----------|------|----------|-------------|
| `entity_id` | string | Yes | SP entity ID |
| `name` | string | Yes | Display name |
| `description` | string | No | Description |
| `enabled` | bool | No | Whether SP is enabled (default: true) |
| `metadata_url` | string | No | URL to SP metadata |
| `metadata_file` | string | No | Local path to metadata file |
| `released_attributes` | list(string) | No | Attributes to release |
| `assertion_lifetime` | number | No | Assertion validity in seconds (default: 300) |
| `sign_assertions` | bool | No | Sign SAML assertions (default: true) |
| `encrypt_assertions` | bool | No | Encrypt SAML assertions (default: false) |

#### Attributes Reference

| Attribute | Type | Description |
|-----------|------|-------------|
| `id` | string | Trusted SP ID |
| `created_at` | string | Creation timestamp |
| `updated_at` | string | Last update timestamp |

## Data Sources

### jans_shibboleth_configuration

Read the current IDP configuration.

```hcl
data "jans_shibboleth_configuration" "current" {}

output "idp_entity_id" {
  value = data.jans_shibboleth_configuration.current.entity_id
}
```

### jans_shibboleth_trusted_sps

List all trusted Service Providers.

```hcl
data "jans_shibboleth_trusted_sps" "all" {}

output "trusted_sp_count" {
  value = length(data.jans_shibboleth_trusted_sps.all.service_providers)
}
```

## Complete Example

```hcl
terraform {
  required_providers {
    jans = {
      source  = "JanssenProject/jans"
      version = "~> 1.0"
    }
  }
}

provider "jans" {
  url           = var.jans_url
  client_id     = var.jans_client_id
  client_secret = var.jans_client_secret
}

# Variables
variable "jans_url" {
  description = "Janssen Auth Server URL"
  type        = string
}

variable "jans_client_id" {
  description = "OAuth client ID"
  type        = string
}

variable "jans_client_secret" {
  description = "OAuth client secret"
  type        = string
  sensitive   = true
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "production"
}

# Shibboleth IDP Configuration
resource "jans_shibboleth_configuration" "idp" {
  entity_id            = "https://idp.${var.environment}.example.com/idp/shibboleth"
  scope                = "example.com"
  enabled              = true
  signing_key_alias    = "idp-signing"
  encryption_key_alias = "idp-encryption"
  
  jans_auth {
    enabled   = true
    client_id = "shibboleth-${var.environment}"
    scopes    = ["openid", "profile", "email"]
  }
}

# Trusted Service Providers
resource "jans_shibboleth_trusted_sp" "internal_app" {
  entity_id   = "https://app.example.com"
  name        = "Internal Application"
  description = "Main internal application"
  enabled     = true
  
  metadata_url = "https://app.example.com/saml/metadata"
  
  released_attributes = [
    "uid",
    "mail",
    "displayName"
  ]
  
  sign_assertions = true
}

resource "jans_shibboleth_trusted_sp" "partner_app" {
  entity_id   = "https://partner.external.com"
  name        = "Partner Application"
  description = "External partner application"
  enabled     = true
  
  metadata_url = "https://partner.external.com/metadata"
  
  released_attributes = [
    "uid",
    "mail"
  ]
  
  sign_assertions    = true
  encrypt_assertions = true
}

# Outputs
output "idp_metadata_url" {
  description = "IDP metadata URL"
  value       = jans_shibboleth_configuration.idp.metadata_url
}

output "trusted_sp_ids" {
  description = "Trusted SP entity IDs"
  value = [
    jans_shibboleth_trusted_sp.internal_app.entity_id,
    jans_shibboleth_trusted_sp.partner_app.entity_id
  ]
}
```

## Import

### Import IDP Configuration

```bash
terraform import jans_shibboleth_configuration.idp shibboleth-config
```

### Import Trusted SP

```bash
terraform import jans_shibboleth_trusted_sp.example "https://sp.example.org"
```

## State Management

For production deployments, use remote state:

```hcl
terraform {
  backend "s3" {
    bucket = "terraform-state"
    key    = "janssen/shibboleth.tfstate"
    region = "us-east-1"
  }
}
```
