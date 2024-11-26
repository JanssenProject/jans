---
tags:
  - cedarling
  - python
  - usage
---

# Python usage

In this example, we will show an example Python script that calls the `cedarling_python` module and calls the `authorize()` function.

- Before beginning, ensure that you have completed the [building steps](./README.md#building) and are currently in a virtual Python environment that has the `cedarling_python` module installed. You can confirm this with `pip list`.
- Run the script `jans/jans-cedarling/bindings/cedarling_python/example.py` from within the virtual environment.

## Output

```
(venv) $ python example.py
Policy store location not provided, use 'CEDARLING_LOCAL_POLICY_STORE' environment variable
Used default policy store path: example_files/policy-store.json

{"id":"0193414e-9672-786a-986c-57f48d41c4e4","time":1731967489,"log_kind":"System","pdp_id":"c0ec33ff-9482-4bdc-83f6-2925a41a3280","msg":"configuration parsed successfully"}
{"id":"0193414e-9672-786a-986c-57f5379086c3","time":1731967489,"log_kind":"System","pdp_id":"c0ec33ff-9482-4bdc-83f6-2925a41a3280","msg":"Cedarling Authz initialized successfully","application_id":"TestApp"}
{"id":"0193414e-9676-7d8a-b55b-3f0097355851","time":1731967489,"log_kind":"Decision","pdp_id":"c0ec33ff-9482-4bdc-83f6-2925a41a3280","msg":"Result of authorize.","application_id":"TestApp","action":"Jans::Action::\"Read\"","resource":"Jans::Application::\"some_id\"","context":{"user_agent":"Linux","operating_system":"Linux","network_type":"Local","network":"127.0.0.1","geolocation":["America"],"fraud_indicators":["Allowed"],"device_health":["Healthy"],"current_time":1731967489},"person_principal":"Jans::User::\"qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0\"","person_diagnostics":{"reason":["840da5d85403f35ea76519ed1a18a33989f855bf1cf8"],"errors":[]},"person_decision":"ALLOW","workload_principal":"Jans::Workload::\"d7f71bea-c38d-4caf-a1ba-e43c74a11a62\"","workload_diagnostics":{"reason":["444da5d85403f35ea76519ed1a18a33989f855bf1cf8"],"errors":[]},"workload_decision":"ALLOW","authorized":true}
Result of workload authorization: ALLOW
Policy ID used:
444da5d85403f35ea76519ed1a18a33989f855bf1cf8
Errors during authorization: 0

Result of person authorization: ALLOW
Policy ID used:
840da5d85403f35ea76519ed1a18a33989f855bf1cf8
Errors during authorization: 0
```

## Explanation

Cedarling creates principal entities from the access, ID and userinfo tokens. The action, resource and context entities are declared in code. These four entities together form the `PARC` format that cedarling evaluates against policies provided in the policy store. The principal entities can be either User, Workload or Role. After forming the entities, cedarling evaluates them against the policies provided in the policy store. If entity is explicitly permitted by a policy, the result of the evaluation is `ALLOW`, otherwise it is `DENY`.

In this case there are two policies in the store, one for User entities and one for Workload entities:

```
@444da5d85403f35ea76519ed1a18a33989f855bf1cf8
permit(
    principal is Jans::Workload,
    action in [Jans::Action::"Read"],
    resource is Jans::Application
)when{
    resource.name == "Some Application"
};

@840da5d85403f35ea76519ed1a18a33989f855bf1cf8
permit(
    principal is Jans::User,
    action in [Jans::Action::"Read"],
    resource is Jans::Application
)when{
    resource.name == "Some Application"
};
```

These two policies say that a Principal entity (User or Workload) is allowed to execute a `Read` action on an Application resource when the resource is named "Some Application". As there are no policies for Role entities, the result of the evaluation for the Role entity is `DENY`.

In the script, the action, resource and context entities are used to create the request and execute the `authorize()` call:

```python

action = 'Jans::Action::"Read"'

resource = ResourceData.from_dict({
    "type": "Jans::Application",
    "id": "some_id",
    "app_id": "application_id",
    "name": "Some Application",
    "url": {
        "host": "jans.test",
        "path": "/protected-endpoint",
        "protocol": "http"
    }
})

context = {
    "current_time": int(time.time()),
    "device_health": ["Healthy"],
    "fraud_indicators": ["Allowed"],
    "geolocation": ["America"],
    "network": "127.0.0.1",
    "network_type": "Local",
    "operating_system": "Linux",
    "user_agent": "Linux"
}

request = Request(
    access_token,
    id_token,
    userinfo_token,
    action=action,
    resource=resource, context=context)

authorize_result = instance.authorize(request)
assert authorize_result.is_allowed()
```

Cedarling will return `is_allowed()` as `True` only if both the User and Workload entity evaluations are `ALLOW`.

## Exposed functions

The `pyo3` binding for cedarling exposes a number of cedarling functions for you to use. The documentation on this can be found [here](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_python/PYTHON_TYPES.md).
