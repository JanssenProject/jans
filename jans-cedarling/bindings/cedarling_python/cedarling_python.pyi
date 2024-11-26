from typing import Optional, List, final, Dict, Any


@final
class MemoryLogConfig:

    log_ttl: int

    def __init__(self, log_ttl: int = 60) -> None: ...


@final
class StdOutLogConfig:
    def __init__(self, /, *args, **kwargs) -> None: ...


@final
class DisabledLoggingConfig:
    def __init__(self, /, *args, **kwargs) -> None: ...


@final
class PolicyStoreSource:
    def __init__(self, json: Optional[str] = None,
                 yaml: Optional[str] = None) -> None: ...


@final
class PolicyStoreConfig:
    source: PolicyStoreSource | None
    store_id: str | None

    def __init__(
        self,
        source: PolicyStoreSource | None = None,
        store_id: str | None = None
    ) -> None: ...


@final
class JwtConfig:
    enabled: bool
    signature_algorithms: List[str] | None

    def __init__(
        self, enabled: bool,
        signature_algorithms: List[str] | None = None) -> None: ...


@final
class BootstrapConfig:
    application_name: str | None
    log_config: MemoryLogConfig | StdOutLogConfig | DisabledLoggingConfig | None
    policy_store_config: PolicyStoreConfig | None
    jwt_config: JwtConfig | None

    def __init__(
        self,
        application_name: str | None = None,
        log_config: MemoryLogConfig | StdOutLogConfig | DisabledLoggingConfig | None = None,
        policy_store_config: PolicyStoreConfig | None = None,
        jwt_config: JwtConfig | None = None
    ) -> None: ...


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
