# This software is available under the Apache-2.0 license.
# See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
#
# Copyright (c) 2024, Gluu, Inc.

from typing import Optional, List, final, Dict, Any

@final
class BootstrapConfig:
    """
    Represents the configuration options for bootstrapping the application.

    Example Usage:
        bootstrap_config = BootstrapConfig({
            "CEDARLING_APPLICATION_NAME": "MyApp",
            "CEDARLING_POLICY_STORE_ID": "12345",
            "CEDARLING_LOG_TYPE": "memory",
            "CEDARLING_LOG_TTL": 30,
            ...
        })
    """

    def __init__(
        self,
        options: dict,
    ) -> None:
        """
        Initialize a new instance of the class with configuration options.

        Args:
            options (dict): A dictionary containing configuration key-value pairs.
            For more information visit https://docs.jans.io/head/cedarling/cedarling-properties/
        Returns:
            None: This method initializes the instance.

        Raises:
            KeyError: If any required configuration key is missing.
            ValueError: If a provided value is invalid or extraction fails.
        """
        ...

    @staticmethod
    def load_from_file(path: str) -> BootstrapConfig:
        """
        Loads the configuration from a file.

        Args:
            path (str): The path to the configuration file.

        Returns:
            BootstrapConfig: An instance of the configuration class.

        Raises:
            ValueError: If a provided value is invalid or decoding fails.
            OSError: If there is an error while reading the file.
        """
        ...

    @staticmethod
    def load_from_json(config_json: str) -> BootstrapConfig:
        """
        Loads the configuration from a JSON string.

        Args:
            config_json (str): The JSON string containing the configuration.

        Returns:
            BootstrapConfig: An instance of the configuration class.

        Raises:
            ValueError: If a provided value is invalid or extraction fails.
        """
        ...

    @staticmethod
    def from_env(options: Dict | None = None) -> BootstrapConfig:
        """
        Loads the configuration from environment variables.

        Reads environment variables matching the configuration keys listed in the
        class documentation. All required keys must be present in the environment.
        You can specify dict, but keys from environment variables have bigger priority.

        Returns:
            BootstrapConfig: An instance of the configuration class.

        Raises:
            KeyError: If any required environment variable is missing.
            ValueError: If a provided value is invalid or extraction fails.
        """
        ...

