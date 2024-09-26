AuthzConfig
============

`AuthzConfig` is a Python wrapper around the Rust `cedarling::AuthzConfig` struct.

Class Definition
----------------

.. class:: AuthzConfig(application_name=None)

    The `AuthzConfig` class represents the configuration for the Authorization component.
    It holds an optional `application_name` that can be set and retrieved.

    :param application_name: Optional. A string representing the name of the application.

Methods
-------

.. method:: __init__(self, application_name=None)

    Initializes a new instance of the `AuthzConfig` class.

    :param application_name: Optional. The name of the application as a string. Defaults to `None`.

.. method:: application_name(self, application_name: str)

    Sets the value of the `application_name` attribute.

    :param application_name: The name of the application as a string.
    :raises ValueError: If the application name is invalid or not provided.

Example
-------

```python

    # Creating a new AuthzConfig instance
    config = AuthzConfig(application_name="MyApp")

    # Setting the application name
    config.application_name = "NewAppName"

    # Getting the application name
    print(config.application_name)
```

___

MemoryLogConfig
===============

`MemoryLogConfig` is a Python wrapper around the Rust `cedarling::LogTypeConfig` struct.
 It is used to configure memory-based logging and is part of the overall logging configuration within the `cedarling` system.
 The configuration takes parameters from `cedarling::MemoryLogConfig`.

Class Definition
----------------

.. class:: MemoryLogConfig(log_ttl=60)

    The `MemoryLogConfig` class allows you to configure memory logging settings, particularly the time-to-live (TTL) for log entries.

    :param log_ttl: Optional. The maximum time to live (in seconds) of log entries. Defaults to `60` seconds (1 minute).

Attributes
----------

.. attribute:: log_ttl

    The time-to-live (TTL) for log entries in memory, measured in seconds. This represents the `CEDARLING_LOG_TTL` setting from the `bootstrap properties` as defined in the `cedarling` documentation.

    :type: int

Methods
-------

.. method:: __init__(self, log_ttl=60)

    Initializes a new instance of the `MemoryLogConfig` class.

    :param log_ttl: Optional. The time-to-live (in seconds) for log entries. Defaults to `60` seconds (1 minute).

Example
-------

```python

    # Creating a new MemoryLogConfig instance with the default TTL
    config = MemoryLogConfig()

    # Creating a new MemoryLogConfig instance with a custom TTL
    config = MemoryLogConfig(log_ttl=120)

    # Accessing the log_ttl attribute
    print(config.log_ttl)

    # Setting a new TTL value
    config.log_ttl = 300
```

___

OffLogConfig
============

`OffLogConfig` is a Python wrapper around the Rust `cedarling::LogTypeConfig` struct.
This configuration represents the "Off" log setting, where the logger is effectively disabled, and all log entries are ignored.

Class Definition
----------------

.. class:: OffLogConfig()

    The `OffLogConfig` class is used when logging is turned off. This configuration disables logging, meaning that no logs are captured or stored.

    This configuration is invariant, meaning once created, it remains constant and cannot be modified.

Methods
-------

.. method:: __init__(self)

    Initializes a new instance of the `OffLogConfig` class. This effectively disables logging.

Example
-------

.. code-block:: python

    # Creating a new OffLogConfig instance to disable logging
    config = OffLogConfig()

___

StdOutLogConfig
============

`StdOutLogConfig` is a Python wrapper around the Rust `cedarling::LogTypeConfig` struct.
This configuration represents the "StdOutLogConfig" log setting, where the logger writes log information to std output stream.

Class Definition
----------------

.. class:: StdOutLogConfig()

    The `StdOutLogConfig` class is used when we want to write log information to std output stream.

    This configuration is invariant, meaning once created, it remains constant and cannot be modified.

Methods
-------

.. method:: __init__(self)

    Initializes a new instance of the `StdOutLogConfig` class. This allows logger write logger to std output stream.

Example
-------

```python

    # Creating a new StdOutLogConfig instance to write log information to std output stream.
    config = StdOutLogConfig()
```

___

PolicyStoreSource
=================

