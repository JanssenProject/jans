
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

.. method:: pop_logs(self)

    Retrieves and removes all logs from storage.

    :returns: A list of log entries as Python objects.
    :rtype: List[PyObject]

    :raises ValueError: If an error occurs while fetching logs.

.. method:: get_log_by_id(self, id)

    Gets a log entry by its ID.

    :param id: The log entry ID.
    :type id: str

    :returns: The log entry as a Python object or None if not found.
    :rtype: Optional[PyObject]

    :raises ValueError: If an error occurs while fetching the log.

.. method:: get_log_ids(self)

    Retrieves all stored log IDs.

    :returns: A list of log entry IDs.
    :rtype: List[str]

.. method:: authorize(self, Request)

   Evaluate Authorization Request.

___

Request
=======

Python wrapper for the Rust `cedarling::Request` struct.
Stores authorization data

Attributes
----------  
:param access_token: A string containing the access token.

Example
-------
```
req = Request(access_token="your_token")
```
___

