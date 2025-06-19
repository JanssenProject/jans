Data source for retrieving Janssen server statistics.
---

# jans_server_stats (Data Source)

This data source provides access to the Janssen server statistics and metrics.

## Example Usage

```terraform
data "jans_server_stats" "current" {
}

output "start_time" {
  value = data.jans_server_stats.current.start_time
}
```

## Schema

### Read-Only

- `db_type` (String) Database type (sql, ldap, etc.)
- `facter_data` (Map of String) Server statistics including memoryfree, swapfree, hostname, ipaddress, uptime, free_disk_space, load_average
- `last_update` (String) Last update timestamp
- `start_time` (String) Server start time
- `current_time` (String) Current server time
- `uptime` (String) Server uptime
- `memory_usage` (Number) Current memory usage
- `cpu_usage` (Number) Current CPU usage

```data "jans_server_stats" "current" {
}

output "start_time" {
  value = data.jans_server_stats.current.start_time
}