@final
class Cedarling:
    def __init__(self, config: BootstrapConfig) -> None:
        """
        Initializes the Cedarling instance with the provided configuration.

        Args:
            config: A BootstrapConfig object with startup settings.

        Raises:
            ValueError: If configuration is invalid or initialization fails.
        """
        ...

    def authorize(self, request: Request) -> AuthorizeResult:
        """
        Execute authorize request.

        Args:
            request: Request struct for authorize.

        Returns:
            AuthorizeResult: The authorization result.

        Raises:
            ProcessTokens: Error encountered while processing JWT token data.
            ActionError: Error encountered while parsing Action to EntityUid.
            CreateContextError: Error encountered while validating context according to the schema.
            InvalidPrincipalError: Error encountered while creating cedar_policy::Request for principal.
            ValidateEntitiesError: Error encountered while validating the entities to the schema.
            EntitiesToJsonError: Error encountered while parsing all entities to json for logging.
            BuildContextError: Error encountered while building the request context.
            IdTokenTrustModeError: Error encountered while running on strict id token trust mode.
            BuildEntityError: Error encountered while building Cedar entities.
            ExecuteRuleError: Error encountered while executing the rule for principals.
            BuildUnsignedRoleEntityError: Error building Role entity for unsigned request.
            RequestValidationError: Error encountered while validating the request.
            RuntimeError: If JSON conversion of tokens or context fails.
        """
        ...

    def authorize_unsigned(self, request: RequestUnsigned) -> AuthorizeResult:
        """
        Authorize request with unsigned data.

        Args:
            request: RequestUnsigned struct for authorize.

        Returns:
            AuthorizeResult: The authorization result.

        Raises:
            ActionError: Error encountered while parsing Action to EntityUid.
            CreateContextError: Error encountered while validating context according to the schema.
            InvalidPrincipalError: Error encountered while creating cedar_policy::Request for principal.
            ValidateEntitiesError: Error encountered while validating the entities to the schema.
            EntitiesToJsonError: Error encountered while parsing all entities to json for logging.
            BuildContextError: Error encountered while building the request context.
            BuildEntityError: Error encountered while building Cedar entities.
            ExecuteRuleError: Error encountered while executing the rule for principals.
            BuildUnsignedRoleEntityError: Error building Role entity for unsigned request.
            RequestValidationError: Error encountered while validating the request.
            RuntimeError: If JSON conversion of context fails.
        """
        ...

    def authorize_multi_issuer(
        self, request: AuthorizeMultiIssuerRequest
    ) -> MultiIssuerAuthorizeResult:
        """
        Authorize multi-issuer request.
        Makes authorization decision based on multiple JWT tokens from different issuers.

        Args:
            request: AuthorizeMultiIssuerRequest struct for authorize.

        Returns:
            MultiIssuerAuthorizeResult: The multi-issuer authorization result.

        Raises:
            ProcessTokens: Error encountered while processing JWT token data.
            ActionError: Error encountered while parsing Action to EntityUid.
            CreateContextError: Error encountered while validating context according to the schema.
            InvalidPrincipalError: Error encountered while creating cedar_policy::Request for principal.
            ValidateEntitiesError: Error encountered while validating the entities to the schema.
            EntitiesToJsonError: Error encountered while parsing all entities to json for logging.
            BuildContextError: Error encountered while building the request context.
            IdTokenTrustModeError: Error encountered while running on strict id token trust mode.
            BuildEntityError: Error encountered while building Cedar entities.
            ExecuteRuleError: Error encountered while executing the rule for principals.
            MultiIssuerValidationError: Error encountered during multi-issuer token validation.
            MultiIssuerEntityError: Error encountered while building multi-issuer entities.
            RequestValidationError: Error encountered while validating the request.
            RuntimeError: If JSON conversion of context fails.
        """
        ...

    def pop_logs(self) -> List[Dict]:
        """
        Retrieves and removes all logs from storage.

        Returns:
            A list of log entries as Python dictionaries.

        Raises:
            RuntimeError: If JSON conversion of log entries fails.
        """
        ...

    def get_log_by_id(self, id: str) -> Optional[Dict]:
        """
        Gets a log entry by its ID.

        Args:
            id: The log entry ID.

        Returns:
            The log entry as a dictionary, or None if not found.

        Raises:
            RuntimeError: If JSON conversion of log entry fails.
        """
        ...

    def get_log_ids(self) -> List[str]:
        """
        Retrieves all stored log IDs.

        Returns:
            A list of log entry IDs.
        """
        ...

    def get_logs_by_tag(self, tag: str) -> List[Dict]:
        """
        Retrieves all logs matching a specific tag. Tags can be 'log_kind', 'log_level' params from log entries.

        Args:
            tag: A string specifying the tag type.

        Returns:
            A list of log entries filtered by the tag, each converted to a Python dictionary.

        Raises:
            RuntimeError: If JSON conversion of log entries fails.
        """
        ...

    def get_logs_by_request_id(self, request_id: str) -> List[Dict]:
        """
        Retrieves log entries associated with a specific request ID.

        Args:
            request_id: The unique identifier for the request.

        Returns:
            A list of dictionaries, each representing a log entry related to the specified request ID.

        Raises:
            RuntimeError: If JSON conversion of log entries fails.
        """
        ...

    def get_logs_by_request_id_and_tag(self, request_id: str, tag: str) -> List[Dict]:
        """
        Retrieves all logs associated with a specific request ID and tag.

        Args:
            request_id: The request ID as a string.
            tag: The tag type as a string.

        Returns:
            A list of log entries matching both the request ID and tag, each converted to a Python dictionary.

        Raises:
            RuntimeError: If JSON conversion of log entries fails.
        """
        ...

    def shut_down(self): ...

    def push_data(
        self, key: str, value: Any, *, ttl_secs: Optional[int] = None
    ) -> None:
        """
        Push a value into the data store with an optional TTL.

        If the key already exists, the value will be replaced.
        If TTL is not provided, the default TTL from configuration is used.

        Args:
            key: The key for the data entry.
            value: The value to store. Can be any JSON-serializable Python object.
            ttl_secs: Optional TTL in seconds (None uses default from config).

        Raises:
            DataError: If the operation fails.
        """
        ...

    def get_data(self, key: str) -> Optional[Any]:
        """
        Get a value from the data store by key.

        Returns None if the key doesn't exist or the entry has expired.
        If metrics are enabled, increments the access count for the entry.

        Args:
            key: The key to retrieve.

        Returns:
            The value as a Python object, or None if not found.

        Raises:
            DataError: If the operation fails.
        """
        ...

    def get_data_entry(self, key: str) -> Optional["DataEntry"]:
        """
        Get a data entry with full metadata by key.

        Returns None if the key doesn't exist or the entry has expired.
        Includes metadata like creation time, expiration, access count, and type.

        Args:
            key: The key to retrieve.

        Returns:
            A DataEntry object with metadata, or None if not found.

        Raises:
            DataError: If the operation fails.
        """
        ...

    def remove_data(self, key: str) -> bool:
        """
        Remove a value from the data store by key.

        Args:
            key: The key to remove.

        Returns:
            True if the key existed and was removed, False otherwise.

        Raises:
            DataError: If the operation fails.
        """
        ...

    def clear_data(self) -> None:
        """
        Clear all entries from the data store.

        Raises:
            DataError: If the operation fails.
        """
        ...

    def list_data(self) -> List["DataEntry"]:
        """
        List all entries with their metadata.

        Returns a list of DataEntry objects containing key, value, type, and timing metadata.

        Returns:
            A list of DataEntry objects.

        Raises:
            DataError: If the operation fails.
        """
        ...

    def get_stats(self) -> "DataStoreStats":
        """
        Get statistics about the data store.

        Returns current entry count, capacity limits, and configuration state.

        Returns:
            A DataStoreStats object.

        Raises:
            DataError: If the operation fails.
        """
        ...

