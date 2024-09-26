from cedarling_python import AuthzConfig, MemoryLogConfig, OffLogConfig, StdOutLogConfig
from cedarling_python import PolicyStoreSource, PolicyStoreConfig, BootstrapConfig


authz_config = AuthzConfig(application_name="example_app_name")
# we can also set value to as property
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

policy_store_config = PolicyStoreConfig(
    source=policy_source, store_id="8b805e22fdd39f3dd33a13d9fb446d8e6314153ca997")
# if we have only one policy store in file we can avoid using store id
policy_store_config = None

# collect all in the BootstrapConfig
bootstrap_config = BootstrapConfig(
    authz_config=authz_config,
    log_config=log_config,
    policy_store_config=policy_store_config)
