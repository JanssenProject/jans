terraform {
  required_providers {
    local = {
      source  = "hashicorp/local"
      version = "~> 2.5"
    }
  }
}

variable "workspace" {
  description = "Target deployment environment (dev, staging, production)"
  type        = string
  default     = "dev"

  validation {
    condition     = contains(["dev", "staging", "production"], var.workspace)
    error_message = "workspace must be one of: dev, staging, production"
  }
}

resource "local_file" "deployment_marker" {
  content  = "Deployed to workspace: ${var.workspace}\nTimestamp: ${timestamp()}\n"
  filename = "${path.module}/output-${var.workspace}.txt"
}

output "deployed_to" {
  value = "Deployment marker written for workspace: ${var.workspace}"
}