@final
class Request:
    tokens: Dict[str, str]
    action: str
    resource: EntityData
    context: Dict[str, Any]

    def __init__(
        self,
        tokens: Dict[str, Any],
        action: str,
        resource: EntityData,
        context: Dict[str, Any],
    ) -> None: ...

@final
class RequestUnsigned:
    principals: List[EntityData]
    action: str
    resource: EntityData
    context: Dict[str, Any]

    def __init__(
        self,
        principals: List[EntityData],
        action: str,
        resource: EntityData,
        context: Dict[str, Any],
    ) -> None: ...

@final
class CedarEntityMapping:
    entity_type: str
    id: str

    def __init__(self, entity_type: str, id: str) -> None: ...

@final
class EntityData:
    cedar_entity_mapping: CedarEntityMapping
    payload: Dict[str, Any]

    def __init__(self, cedar_entity_mapping: CedarEntityMapping, **kwargs) -> None: ...
    @classmethod
    def from_dict(cls, value: Dict[str, Any]) -> "EntityData":
        """
        Create an EntityData instance from a dictionary.

        Args:
            value: Dictionary containing entity data.

        Returns:
            EntityData instance.

        Raises:
            ValueError: If the dictionary cannot be converted to EntityData.
        """
        ...

@final
class AuthorizeResult:
    def is_allowed(self) -> bool: ...
    def workload(self) -> AuthorizeResultResponse | None: ...
    def person(self) -> AuthorizeResultResponse | None: ...
    def principal(self, principal: str) -> AuthorizeResultResponse | None: ...
    def request_id(self) -> str: ...

@final
class AuthorizeResultResponse:
    decision: Decision
    diagnostics: Diagnostics

@final
class Decision:
    value: str

    def __str__(self) -> str: ...
    def __eq__(self, value: object) -> bool: ...

@final
class Diagnostics:
    reason: set[str]
    errors: List[PolicyEvaluationError]

@final
class PolicyEvaluationError:
    id: str
    error: str

@final
class TokenInput:
    mapping: str
    payload: str

    def __init__(self, mapping: str, payload: str) -> None: ...

@final
class AuthorizeMultiIssuerRequest:
    tokens: List[TokenInput]
    action: str
    resource: EntityData
    context: Dict[str, Any]

    def __init__(
        self,
        tokens: List[TokenInput],
        action: str,
        resource: EntityData,
        context: Dict[str, Any] | None = None,
    ) -> None: ...

@final
class MultiIssuerAuthorizeResult:
    def is_allowed(self) -> bool: ...
    def response(self) -> AuthorizeResultResponse: ...
    def request_id(self) -> str: ...

@final
class DataEntry:
    """
    A data entry in the DataStore with value and metadata.

    Attributes:
        key: The key for this entry.
        data_type: The inferred Cedar type of the value.
        created_at: Timestamp when this entry was created (RFC 3339 format).
        expires_at: Timestamp when this entry expires (RFC 3339 format), or None if no TTL.
        access_count: Number of times this entry has been accessed.
    """

    key: str
    data_type: "CedarType"
    created_at: str
    expires_at: Optional[str]
    access_count: int

    def value(self) -> Any:
        """
        Get the value stored in this entry.

        Returns:
            The value as a Python object (dict, list, str, int, float, bool, etc.).
        """
        ...

    def __str__(self) -> str: ...
    def __repr__(self) -> str: ...

@final
class DataStoreStats:
    """
    Statistics about the DataStore.

    Attributes:
        entry_count: Number of entries currently stored.
        max_entries: Maximum number of entries allowed (0 = unlimited).
        max_entry_size: Maximum size per entry in bytes (0 = unlimited).
        metrics_enabled: Whether metrics tracking is enabled.
        total_size_bytes: Total size of all entries in bytes (approximate, based on JSON serialization).
        avg_entry_size_bytes: Average size per entry in bytes (0 if no entries).
        capacity_usage_percent: Percentage of capacity used (0.0-100.0, based on entry count).
        memory_alert_threshold: Memory usage threshold percentage (from config).
        memory_alert_triggered: Whether memory usage exceeds the alert threshold.
    """

    entry_count: int
    max_entries: int
    max_entry_size: int
    metrics_enabled: bool
    total_size_bytes: int
    avg_entry_size_bytes: int
    capacity_usage_percent: float
    memory_alert_threshold: float
    memory_alert_triggered: bool

@final
class CedarType:
    """
    Represents the type of a Cedar value based on JSON structure.

    Values:
        String: String type
        Long: Long (integer) type
        Bool: Boolean type
        Set: Set (array) type
        Record: Record (object) type
        Entity: Entity reference type
        Ip: IP address extension type (ipaddr)
        Decimal: Decimal extension type
        DateTime: DateTime extension type
        Duration: Duration extension type
    """

    def __str__(self) -> str: ...
    def __repr__(self) -> str: ...
    def __eq__(self, other: "CedarType") -> bool: ...
