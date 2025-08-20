
data "jans_audit_logs" "example" {
  pattern    = "authentication"
  limit      = 100
  start_date = "2025-01-01"
  end_date   = "2025-12-31"
}

output "audit_log_count" {
  value = data.jans_audit_logs.example.total_entries_count
}

output "first_log_entry" {
  value = length(data.jans_audit_logs.example.entries) > 0 ? data.jans_audit_logs.example.entries[0] : ""
}
