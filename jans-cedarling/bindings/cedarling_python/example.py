from cedarling_python import AuthzConfig, MemoryLogConfig


authz_config = AuthzConfig(application_name="example_app_name")
authz_config.application_name = "example_app_name2"

log_config = MemoryLogConfig()
log_config.log_ttl = 120
