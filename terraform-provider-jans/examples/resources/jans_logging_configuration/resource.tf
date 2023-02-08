resource "jans_logging_configuration" "global" {
  logging_level               = "INFO"
  logging_layout              = "text"
  http_logging_enabled        = false
  disable_jdk_logger          = true
  enabled_oauth_audit_logging = false
}