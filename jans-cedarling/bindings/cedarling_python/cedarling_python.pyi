from typing import Optional, List, final, Dict, Any


@final
class BootstrapConfig:
    """
    Represents the configuration options for bootstrapping the application.

    Attributes:
        application_name (str): The name of the application.
        policy_store_id (str): The ID of the policy store.
        log_type (str): Type of logging. Defaults to "memory".
        log_ttl (int): Log time-to-live in seconds. Defaults to 60.
        user_authz (str): User authorization status. Defaults to "enabled".
        workload_authz (str): Workload authorization status. Defaults to "enabled".
        usr_workload_bool_op (str): Logical operator for user workload. Defaults to "AND".
        mapping_user (str or None): Mapping name of Cedar Context schema User entity.
        mapping_workload (str or None): Mapping name of cedar schema Workload entity.
        mapping_id_token (str or None): Mapping name of cedar schema id_token entity.
        mapping_access_token (str or None): Mapping name of cedar schema access_token entity.
        mapping_userinfo_token (str or None): Mapping name of cedar schema userinfo_token entity.
        local_jwks (str or None): Local JWKS (JSON Web Key Set). Defaults to None.
        local_policy_store (str or None): Local policy store configuration. Defaults to None.
        policy_store_local_fn (str or None): Local policy store function. Defaults to None.
        jwt_sig_validation (str): JWT signature validation status. Defaults to "enabled".
        jwt_status_validation (str): JWT status validation status. Defaults to "enabled".
        jwt_signature_algorithms_supported (list): Supported JWT signature algorithms. Defaults to an empty list.
        at_iss_validation (str): Access token issuer validation status. Defaults to "enabled".
        at_jti_validation (str): Access token JWT ID validation status. Defaults to "enabled".
        at_nbf_validation (str): Access token "not before" validation status. Defaults to "enabled".
        at_exp_validation (str): Access token expiration validation status. Defaults to "enabled".
        idt_iss_validation (str): ID token issuer validation status. Defaults to "enabled".
        idt_sub_validation (str): ID token subject validation status. Defaults to "enabled".
        idt_exp_validation (str): ID token expiration validation status. Defaults to "enabled".
        idt_iat_validation (str): ID token issued-at validation status. Defaults to "enabled".
        idt_aud_validation (str): ID token audience validation status. Defaults to "enabled".
        userinfo_iss_validation (str): User info issuer validation status. Defaults to "enabled".
        userinfo_sub_validation (str): User info subject validation status. Defaults to "enabled".
        userinfo_aud_validation (str): User info audience validation status. Defaults to "enabled".
        userinfo_exp_validation (str): User info expiration validation status. Defaults to "enabled".
        id_token_trust_mode (str): Trust mode for ID tokens. Defaults to "strict".
        lock (str): Lock mechanism status. Defaults to "disabled".
        lock_master_configuration_uri (str or None): Master configuration URI for locks. Defaults to None.
        dynamic_configuration (str): Dynamic configuration status. Defaults to "disabled".
        lock_ssa_jwt (str or None): JWT for SSA locks. Defaults to None.
        audit_log_interval (int): Interval for audit logs in seconds. Defaults to 0.
        audit_health_interval (int): Interval for health audit in seconds. Defaults to 0.
        audit_health_telemetry_interval (int): Interval for telemetry health audit in seconds. Defaults to 0.
        listen_sse (str): Server-Sent Events listening status. Defaults to "disabled".

    Example Usage:
        bootstrap_config = BootstrapConfig({
            "application_name": "MyApp",
            "policy_store_id": "12345",
            "log_type": "memory",
            "log_ttl": 30,
        })
    """

    application_name: str
    policy_store_uri: str | None
    policy_store_id: str | None
    log_type: str | None
    log_ttl: int | None
    user_authz: bool | None
    workload_authz: bool | None
    usr_workload_bool_op: str | None
    mapping_user: str | None
    mapping_workload: str | None
    mapping_id_token: str | None
    mapping_access_token: str | None
    mapping_userinfo_token: str | None
    local_jwks: str | None
    local_policy_store: str | None
    policy_store_local_fn: str | None
    jwt_sig_validation: str
    jwt_status_validation: str
    jwt_signature_algorithms_supported: List[str]
    at_iss_validation: str
    at_jti_validation: str
    at_nbf_validation: str
    at_exp_validation: str
    idt_iss_validation: str
    idt_sub_validation: str
    idt_exp_validation: str
    idt_iat_validation: str
    idt_aud_validation: str
    userinfo_iss_validation: str
    userinfo_sub_validation: str
    userinfo_aud_validation: str
    userinfo_exp_validation: str
    id_token_trust_mode: str | None
    lock: str
    lock_master_configuration_uri: str | None
    dynamic_configuration: str
    lock_ssa_jwt: str | None
    audit_log_interval: int
    audit_health_interval: int
    audit_health_telemetry_interval: int
    listen_sse: str

    def __init__(
        self,
        options: dict,
    ) -> None: ...
    """
    Initialize a new instance of the class with configuration options.

    Args:
        options (dict): A Python dictionary containing configuration key-value pairs. 
            The following keys are required:
            - "application_name" (str): The name of the application.
            - "policy_store_id" (str): The ID of the policy store.
            
            Optional keys and their default values:
            - "policy_store_uri" (str or None): URI of the policy store. Defaults to None.
            - "log_type" (str): Type of logging. Defaults to "memory".
            - "log_ttl" (int): Log time-to-live in seconds. Defaults to 60.
            - "user_authz" (str): User authorization status. Defaults to "enabled".
            - "workload_authz" (str): Workload authorization status. Defaults to "enabled".
            - "usr_workload_bool_op" (str): Logical operator for user workload. Defaults to "AND".
            - "local_jwks" (str or None): Local JWKS (JSON Web Key Set). Defaults to None.
            - "local_policy_store" (str or None): Local policy store configuration. Defaults to None.
            - "policy_store_local_fn" (str or None): Local policy store function. Defaults to None.
            - "jwt_sig_validation" (str): JWT signature validation status. Defaults to "enabled".
            - "jwt_status_validation" (str): JWT status validation status. Defaults to "enabled".
            - "jwt_signature_algorithms_supported" (list): Supported JWT signature algorithms. Defaults to an empty list.
            - "at_iss_validation" (str): Access token issuer validation status. Defaults to "enabled".
            - "at_jti_validation" (str): Access token JWT ID validation status. Defaults to "enabled".
            - "at_nbf_validation" (str): Access token "not before" validation status. Defaults to "enabled".
            - "at_exp_validation" (str): Access token expiration validation status. Defaults to "enabled".
            - "idt_iss_validation" (str): ID token issuer validation status. Defaults to "enabled".
            - "idt_sub_validation" (str): ID token subject validation status. Defaults to "enabled".
            - "idt_exp_validation" (str): ID token expiration validation status. Defaults to "enabled".
            - "idt_iat_validation" (str): ID token issued-at validation status. Defaults to "enabled".
            - "idt_aud_validation" (str): ID token audience validation status. Defaults to "enabled".
            - "userinfo_iss_validation" (str): User info issuer validation status. Defaults to "enabled".
            - "userinfo_sub_validation" (str): User info subject validation status. Defaults to "enabled".
            - "userinfo_aud_validation" (str): User info audience validation status. Defaults to "enabled".
            - "userinfo_exp_validation" (str): User info expiration validation status. Defaults to "enabled".
            - "id_token_trust_mode" (str): Trust mode for ID tokens. Defaults to "strict".
            - "lock" (str): Lock mechanism status. Defaults to "disabled".
            - "lock_master_configuration_uri" (str or None): Master configuration URI for locks. Defaults to None.
            - "dynamic_configuration" (str): Dynamic configuration status. Defaults to "disabled".
            - "lock_ssa_jwt" (str or None): JWT for SSA locks. Defaults to None.
            - "audit_log_interval" (int): Interval for audit logs in seconds. Defaults to 0.
            - "audit_health_interval" (int): Interval for health audit in seconds. Defaults to 0.
            - "audit_health_telemetry_interval" (int): Interval for telemetry health audit in seconds. Defaults to 0.
            - "listen_sse" (str): Server-Sent Events listening status. Defaults to "disabled".

    Returns:
        An instance of the class.

    Raises:
        KeyError: If any required configuration key is missing.
        ValueError: If a provided value is invalid or extraction fails.
    """

    def disable_all_jwt_validation(self) -> None: ...


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
