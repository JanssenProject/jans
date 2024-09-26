from cedarling_python import AuthzConfig, MemoryLogConfig, OffLogConfig, StdOutLogConfig
from cedarling_python import PolicyStoreSource


authz_config = AuthzConfig(application_name="example_app_name")
authz_config.application_name = "example_app_name2"

# use log config to store logs in memory with a time-to-live of 120 seconds
# by default it is 60 seconds
log_config = MemoryLogConfig()
log_config.log_ttl = 120

# use disabled log config to ignore all logging
# log_config = OffLogConfig()

# use log config to print logs to stdout
# log_config = StdOutLogConfig()

# Create policy source configuration
policy_raw_json = open("example_files/policy-store.json",
                       mode="r", encoding="utf8").read()
# for now we support only json source
policy_source = PolicyStoreSource(json=policy_raw_json)