`PolicyStoreSource` is a Python wrapper around the Rust `cedarling::PolicyStoreSource` struct. It represents the source from which policies are read. Currently, the supported source for reading policies is a JSON string.

Class Definition
----------------

.. class:: PolicyStoreSource(json=None)

    The `PolicyStoreSource` class is used to specify the source from which the policy data is loaded. At present, it supports reading policies from a JSON string.

    :param json: Optional. A JSON-formatted string that represents the policy data.

Methods
-------

.. method:: __init__(self, json=None)

    Initializes a new instance of the `PolicyStoreSource` class with a JSON string. If no JSON string is provided, a `ValueError` is raised.

    :param json: A JSON-formatted string. If not provided, raises a `ValueError`.
    :raises ValueError: If the `json` parameter is not specified.

Example
-------

```python

    # Creating a new PolicyStoreSource instance with a JSON string
    json_string = '{...}'
    config = PolicyStoreSource(json=json_string)

    # Attempting to create a PolicyStoreSource without a JSON string (raises ValueError)
    try:
        invalid_config = PolicyStoreSource()
    except ValueError as e:
        print(f"Error: {e}")
```

___

PolicyStoreConfig
=================

`PolicyStoreConfig` is a Python wrapper around the Rust `cedarling::PolicyStoreConfig` struct.
 It represents the configuration for the policy store, including the source from which policies are read and the optional policy store ID.

Class Definition
----------------

.. class:: PolicyStoreConfig(source=None, store_id=None)

    The `PolicyStoreConfig` class is used to configure how and where policies are loaded. The `source` specifies the location (e.g., JSON) of the policy, and `store_id` represents the ID of the policy store, which is optional.

    :param source: Optional. A `PolicyStoreSource` object representing the policy source.
    :param store_id: Optional. A string representing the policy store ID. If not specified, only one policy store is assumed in the `source`.

Attributes
----------

.. attribute:: source

    The source from which the policy is read. This attribute is required for policy configuration.

    :type: PolicyStoreSource or None

.. attribute:: store_id

    The ID of the policy store. If this is not provided, the assumption is that there is only one policy store in the `source`.

    :type: str or None

Methods
-------

.. method:: __init__(self, source=None, store_id=None)

    Initializes a new instance of the `PolicyStoreConfig` class. Both `source` and `store_id` are optional, but the `source` must be provided for the configuration to be valid.

    :param source: Optional. A `PolicyStoreSource` object.
    :param store_id: Optional. A string representing the ID of the policy store.

Example
-------

```python

    # Creating a new PolicyStoreConfig instance with a source and store_id
    source = PolicyStoreSource(json='{"policy": {"id": "policy1", "rules": []}}')
    config = PolicyStoreConfig(source=source, store_id="store1")

    # Creating a PolicyStoreConfig instance without a store_id
    config_without_store_id = PolicyStoreConfig(source=source)

    # Accessing attributes
    print(config.source)
    print(config.store_id)
    
    # Attempting to create PolicyStoreConfig without a source will raise an error during conversion
    try:
        invalid_config = PolicyStoreConfig(store_id="store1")
        # This will raise an error when converted to cedarling::PolicyStoreConfig
    except ValueError as e:
        print(f"Error: {e}")
```

___

BootstrapConfig
===============

`BootstrapConfig` is a Python wrapper around the Rust `cedarling::BootstrapConfig` struct.
 It represents the main configuration for bootstrapping the `Cedarling` application.
 This configuration includes settings for authorization (`AuthzConfig`), logging (`LogConfig`), and policy store (`PolicyStoreConfig`).

Class Definition
----------------

.. class:: BootstrapConfig(authz_config=None, log_config=None, policy_store_config=None)

    The `BootstrapConfig` class is used to configure the initial properties for the `Cedarling` application. This includes setting up authorization, logging, and the policy store.

    :param authz_config: An `AuthzConfig` object representing the authorization configuration.
    :param log_config: A logging configuration, which can be one of the following:
                       - `OffLogConfig`: Disable logging.
                       - `MemoryLogConfig`: Configure memory-based logging.
                       - `StdOutLogConfig`: Configure logging to standard output.
    :param policy_store_config: A `PolicyStoreConfig` object representing the policy store configuration.

