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
                    - "application_name" (str): The name of the application.
                    - "policy_store_id" (str): The ID of the policy store.

                Optional keys:
                    - "policy_store_uri" (str, optional): URI of the policy store. Defaults to None.
                    - "log_type" (str, optional): Type of logging. Defaults to "memory".
                    - "log_ttl" (int, optional): Log time-to-live in seconds. Defaults to 60.
                    - "user_authz" (str, optional): User authorization status. Defaults to "enabled".
                    - "workload_authz" (str, optional): Workload authorization status. Defaults to "enabled".
                    - "usr_workload_bool_op" (str, optional): Logical operator for user workload. Defaults to "AND".
                    - "local_jwks" (str, optional): Local JWKS (JSON Web Key Set). Defaults to None.
                    - "local_policy_store" (str, optional): Local policy store configuration. Defaults to None.
                    - "policy_store_local_fn" (str, optional): Local policy store function. Defaults to None.
                    - "jwt_sig_validation" (str, optional): JWT signature validation status. Defaults to "enabled".
                    - "jwt_status_validation" (str, optional): JWT status validation status. Defaults to "enabled".
                    - "jwt_signature_algorithms_supported" (list, optional): Supported JWT signature algorithms. Defaults to an empty list.
                    - "at_iss_validation" (str, optional): Access token issuer validation status. Defaults to "enabled".
                    - "at_jti_validation" (str, optional): Access token JWT ID validation status. Defaults to "enabled".
                    - "at_nbf_validation" (str, optional): Access token "not before" validation status. Defaults to "enabled".
                    - "at_exp_validation" (str, optional): Access token expiration validation status. Defaults to "enabled".
                    - "idt_iss_validation" (str, optional): ID token issuer validation status. Defaults to "enabled".
                    - "idt_sub_validation" (str, optional): ID token subject validation status. Defaults to "enabled".
                    - "idt_exp_validation" (str, optional): ID token expiration validation status. Defaults to "enabled".
                    - "idt_iat_validation" (str, optional): ID token issued-at validation status. Defaults to "enabled".
                    - "idt_aud_validation" (str, optional): ID token audience validation status. Defaults to "enabled".
                    - "userinfo_iss_validation" (str, optional): User info issuer validation status. Defaults to "enabled".
                    - "userinfo_sub_validation" (str, optional): User info subject validation status. Defaults to "enabled".
                    - "userinfo_aud_validation" (str, optional): User info audience validation status. Defaults to "enabled".
                    - "userinfo_exp_validation" (str, optional): User info expiration validation status. Defaults to "enabled".
                    - "id_token_trust_mode" (str, optional): Trust mode for ID tokens. Defaults to "strict".
                    - "lock" (str, optional): Lock mechanism status. Defaults to "disabled".
                    - "lock_master_configuration_uri" (str, optional): Master configuration URI for locks. Defaults to None.
                    - "dynamic_configuration" (str, optional): Dynamic configuration status. Defaults to "disabled".
                    - "lock_ssa_jwt" (str, optional): JWT for SSA locks. Defaults to None.
                    - "audit_log_interval" (int, optional): Interval for audit logs in seconds. Defaults to 0.
                    - "audit_health_interval" (int, optional): Interval for health audit in seconds. Defaults to 0.
                    - "audit_health_telemetry_interval" (int, optional): Interval for telemetry health audit in seconds. Defaults to 0.
                    - "listen_sse" (str, optional): Server-Sent Events listening status. Defaults to "disabled".

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

@final
class Cedarling:

    def __init__(self, config: BootstrapConfig) -> None: ...

    def pop_logs(self) -> List[Dict]: ...

    def get_log_by_id(self, id: str) -> Optional[Dict]: ...

    def get_log_ids(self) -> List[str]: ...

    def authorize(self, request: Request) -> AuthorizeResult: ...


@final
class Request:
    access_token: str
    id_token: str
    userinfo_token: str
    action: str
    resource: ResourceData
    context: Dict[str, Any]

    def __init__(self,
                 access_token: str,
                 id_token: str,
                 userinfo_token: str,
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
