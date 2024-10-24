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
from cedarling_python import Cedarling, ResourceData, Request

# use MemoryLogConfig to store logs in memory with a time-to-live of 120 seconds
# by default it is 60 seconds
log_config = MemoryLogConfig(log_ttl=100)
# we can also set value to as property
# log_config.log_ttl = 120

# use DisabledLoggingConfig to ignore all logging
# log_config = DisabledLoggingConfig()

# use StdOutLogConfig to print logs to stdout
log_config = StdOutLogConfig()

# Create policy source configuration
with open("policy-store.json",
          mode="r", encoding="utf8") as f:
    policy_raw_json = f.read()
"""
This policy store contains one policy as such:
permit(
    principal is Jans::Workload,
    action in [Jans::Action::"Update"],
    resource is Jans::Issue
)when{
    principal.org_id == resource.org_id
};
"""
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


# //// Execute authentication request ////

# Creating cedar resource object.
# field resource_type and id is mandatory
# other fields are attributes of the resource.
resource = ResourceData(resource_type="Jans::Issue",
                        id="random_id", org_id="some_long_id")
# or we can init resource using dict
resource = ResourceData.from_dict({
    "type": "Jans::Issue",
    "id": "random_id",
    "org_id": "some_long_id"
})

action_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly9hZG1pbi11aS10ZXN0LmdsdXUub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19._eQT-DsfE_kgdhA0YOyFxxPEMNw44iwoelWa5iU1n9s"
"""
Decoded access token:
{
  "sub": "boG8dfc5MKTn37o7gsdCeyqL8LpWQtgoO41m1KZwdq0",
  "code": "bf1934f6-3905-420a-8299-6b2e3ffddd6e",
  "iss": "https://admin-ui-test.gluu.org",
  "token_type": "Bearer",
  "client_id": "5b4487c4-8db1-409d-a653-f907b8094039",
  "aud": "5b4487c4-8db1-409d-a653-f907b8094039",
  "acr": "basic",
  "x5t#S256": "",
  "scope": [
    "openid",
    "profile"
  ],
  "org_id": "some_long_id",
  "auth_time": 1724830746,
  "exp": 1724945978,
  "iat": 1724832259,
  "jti": "lxTmCVRFTxOjJgvEEpozMQ",
  "name": "Default Admin User",
  "status": {
    "status_list": {
      "idx": 201,
      "uri": "https://admin-ui-test.gluu.org/jans-auth/restv1/status_list"
    }
  }
}
"""

# Creating cedarling request
request = Request(
    action_token,
    action='Jans::Action::"Update"',
    context={}, resource=resource)

# Authorize call
authorize_result = instance.authorize(request)
print(*instance.pop_logs(), sep="\n\n")

# if you change org_id result will be false
assert authorize_result.is_allowed()

# watch on the decision for workload
workload_result = authorize_result.workload()
print(workload_result.decision)

# show diagnostic information
workload_diagnostic = workload_result.diagnostics
for i, reason in enumerate(workload_diagnostic.reason):
    if i == 0:
        print("reason policies:")
    print(reason)

for i, error in enumerate(workload_diagnostic.errors):
    if i == 0:
        print("errors:")
    print("id:", error.id, "error:", error.error)
```

- Save the script in a file called `example.py`
- Save [this](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_python/example_files/policy-store.json) demo policy store file to `policy-store.json` in the same location
- Run the example script:

```
(venv) $ python example.py
Logs stored in memory:
{"id":"01929b39-b7d9-7e58-8abb-329c19c3d821","time":1729181104,"log_kind":"System","pdp_id":"1039a067-88de-4e8b-8d5f-421fc912b94f","msg":"PolicyStore loaded successfully","application_id":"TestApp"}
{"id":"01929b39-b7d9-7e58-8abb-329dfb797de1","time":1729181104,"log_kind":"System","pdp_id":"1039a067-88de-4e8b-8d5f-421fc912b94f","msg":"JWT service loaded successfully","application_id":"TestApp"}
{"id":"01929b39-b7d9-7e58-8abb-329e91d2b37f","time":1729181104,"log_kind":"System","pdp_id":"1039a067-88de-4e8b-8d5f-421fc912b94f","msg":"Cedarling Authz initialized successfully","application_id":"TestApp"}
Logs stored in memory:

{"id":"01929b39-b7db-766c-95a8-4790ff90532d","time":1729181104,"log_kind":"Decision","pdp_id":"1039a067-88de-4e8b-8d5f-421fc912b94f","msg":"Result of authorize with resource as workload entity","application_id":"TestApp","principal":"Jans::Workload::\"5b4487c4-8db1-409d-a653-f907b8094039\"","action":"Jans::Action::\"Update\"","resource":"Jans::Issue::\"random_id\"","context":{},"decision":"ALLOW","diagnostics":{"reason":["840da5d85403f35ea76519ed1a18a33989f855bf1cf8"],"errors":[]}}
```

As you can see, the logs from cedarling's initialization and the result of the authorize call is printed to the console.

## Exposed functions
The `pyo3` binding for cedarling exposes a number of cedarling functions for you to use. The documentation on this can be found [here](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_python/PYTHON_TYPES.md).
