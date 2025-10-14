# Example: Session Management with Janssen Terraform Provider

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

# Data Source: List all active sessions
data "jans_sessions" "all" {}

# Output total number of active sessions
output "total_sessions" {
  description = "Total number of active sessions"
  value       = length(data.jans_sessions.all.sessions)
}

# Output session details
output "session_details" {
  description = "Details of all active sessions"
  value = [
    for s in data.jans_sessions.all.sessions : {
      sid       = s.sid
      user_dn   = s.user_dn
      state     = s.state
      created   = s.creation_date
      last_used = s.last_used_at
    }
  ]
}

# Filter sessions by specific criteria
locals {
  # Find admin sessions
  admin_sessions = [
    for s in data.jans_sessions.all.sessions :
    s if can(regex("admin", lower(s.user_dn)))
  ]
  
  # Find sessions created today
  recent_sessions = [
    for s in data.jans_sessions.all.sessions :
    s if can(regex("^${formatdate("YYYY-MM-DD", timestamp())}", s.creation_date))
  ]
}

output "admin_session_count" {
  description = "Number of admin sessions"
  value       = length(local.admin_sessions)
}

output "recent_session_count" {
  description = "Sessions created today"
  value       = length(local.recent_sessions)
}

# Example: Conditional session revocation
# Revoke sessions for a specific user (requires explicit approval)
resource "jans_session_revocation" "revoke_user" {
  count = var.revoke_user_sessions ? 1 : 0
  
  user_dn = var.user_dn_to_revoke
  
  triggers = {
    timestamp = timestamp()
    reason    = var.revocation_reason
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

variable "revoke_user_sessions" {
  type        = bool
  default     = false
  description = "Enable session revocation"
}

variable "user_dn_to_revoke" {
  type        = string
  default     = ""
  description = "User DN whose sessions should be revoked"
}

variable "revocation_reason" {
  type        = string
  default     = "Security policy"
  description = "Reason for revoking sessions"
}
