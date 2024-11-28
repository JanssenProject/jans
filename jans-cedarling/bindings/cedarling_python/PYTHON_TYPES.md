
# Cedarling Python bindings types documentation

This document describes the Cedarling Python bindings types.
Documentation was generated from python types.

MemoryLogConfig
===============

A Python wrapper for the Rust `cedarling::LogTypeConfig`, used to configure memory-based logging.

Attributes
----------  
:param log_ttl: Optional TTL for log entries (in seconds), default is `60`.

Example
-------
```
# Initialize with default TTL
config = MemoryLogConfig()              
# Initialize with custom TTL
config = MemoryLogConfig(log_ttl=120)   
print(config.log_ttl)                    # Accessing TTL
config.log_ttl = 300                     # Updating TTL
```
___

DisabledLoggingConfig
======================

A Python wrapper for the Rust `cedarling::LogTypeConfig` struct.
This class configures logging to be disabled, meaning no log entries are captured.

Attributes
----------
- `None`: This class has no attributes.

Example
-------
```
# Disable logging
config = DisabledLoggingConfig()
```
___

StdOutLogConfig
================

A Python wrapper for the Rust `cedarling::LogTypeConfig` struct.
Represents the configuration for logging to the standard output stream.

Attributes
----------
This configuration is constant and cannot be modified.

Example
-------
```
# Create an instance for logging to standard output
config = StdOutLogConfig()
```
___

PolicyStoreSource
=================

A Python wrapper for the Rust `cedarling::PolicyStoreSource` struct.
This class specifies the source for reading policy data, currently supporting
JSON strings.

Attributes
----------  
:param json: Optional JSON string for policy data.

Example
-------
```
# Initialize with a JSON string
config = PolicyStoreSource(json='{...}')
```
___

PolicyStoreConfig
=================

A Python wrapper for the Rust `cedarling::PolicyStoreConfig` struct.
Configures how and where policies are loaded, specifying the source and optional store ID.

Attributes
----------  
:param source: Optional `PolicyStoreSource` for the policy location.  
:param store_id: Optional store ID; assumes one store if not provided.

Example
-------
```
# Create a PolicyStoreConfig with a source and store_id
source = PolicyStoreSource(json='{...')
config = PolicyStoreConfig(source=source, store_id="store1")

# Create without store_id
config_without_store_id = PolicyStoreConfig(source=source)

# Access attributes
print(config.source)
print(config.store_id)
```
___

BootstrapConfig
===============

A Python wrapper for the Rust `cedarling::BootstrapConfig` struct.
Configures the `Cedarling` application, including authorization, logging, and policy store settings.

Attributes
----------  
:param application_name: The name of this application.  
:param authz_config: An `AuthzConfig` object for authorization settings.  
:param log_config: A logging configuration (can be `DisabledLoggingConfig`, `MemoryLogConfig`, or `StdOutLogConfig`).  
:param policy_store_config: A `PolicyStoreConfig` object for the policy store configuration.  
:param jwt_config: A `JwtConfig` object for JWT validation settings.

Example
-------
```
from cedarling import BootstrapConfig, AuthzConfig, MemoryLogConfig, PolicyStoreConfig

# Create a BootstrapConfig with memory logging
authz = AuthzConfig(application_name="MyApp")
log_config = MemoryLogConfig(log_ttl=300)
policy_store = PolicyStoreConfig(source=PolicyStoreSource(json='{...}'))
jwt_config = JwtConfig(enabled=False)

bootstrap_config = BootstrapConfig(application_name="MyApp",authz_config=authz, log_config=log_config, policy_store_config=policy_store, jwt_config=jwt_config)
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

# authorize_errors.CreateRequestRoleEntityError
Error encountered while creating cedar_policy::Request for role entity principal
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

# authorize_errors.DecodeTokens
Error encountered while decoding JWT token data
___

# authorize_errors.EntitiesError
Error encountered while collecting all entities
___

# authorize_errors.EntitiesToJsonError
Error encountered while parsing all entities to json for logging
___

# authorize_errors.ResourceEntityError
Error encountered while creating resource entity
___

# authorize_errors.RoleEntityError
Error encountered while creating role entity
___