Attributes
----------

.. attribute:: authz_config

    A set of properties used to configure the `Authz` (authorization) in the `Cedarling` application.

    :type: AuthzConfig

.. attribute:: log_config

    A set of properties used to configure logging in the `Cedarling` application. The log configuration must be one of the following:
    - `OffLogConfig`: Disables logging.
    - `MemoryLogConfig`: Configures memory-based logging.
    - `StdOutLogConfig`: Logs to standard output.

    :type: LogConfig

.. attribute:: policy_store_config

    A set of properties used to load and configure the policy store in the `Cedarling` application.

    :type: PolicyStoreConfig

Methods
-------

.. method:: __init__(self, authz_config=None, log_config=None, policy_store_config=None)

    Initializes a new instance of the `BootstrapConfig` class.

    :param authz_config: Optional. An `AuthzConfig` object representing the authorization configuration.
    :param log_config: Optional. A logging configuration (`OffLogConfig`, `MemoryLogConfig`, `StdOutLogConfig`).
    :param policy_store_config: Optional. A `PolicyStoreConfig` object for configuring the policy store.

.. method:: set_log_config(self, value)

    Sets the log configuration. The value must be one of the following types: `OffLogConfig`, `MemoryLogConfig`, or `StdOutLogConfig`.

    :param value: The log configuration object.
    :raises ValueError: If the provided log configuration is not a valid type.

Example
-------

```python

    from cedarling import BootstrapConfig, AuthzConfig, MemoryLogConfig, PolicyStoreConfig

    # Creating a new BootstrapConfig with memory log configuration
    authz = AuthzConfig(application_name="MyApp")
    log_config = MemoryLogConfig(log_ttl=300)
    policy_store = PolicyStoreConfig(source=PolicyStoreSource(json='{...}'))

    bootstrap_config = BootstrapConfig(authz_config=authz, log_config=log_config, policy_store_config=policy_store)

    # Setting log config to OffLogConfig
    bootstrap_config.log_config = OffLogConfig()

    # Attempting to set an invalid log configuration will raise a ValueError
    try:
        bootstrap_config.log_config = "InvalidConfig"
    except ValueError as e:
        print(f"Error: {e}")
```

___

Cedarling
=========

The `Cedarling` class is a Python wrapper around the Rust `cedarling::Cedarling` struct.
It represents an instance of the Cedarling application, which serves as a performant local authorization service running the Cedar Engine.
Policies and schemas are loaded at startup from a local "Policy Store", and Cedarling can answer authorization questions such as whether an action
should be allowed on a resource based on provided JWT tokens.

Class Definition
----------------

.. class:: Cedarling(config)

    The `Cedarling` class is used to create and interact with a Cedarling instance. It provides methods to retrieve logs and log details, as well as a list of log IDs.

    :param config: A `BootstrapConfig` object that configures the Cedarling instance at startup.

Attributes
----------

This class does not expose attributes directly. Interaction occurs via the provided methods.

Methods
-------

.. method:: __init__(self, config)

    Initializes a new instance of the `Cedarling` class with the specified bootstrap configuration.

    :param config: A `BootstrapConfig` object containing configuration details for the Cedarling application.

.. method:: pop_logs(self)

    Returns the current logs and removes them from the storage. This method is useful for retrieving logs generated by the Cedarling instance.

    :returns: A list of log entries (converted to Python objects).
    :rtype: List[PyObject]

    :raises ValueError: If an error occurs while fetching logs.

.. method:: get_log_by_id(self, id)

    Retrieves a specific log entry by its ID.

    :param id: The ID of the log entry to retrieve.
    :type id: str

    :returns: The log entry as a Python object if found, or None if no log with the specified ID exists.
    :rtype: Optional[PyObject]

    :raises ValueError: If an error occurs while fetching the log.

.. method:: get_log_ids(self)

    Returns a list of all log IDs stored in the Cedarling instance.

    :returns: A list of log entry IDs.
    :rtype: List[str]

___
