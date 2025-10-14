# Complete Example: All Janssen Terraform Provider Resources

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

#
# SESSION MANAGEMENT
#

# List all active sessions
data "jans_sessions" "all_sessions" {}

# Monitor sessions
output "session_monitoring" {
  description = "Session monitoring dashboard"
  value = {
    total_sessions = length(data.jans_sessions.all_sessions.sessions)
    sessions = [
      for s in data.jans_sessions.all_sessions.sessions : {
        user    = s.user_dn
        sid     = s.sid
        state   = s.state
        created = s.creation_date
      }
    ]
  }
}

#
# TOKEN MANAGEMENT
#

# Search all tokens
data "jans_tokens" "all_tokens" {
  limit       = 100
  start_index = 0
}

# Monitor tokens
output "token_monitoring" {
  description = "Token monitoring dashboard"
  value = {
    total_tokens = data.jans_tokens.all_tokens.total_entries
    active_count = data.jans_tokens.all_tokens.entries_count
    sample_tokens = [
      for t in slice(data.jans_tokens.all_tokens.tokens, 0, min(3, length(data.jans_tokens.all_tokens.tokens))) : {
        client_id  = t.client_id
        token_type = t.token_type
        grant_type = t.grant_type
        created    = t.creation_date
        expires    = t.expiration_date
      }
    ]
  }
}

#
# STATISTICS
#

# Get current month statistics
data "jans_statistics" "current" {
  month = formatdate("YYYYMM", timestamp())
}

output "statistics_monitoring" {
  description = "Statistics monitoring"
  value = {
    month           = formatdate("YYYYMM", timestamp())
    stats_available = length(jsondecode(data.jans_statistics.current.statistics)) > 0
    stats_data      = data.jans_statistics.current.statistics
  }
}

#
# SECURITY OPERATIONS (Conditional)
#

# Session revocation (requires explicit enable)
resource "jans_session_revocation" "security_revoke_session" {
  count = var.enable_session_revocation ? 1 : 0
  
  user_dn = var.revoke_user_dn
  
  triggers = {
    timestamp = timestamp()
    reason    = "Security operation"
  }
}

# Token revocation (requires explicit enable)
resource "jans_token_revocation" "security_revoke_token" {
  count = var.enable_token_revocation ? 1 : 0
  
  token_code = var.revoke_token_code
  
  triggers = {
    timestamp = timestamp()
    reason    = "Security operation"
  }
}

# SSA revocation (requires explicit enable)
resource "jans_ssa_revocation" "security_revoke_ssa" {
  count = var.enable_ssa_revocation ? 1 : 0
  
  jti = var.ssa_jti
  
  triggers = {
    timestamp = timestamp()
    reason    = "Security operation"
  }
}

#
# CONSOLIDATED DASHBOARD OUTPUT
#

output "janssen_dashboard" {
  description = "Complete Janssen monitoring dashboard"
  value = {
    timestamp = timestamp()
    
    sessions = {
      total     = length(data.jans_sessions.all_sessions.sessions)
      active    = length([for s in data.jans_sessions.all_sessions.sessions : s if s.state == "authenticated"])
    }
    
    tokens = {
      total       = data.jans_tokens.all_tokens.total_entries
      active      = data.jans_tokens.all_tokens.entries_count
      access_tokens = length([for t in data.jans_tokens.all_tokens.tokens : t if t.token_type == "access_token"])
    }
    
    statistics = {
      month_queried   = formatdate("YYYYMM", timestamp())
      data_available  = length(jsondecode(data.jans_statistics.current.statistics)) > 0
    }
    
    security_actions = {
      sessions_revoked = var.enable_session_revocation ? 1 : 0
      tokens_revoked   = var.enable_token_revocation ? 1 : 0
      ssas_revoked     = var.enable_ssa_revocation ? 1 : 0
    }
  }
}

#
# VARIABLES
#

variable "jans_url" {
  type        = string
  description = "Janssen server URL"
}

variable "client_id" {
  type        = string
  description = "OAuth client ID with required scopes"
}

variable "client_secret" {
  type        = string
  sensitive   = true
  description = "OAuth client secret"
}

variable "insecure_client" {
  type        = bool
  default     = false
  description = "Skip TLS verification (for testing only)"
}

# Security operation flags
variable "enable_session_revocation" {
  type        = bool
  default     = false
  description = "Enable session revocation (destructive)"
}

variable "revoke_user_dn" {
  type        = string
  default     = ""
  description = "User DN to revoke sessions for"
}

variable "enable_token_revocation" {
  type        = bool
  default     = false
  description = "Enable token revocation (destructive)"
}

variable "revoke_token_code" {
  type        = string
  default     = ""
  description = "Token code to revoke"
}

variable "enable_ssa_revocation" {
  type        = bool
  default     = false
  description = "Enable SSA revocation (destructive)"
}

variable "ssa_jti" {
  type        = string
  default     = ""
  description = "SSA JWT ID to revoke"
}
