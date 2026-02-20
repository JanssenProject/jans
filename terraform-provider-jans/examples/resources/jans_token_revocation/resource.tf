# Configure the Janssen Provider
terraform {
  required_providers {
    jans = {
      source = "jans/jans"
    }
  }
}

provider "jans" {
  url           = "https://moabu-noble-sawfish.gluu.info"
  client_id     = var.client_id
  client_secret = var.client_secret
  insecure_client = true  # Only for development/testing
}

# Example 1: Revoke a specific token
resource "jans_token_revocation" "revoke_token" {
  token_code = var.token_to_revoke
}

# Example 2: Emergency token revocation with triggers
resource "jans_token_revocation" "emergency_revocation" {
  token_code = var.compromised_token

  triggers = {
    # Trigger when security alert is raised
    alert_id  = var.security_alert_id
    timestamp = timestamp()
  }
}

# Example 3: Revoke token on client deactivation
data "jans_tokens" "client_tokens" {
  limit = 100
}

# Note: In practice, you'd use a null_resource with local-exec to iterate
# This example shows the pattern
resource "jans_token_revocation" "revoke_on_client_removal" {
  for_each = toset(var.tokens_to_revoke)
  
  token_code = each.value

  triggers = {
    client_id = var.deactivated_client_id
  }
}

# Example 4: Token rotation scenario
resource "jans_oidc_client" "rotating_client" {
  client_name                = "Rotating Client"
  grant_types                = ["client_credentials"]
  application_type           = "web"
  token_endpoint_auth_method = "client_secret_basic"
}

# Revoke old tokens when client secret is rotated
resource "jans_token_revocation" "rotate_tokens" {
  count = length(var.old_tokens)
  
  token_code = var.old_tokens[count.index]

  triggers = {
    # Trigger when client secret changes
    client_secret_version = var.client_secret_version
  }
}

# Variables
variable "token_to_revoke" {
  description = "Token code to revoke"
  type        = string
  sensitive   = true
}

variable "compromised_token" {
  description = "Compromised token to revoke"
  type        = string
  sensitive   = true
}

variable "security_alert_id" {
  description = "Security alert ID that triggered revocation"
  type        = string
  default     = "ALERT-2025-001"
}

variable "tokens_to_revoke" {
  description = "List of token codes to revoke"
  type        = list(string)
  sensitive   = true
  default     = []
}

variable "deactivated_client_id" {
  description = "Client ID being deactivated"
  type        = string
}

variable "old_tokens" {
  description = "Old tokens to revoke during rotation"
  type        = list(string)
  sensitive   = true
  default     = []
}

variable "client_secret_version" {
  description = "Client secret version for tracking rotations"
  type        = string
  default     = "v1"
}
