from cedarling_python import MemoryLogConfig, DisabledLoggingConfig, StdOutLogConfig
from cedarling_python import PolicyStoreSource, PolicyStoreConfig, BootstrapConfig, JwtConfig
from cedarling_python import Cedarling
from cedarling_python import ResourceData, Request

# use log config to store logs in memory with a time-to-live of 120 seconds
# by default it is 60 seconds
log_config = MemoryLogConfig(log_ttl=100)
# we can also set value to as property
# log_config.log_ttl = 120

# use disabled log config to ignore all logging
# log_config = DisabledLoggingConfig()

# use log config to print logs to stdout
log_config = StdOutLogConfig()

# Create policy source configuration
with open("example_files/policy-store.json",
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


# //// Execute authentication request ////

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
id_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib0c4ZGZjNU1LVG4zN283Z3NkQ2V5cUw4THBXUXRnb080MW0xS1p3ZHEwIiwiY29kZSI6ImJmMTkzNGY2LTM5MDUtNDIwYS04Mjk5LTZiMmUzZmZkZGQ2ZSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJjbGllbnRfaWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhdWQiOiI1YjQ0ODdjNC04ZGIxLTQwOWQtYTY1My1mOTA3YjgwOTQwMzkiLCJhY3IiOiJiYXNpYyIsIng1dCNTMjU2IjoiIiwic2NvcGUiOlsib3BlbmlkIiwicHJvZmlsZSJdLCJvcmdfaWQiOiJzb21lX2xvbmdfaWQiLCJhdXRoX3RpbWUiOjE3MjQ4MzA3NDYsImV4cCI6MTcyNDk0NTk3OCwiaWF0IjoxNzI0ODMyMjU5LCJqdGkiOiJseFRtQ1ZSRlR4T2pKZ3ZFRXBvek1RIiwibmFtZSI6IkRlZmF1bHQgQWRtaW4gVXNlciIsInN0YXR1cyI6eyJzdGF0dXNfbGlzdCI6eyJpZHgiOjIwMSwidXJpIjoiaHR0cHM6Ly9hZG1pbi11aS10ZXN0LmdsdXUub3JnL2phbnMtYXV0aC9yZXN0djEvc3RhdHVzX2xpc3QifX19._eQT-DsfE_kgdhA0YOyFxxPEMNw44iwoelWa5iU1n9s"

request = Request(
    action_token,
    id_token,
    action='Jans::Action::"Update"',
    context={}, resource=resource)

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
