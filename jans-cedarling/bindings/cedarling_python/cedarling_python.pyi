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

                Required keys:
                    - "CEDARLING_APPLICATION_NAME" (str): The name of the application.
                    - "CEDARLING_POLICY_STORE_ID" (str): The ID of the policy store.

                Optional keys:
                    - "CEDARLING_POLICY_STORE_URI" (str, optional): URI of the policy store. Defaults to None.
                    - "CEDARLING_LOG_TYPE" (str, optional): Type of logging. Defaults to "memory".
                    - "CEDARLING_LOG_TTL" (int, optional): Log time-to-live in seconds. Defaults to 60.
                    - "CEDARLING_USER_AUTHZ" (str, optional): User authorization status. Defaults to "enabled".
                    - "CEDARLING_WORKLOAD_AUTHZ" (str, optional): Workload authorization status. Defaults to "enabled".
                    - "CEDARLING_USR_WORKLOAD_BOOL_OP" (str, optional): Logical operator for user workload. Defaults to "AND".
                    - "CEDARLING_LOCAL_JWKS" (str, optional): Local JWKS (JSON Web Key Set). Defaults to None.
                    - "CEDARLING_LOCAL_POLICY_STORE" (str, optional): Local policy store configuration. Defaults to None.
                    - "CEDARLING_POLICY_STORE_LOCAL_FN" (str, optional): Local policy store function. Defaults to None.
                    - "CEDARLING_WORKLOAD_MAPPING" (str, optional): The workload's entity type name. Defaults to "Jans::Workload".
                    - "CEDARLING_USER_MAPPING" (str, optional): The workload's entity type name. Defaults to "Jans::User".
                    - "CEDARLING_ROLE_MAPPING" (str, optional): The workload's entity type name. Defaults to "Jans::Role".
                    - "CEDARLING_JWT_SIG_VALIDATION" (str, optional): JWT signature validation status. Defaults to "enabled".
                    - "CEDARLING_JWT_STATUS_VALIDATION" (str, optional): JWT status validation status. Defaults to "enabled".
                    - "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED" (list, optional): Supported JWT signature algorithms. Defaults to an empty list.
                    - "CEDARLING_TOKEN_CONFIGS" (dict, optional): Token Validation Settings. See docs for what this defaults to.
                    - "CEDARLING_ID_TOKEN_TRUST_MODE" (str, optional): Trust mode for ID tokens. Defaults to "strict".
                    - "CEDARLING_LOCK" (str, optional): Lock mechanism status. Defaults to "disabled".
                    - "CEDARLING_LOCK_MASTER_CONFIGURATION_URI" (str, optional): Master configuration URI for locks. Defaults to None.
                    - "CEDARLING_DYNAMIC_CONFIGURATION" (str, optional): Dynamic configuration status. Defaults to "disabled".
                    - "CEDARLING_LOCK_SSA_JWT" (str, optional): JWT for SSA locks. Defaults to None.
                    - "CEDARLING_AUDIT_LOG_INTERVAL" (int, optional): Interval for audit logs in seconds. Defaults to 0.
                    - "CEDARLING_AUDIT_HEALTH_INTERVAL" (int, optional): Interval for health audit in seconds. Defaults to 0.
                    - "CEDARLING_AUDIT_HEALTH_TELEMETRY_INTERVAL" (int, optional): Interval for telemetry health audit in seconds. Defaults to 0.
                    - "CEDARLING_LISTEN_SSE" (str, optional): Server-Sent Events listening status. Defaults to "disabled".

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
    resource: ResourceData
    context: Dict[str, Any]

    def __init__(self,
                 tokens: Dict[str, Any],
                 action: str,
                 resource: ResourceData,
                 context: Dict[str, Any]) -> None: ...


@final
class ResourceData:
    resource_type: str
    id: str
    payload: Dict[str, Any]

    def __init__(self, resource_type: str, id: str, **kwargs) -> None: ...

    @classmethod
    def from_dict(cls, value: Dict[str, Any]) -> "ResourceData": ...


@final
class AuthorizeResult:
    def is_allowed(self) -> bool: ...

    def workload(self) -> AuthorizeResultResponse | None: ...

    def person(self) -> AuthorizeResultResponse | None: ...

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
