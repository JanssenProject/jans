# Example: Token Management with Janssen Terraform Provider

terraform {
  required_providers {
    jans = {
      source  = "janssenproject/jans"
      version = "0.1.0"
    }
  }
}

provider "jans" {
  url             = var.jans_url
  client_id       = var.client_id
  client_secret   = var.client_secret
  insecure_client = var.insecure_client
}

# Data Source: Search all tokens
data "jans_tokens" "all" {
  limit       = 100
  start_index = 0
}

# Data Source: Search tokens by pattern
data "jans_tokens" "search" {
  pattern     = var.search_pattern
  limit       = 50
  start_index = 0
}

# Data Source: Get tokens for specific client
data "jans_tokens" "client_specific" {
  client_id = var.specific_client_id
}

# Outputs for monitoring
output "total_tokens" {
  description = "Total number of active tokens"
  value       = data.jans_tokens.all.total_entries
}

output "token_summary" {
  description = "Summary of token types"
  value = {
    total       = data.jans_tokens.all.total_entries
    returned    = data.jans_tokens.all.entries_count
    sample_count = length(data.jans_tokens.all.tokens)
  }
}

output "token_details" {
  description = "Details of first 5 tokens"
  value = [
    for i, t in slice(data.jans_tokens.all.tokens, 0, min(5, length(data.jans_tokens.all.tokens))) : {
      index        = i + 1
      client_id    = t.client_id
      token_type   = t.token_type
      grant_type   = t.grant_type
      scope        = t.scope
      created      = t.creation_date
      expires      = t.expiration_date
    }
  ]
}

# Token analytics
locals {
  # Group tokens by client
  tokens_by_client = {
    for t in data.jans_tokens.all.tokens :
    t.client_id => t...
  }
  
  # Group tokens by type
  tokens_by_type = {
    for t in data.jans_tokens.all.tokens :
    t.token_type => t...
  }
  
  # Find expiring soon (within 5 minutes)
  expiring_soon = [
    for t in data.jans_tokens.all.tokens :
    t if can(regex("^202[4-9]", t.expiration_date))
  ]
}

output "token_analytics" {
  description = "Token analytics by client and type"
  value = {
    unique_clients     = length(keys(local.tokens_by_client))
    token_types        = keys(local.tokens_by_type)
    expiring_soon      = length(local.expiring_soon)
  }
}

# Conditional token revocation
resource "jans_token_revocation" "revoke_specific" {
  count = var.enable_token_revocation ? 1 : 0
  
  token_code = var.token_code_to_revoke
  
  triggers = {
    timestamp = timestamp()
    reason    = var.revocation_reason
  }
}

# Example: Automated cleanup using null_resource
# This demonstrates how to revoke multiple tokens programmatically
resource "null_resource" "token_cleanup" {
  count = var.enable_automated_cleanup ? 1 : 0
  
  triggers = {
    always_run = timestamp()
  }
  
  provisioner "local-exec" {
    command = "echo 'Token cleanup would run here'"
  }
}

# Variables
variable "jans_url" {
  type        = string
  description = "Janssen server URL"
}

variable "client_id" {
  type        = string
  description = "OAuth client ID"
}

variable "client_secret" {
  type        = string
  sensitive   = true
  description = "OAuth client secret"
}

variable "insecure_client" {
  type        = bool
  default     = false
  description = "Skip TLS verification (testing only)"
}

variable "search_pattern" {
  type        = string
  default     = ""
  description = "Pattern to search for in tokens"
}

variable "specific_client_id" {
  type        = string
  default     = ""
  description = "Specific client ID to filter tokens"
}

variable "enable_token_revocation" {
  type        = bool
  default     = false
  description = "Enable token revocation"
}

variable "token_code_to_revoke" {
  type        = string
  default     = ""
  description = "Token code to revoke"
}

variable "revocation_reason" {
  type        = string
  default     = "Security policy"
  description = "Reason for token revocation"
}

variable "enable_automated_cleanup" {
  type        = bool
  default     = false
  description = "Enable automated token cleanup"
}
