# Example: Statistics and Monitoring with Janssen Terraform Provider

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

# Data Source: Get statistics for current month
data "jans_statistics" "current_month" {
  month = formatdate("YYYYMM", timestamp())
}

# Data Source: Get statistics for specific month
data "jans_statistics" "specific_month" {
  count = var.specific_month != "" ? 1 : 0
  month = var.specific_month
}

# Data Source: Get statistics for date range
data "jans_statistics" "date_range" {
  count = var.enable_date_range ? 1 : 0
  
  start_month = var.start_month
  end_month   = var.end_month
}

# Parse and output statistics
output "current_month_stats" {
  description = "Statistics for current month"
  value       = data.jans_statistics.current_month.statistics
}

output "stats_available" {
  description = "Whether statistics data is available"
  value       = length(jsondecode(data.jans_statistics.current_month.statistics)) > 0
}

# Example: Integration with monitoring tools
# This shows how to use Janssen statistics with external monitoring

locals {
  stats_data = jsondecode(data.jans_statistics.current_month.statistics)
  
  # Check if stats are available
  has_stats = length(local.stats_data) > 0
  
  # Example: Extract specific metrics if available
  # (Actual structure depends on your Janssen configuration)
  parsed_stats = local.has_stats ? {
    month       = formatdate("YYYYMM", timestamp())
    data_points = length(local.stats_data)
    raw_data    = local.stats_data
  } : {
    month       = formatdate("YYYYMM", timestamp())
    data_points = 0
    raw_data    = []
  }
}

output "parsed_statistics" {
  description = "Parsed statistics data"
  value       = local.parsed_stats
}

# Example: Export to file for external processing
resource "local_file" "stats_export" {
  count = var.export_stats ? 1 : 0
  
  filename = "${path.module}/stats_${formatdate("YYYYMMDD", timestamp())}.json"
  content  = data.jans_statistics.current_month.statistics
}

# Example: Monitoring integration placeholder
# This demonstrates how you might integrate with monitoring tools

output "monitoring_metrics" {
  description = "Metrics for monitoring systems"
  value = {
    timestamp      = timestamp()
    month          = formatdate("YYYYMM", timestamp())
    stats_available = local.has_stats
    data_points    = local.parsed_stats.data_points
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

variable "specific_month" {
  type        = string
  default     = ""
  description = "Specific month to query (YYYYMM format)"
}

variable "enable_date_range" {
  type        = bool
  default     = false
  description = "Enable date range query"
}

variable "start_month" {
  type        = string
  default     = ""
  description = "Start month for range query (YYYYMM format)"
}

variable "end_month" {
  type        = string
  default     = ""
  description = "End month for range query (YYYYMM format)"
}

variable "export_stats" {
  type        = bool
  default     = false
  description = "Export statistics to JSON file"
}
