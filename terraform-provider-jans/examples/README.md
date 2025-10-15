# Janssen Terraform Provider - Examples

This directory contains comprehensive examples for all resources and data sources in the Janssen Terraform Provider.

## Directory Structure

```
examples/
├── sessions/       - Session management examples
├── tokens/         - Token management examples
├── statistics/     - Statistics and monitoring examples
├── complete/       - Complete example with all resources
└── README.md       - This file
```

## Quick Start

### 1. Prerequisites

```bash
# Install provider
cd terraform-provider-jans
make install

# Verify installation
ls ~/.terraform.d/plugins/janssenproject/jans/0.1.0/linux_amd64/
```

### 2. Configure Credentials

Create a `terraform.tfvars` file in any example directory:

```hcl
jans_url        = "https://your-janssen-server.com"
client_id       = "your-client-id"
client_secret   = "your-client-secret"
insecure_client = false  # Set to true for self-signed certs
```

### 3. Run Examples

```bash
# Choose an example directory
cd examples/sessions  # or tokens, statistics, complete

# Initialize Terraform
terraform init

# Plan changes
terraform plan

# Apply (read-only data sources are safe)
terraform apply
```

## Required OAuth Scopes

Ensure your OAuth client has these scopes configured:

### Data Sources (Read-Only)
```
https://jans.io/oauth/jans-auth-server/session.readonly
https://jans.io/oauth/config/token.readonly
https://jans.io/oauth/config/stats.readonly
```

### Resources (Write Operations)
```
https://jans.io/oauth/jans-auth-server/session.delete
https://jans.io/oauth/config/token.delete
https://jans.io/oauth/config/ssa.delete
```

## Examples Overview

### 1. Session Management (`sessions/`)

**Features**:
- List all active sessions
- Filter sessions by user or date
- Conditional session revocation
- Session monitoring dashboard

**Safe to run**: Yes (unless you enable revocation)

**Usage**:
```bash
cd examples/sessions
terraform apply
```

**Sample Output**:
```
total_sessions = 5
admin_session_count = 1
session_details = [
  {
    sid = "abc123..."
    user_dn = "uid=admin,ou=people,o=jans"
    state = "authenticated"
    created = "2025-10-13T12:00:00"
  },
  ...
]
```

### 2. Token Management (`tokens/`)

**Features**:
- Search tokens by pattern
- Filter tokens by client
- Token analytics (by type, client, expiration)
- Conditional token revocation

**Safe to run**: Yes (unless you enable revocation)

**Usage**:
```bash
cd examples/tokens
terraform apply
```

**Sample Output**:
```
total_tokens = 42
token_analytics = {
  unique_clients = 8
  token_types = ["access_token", "refresh_token"]
  expiring_soon = 3
}
```

### 3. Statistics (`statistics/`)

**Features**:
- Get statistics for current month
- Query specific month or date range
- Export statistics to JSON file
- Monitoring integration examples

**Safe to run**: Yes (read-only)

**Usage**:
```bash
cd examples/statistics
terraform apply -var="export_stats=true"
```

**Sample Output**:
```
current_month_stats = "[{...}]"
stats_available = true
parsed_statistics = {
  month = "202410"
  data_points = 15
}
```

### 4. Complete Example (`complete/`)

**Features**:
- All data sources in one configuration
- Consolidated monitoring dashboard
- All revocation resources (disabled by default)
- Complete Janssen observability

**Safe to run**: Yes (data sources only by default)

**Usage**:
```bash
cd examples/complete
terraform apply
```

**Sample Output**:
```
janssen_dashboard = {
  timestamp = "2025-10-13T12:30:00Z"
  sessions = {
    total = 5
    active = 4
  }
  tokens = {
    total = 42
    active = 38
    access_tokens = 35
  }
  statistics = {
    month_queried = "202410"
    data_available = true
  }
  security_actions = {
    sessions_revoked = 0
    tokens_revoked = 0
    ssas_revoked = 0
  }
}
```

## Enabling Revocation Operations

**⚠️ WARNING**: Revocation operations are destructive and will modify server state!

To enable revocation in any example:

```bash
# Session revocation
terraform apply \
  -var="enable_session_revocation=true" \
  -var="revoke_user_dn=uid=testuser,ou=people,o=jans"

# Token revocation
terraform apply \
  -var="enable_token_revocation=true" \
  -var="revoke_token_code=abc123..."

# SSA revocation
terraform apply \
  -var="enable_ssa_revocation=true" \
  -var="ssa_jti=unique-jti-123"
```

## Integration Examples

### With Monitoring Tools

```hcl
# Datadog integration example
resource "datadog_monitor" "session_count" {
  name    = "Janssen - High Session Count"
  type    = "metric alert"
  message = "Session count is above threshold"
  
  query = "avg(last_5m):${length(data.jans_sessions.all.sessions)} > 100"
}

# Prometheus metrics export
resource "local_file" "prometheus_metrics" {
  filename = "janssen_metrics.prom"
  content = <<-EOT
    janssen_sessions_total ${length(data.jans_sessions.all.sessions)}
    janssen_tokens_total ${data.jans_tokens.all.total_entries}
  EOT
}
```

### With Security Automation

```hcl
# Automated security response
resource "jans_session_revocation" "compromised_user" {
  count = var.user_compromised ? 1 : 0
  
  user_dn = var.compromised_user_dn
  
  triggers = {
    detection_time = timestamp()
    threat_level   = var.threat_level
  }
}
```

## Best Practices

1. **Read-Only First**: Start with data sources to verify connectivity
2. **Enable Revocation Carefully**: Always test revocation in non-production first
3. **Use Variables**: Never hardcode credentials
4. **Monitor Regularly**: Set up automated dashboards
5. **Document Changes**: Use triggers to track why revocations occurred

## Troubleshooting

### 401 Unauthorized
```
Error: did not get correct response code: 401 Unauthorized
```
**Solution**: Check OAuth scopes are configured for your client

### 404 Not Found
```
Error: did not get correct response code: 404 Not Found
```
**Solution**: Verify endpoint paths and ensure Config API is enabled

### Empty Results
```
total_sessions = 0
```
**Solution**: This is normal if no sessions/tokens exist. Create some by authenticating.

## Support

For issues or questions:
1. Check OAuth scopes are configured correctly
2. Verify Janssen Config API is accessible
3. Review Terraform debug logs: `export TF_LOG=DEBUG`
4. Check provider version compatibility
