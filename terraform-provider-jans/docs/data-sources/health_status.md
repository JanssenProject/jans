---
page_title: "jans_health_status Data Source - terraform-provider-jans"
subcategory: ""
description: |-
  Data source for retrieving Janssen health status information.
---

# jans_health_status (Data Source)

This data source provides access to the Janssen server health status.

## Example Usage

```terraform
data "jans_health_status" "current" {
}

output "server_status" {
  value = data.jans_health_status.current.status
}
```

## Schema

### Read-Only

- `status` (String) Overall health status of the server
- `checks` (List of Object) Individual health check results
  - `name` (String) Name of the health check
  - `status` (String) Status of the individual check
- `db_type` (String) Database type (sql, ldap, etc.)
- `facter_data` (Map of String) Server system information including memory, disk usage, hostname, etc.
- `last_update` (String) Last update timestamp