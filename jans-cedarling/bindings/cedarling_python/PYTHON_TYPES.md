
# Cedarling Python bindings types documentation

This document describes the Cedarling Python bindings types.
Documentation was generated from python types.

BootstrapConfig
===================

A Python wrapper for the Rust `BootstrapConfig` struct.
Configures the application, including authorization, logging, JWT validation, and policy store settings.

Attributes
----------  
:param application_name: A human-friendly identifier for the application.  
:param policy_store_uri: Optional URI of the policy store JSON file.  
:param policy_store_id: An identifier for the policy store.  
:param log_type: Log type, e.g., 'none', 'memory', 'std_out', or 'lock'.  
:param log_ttl: (Optional) TTL (time to live) in seconds for log entities when `log_type` is 'memory'. The default is 60s.  
:param decision_log_user_claims: List of claims to map from user entity, such as ["sub", "email", "username", ...]  
:param decision_log_workload_claims: List of claims to map from user entity, such as ["client_id", "rp_id", ...]  
:param decision_log_default_jwt_id: Token claims that will be used for decision logging. Default is "jti".  
:param user_authz: Enables querying Cedar engine authorization for a User principal.  
:param workload_authz: Enables querying Cedar engine authorization for a Workload principal.  
:param usr_workload_bool_op: Boolean operation ('AND' or 'OR') for combining `USER` and `WORKLOAD` authz results.  
:param local_jwks: Path to a local file containing a JWKS.  
:param local_policy_store: A JSON string containing a policy store.  
:param policy_store_local_fn: Path to a policy store JSON file.  
:param jwt_sig_validation: Validates JWT signatures if enabled.  
:param jwt_status_validation: Validates JWT status on startup if enabled.  
:param jwt_signature_algorithms_supported: A list of supported JWT signature algorithms.  
:param at_iss_validation: When enabled, the `iss` (Issuer) claim must be present in the Access Token and thescheme must be `https`.  
:param at_jti_validation: When enabled, the `jti` (JWT ID) claim must be present in the Access Token.  
:param at_nbf_validation: When enabled, the `nbf` (Not Before) claim must be present in the Access Token.  
:param at_exp_validation: When enabled, the `exp` (Expiration) claim must be present in the Access Token.  
:param idt_iss_validation: When enabled, the `iss` (Issuer) claim must be present in the ID Token.  
:param idt_sub_validation: When enabled, the `sub` (Subject) claim must be present in the ID Token.  
:param idt_exp_validation: When enabled, the `exp` (Expiration) claim must be present in the ID Token.  
:param idt_iat_validation: When enabled, the `iat` (Issued At) claim must be present in the ID Token.  
:param idt_aud_validation: When enabled, the `aud` (Audience) claim must be present in the ID Token.  
:param userinfo_iss_validation: When enabled, the `iss` (Issuer) claim must be present in the Userinfo Token.  
:param userinfo_sub_validation: When enabled, the `sub` (Subject) claim must be present in the Userinfo Token.  
:param userinfo_aud_validation: When enabled, the `aud` (Audience) claim must be present in the Userinfo Token.  
:param userinfo_exp_validation: When enabled, the `exp` (Expiration) claim must be present in the Userinfo Token.  
:param id_token_trust_mode: Trust mode for ID tokens, either 'None' or 'Strict'.  
:param lock: Enables integration with Lock Master for policies and SSE events.  
:param lock_master_configuration_uri: URI where Cedarling can get JSON file with all required metadata about Lock Master, i.e. .well-known/lock-master-configuration.  
:param dynamic_configuration: Toggles listening for SSE config updates.  
:param lock_ssa_jwt: SSA for DCR in a Lock Master deployment. Cedarling will validate this SSA JWT prior to DCR.  
:param audit_log_interval: Interval (in seconds) for sending log messages to Lock Master (0 to disable).  
:param audit_health_interval: Interval (in seconds) for sending health updates to Lock Master (0 to disable).  
:param audit_health_telemetry_interval: Interval (in seconds) for sending telemetry updates to Lock Master (0 to disable).  
:param listen_sse: Toggles listening for updates from the Lock Server.

Example
-------
```python
from cedarling import BootstrapConfig
# Example configuration
bootstrap_config = BootstrapConfig({
    "application_name": "MyApp",
    "policy_store_uri": None,
    "policy_store_id": "policy123",
    "log_type": "memory",
    "log_ttl": 86400,
    "decision_log_user_claims": ["sub", "email", "username"]
    "decision_log_workload_claims": ["client_id", "rp_id"]
    "decision_log_default_jwt_id":"jti"
    "user_authz": "enabled",
    "workload_authz": "enabled",
    "usr_workload_bool_op": "AND",
    "local_jwks": "./path/to/your_jwks.json",
    "local_policy_store": None,
    "policy_store_local_fn": "./path/to/your_policy_store.json",
    "jwt_sig_validation": "enabled",
    "jwt_status_validation": "disabled",
    "at_iss_validation": "enabled",
    "at_jti_validation": "enabled",
    "at_nbf_validation": "disabled",
    "idt_iss_validation": "enabled",
    "idt_sub_validation": "enabled",
    "idt_exp_validation": "enabled",
    "idt_iat_validation": "enabled",
    "idt_aud_validation": "enabled",
    "userinfo_iss_validation": "enabled",
    "userinfo_sub_validation": "enabled",
    "userinfo_aud_validation": "enabled",
    "userinfo_exp_validation": "enabled",
    "id_token_trust_mode": "Strict",
    "lock": "disabled",
    "lock_master_configuration_uri": None,
    "dynamic_configuration": "disabled",
    "lock_ssa_jwt": None,
    "audit_log_interval": 0,
    "audit_health_interval": 0,
    "audit_health_telemetry_interval": 0,
    "listen_sse": "disabled",
})
```
___

