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
    jwt_sig_validation: bool
    jwt_status_validation: bool
    jwt_signature_algorithms_supported: List[str]
    at_iss_validation: bool
    at_jti_validation: bool
    at_nbf_validation: bool
    at_exp_validation: bool
    idt_iss_validation: bool
    idt_sub_validation: bool
    idt_exp_validation: bool
    idt_iat_validation: bool
    idt_aud_validation: bool
    userinfo_iss_validation: bool
    userinfo_sub_validation: bool
    userinfo_aud_validation: bool
    userinfo_exp_validation: bool
    id_token_trust_mode: str | None
    lock: bool
    lock_master_configuration_uri: str | None
    dynamic_configuration: bool
    lock_ssa_jwt: str | None
    audit_log_interval: int
    audit_health_interval: int
    audit_health_telemetry_interval: int
    listen_sse: bool

    def __init__(
        self,
        application_name: str,
        policy_store_id: str,
        policy_store_uri: str | None = None,
        log_type: str = 'none',
        log_ttl: int | None = 60,
        user_authz: bool = True,
        workload_authz: bool = True,
        usr_workload_bool_op: str = "AND",
        local_jwks: str | None = None,
        local_policy_store: str | None = None,
        policy_store_local_fn: str | None = None,
        jwt_sig_validation: bool = True,
        jwt_status_validation: bool = False,
        jwt_signature_algorithms_supported: List[str] = ["RS256"],
        at_iss_validation: bool = True,
        at_jti_validation: bool = True,
        at_nbf_validation: bool = True,
        at_exp_validation: bool = True,
        idt_iss_validation: bool = True,
        idt_sub_validation: bool = True,
        idt_exp_validation: bool = True,
        idt_iat_validation: bool = True,
        idt_aud_validation: bool = True,
        userinfo_iss_validation: bool = True,
        userinfo_sub_validation: bool = True,
        userinfo_aud_validation: bool = True,
        userinfo_exp_validation: bool = True,
        id_token_trust_mode: str | None = "none",
        lock: bool = True,
        lock_master_configuration_uri: str | None = None,
        dynamic_configuration: bool = False,
        lock_ssa_jwt: str | None = None,
        audit_log_interval: int = 0,
        audit_health_interval: int = 0,
        audit_health_telemetry_interval: int = 0,
        listen_sse: bool = False,
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
