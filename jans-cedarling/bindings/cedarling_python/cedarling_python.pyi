from typing import Optional, List, final


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
    def __init__(self, json: Optional[str] = None) -> None: ...


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
    # under the hood next fields are optional but
    # we want enforce to provide them
    # and if pass not initialized field to the Cedarling it will raise error

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

    def pop_logs(self) -> List[dict]: ...

    def get_log_by_id(self, id: str) -> Optional[dict]: ...

    def get_log_ids(self) -> List[str]: ...