Cedarling
=========

A Python wrapper for the Rust `cedarling::Cedarling` struct.
Represents an instance of the Cedarling application, a local authorization service
that answers authorization questions based on JWT tokens.

Attributes
----------  
:param config: A `BootstrapConfig` object for initializing the Cedarling instance.

Methods
-------
.. method:: __init__(self, config)

    Initializes the Cedarling instance with the provided configuration.

    :param config: A `BootstrapConfig` object with startup settings.

.. method:: pop_logs(self) -> List[dict]

    Retrieves and removes all logs from storage.

    :returns: A list of log entries as Python objects.

    :raises ValueError: If an error occurs while fetching logs.

.. method:: get_log_by_id(self, id: str) -> dict|None

    Gets a log entry by its ID.

    :param id: The log entry ID.

    :raises ValueError: If an error occurs while fetching the log.

.. method:: get_log_ids(self) -> List[str]

    Retrieves all stored log IDs.

.. method:: authorize(self, request: Request) -> AuthorizeResult

    Execute authorize request
    :param request: Request struct for authorize.

___

ResourceData
============

A Python wrapper for the Rust `cedarling::ResourceData` struct. This class represents
a resource entity with a type, ID, and attributes. Attributes are stored as a payload
in a dictionary format.

Attributes
----------  
:param resource_type: Type of the resource entity.  
:param id: ID of the resource entity.  
:param payload: Optional dictionary of attributes.

Methods
-------
.. method:: __init__(self, resource_type: str, id: str, **kwargs: dict)
    Initialize a new ResourceData. In kwargs the payload is a dictionary of entity attributes.

.. method:: from_dict(cls, value: dict) -> ResourceData
    Initialize a new ResourceData from a dictionary.
    To pass `resource_type` you need to use `type` key.
___

Request
=======

A Python wrapper for the Rust `cedarling::Request` struct. Represents
authorization data with access token, action, resource, and context.

Attributes
----------  
:param access_token: The access token string.  
:param id_token: The id token string.  
:param userinfo_token: The userinfo token string.  
:param action: The action to be authorized.  
:param resource: Resource data (wrapped `ResourceData` object).  
:param context: Python dictionary with additional context.

Example
-------
```python
# Create a request for authorization
request = Request(access_token="token123", action="read", resource=resource, context={})
```
___

AuthorizeResult
===============

A Python wrapper for the Rust `cedarling::AuthorizeResult` struct.
Represents the result of an authorization request.

Methods
-------
.. method:: is_allowed(self) -> bool
    Returns whether the request is allowed.

.. method:: workload(self) -> AuthorizeResultResponse
    Returns the detailed response as an `AuthorizeResultResponse` object.

___

AuthorizeResultResponse
=======================

A Python wrapper for the Rust `cedar_policy::Response` struct.
Represents the result of an authorization request.

Attributes
----------  
:param decision: The authorization decision (wrapped `Decision` object).  
:param diagnostics: Additional information on the decision (wrapped `Diagnostics` object).
___

Decision
========

Represents the decision result of a Cedar policy authorization.

Methods
-------
value() -> str
    Returns the string value of the decision.
__str__() -> str
    Returns the string representation of the decision.
__repr__() -> str
    Returns the detailed type representation of the decision.
__eq__(other: Decision) -> bool
    Compares two `Decision` objects for equality.
___

Diagnostics
===========

Provides detailed information about how a policy decision was made, including policies that contributed to the decision and any errors encountered during evaluation.

Attributes
----------
reason : set of str
    A set of `PolicyId`s for the policies that contributed to the decision. If no policies applied, this set is empty.
errors : list of PolicyEvaluationError
    A list of errors that occurred during the authorization process. These are unordered as policies may be evaluated in any order.
___

PolicyEvaluationError
=====================

Represents an error that occurred when evaluating a Cedar policy.

Attributes
----------
id : str
    The ID of the policy that caused the error.
error : str
    The error message describing the evaluation failure.
___

# authorize_errors.AccessTokenEntitiesError
Error encountered while creating access token entities
___

# authorize_errors.ActionError
Error encountered while parsing Action to EntityUid
___

# authorize_errors.AuthorizeError
Exception raised by authorize_errors
___

# authorize_errors.CreateContextError
Error encountered while validating context according to the schema
___

# authorize_errors.CreateIdTokenEntityError
Error encountered while creating id token entities
___

# authorize_errors.CreateRequestUserEntityError
Error encountered while creating cedar_policy::Request for user entity principal
___

# authorize_errors.CreateRequestWorkloadEntityError
Error encountered while creating cedar_policy::Request for workload entity principal
___

# authorize_errors.CreateUserEntityError
Error encountered while creating User entity
___

# authorize_errors.CreateUserinfoTokenEntityError
Error encountered while creating Userinfo_token entity
___

# authorize_errors.EntitiesError
Error encountered while collecting all entities
___

# authorize_errors.EntitiesToJsonError
Error encountered while parsing all entities to json for logging
___

# authorize_errors.ProcessTokens
Error encountered while processing JWT token data
___

# authorize_errors.ResourceEntityError
Error encountered while creating resource entity
___

# authorize_errors.RoleEntityError
Error encountered while creating role entity
___

