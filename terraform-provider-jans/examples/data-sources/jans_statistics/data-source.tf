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

# Data source to retrieve current month statistics
data "jans_statistics" "current_month" {}

# Output statistics
output "current_statistics" {
  value = data.jans_statistics.current_month.statistics
}

# Data source to retrieve specific month statistics
data "jans_statistics" "october_2025" {
  month = "202510"
}

output "october_statistics" {
  value = data.jans_statistics.october_2025.statistics
}

# Example: Monitor authentication metrics
output "auth_metrics" {
  value = {
    month      = data.jans_statistics.current_month.month
    stat_count = length(data.jans_statistics.current_month.statistics)
    statistics = data.jans_statistics.current_month.statistics
  }
  description = "Authentication and usage statistics for monitoring"
}
