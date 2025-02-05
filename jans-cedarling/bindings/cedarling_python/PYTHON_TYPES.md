
# Cedarling Python bindings types documentation

This document describes the Cedarling Python bindings types.
Documentation was generated from python types.

BootstrapConfig
=========

A Python wrapper for the Rust `cedarling::BootstrapConfig` struct.
Configures the `Cedarling` application, including authorization, logging, and policy store settings.

Methods
-------
.. method:: __init__(self, options)

    Initializes the Cedarling instance with the provided configuration.

    :param options: A `dict` with startup settings.

.. method:: load_from_file(str) -> BootstrapConfig

    Loads the bootstrap config from a file.

    :returns: A BootstrapConfig instance

    :raises ValueError: If a provided value is invalid or decoding fails.
    :raises OSError: If there is an error reading while the file.

.. method:: load_from_json(str) -> BootstrapConfig

    Loads the bootstrap config from a JSON string.

    :returns: A BootstrapConfig instance

    :raises ValueError: If a provided value is invalid or decoding fails.

.. method:: from_env(config=None) -> BootstrapConfig

    Loads the bootstrap config from environment variables, optionally merging with provided config.

    :param config: Optional dictionary with additional configuration to merge with environment variables.
    :returns: A BootstrapConfig instance
    :raises ValueError: If a provided value is invalid or decoding fails.
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
:param tokens: A class containing the JWTs what will be used for the request.  
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

# authorize_errors.ActionError
Error encountered while parsing Action to EntityUid
___

# authorize_errors.AuthorizeError
Exception raised by authorize_errors
___

# authorize_errors.BuildContextError
Error encountered while building the request context
___

# authorize_errors.BuildEntityError
Error encountered while running on strict id token trust mode
___

# authorize_errors.CreateContextError
Error encountered while validating context according to the schema
___

# authorize_errors.EntitiesError
Error encountered while collecting all entities
___

# authorize_errors.EntitiesToJsonError
Error encountered while parsing all entities to json for logging
___

# authorize_errors.IdTokenTrustModeError
Error encountered while running on strict id token trust mode
___

# authorize_errors.ProcessTokens
Error encountered while processing JWT token data
___

# authorize_errors.UserRequestValidationError
Error encountered while creating cedar_policy::Request for user entity principal
___

# authorize_errors.WorkloadRequestValidationError
Error encountered while creating cedar_policy::Request for workload entity principal
___

