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
            For more imformation visit https://docs.jans.io/head/cedarling/cedarling-properties/
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

    def __init__(self, config: BootstrapConfig) -> None: ...

    def authorize(self, request: Request) -> AuthorizeResult: ...

    def authorize_unsigned(
        self, request: RequestUnsigned) -> AuthorizeResult: ...

    def pop_logs(self) -> List[Dict]: ...

    def get_log_by_id(self, id: str) -> Optional[Dict]: ...

    def get_log_ids(self) -> List[str]: ...

    def get_logs_by_tag(self, tag: str) -> List[Dict]: ...

    def get_logs_by_request_id(self, request_id: str) -> List[Dict]: ...

    def get_logs_by_request_id_and_tag(
        self, request_id: str, tag: str) -> List[Dict]: ...


@final
class Request:
    tokens: Dict[str, str]
    action: str
    resource: EntityData
    context: Dict[str, Any]

    def __init__(self,
                 tokens: Dict[str, Any],
                 action: str,
                 resource: EntityData,
                 context: Dict[str, Any]) -> None: ...


@final
class RequestUnsigned:
    principals: List[EntityData]
    action: str
    resource: EntityData
    context: Dict[str, Any]

    def __init__(self,
                 principals: List[EntityData],
                 action: str,
                 resource: EntityData,
                 context: Dict[str, Any]) -> None: ...


@final
class EntityData:
    entity_type: str
    id: str
    payload: Dict[str, Any]

    def __init__(self, entity_type: str, id: str, **kwargs) -> None: ...

    @classmethod
    def from_dict(cls, value: Dict[str, Any]) -> "EntityData": ...


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
