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

# Data source to list all active user sessions
data "jans_sessions" "active_sessions" {}

# Output the sessions
output "active_sessions" {
  value = data.jans_sessions.active_sessions.sessions
}

# Output session count
output "session_count" {
  value = length(data.jans_sessions.active_sessions.sessions)
}

# Output sessions for a specific user
output "user_sessions" {
  value = [
    for session in data.jans_sessions.active_sessions.sessions :
    session if can(regex("inum=admin", session.user_dn))
  ]
  description = "Sessions for admin user"
}
