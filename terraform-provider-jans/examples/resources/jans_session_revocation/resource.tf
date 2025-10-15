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

# Example 1: Revoke sessions for a specific user
resource "jans_session_revocation" "revoke_user_sessions" {
  user_dn = "inum=user123,ou=people,o=jans"
}

# Example 2: Conditional session revocation with triggers
resource "jans_session_revocation" "security_incident" {
  user_dn = var.compromised_user_dn

  triggers = {
    # Trigger revocation when incident ID changes
    incident_id = var.security_incident_id
    timestamp   = timestamp()
  }
}

# Example 3: Revoke sessions on user role change
resource "jans_user" "admin_user" {
  user_id     = "admin.user"
  given_name  = "Admin"
  family_name = "User"
  mail        = "admin@example.com"
  display_name = "Admin User"
  user_password = var.user_password
  status = "active"
}

# Automatically revoke sessions when user is deactivated
resource "jans_session_revocation" "on_user_deactivation" {
  user_dn = jans_user.admin_user.dn

  triggers = {
    # Revoke when user status changes
    user_status = jans_user.admin_user.status
  }

  # This will execute whenever the user status changes
  lifecycle {
    replace_triggered_by = [jans_user.admin_user.status]
  }
}

# Variables
variable "compromised_user_dn" {
  description = "DN of user with compromised account"
  type        = string
  default     = "inum=compromised-user,ou=people,o=jans"
}

variable "security_incident_id" {
  description = "Security incident tracking ID"
  type        = string
  default     = "INC-2025-001"
}

variable "user_password" {
  description = "User password"
  type        = string
  sensitive   = true
}
