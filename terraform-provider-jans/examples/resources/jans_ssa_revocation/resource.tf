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

# Example 1: Revoke SSA by JWT ID (JTI)
resource "jans_ssa_revocation" "revoke_by_jti" {
  jti = "550e8400-e29b-41d4-a716-446655440000"
}

# Example 2: Revoke all SSAs for an organization
resource "jans_ssa_revocation" "revoke_by_org" {
  org_id = "acme-corp"
}

# Example 3: Revoke both by JTI and Org ID
resource "jans_ssa_revocation" "revoke_specific_org_ssa" {
  jti    = var.ssa_jti
  org_id = var.organization_id
}

# Example 4: Conditional revocation with triggers
resource "jans_ssa_revocation" "conditional_revocation" {
  jti = var.ssa_jti

  triggers = {
    # Trigger when organization is suspended
    org_status = var.org_status
    # Trigger on compliance violation
    compliance_violation = var.compliance_violation_id
    # Time-based trigger
    revocation_time = timestamp()
  }
}

# Example 5: Revoke SSA on certificate expiration
resource "jans_ssa_revocation" "certificate_expiry" {
  org_id = var.organization_id

  triggers = {
    # Trigger when certificate expires
    cert_expiry_date = var.certificate_expiry_date
    cert_thumbprint  = var.certificate_thumbprint
  }

  lifecycle {
    # Only recreate if certificate info changes
    replace_triggered_by = [
      var.certificate_expiry_date,
      var.certificate_thumbprint
    ]
  }
}

# Example 6: Dynamic SSA revocation based on audit findings
resource "jans_ssa_revocation" "audit_based_revocation" {
  jti = var.flagged_ssa_jti

  triggers = {
    audit_finding_id = var.audit_finding_id
    severity         = var.finding_severity
    remediation_by   = var.remediation_deadline
  }
}

# Variables
variable "ssa_jti" {
  description = "JWT ID of the SSA to revoke"
  type        = string
}

variable "organization_id" {
  description = "Organization ID for SSA revocation"
  type        = string
}

variable "org_status" {
  description = "Organization status (active, suspended, revoked)"
  type        = string
  default     = "active"
}

variable "compliance_violation_id" {
  description = "Compliance violation ID"
  type        = string
  default     = "NONE"
}

variable "certificate_expiry_date" {
  description = "Certificate expiry date"
  type        = string
  default     = "2025-12-31"
}

variable "certificate_thumbprint" {
  description = "Certificate thumbprint"
  type        = string
  default     = ""
}

variable "flagged_ssa_jti" {
  description = "SSA JTI flagged by audit"
  type        = string
  default     = "audit-flagged-ssa"
}

variable "audit_finding_id" {
  description = "Audit finding identifier"
  type        = string
  default     = "AUDIT-2025-001"
}

variable "finding_severity" {
  description = "Severity of audit finding"
  type        = string
  default     = "high"
}

variable "remediation_deadline" {
  description = "Remediation deadline date"
  type        = string
  default     = "2025-11-01"
}
