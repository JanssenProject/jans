from typing import *


class MemoryLogConfig:

    log_ttl: int

    def __init__(self, log_ttl: int = 60) -> None: ...


class StdOutLogConfig:
    def __init__(self) -> None: ...


class DisabledLoggingConfig:
    def __init__(self) -> None: ...


LogConfigs = MemoryLogConfig | StdOutLogConfig | DisabledLoggingConfig


class PolicyStoreSource:
    def __init__(self, json: Optional[str] = None) -> None: ...


class PolicyStoreConfig:
    def __init__(
        self,
        source: Optional[PolicyStoreSource] = None,
        store_id: Optional[str] = None
    ) -> None: ...


class JwtConfig:
    enabled: bool
    signature_algorithms: List[str]

    def __init__(
        self, enabled: bool,
        signature_algorithms: List[str] = None) -> None: ...


class BootstrapConfig:
    # under the hood next fields are optional but
    # we want enforce to provide them
    # and if pass not initialized field to the Cedarling it will raise error

    application_name: str
    log_config: LogConfigs
    policy_store_config: PolicyStoreConfig
    jwt_config: JwtConfig

    def __init__(
        self,
        application_name: str = None,
        log_config: LogConfigs = None,
        policy_store_config: PolicyStoreConfig = None,
        jwt_config: JwtConfig = None
    ) -> None: ...


class Cedarling:
    def __init__(self, config: BootstrapConfig) -> None: ...

    def pop_logs(self) -> List[dict]: ...

    def get_log_by_id(self, id: str) -> Optional[dict]: ...

    def get_log_ids(self) -> List[str]: ...
