---
tags:
  - cedarling
  - python
  - usage
---

# Python usage

In this example, we will show an example Python script that calls the `cedarling_python` module and calls the `authz` function. Before beginning, ensure that you have completed the [building steps](./README.md#building) and are currently in a virtual Python environment that has the `cedarling_python` module installed.

## Demo script

```python
from cedarling_python import MemoryLogConfig, DisabledLoggingConfig, StdOutLogConfig
from cedarling_python import PolicyStoreSource, PolicyStoreConfig, BootstrapConfig, JwtConfig
from cedarling_python import Cedarling, Request


# use log config to store logs in memory with a time-to-live of 120 seconds
# by default it is 60 seconds
log_config = MemoryLogConfig(log_ttl=100)
# we can also set value to as property
# log_config.log_ttl = 120

# use disabled log config to ignore all logging
# log_config = DisabledLoggingConfig()

# use log config to print logs to stdout
# log_config = StdOutLogConfig()

with open("policy-store.json",
          mode="r", encoding="utf8") as f:
    policy_raw_json = f.read()
# for now we support only json source
policy_source = PolicyStoreSource(json=policy_raw_json)

policy_store_config = PolicyStoreConfig(
    source=policy_source, store_id="8b805e22fdd39f3dd33a13d9fb446d8e6314153ca997")
# if we have only one policy store in file we can avoid using store id
policy_store_config.store_id = None

# Create jwt configuration
# do not validate JWT tokens
jwt_config = JwtConfig(enabled=False)

# collect all in the BootstrapConfig
bootstrap_config = BootstrapConfig(
    application_name="TestApp",
    log_config=log_config,
    policy_store_config=policy_store_config,
    jwt_config=jwt_config
)

# initialize cedarling instance
# all values in the bootstrap_config is parsed and validated at this step.
instance = Cedarling(bootstrap_config)

# returns a list of all active log ids
# active_log_ids = instance.get_log_ids()

# get log entry by id
# log_entry = instance.get_log_by_id(active_log_ids[0])


# show logs
print("Logs stored in memory:")
print(*instance.pop_logs(), sep="\n\n")

example_access_token = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzb21lX3VuaXF1ZV9pZCIsImlzcyI6Imh0dHA6Oi8vZXhhbXBsZS5jb20ifQ.0EBtuIHAKtPZRKqX6LBXOZ52erMB2xuMTfVSoaYHK-o'
request = Request(access_token=example_access_token)
# if
instance.authorize(request)
```

- Save the script in a file called `example.py`
- Save [this](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_python/example_files/policy-store.json) demo policy store file to `policy-store.json` in the same location
- Run the example script:

```
(venv) $ python example.py
Logs stored in memory:
{'id': '01926d88-1db0-73c1-bbc8-b0c7a41ac7af', 'time': 1728414490, 'log_kind': 'System', 'pdp_id': 'ce9a0f45-0fe3-4153-90c6-9e05acf68ff3', 'msg': 'PolicyStore loaded successfully', 'application_id': 'TestApp'}

{'id': '01926d88-1db0-73c1-bbc8-b0c86edd7f0e', 'time': 1728414490, 'log_kind': 'System', 'pdp_id': 'ce9a0f45-0fe3-4153-90c6-9e05acf68ff3', 'msg': 'JWT service loaded successfully', 'application_id': 'TestApp'}

{'id': '01926d88-1db0-73c1-bbc8-b0c966e9723c', 'time': 1728414490, 'log_kind': 'System', 'pdp_id': 'ce9a0f45-0fe3-4153-90c6-9e05acf68ff3', 'msg': 'Cedarling Authz initialized successfully', 'application_id': 'TestApp'}
```

As you can see, the logs from cedarling's initialization are being printed to the console. The demo script is a work-in-progress and does not feature all of cedarling's functionality yet.

## Exposed functions
The `pyo3` binding for cedarling exposes a number of cedarling functions for you to use. The documentation on this can be found [here](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_python/PYTHON_TYPES.md).
