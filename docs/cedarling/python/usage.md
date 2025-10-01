---
tags:
  - cedarling
  - python
  - usage
---

# Python usage

Cedarling provides two main authorization methods:

1. `authorize()` - Standard authorization using JWT tokens
2. `authorize_unsigned()` - Authorization with direct principal definitions (no token validation)

## Standard Token-Based Authorization

This method uses JWT tokens (access_token, id_token, userinfo_token, or more) for authentication.

### Prerequisites
- Before beginning, ensure that you have completed the [building steps](./README.md#building) and are currently in a virtual Python environment that has the `cedarling_python` module installed. You can confirm this with `pip list`.
- Run the script `jans/jans-cedarling/bindings/cedarling_python/example.py` from within the virtual environment.

### Example Usage
```python
# Create resource entity
resource = EntityData.from_dict({
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

# Define context
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

# Create request with JWT tokens
request = Request(
    tokens={
        "access_token": "eyJhbGciOiJIUzI1NiIs...",
        "id_token": "eyJraWQiOiJYTAwN2EyLTZkM...",
        "userinfo_token": "eyJraWQiOiJjb25uZW..."
    },
    action='Jans::Action::"Read"',
    resource=resource,
    context=context
)

# Authorize call
result = instance.authorize(request)

# Check results
print(f"Workload decision: {result.workload().decision}")
print(f"User decision: {result.person().decision}")
```

### Output

```bash
(venv) $ python example.py
{"request_id":"019474da-12ee-7315-bb12-35f46a9bc2b2","timestamp":"2025-01-17T09:20:36.334Z","log_kind":"System","pdp_id":"4cf7864b-50d4-4492-8cd1-3ddb424e2711","level":"INFO","msg":"Cedarling Authz initialized successfully","application_id":"My App","cedar_lang_version":"4.1.0","cedar_sdk_version":"4.2.2"}
{"request_id":"019474da-12f3-74f1-8f3e-da7624806135","timestamp":"2025-01-17T09:20:36.339Z","log_kind":"Decision","pdp_id":"4cf7864b-50d4-4492-8cd1-3ddb424e2711","policystore_id":"gICAgcHJpbmNpcGFsIGlz","policystore_version":"undefined","principal":"User & Workload","User":{"email":{"domain":"jans.test","uid":"admin"},"sub":"qzxn1Scrb9lWtGxVedMCky-Ql_ILspZaQA6fyuYktw0"},"Workload":{"client_id":"d7f71bea-c38d-4caf-a1ba-e43c74a11a62"},"diagnostics":{"reason":[{"id":"840da5d85403f35ea76519ed1a18a33989f855bf1cf8","description":"simple policy example for principal user"}],"errors":[]},"action":"Jans::Action::\"Read\"","resource":"Jans::Application::\"some_id\"","decision":"ALLOW","tokens":{"id_token":{"jti":"ijLZO1ooRyWrgIn7cIdNyA"},"Userinfo":{"jti":"OIn3g1SPSDSKAYDzENVoug"},"access":{"jti":"uZUh1hDUQo6PFkBPnwpGzg"}},"decision_time_micro_sec":3}
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

Cedarling creates principal entities from either the access, ID and userinfo tokens, or a combination of the three depending on bootstrap configurations. For example, to create a Workload entity only one token is sufficient. But to create a user entity at least ID or Userinfo tokens are needed. This is defined by `CEDARLING_USER_AUTHZ` and `CEDARLING_WORKLOAD_AUTHZ` in the [bootstrap configuration](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_python/cedarling_python.pyi#L10). Cedarling will make a best attempt to create entities based on tokens provided; if it is unable to do so it will raise an `EntitiesError` exception.
The action, resource and context entities are declared in code. These four entities together form the `PARC` format that cedarling evaluates against policies provided in the policy store. The principal entities can be either User, Workload or Role. After forming the entities, cedarling evaluates them against the policies provided in the policy store. If entity is explicitly permitted by a policy, the result of the evaluation is `ALLOW`, otherwise it is `DENY`.

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

resource = EntityData.from_dict({
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
    tokens=Tokens(access_token, id_token, userinfo_token),
    action=action,
    resource=resource, context=context)

authorize_result = instance.authorize(request)
assert authorize_result.is_allowed()
```

Cedarling will return `is_allowed()` as `True` only if the authorization queries set in the bootstrap return `True`. In case of the example, both `CEDARLING_USER_AUTHZ` and `CEDARLING_WORKLOAD_AUTHZ` were set to `enabled`, so cedarling will only return True if both user and workload evaluations are true. 

## Unsigned Authorization (direct principals)

```python
# Create resource entity
resource = EntityData.from_dict({
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

# Define principals directly (no tokens)
principals = [
    EntityData.from_dict({
        "type": "Jans::TestPrincipal1",
        "id": "test_user1",
        "is_ok": True
    }),
    EntityData.from_dict({
        "type": "Jans::TestPrincipal2",
        "id": "test_user2",
        "is_ok": True
    })
]

# Create request
request = RequestUnsigned(
    principals=principals,
    action='Jans::Action::"UpdateTestPrincipal"',
    resource=resource,
    context={}
)

# Authorize call
result = instance.authorize_unsigned(request)

# Check results per principal type
principal1_result = result.principal("Jans::TestPrincipal1")
print(f"Principal1 decision: {principal1_result.decision}")

principal2_result = result.principal("Jans::TestPrincipal2")
print(f"Principal2 decision: {principal2_result.decision}")
```

Key differences from standard authorization:

- No token validation performed
- Principals defined directly in code
- Results checked per principal type rather than user/workload
- Faster since it skips token parsing/validation

## Exposed functions

The `pyo3` binding exposes these main authorization functions:

- `authorize(request: Request) -> AuthorizationResult`
- `authorize_unsigned(request: RequestUnsigned) -> AuthorizationResult`

Full documentation can be found [here](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_python/PYTHON_TYPES.md).
