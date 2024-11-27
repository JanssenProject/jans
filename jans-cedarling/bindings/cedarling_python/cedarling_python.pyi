from typing import Optional, List, final, Dict, Any

@final
class BootstrapConfig:
    application_name: str
    policy_store_uri: str | None
    policy_store_id: str | None
    log_type: str | None
    log_ttl: int | None
    user_authz: bool | None
    workload_authz: bool | None
    usr_workload_bool_op: str | None
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
        application_name: str,
        policy_store_id: str,
        policy_store_uri: str | None = None,
        log_type: str = 'none',
        log_ttl: int | None = 60,
        user_authz: str = 'enabled',
        workload_authz: str = 'enabled',
        usr_workload_bool_op: str = "AND",
        local_jwks: str | None = None,
        local_policy_store: str | None = None,
        policy_store_local_fn: str | None = None,
        jwt_sig_validation: str = 'enabled',
        jwt_status_validation: str = 'enabled',
        jwt_signature_algorithms_supported: List[str] = ["RS256"],
        at_iss_validation: str = 'enabled',
        at_jti_validation: str = 'enabled',
        at_nbf_validation: str = 'enabled',
        at_exp_validation: str = 'enabled',
        idt_iss_validation: str = 'enabled',
        idt_sub_validation: str = 'enabled',
        idt_exp_validation: str = 'enabled',
        idt_iat_validation: str = 'enabled',
        idt_aud_validation: str = 'enabled',
        userinfo_iss_validation: str = 'enabled',
        userinfo_sub_validation: str = 'enabled',
        userinfo_aud_validation: str = 'enabled',
        userinfo_exp_validation: str = 'enabled',
        id_token_trust_mode: str | None = "none",
        lock: str = 'disabled',
        lock_master_configuration_uri: str | None = None,
        dynamic_configuration: str = 'disabled',
        lock_ssa_jwt: str | None = None,
        audit_log_interval: int = 0,
        audit_health_interval: int = 0,
        audit_health_telemetry_interval: int = 0,
        listen_sse: str = 'disabled',
    ) -> None: ...

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
